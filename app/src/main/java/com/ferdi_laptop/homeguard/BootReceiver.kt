package com.ferdi_laptop.homeguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Menyalakan kembali pemantauan setelah HP dinyalakan ulang,
 * sehingga peringatan rumah tetap masuk tanpa perlu membuka aplikasi dulu.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        HomeRepo.siapkan(context)
        HomeService.mulai(context)

        // Daftarkan ulang geofence, karena daftar geofence ikut terhapus saat HP mati
        if (GeofenceHelper.ambilRumah(context) != null && GeofenceHelper.adaIzinLokasi(context)) {
            GeofenceHelper.daftarkan(context) { _, _ -> }
        }
    }
}
