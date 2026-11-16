package com.ferdi_laptop.homeguard

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.ColorUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource

/**
 * Halaman 1: Beranda.
 * Menampilkan kondisi rumah, skenario cepat, dan kontrol perangkat.
 */
class BerandaFragment : Fragment(), HomeRepo.Pendengar {

    private enum class Level { OK, WARN, DANGER, INFO, NEUTRAL }

    private class KontrolUi(
        val card: MaterialCardView,
        val wrap: View,
        val icon: ImageView,
        val status: TextView,
        val saklar: SwitchCompat,
        val teksOn: String,
        val teksOff: String,
        val warnaAktif: Int,
        val warnaLatarAktif: Int
    )

    // Header
    private lateinit var tvSapaan: TextView
    private lateinit var tvNamaRumah: TextView
    private lateinit var tvKoneksi: TextView
    private lateinit var dotKoneksi: View
    private lateinit var tvState: TextView
    private lateinit var tvStateDesc: TextView
    private lateinit var tvRingkas: TextView
    private lateinit var dotRingkas: View

    // Peringatan
    private lateinit var cardAlert: MaterialCardView
    private lateinit var ivAlertIcon: ImageView
    private lateinit var tvAlertTitle: TextView
    private lateinit var tvAlert: TextView
    private lateinit var tvAlertWaktu: TextView

    // Sensor
    private lateinit var tvUpdate: TextView
    private lateinit var tvSuhu: TextView
    private lateinit var tvLembap: TextView
    private lateinit var tvGas: TextView
    private lateinit var tvGasNilai: TextView
    private lateinit var cardGasSensor: MaterialCardView
    private lateinit var ivGas: ImageView
    private lateinit var tvPintu: TextView
    private lateinit var tvPintuKunci: TextView
    private lateinit var cardPintu: MaterialCardView
    private lateinit var ivPintu: ImageView
    private lateinit var tvDaya: TextView
    private lateinit var tvKwh: TextView

    // Kontrol
    private lateinit var switchLampu: SwitchCompat
    private lateinit var switchSoket: SwitchCompat
    private lateinit var switchAc: SwitchCompat
    private lateinit var switchKatup: SwitchCompat
    private val daftarKontrol = mutableListOf<KontrolUi>()

    private var aksiTerakhir = 0L

    // Peta lokasi
    private lateinit var petaView: MapView
    private lateinit var tvPetaStatus: TextView
    private lateinit var tvPetaJarak: TextView
    private lateinit var ivPeta: ImageView
    private var peta: GoogleMap? = null
    private var posisiSaya: Location? = null

