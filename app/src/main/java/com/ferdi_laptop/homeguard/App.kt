package com.ferdi_laptop.homeguard

import android.app.Application

/**
 * Titik awal aplikasi. Menyiapkan koneksi MQTT dan penyimpanan data satu kali saja,
 * supaya halaman mana pun, notifikasi, dan layanan latar memakai data yang sama.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        HomeRepo.siapkan(this)
    }
}
