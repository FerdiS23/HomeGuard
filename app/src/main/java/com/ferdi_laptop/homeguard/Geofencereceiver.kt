package com.ferdi_laptop.homeguard   // sesuaikan dengan package project Anda

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/**
 * Dipanggil Google Play Services saat HP masuk/keluar area geofence,
 * walaupun aplikasi sedang tidak dibuka.
 */
class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val ids = event.triggeringGeofences?.map { it.requestId } ?: return

        // Beri waktu beberapa detik agar koneksi MQTT sempat terkirim sebelum proses dihentikan
        val pending = goAsync()
        HomeRepo.siapkan(context)

        when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                if (GeofenceHelper.ID_RUMAH in ids) {
                    MqttManager.publish("user/status", "away")       // penghuni keluar rumah
                }
            }
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                if (GeofenceHelper.ID_DEKAT in ids) {
                    MqttManager.publish("user/status", "near")       // mendekati rumah
                    HomeNotifier.tampilkanDekat(context)             // tanya AC / lampu
                }
                if (GeofenceHelper.ID_RUMAH in ids) {
                    MqttManager.publish("user/status", "home")       // sudah di rumah
                }
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({ pending.finish() }, 8000)
    }
}