    private val mintaIzinLokasi = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { hasil ->
        val diberi = hasil[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                hasil[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (diberi) bacaPosisi() else gambarPeta()
    }

    // ------------------------------------------------------------ siklus
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?
    ): View = inflater.inflate(R.layout.fragment_beranda, container, false)

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)
        ikatTampilan(view)
        aturKolomGrid(view)
        aturKontrol(view)
        perbaruiKontrol()
        tvSapaan.text = sapaan()
        siapkanPeta(view, saved)
    }

    override fun onResume() {
        super.onResume()
        tvNamaRumah.text = Prefs.namaRumah(requireContext())
        HomeRepo.daftar(this)
        onDataBerubah()
        petaView.onResume()
        bacaPosisi()
    }

    override fun onPause() {
        petaView.onPause()
        HomeRepo.lepas(this)
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        petaView.onStart()
    }

    override fun onStop() {
        petaView.onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        petaView.onDestroy()
        peta = null
        super.onDestroyView()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        petaView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        petaView.onSaveInstanceState(outState)
    }

    // ------------------------------------------------------------ pengikatan
    private fun ikatTampilan(v: View) {
        tvSapaan = v.findViewById(R.id.tvSapaan)
        tvNamaRumah = v.findViewById(R.id.tvNamaRumah)
        tvKoneksi = v.findViewById(R.id.tvKoneksi)
        dotKoneksi = v.findViewById(R.id.dotKoneksi)
        tvState = v.findViewById(R.id.tvState)
        tvStateDesc = v.findViewById(R.id.tvStateDesc)
        tvRingkas = v.findViewById(R.id.tvRingkas)
        dotRingkas = v.findViewById(R.id.dotRingkas)

        cardAlert = v.findViewById(R.id.cardAlert)
        ivAlertIcon = v.findViewById(R.id.ivAlertIcon)
        tvAlertTitle = v.findViewById(R.id.tvAlertTitle)
        tvAlert = v.findViewById(R.id.tvAlert)
        tvAlertWaktu = v.findViewById(R.id.tvAlertWaktu)

        tvUpdate = v.findViewById(R.id.tvUpdate)
        tvSuhu = v.findViewById(R.id.tvSuhu)
        tvLembap = v.findViewById(R.id.tvLembap)
        tvGas = v.findViewById(R.id.tvGas)
        tvGasNilai = v.findViewById(R.id.tvGasNilai)
        cardGasSensor = v.findViewById(R.id.cardGasSensor)
        ivGas = v.findViewById(R.id.ivGas)
        tvPintu = v.findViewById(R.id.tvPintu)
        tvPintuKunci = v.findViewById(R.id.tvPintuKunci)
        cardPintu = v.findViewById(R.id.cardPintu)
        ivPintu = v.findViewById(R.id.ivPintu)
        tvDaya = v.findViewById(R.id.tvDaya)
        tvKwh = v.findViewById(R.id.tvKwh)

        switchLampu = ikatKontrol(
            v, R.id.cardLampu, R.id.wrapLampu, R.id.ivLampu, R.id.tvLampuStatus,
            R.id.switchLampu, "Menyala", "Mati", R.color.hg_yellow, R.color.hg_yellow_bg
        )
        switchSoket = ikatKontrol(
            v, R.id.cardSoket, R.id.wrapSoket, R.id.ivSoket, R.id.tvSoketStatus,
            R.id.switchSoket, "Menyala", "Mati", R.color.hg_violet, R.color.hg_violet_bg
        )
        switchAc = ikatKontrol(
            v, R.id.cardAc, R.id.wrapAc, R.id.ivAc, R.id.tvAcStatus,
            R.id.switchAc, "Menyala", "Mati", R.color.hg_sky, R.color.hg_sky_bg
        )
        switchKatup = ikatKontrol(
            v, R.id.cardKatup, R.id.wrapKatup, R.id.ivKatup, R.id.tvKatupStatus,
            R.id.switchKatup, "Terbuka", "Tertutup", R.color.hg_teal, R.color.hg_teal_bg
        )

        // Kartu peringatan membuka halaman Riwayat
        cardAlert.setOnClickListener {
            (activity as? MainActivity)?.pindahTab(MainActivity.TAB_RIWAYAT)
        }
        // Status koneksi membuka pengaturan server
        v.findViewById<View>(R.id.chipKoneksi).setOnClickListener {
            startActivity(Intent(requireContext(), ServerActivity::class.java))
        }
    }

    private fun ikatKontrol(
        v: View, cardId: Int, wrapId: Int, iconId: Int, statusId: Int, saklarId: Int,
        teksOn: String, teksOff: String, warnaAktif: Int, warnaLatarAktif: Int
    ): SwitchCompat {
        val saklar = v.findViewById<SwitchCompat>(saklarId)
        val card = v.findViewById<MaterialCardView>(cardId)
        card.setOnClickListener { saklar.performClick() }
        daftarKontrol.add(
            KontrolUi(
                card, v.findViewById(wrapId), v.findViewById(iconId), v.findViewById(statusId),
                saklar, teksOn, teksOff, warnaAktif, warnaLatarAktif
            )
        )
        return saklar
    }

    /** Dua kolom di HP, tiga kolom di layar lebar. */
    private fun aturKolomGrid(v: View) {
        val kolom = if (resources.configuration.screenWidthDp >= 600) 3 else 2
        v.findViewById<GridLayout>(R.id.gridSensor).columnCount = kolom
        v.findViewById<GridLayout>(R.id.gridKontrol).columnCount = kolom
    }

    // ------------------------------------------------------------ kontrol
    private fun aturKontrol(v: View) {
        pasangSaklar(switchLampu, "ruangtamu/lampu", "nyala", "mati")
        pasangSaklar(switchSoket, "ruangtamu/soket", "nyala", "mati")
        pasangSaklar(switchAc, "ruangtamu/ac", "nyala", "mati")

        // Katup gas selalu minta konfirmasi sebelum dibuka
        switchKatup.setOnClickListener {
            if (switchKatup.isChecked) {
                switchKatup.isChecked = false
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Buka katup gas?")
                    .setMessage(
                        "Pastikan tidak ada kebocoran dan tidak ada kompor yang tertinggal menyala."
                    )
                    .setPositiveButton("Buka") { _, _ ->
                        switchKatup.isChecked = true
                        kirim("cmd/gas", "open")
                        perbaruiKontrol()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            } else {
                kirim("cmd/gas", "close")
                perbaruiKontrol()
            }
        }

        v.findViewById<MaterialButton>(R.id.btnSemuaMati).setOnClickListener {
            jalankanSkenario("Semua Mati", lampu = false, soket = false, ac = false)
        }
        v.findViewById<MaterialButton>(R.id.btnModeTidur).setOnClickListener {
            jalankanSkenario("Mode Tidur", lampu = false, soket = false, ac = true)
        }
        v.findViewById<MaterialButton>(R.id.btnModePergi).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Aktifkan Mode Pergi?")
                .setMessage("Lampu, stop kontak, dan AC akan dimatikan, lalu rumah diberi tahu bahwa Anda pergi.")
                .setPositiveButton("Aktifkan") { _, _ ->
                    jalankanSkenario("Mode Pergi", lampu = false, soket = false, ac = false)
                    kirim("user/status", "away")
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun pasangSaklar(saklar: SwitchCompat, subTopic: String, nilaiOn: String, nilaiOff: String) {
        saklar.setOnClickListener {
            kirim(subTopic, if (saklar.isChecked) nilaiOn else nilaiOff)
            perbaruiKontrol()
        }
    }

    private fun jalankanSkenario(nama: String, lampu: Boolean, soket: Boolean, ac: Boolean) {
        kirim("ruangtamu/lampu", if (lampu) "nyala" else "mati")
        kirim("ruangtamu/soket", if (soket) "nyala" else "mati")
        kirim("ruangtamu/ac", if (ac) "nyala" else "mati")

        switchLampu.isChecked = lampu
        switchSoket.isChecked = soket
        switchAc.isChecked = ac
        perbaruiKontrol()

        Toast.makeText(requireContext(), "$nama dijalankan", Toast.LENGTH_SHORT).show()
    }

    private fun kirim(subTopic: String, pesan: String) {
        aksiTerakhir = SystemClock.elapsedRealtime()
        HomeRepo.kirim(subTopic, pesan)
    }

    /** Kartu kontrol berwarna saat menyala, putih polos saat mati. */
    private fun perbaruiKontrol() {
        daftarKontrol.forEach { k ->
            val aktif = k.saklar.isChecked
            k.status.text = if (aktif) k.teksOn else k.teksOff
            if (aktif) {
                k.card.setCardBackgroundColor(warna(k.warnaLatarAktif))
                k.card.strokeColor = warna(k.warnaAktif)
                k.card.strokeWidth = dp(2)
                k.wrap.setBackgroundResource(R.drawable.bg_icon_solid)
                k.wrap.backgroundTintList = ColorStateList.valueOf(warna(k.warnaAktif))
                k.icon.imageTintList = ColorStateList.valueOf(warna(R.color.hg_surface))
                k.status.setTextColor(warna(k.warnaAktif))
            } else {
                k.card.setCardBackgroundColor(warna(R.color.hg_surface))
                k.card.strokeColor = warna(R.color.hg_stroke)
                k.card.strokeWidth = dp(1)
                k.wrap.setBackgroundResource(R.drawable.bg_icon_neutral)
                k.wrap.backgroundTintList = null
                k.icon.imageTintList = ColorStateList.valueOf(warna(R.color.hg_text_muted))
                k.status.setTextColor(warna(R.color.hg_text_muted))
            }
        }
    }

    // ------------------------------------------------------------ warna
    private fun warna(id: Int) = ContextCompat.getColor(requireContext(), id)

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private fun warnaIsi(l: Level): Int = when (l) {
        Level.OK -> R.color.hg_ok
        Level.WARN -> R.color.hg_warn
        Level.DANGER -> R.color.hg_danger
        Level.INFO -> R.color.hg_info
        Level.NEUTRAL -> R.color.hg_text_muted
    }

    private fun warnaLatar(l: Level): Int = when (l) {
        Level.OK -> R.color.hg_ok_bg
        Level.WARN -> R.color.hg_warn_bg
        Level.DANGER -> R.color.hg_danger_bg
        Level.INFO -> R.color.hg_info_bg
        Level.NEUTRAL -> R.color.hg_neutral_bg
    }

    private fun terapkanKartu(card: MaterialCardView, icon: ImageView, l: Level) {
        card.setCardBackgroundColor(warna(warnaLatar(l)))
        icon.imageTintList = ColorStateList.valueOf(warna(warnaIsi(l)))
    }

    // ------------------------------------------------------------ data masuk
    override fun onDataBerubah() {
        if (!isAdded) return
        tampilkanKoneksi()
        tampilkanState()
        tampilkanPeringatan()
        tampilkanSensor()
        tampilkanRingkasan()
    }

    private fun tampilkanKoneksi() {
        val tersambung = HomeRepo.terhubungBroker
        tvKoneksi.text = if (tersambung) "Terhubung" else "Terputus"
        dotKoneksi.backgroundTintList = ColorStateList.valueOf(
            warna(if (tersambung) R.color.hg_dot_ok else R.color.hg_dot_off)
        )
    }

    private fun tampilkanState() {
        tvState.text = when (HomeRepo.stateRumah) {
            "HOME" -> "Anda di Rumah"
            "PENDING_AWAY" -> "Bersiap Pergi"
            "AWAY" -> "Mode Pergi"
            else -> "Menunggu data"
        }
        tvStateDesc.text = when (HomeRepo.stateRumah) {
            "HOME" -> "Penghuni berada di rumah"
            "PENDING_AWAY" -> "Memeriksa kondisi rumah sebelum Mode Pergi..."
            "AWAY" -> "Rumah kosong, listrik yang tidak penting dimatikan"
            else -> "Status dari rumah Anda"
        }
    }

    private fun tampilkanRingkasan() {
        val (teks, titik) = HomeRepo.ringkasan(requireContext())
        tvRingkas.text = teks
        dotRingkas.backgroundTintList = ColorStateList.valueOf(warna(titik))
    }

    private fun levelPeringatan(tipe: String): Level = when (tipe) {
        "GAS", "KOMPOR", "SECURITY" -> Level.DANGER
        "ANOMALI", "OFFLINE" -> Level.WARN
        "PRE_ARRIVAL" -> Level.INFO
        else -> Level.OK
    }

    private fun tampilkanPeringatan() {
        val p = HomeRepo.peringatanTerakhir ?: return
        val level = levelPeringatan(p.tipe)

        tvAlertTitle.text = TeksPeringatan.judul(p.tipe)
        tvAlert.text = p.pesan
        tvAlertWaktu.text = "Pukul ${jam(p.waktu)}"
        tvAlertWaktu.visibility = View.VISIBLE

        cardAlert.setCardBackgroundColor(warna(warnaLatar(level)))
        ivAlertIcon.setImageResource(TeksPeringatan.ikon(p.tipe))
        ivAlertIcon.imageTintList = ColorStateList.valueOf(warna(warnaIsi(level)))
    }

    private fun tampilkanSensor() {
        val s = HomeRepo.sensor ?: return

        tvSuhu.text = angka(s.suhu, "%.1f °C")
        tvLembap.text = angka(s.lembap, "%.0f %%")
        tvDaya.text = angka(s.daya, "%.0f W")
        tvKwh.text = angka(s.kwh, "%.3f kWh")
        tvUpdate.text = if (HomeRepo.rumahOffline) "Data terhenti ${jam(s.waktu)}"
        else "Diperbarui ${jam(s.waktu)}"

        // Gas: status yang mudah dipahami, angka sensor sebagai keterangan
        val bahaya = s.gas > Prefs.ambangGas(requireContext())
        tvGas.text = if (bahaya) "Bahaya" else "Aman"
        tvGas.setTextColor(warna(if (bahaya) R.color.hg_danger else R.color.hg_ok))
        tvGasNilai.text = "Nilai sensor: ${s.gas}"
        terapkanKartu(cardGasSensor, ivGas, if (bahaya) Level.DANGER else Level.OK)

        // Pintu: aman bila tertutup dan terkunci
        val aman = s.pintuTutup && s.terkunci
        tvPintu.text = if (s.pintuTutup) "Tertutup" else "Terbuka"
        tvPintuKunci.text = if (s.terkunci) "Terkunci" else "Tidak terkunci"
        tvPintuKunci.setTextColor(warna(if (aman) R.color.hg_ok else R.color.hg_warn))
        terapkanKartu(cardPintu, ivPintu, if (aman) Level.OK else Level.WARN)

        // Jangan menimpa saklar yang baru saja disentuh pengguna
        if (SystemClock.elapsedRealtime() - aksiTerakhir > 6000) {
            switchLampu.isChecked = s.lampu
            switchSoket.isChecked = s.soket
            switchAc.isChecked = s.ac
            switchKatup.isChecked = s.katup
            perbaruiKontrol()
        }
    }

    // ------------------------------------------------------------ peta
    private fun siapkanPeta(v: View, saved: Bundle?) {
        petaView = v.findViewById(R.id.petaRumah)
        tvPetaStatus = v.findViewById(R.id.tvPetaStatus)
        tvPetaJarak = v.findViewById(R.id.tvPetaJarak)
        ivPeta = v.findViewById(R.id.ivPeta)

        petaView.onCreate(saved)
        petaView.getMapAsync { gmap ->
            peta = gmap
            gmap.uiSettings.isMapToolbarEnabled = false
            gambarPeta()
        }

        v.findViewById<View>(R.id.cardPeta).setOnClickListener {
            if (!adaIzinLokasi()) {
                mintaIzinLokasi.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                startActivity(Intent(requireContext(), RumahActivity::class.java))
            }
        }
    }

    private fun adaIzinLokasi(): Boolean {
        val ctx = context ?: return false
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun bacaPosisi() {
        if (!adaIzinLokasi()) {
            gambarPeta()
            return
        }
        LocationServices.getFusedLocationProviderClient(requireContext())
            .getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token
            )
            .addOnSuccessListener { lokasi ->
                if (!isAdded) return@addOnSuccessListener
                if (lokasi != null) posisiSaya = lokasi
                gambarPeta()
            }
            .addOnFailureListener { if (isAdded) gambarPeta() }
    }

    /**
     * Menggambar rumah, dua lingkaran radius, dan posisi pengguna,
     * lalu menyimpulkan apakah pengguna sedang di rumah, dekat, atau jauh.
     */
    private fun gambarPeta() {
        val g = peta ?: return
        val ctx = context ?: return
        g.clear()

        val rumah = GeofenceHelper.ambilRumah(ctx)
        if (rumah == null) {
            tvPetaStatus.text = "Lokasi rumah belum diatur"
            tvPetaJarak.text = "Ketuk kartu ini untuk mengaturnya"
            tvPetaStatus.setTextColor(warna(R.color.hg_text))
            ivPeta.imageTintList = ColorStateList.valueOf(warna(R.color.hg_text_muted))
            return
        }

        val titikRumah = LatLng(rumah.first, rumah.second)
        val rRumah = Prefs.radiusRumah(ctx).toDouble()
        val rDekat = Prefs.radiusDekat(ctx).toDouble()
        val biruTua = warna(R.color.hg_primary)
        val biruMuda = warna(R.color.hg_info)

        g.addMarker(MarkerOptions().position(titikRumah).title(Prefs.namaRumah(ctx)))
        g.addCircle(
            CircleOptions().center(titikRumah).radius(rDekat).strokeWidth(3f)
                .strokeColor(ColorUtils.setAlphaComponent(biruMuda, 140))
                .fillColor(ColorUtils.setAlphaComponent(biruMuda, 28))
        )
        g.addCircle(
            CircleOptions().center(titikRumah).radius(rRumah).strokeWidth(3f)
                .strokeColor(biruTua)
                .fillColor(ColorUtils.setAlphaComponent(biruTua, 45))
        )

        val saya = posisiSaya
        if (saya == null) {
            tvPetaStatus.text = if (adaIzinLokasi()) "Mencari posisi Anda" else "Izinkan akses lokasi"
            tvPetaJarak.text =
                if (adaIzinLokasi()) "Pastikan GPS menyala" else "Ketuk kartu ini untuk memberi izin"
            tvPetaStatus.setTextColor(warna(R.color.hg_text))
            ivPeta.imageTintList = ColorStateList.valueOf(warna(R.color.hg_text_muted))
            g.moveCamera(CameraUpdateFactory.newLatLngZoom(titikRumah, zoomUntuk(rDekat)))
            return
        }

        val titikSaya = LatLng(saya.latitude, saya.longitude)
        g.addMarker(
            MarkerOptions().position(titikSaya).title("Posisi Anda")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )

        val hasil = FloatArray(1)
        Location.distanceBetween(saya.latitude, saya.longitude, rumah.first, rumah.second, hasil)
        val jarak = hasil[0].toDouble()

        val (teks, warnaStatus) = when {
            jarak <= rRumah -> "Anda berada di rumah" to R.color.hg_ok
            jarak <= rDekat -> "Anda dekat dengan rumah" to R.color.hg_warn
            else -> "Anda jauh dari rumah" to R.color.hg_text_muted
        }
        tvPetaStatus.text = teks
        tvPetaStatus.setTextColor(warna(warnaStatus))
        ivPeta.imageTintList = ColorStateList.valueOf(warna(warnaStatus))
        tvPetaJarak.text = "Jarak ${jarakRamah(jarak)} dari rumah"

        // Perbesaran diatur agar rumah dan posisi Anda sama-sama terlihat
        petaView.post {
            try {
                val batas = LatLngBounds.builder().include(titikRumah).include(titikSaya).build()
                g.moveCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        batas, petaView.width, petaView.height, dp(48)
                    )
                )
            } catch (e: Exception) {
                g.moveCamera(CameraUpdateFactory.newLatLngZoom(titikRumah, zoomUntuk(rDekat)))
            }
        }
    }

    private fun jarakRamah(meter: Double): String =
        if (meter < 1000) String.format(Locale.US, "%.0f m", meter)
        else String.format(Locale.US, "%.1f km", meter / 1000.0)

    private fun zoomUntuk(radiusMeter: Double): Float {
        val r = radiusMeter.coerceAtLeast(50.0)
        return (16.0 - kotlin.math.ln(r / 150.0) / kotlin.math.ln(2.0)).toFloat().coerceIn(10f, 17f)
    }

    // ------------------------------------------------------------ bantu
    private fun angka(nilai: Double, format: String): String =
        if (nilai.isNaN()) "--" else String.format(Locale.US, format, nilai)

    private fun jam(waktu: Long): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(waktu))

    private fun sapaan(): String {
        val jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            jam < 11 -> "Selamat pagi"
            jam < 15 -> "Selamat siang"
            jam < 18 -> "Selamat sore"
            else -> "Selamat malam"
        }
    }
}