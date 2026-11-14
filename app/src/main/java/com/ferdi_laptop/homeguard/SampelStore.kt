package com.ferdi_laptop.homeguard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * Rekaman ringan data sensor untuk halaman Statistik.
 * Satu sampel disimpan paling cepat tiap 3 menit, dan hanya 24 jam terakhir yang dipertahankan.
 */
object SampelStore {

    private const val NAMA = "homeguard_sampel"
    private const val KUNCI = "daftar"
    private const val K_KWH_AWAL = "kwh_awal_hari"
    private const val K_HARI = "hari_tercatat"

    private const val JEDA_MS = 3 * 60 * 1000L
    private const val RENTANG_MS = 24 * 60 * 60 * 1000L

    /** Waktu sampel terakhir, disimpan di memori agar JSON tidak diurai tiap pesan masuk. */
    private var terakhirCatat = 0L

    data class Sampel(val waktu: Long, val suhu: Float, val lembap: Float, val daya: Float)

    private fun p(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(NAMA, Context.MODE_PRIVATE)

    fun ambil(ctx: Context): List<Sampel> {
        val teks = p(ctx).getString(KUNCI, null) ?: return emptyList()
        return try {
            val arr = JSONArray(teks)
            val batas = System.currentTimeMillis() - RENTANG_MS
            val hasil = ArrayList<Sampel>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val w = o.optLong("w")
                if (w >= batas) {
                    hasil.add(
                        Sampel(
                            w,
                            o.optDouble("s", Double.NaN).toFloat(),
                            o.optDouble("l", Double.NaN).toFloat(),
                            o.optDouble("d", Double.NaN).toFloat()
                        )
                    )
                }
            }
            hasil.sortedBy { it.waktu }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Dipanggil tiap data sensor masuk. Menyimpan sendiri hanya bila sudah lewat jedanya. */
    fun catat(ctx: Context, suhu: Double, lembap: Double, daya: Double, kwh: Double) {
        catatKwhHarian(ctx, kwh)

        val sekarang = System.currentTimeMillis()
        if (terakhirCatat != 0L && sekarang - terakhirCatat < JEDA_MS) return

        val daftar = ambil(ctx).toMutableList()
        if (daftar.isNotEmpty() && sekarang - daftar.last().waktu < JEDA_MS) {
            terakhirCatat = daftar.last().waktu
            return
        }
        terakhirCatat = sekarang

        daftar.add(Sampel(sekarang, suhu.toFloat(), lembap.toFloat(), daya.toFloat()))

        val arr = JSONArray()
        daftar.forEach {
            arr.put(
                JSONObject()
                    .put("w", it.waktu)
                    .put("s", it.suhu.toDouble())
                    .put("l", it.lembap.toDouble())
                    .put("d", it.daya.toDouble())
            )
        }
        p(ctx).edit().putString(KUNCI, arr.toString()).apply()
    }

    /**
     * Mencatat nilai kWh pada pergantian hari, supaya pemakaian hari ini
     * bisa dihitung sebagai selisih terhadap angka awal hari.
     */
    private fun catatKwhHarian(ctx: Context, kwh: Double) {
        if (kwh.isNaN()) return
        val hariIni = kodeHari()
        val pref = p(ctx)
        val tersimpan = pref.getInt(K_HARI, -1)
        if (tersimpan != hariIni) {
            pref.edit().putInt(K_HARI, hariIni).putFloat(K_KWH_AWAL, kwh.toFloat()).apply()
        }
    }

    /** Pemakaian energi hari ini dalam kWh, atau NaN bila belum bisa dihitung. */
    fun kwhHariIni(ctx: Context, kwhSekarang: Double): Float {
        if (kwhSekarang.isNaN()) return Float.NaN
        val pref = p(ctx)
        if (pref.getInt(K_HARI, -1) != kodeHari()) return Float.NaN
        val awal = pref.getFloat(K_KWH_AWAL, Float.NaN)
        if (awal.isNaN()) return Float.NaN
        val selisih = kwhSekarang.toFloat() - awal
        return if (selisih < 0f) Float.NaN else selisih   // meteran sempat direset
    }

    fun kosongkan(ctx: Context) {
        terakhirCatat = 0L
        p(ctx).edit().clear().apply()
    }

    private fun kodeHari(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }
}
