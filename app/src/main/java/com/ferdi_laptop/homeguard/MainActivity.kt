package com.ferdi_laptop.homeguard

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Rangka aplikasi: satu layar dengan navigasi bawah berisi empat halaman.
 *
 *  1. Beranda    - dashboard dan kontrol perangkat
 *  2. Riwayat    - catatan peringatan
 *  3. Statistik  - grafik dan perkiraan biaya listrik
 *  4. Pengaturan - rumah, server, keamanan
 */
class MainActivity : AppCompatActivity() {

    companion object {
        /** Dipakai halaman lain untuk langsung membuka tab tertentu. */
        const val EXTRA_TAB = "tab_awal"
        const val TAB_BERANDA = 0
        const val TAB_RIWAYAT = 1
        const val TAB_STATISTIK = 2
        const val TAB_PENGATURAN = 3
    }

    private lateinit var nav: BottomNavigationView

    private val mintaIzinNotif = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ditolak pun aplikasi tetap jalan, hanya tanpa notifikasi */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        HomeRepo.siapkan(this)
        HomeService.mulai(this)
        mintaIzinNotifikasi()

        nav = findViewById(R.id.navBawah)
        nav.setOnItemSelectedListener { item ->
            bukaHalaman(
                when (item.itemId) {
                    R.id.navRiwayat -> RiwayatFragment()
                    R.id.navStatistik -> StatistikFragment()
                    R.id.navPengaturan -> PengaturanFragment()
                    else -> BerandaFragment()
                }
            )
            true
        }

        if (savedInstanceState == null) {
            nav.selectedItemId = when (intent.getIntExtra(EXTRA_TAB, TAB_BERANDA)) {
                TAB_RIWAYAT -> R.id.navRiwayat
                TAB_STATISTIK -> R.id.navStatistik
                TAB_PENGATURAN -> R.id.navPengaturan
                else -> R.id.navBeranda
            }
        }
    }

    private fun bukaHalaman(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.wadahHalaman, f)
            .commit()
    }

    /** Pindah tab dari dalam halaman, misalnya dari kartu peringatan di Beranda. */
    fun pindahTab(tab: Int) {
        nav.selectedItemId = when (tab) {
            TAB_RIWAYAT -> R.id.navRiwayat
            TAB_STATISTIK -> R.id.navStatistik
            TAB_PENGATURAN -> R.id.navPengaturan
            else -> R.id.navBeranda
        }
    }

    private fun mintaIzinNotifikasi() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            mintaIzinNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
