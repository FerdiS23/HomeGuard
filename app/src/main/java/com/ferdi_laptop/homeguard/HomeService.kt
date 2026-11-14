package com.ferdi_laptop.homeguard

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat

/**
 * Layanan latar depan yang menjaga koneksi MQTT tetap hidup.
 *
 * Tanpa ini, peringatan gas dan keamanan hanya terlihat saat aplikasi sedang dibuka.
 * Android menampilkan satu notifikasi tetap selama layanan berjalan, dan itu memang diwajibkan.
 */
class HomeService : Service() {

    companion object {
        /** Menyalakan layanan bila pengguna mengizinkannya di halaman Pengaturan. */
        fun mulai(ctx: Context) {
            if (!Prefs.layananAktif(ctx)) return
            try {
                ContextCompat.startForegroundService(ctx, Intent(ctx, HomeService::class.java))
            } catch (e: Exception) {
                // Android membatasi layanan yang dimulai dari latar belakang; abaikan bila ditolak
            }
        }

        fun hentikan(ctx: Context) {
            try {
                ctx.stopService(Intent(ctx, HomeService::class.java))
            } catch (e: Exception) {
                // sudah berhenti
            }
        }
    }

    private val pendengar = object : HomeRepo.Pendengar {
        override fun onDataBerubah() = perbaruiNotifikasi()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        HomeRepo.siapkan(this)
        tampilkanDiDepan()
        HomeRepo.daftar(pendengar)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        HomeRepo.siapkan(this)
        return START_STICKY   // dihidupkan lagi oleh sistem bila sempat dimatikan
    }

    override fun onDestroy() {
        HomeRepo.lepas(pendengar)
        super.onDestroy()
    }

    private fun ringkas(): String {
        val (teks, _) = HomeRepo.ringkasan(this)
        return teks
    }

    private fun tampilkanDiDepan() {
        val notif = HomeNotifier.notifikasiLayanan(this, ringkas())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                HomeNotifier.NOTIF_LAYANAN_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(HomeNotifier.NOTIF_LAYANAN_ID, notif)
        }
    }

    private fun perbaruiNotifikasi() {
        try {
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.notify(HomeNotifier.NOTIF_LAYANAN_ID, HomeNotifier.notifikasiLayanan(this, ringkas()))
        } catch (e: Exception) {
            // izin notifikasi dicabut pengguna
        }
    }
}
