package com.ferdi_laptop.homeguard   // sesuaikan dengan package project Anda

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat

/**
 * Menangani tombol "Nyalakan AC" dan "Nyalakan Lampu" pada notifikasi.
 */
class NotifActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        HomeRepo.siapkan(context)

        when (intent.action) {
            HomeNotifier.ACTION_AC -> MqttManager.publish("cmd/ac", "on")        // target default 24 C di ESP32
            HomeNotifier.ACTION_LAMPU -> MqttManager.publish("cmd/lampu", "on")
        }

        NotificationManagerCompat.from(context).cancel(HomeNotifier.NOTIF_ID)

        Handler(Looper.getMainLooper()).postDelayed({ pending.finish() }, 6000)
    }
}