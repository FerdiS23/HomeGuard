package com.ferdi_laptop.homeguard

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
            HomeNotifier.ACTION_AC -> {
                // Topik dan isi pesan disamakan dengan saklar AC di Beranda
                MqttManager.publish(HomeRepo.TOPIK_AC, "nyala")
                MqttManager.publish(HomeRepo.TOPIK_AC_SUHU, Prefs.acSuhu(context).toString())
            }

            HomeNotifier.ACTION_LAMPU -> MqttManager.publish("ruangtamu/lampu", "nyala")
        }

        NotificationManagerCompat.from(context).cancel(HomeNotifier.NOTIF_ID)

        // Beri waktu beberapa detik agar pesan MQTT sempat terkirim sebelum receiver selesai
        Handler(Looper.getMainLooper()).postDelayed({ pending.finish() }, 6000)
    }
}