package com.ferdi_laptop.homeguard

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

/**
 * Halaman 8: Pengaturan Rumah.
 * Nama rumah, titik lokasi, radius geofence, batas gas, tarif listrik,
 * dan tombol uji coba lokasi.
 */
class RumahActivity : AppCompatActivity() {

    private lateinit var etNamaRumah: TextInputEditText
    private lateinit var etAmbangGas: TextInputEditText
    private lateinit var etTarif: TextInputEditText
    private lateinit var etAmbangOffline: TextInputEditText

    private lateinit var sliderDekat: Slider
    private lateinit var sliderRumah: Slider
    private lateinit var tvRadiusDekat: TextView
    private lateinit var tvRadiusRumah: TextView

    private lateinit var tvLokasiRumah: TextView
    private lateinit var tvGeofenceStatus: TextView

    // Tahap 1: izin lokasi biasa
    private val izinAwal = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { hasil ->
        if (hasil[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            mintaIzinBackground()
        } else {
            pesan("Izin lokasi diperlukan agar rumah bisa mendeteksi Anda mendekat")
        }
    }

    // Tahap 2: izin "Izinkan sepanjang waktu", diminta terpisah sesuai aturan Android
    private val izinBackground = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { diberikan ->
        if (!diberikan) {
            pesan("Tanpa \"Izinkan sepanjang waktu\", notifikasi dekat rumah mungkin tidak muncul saat aplikasi ditutup")
        }
        simpanLokasiRumah()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rumah)

        etNamaRumah = findViewById(R.id.etNamaRumah)
        etAmbangGas = findViewById(R.id.etAmbangGas)
        etTarif = findViewById(R.id.etTarif)
        etAmbangOffline = findViewById(R.id.etAmbangOffline)

        sliderDekat = findViewById(R.id.sliderDekat)
        sliderRumah = findViewById(R.id.sliderRumah)
        tvRadiusDekat = findViewById(R.id.tvRadiusDekat)
        tvRadiusRumah = findViewById(R.id.tvRadiusRumah)

        tvLokasiRumah = findViewById(R.id.tvLokasiRumah)
        tvGeofenceStatus = findViewById(R.id.tvGeofenceStatus)

        isiNilaiSekarang()

        sliderDekat.addOnChangeListener { _, nilai, _ ->
            tvRadiusDekat.text = "Radius dekat rumah: ${nilai.toInt()} m"
        }
        sliderRumah.addOnChangeListener { _, nilai, _ ->
            tvRadiusRumah.text = "Radius area rumah: ${nilai.toInt()} m"
        }

        findViewById<View>(R.id.btnKembali).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnSimpanRumah).setOnClickListener { mulaiAturRumah() }
        findViewById<MaterialButton>(R.id.btnSimpanPengaturanRumah).setOnClickListener { simpan() }

