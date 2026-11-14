package com.ferdi_laptop.homeguard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Pintu masuk aplikasi. Tidak menampilkan apa pun sendiri, hanya memilih
 * halaman pertama yang pantas dibuka:
 *
 *   belum pernah pakai -> Panduan
 *   kunci aktif        -> Kunci Aplikasi
 *   selain itu         -> Beranda
 */
class PembukaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HomeRepo.siapkan(this)

        val tujuan = when {
            !Prefs.panduanSelesai(this) -> Intent(this, PanduanActivity::class.java)
            Prefs.kunciAktif(this) -> Intent(this, KunciActivity::class.java).apply {
                putExtra(KunciActivity.EXTRA_MODE, KunciActivity.MODE_BUKA)
            }

            else -> Intent(this, MainActivity::class.java)
        }

        startActivity(tujuan)
        finish()
    }
}
