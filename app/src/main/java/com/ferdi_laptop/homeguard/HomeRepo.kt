package com.ferdi_laptop.homeguard

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject

/**
 * Satu sumber kebenaran untuk seluruh halaman.
 *
 * Semua pesan MQTT masuk ke sini, disimpan, lalu disebarkan ke halaman yang sedang terbuka.
 * Dengan begitu Beranda, Riwayat, dan Statistik melihat data yang sama, dan data tetap
 * tercatat walaupun tidak ada halaman yang sedang menampilkannya.
 */
object HomeRepo {
    // Topik perintah AC. Ubah di sini bila topik di ESP32 berbeda.
    const val TOPIK_AC = "ruangtamu/ac"

    /** Suhu yang diatur untuk AC. Isi pesannya angka bulat derajat Celsius, misalnya 24. */
    const val TOPIK_AC_SUHU = "ruangtamu/suhu_ac"

    /** Kondisi sensor terakhir yang diterima dari rumah. */
    data class Sensor(
        val suhu: Double,
        val lembap: Double,
        val gas: Int,
        val daya: Double,
        val kwh: Double,
        val pintuTutup: Boolean,
        val terkunci: Boolean,
        val lampu: Boolean,
        val soket: Boolean,
        val ac: Boolean,
        val acSuhu: Int,
        val katup: Boolean,
        val waktu: Long
    )

    interface Pendengar {
        fun onDataBerubah()
    }

    private lateinit var app: Context
    private var siap = false
    private val pendengar = mutableListOf<Pendengar>()
    private val handler = Handler(Looper.getMainLooper())

    // ------------------------------------------------------------ keadaan
    var sensor: Sensor? = null
        private set

    var stateRumah: String = ""
        private set

    var terhubungBroker: Boolean = false
        private set

    /** True bila sudah lama tidak ada data sensor masuk, artinya perangkat rumah mungkin mati. */
    var rumahOffline: Boolean = false
        private set

    var peringatanTerakhir: RiwayatStore.Catatan? = null
        private set

    // ------------------------------------------------------------ pemasangan
    fun siapkan(ctx: Context) {
        if (siap) return
        siap = true
        app = ctx.applicationContext

        MqttManager.onStatus = { tersambung ->
            terhubungBroker = tersambung
            beriTahu()
        }
        MqttManager.onMessage = { topic, payload -> terima(topic, payload) }

        MqttManager.connect(app)
        handler.post(pengawas)
    }

    fun daftar(p: Pendengar) {
        if (!pendengar.contains(p)) pendengar.add(p)
    }

    fun lepas(p: Pendengar) {
        pendengar.remove(p)
    }

    private fun beriTahu() {
        // Salin dulu agar aman bila ada halaman yang melepas diri saat dipanggil
        pendengar.toList().forEach { it.onDataBerubah() }
    }

    // ------------------------------------------------------------ perintah
    fun kirim(subTopic: String, pesan: String) {
        MqttManager.publish(subTopic, pesan)
    }

    // ------------------------------------------------------------ pesan masuk
    private fun terima(topic: String, payload: String) {
        when {
            topic.endsWith("/state") -> {
                stateRumah = payload
                beriTahu()
            }

            topic.endsWith("/alert") -> simpanPeringatan(payload)
            topic.endsWith("/sensor") -> simpanSensor(payload)
        }
    }

    private fun simpanPeringatan(payload: String) {
        val tipe: String
        val pesan: String
        try {
            val j = JSONObject(payload)
            tipe = j.optString("type")
            pesan = j.optString("msg")
        } catch (e: Exception) {
            return
        }

        RiwayatStore.tambah(app, tipe, pesan)
        peringatanTerakhir = RiwayatStore.Catatan(System.currentTimeMillis(), tipe, pesan)

        if (Prefs.notifPeringatan(app)) {
            HomeNotifier.tampilkanPeringatan(app, tipe, pesan)
        }
        beriTahu()
    }

    private fun simpanSensor(payload: String) {
        val j = try {
            JSONObject(payload)
        } catch (e: Exception) {
            return
        }

        sensor = Sensor(
            suhu = j.optDouble("suhu", Double.NaN),
            lembap = j.optDouble("lembap", Double.NaN),
            gas = j.optInt("gas", -1),
            daya = j.optDouble("daya", Double.NaN),
            kwh = j.optDouble("kwh", Double.NaN),
            pintuTutup = j.optInt("pintu_tutup") == 1,
            terkunci = j.optInt("terkunci") == 1,
            lampu = j.optInt("lampu") == 1,
            soket = j.optInt("soket") == 1,
            ac = j.optInt("ac") == 1,
            acSuhu = j.optInt("ac_suhu", -1),
            katup = j.optInt("gas_valve") == 1,
            waktu = System.currentTimeMillis()
        )

        if (rumahOffline) {
            rumahOffline = false   // data masuk lagi, rumah kembali terhubung
        }

        // SampelStore memakai JSONObject yang menolak NaN, jadi nilai yang tidak terbaca diganti 0.
        // Dibungkus try agar kegagalan pencatatan statistik tidak pernah menghentikan aplikasi.
        fun aman(x: Double) = if (x.isNaN()) 0.0 else x
        try {
            SampelStore.catat(
                app,
                aman(j.optDouble("suhu", Double.NaN)),
                aman(j.optDouble("lembap", Double.NaN)),
                aman(j.optDouble("daya", Double.NaN)),
                aman(j.optDouble("kwh", Double.NaN))
            )
        } catch (e: Exception) {
            // abaikan
        }
        beriTahu()
    }

    // ------------------------------------------------------------ pengawas data
    /**
     * Memeriksa tiap 20 detik apakah data sensor masih mengalir.
     * Status "Terhubung" hanya berarti HP tersambung ke broker, bukan perangkat rumah masih hidup.
     */
    private val pengawas = object : Runnable {
        override fun run() {
            val s = sensor
            val ambang = Prefs.ambangOffline(app) * 1000L
            val offlineBaru = s != null && System.currentTimeMillis() - s.waktu > ambang

            if (offlineBaru != rumahOffline) {
                rumahOffline = offlineBaru
                if (offlineBaru) {
                    RiwayatStore.tambah(app, "OFFLINE", "Tidak ada data dari perangkat rumah")
                    if (Prefs.notifPeringatan(app)) {
                        HomeNotifier.tampilkanPeringatan(
                            app, "OFFLINE", "Tidak ada data dari perangkat rumah"
                        )
                    }
                }
                beriTahu()
            }
            handler.postDelayed(this, 20_000L)
        }
    }

    /** Keterangan singkat kondisi rumah untuk ditampilkan di header Beranda. */
    fun ringkasan(ctx: Context): Pair<String, Int> {
        val s = sensor
        return when {
            s == null -> "Menunggu data sensor" to R.color.hg_dot_wait
            rumahOffline -> "Perangkat rumah tidak mengirim data" to R.color.hg_dot_off
            s.gas > Prefs.ambangGas(ctx) -> "Kadar gas tinggi, periksa segera" to R.color.hg_dot_off
            stateRumah == "AWAY" && !(s.pintuTutup && s.terkunci) ->
                "Pintu belum tertutup dan terkunci" to R.color.hg_dot_wait

            else -> "Semua kondisi aman" to R.color.hg_dot_ok
        }
    }
}