        // Uji coba lokasi tanpa berpindah tempat
        findViewById<MaterialButton>(R.id.btnLokRumah).setOnClickListener {
            HomeRepo.kirim("user/status", "home")
            pesan("Status di rumah dikirim")
        }
        findViewById<MaterialButton>(R.id.btnLokPergi).setOnClickListener {
            HomeRepo.kirim("user/status", "away")
            pesan("Status pergi dikirim")
        }
        findViewById<MaterialButton>(R.id.btnLokDekat).setOnClickListener {
            HomeRepo.kirim("user/status", "near")
            HomeNotifier.tampilkanDekat(this)
            pesan("Status dekat dikirim")
        }
    }

    // ------------------------------------------------------------ isi & simpan
    private fun isiNilaiSekarang() {
        etNamaRumah.setText(Prefs.namaRumah(this))
        etAmbangGas.setText(Prefs.ambangGas(this).toString())
        etTarif.setText(String.format(Locale.US, "%.2f", Prefs.tarifKwh(this)))
        etAmbangOffline.setText(Prefs.ambangOffline(this).toString())

        val dekat = paskan(sliderDekat, Prefs.radiusDekat(this).toFloat())
        val rumah = paskan(sliderRumah, Prefs.radiusRumah(this).toFloat())
        sliderDekat.value = dekat
        sliderRumah.value = rumah
        tvRadiusDekat.text = "Radius dekat rumah: ${dekat.toInt()} m"
        tvRadiusRumah.text = "Radius area rumah: ${rumah.toInt()} m"

        tampilkanLokasiRumah()
    }

    /**
     * Slider menolak nilai yang bukan kelipatan stepSize dari valueFrom,
     * jadi nilai tersimpan dibulatkan dulu sebelum dipasang.
     */
    private fun paskan(slider: Slider, nilai: Float): Float {
        val batas = nilai.coerceIn(slider.valueFrom, slider.valueTo)
        val langkah = slider.stepSize
        if (langkah <= 0f) return batas
        val n = Math.round((batas - slider.valueFrom) / langkah)
        return (slider.valueFrom + n * langkah).coerceIn(slider.valueFrom, slider.valueTo)
    }

    private fun simpan() {
        Prefs.setNamaRumah(this, etNamaRumah.text?.toString()?.trim().orEmpty())
        Prefs.setAmbangGas(
            this,
            etAmbangGas.text?.toString()?.trim()?.toIntOrNull() ?: Prefs.AMBANG_GAS_BAWAAN
        )
        Prefs.setTarifKwh(
            this,
            etTarif.text?.toString()?.trim()?.replace(',', '.')?.toFloatOrNull() ?: Prefs.TARIF_BAWAAN
        )
        Prefs.setAmbangOffline(
            this,
            etAmbangOffline.text?.toString()?.trim()?.toIntOrNull() ?: Prefs.AMBANG_OFFLINE_BAWAAN
        )
        Prefs.setRadiusDekat(this, sliderDekat.value.toInt())
        Prefs.setRadiusRumah(this, sliderRumah.value.toInt())

        // Radius berubah, jadi geofence perlu didaftarkan ulang
        if (GeofenceHelper.ambilRumah(this) != null && GeofenceHelper.adaIzinLokasi(this)) {
            GeofenceHelper.daftarkan(this) { _, keterangan -> tvGeofenceStatus.text = keterangan }
        }

        isiNilaiSekarang()
        pesan("Pengaturan rumah disimpan")
    }

    // ------------------------------------------------------------ lokasi
    private fun mulaiAturRumah() {
        val izin = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 33) izin.add(Manifest.permission.POST_NOTIFICATIONS)
        izinAwal.launch(izin.toTypedArray())
    }

    private fun mintaIzinBackground() {
        val perlu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!perlu) {
            simpanLokasiRumah()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Izinkan lokasi sepanjang waktu")
            .setMessage(
                "Agar HomeGuard bisa mendeteksi Anda mendekati rumah saat aplikasi ditutup, " +
                        "pilih \"Izinkan sepanjang waktu\" pada layar berikutnya."
            )
            .setPositiveButton("Lanjut") { _, _ ->
                izinBackground.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            .setNegativeButton("Lewati") { _, _ -> simpanLokasiRumah() }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun simpanLokasiRumah() {
        if (!GeofenceHelper.adaIzinLokasi(this)) return

        LocationServices.getFusedLocationProviderClient(this)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { lokasi ->
                if (lokasi == null) {
                    pesan("Lokasi belum tersedia. Nyalakan GPS lalu coba lagi")
                    return@addOnSuccessListener
                }
                GeofenceHelper.simpanRumah(this, lokasi.latitude, lokasi.longitude)
                tampilkanLokasiRumah()
                GeofenceHelper.daftarkan(this) { _, keterangan -> tvGeofenceStatus.text = keterangan }
            }
            .addOnFailureListener { pesan("Gagal membaca lokasi: ${it.message}") }
    }

    private fun tampilkanLokasiRumah() {
        val rumah = GeofenceHelper.ambilRumah(this)
        tvLokasiRumah.text = if (rumah == null) {
            "Lokasi rumah belum diatur"
        } else {
            String.format(Locale.US, "Koordinat: %.5f, %.5f", rumah.first, rumah.second)
        }
    }

    private fun pesan(teks: String) = Toast.makeText(this, teks, Toast.LENGTH_LONG).show()
}
