package com.ferdi_laptop.homeguard

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

/**
 * Semua notifikasi aplikasi:
 *  - "Anda hampir sampai di rumah" dengan tombol AC dan lampu
 *  - Peringatan gas, kompor, keamanan, dan perangkat rumah tidak mengirim data
 *  - Notifikasi tetap milik layanan latar
 */
object HomeNotifier {

    private const val CHANNEL_DEKAT = "homeguard_dekat"
    private const val CHANNEL_PERINGATAN = "homeguard_peringatan"
    const val CHANNEL_LAYANAN = "homeguard_layanan"

    const val NOTIF_ID = 1001
    const val NOTIF_LAYANAN_ID = 1002
    private const val NOTIF_PERINGATAN_ID = 1100

    const val ACTION_AC = "com.ferdi_laptop.homeguard.ACTION_AC"
    const val ACTION_LAMPU = "com.ferdi_laptop.homeguard.ACTION_LAMPU"

    // Batas waktu "malam" (jam 24 jam). Ubah sesuai kebutuhan.
    private const val JAM_MALAM_MULAI = 18   // mulai 18:00
    private const val JAM_MALAM_SELESAI = 5  // sampai 04:59

    fun sudahMalam(): Boolean {
        val jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return jam >= JAM_MALAM_MULAI || jam < JAM_MALAM_SELESAI
    }

    // ------------------------------------------------------------ dasar
    private fun buatSemuaChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = ctx.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DEKAT, "Dekat rumah", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Tawaran menyalakan AC atau lampu saat hampir tiba" }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PERINGATAN, "Peringatan rumah", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Gas, kompor, keamanan, dan perangkat yang tidak mengirim data" }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_LAYANAN, "Pemantauan berjalan", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Pemberitahuan tetap selama rumah dipantau" }
        )
    }

    private fun bolehTampil(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    private fun bukaAplikasi(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0, Intent(ctx, PembukaActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun aksi(ctx: Context, action: String, kode: Int): PendingIntent {
        val intent = Intent(ctx, NotifActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            ctx, kode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    // ------------------------------------------------------------ dekat rumah
    @SuppressLint("MissingPermission")
    fun tampilkanDekat(ctx: Context) {
        if (!bolehTampil(ctx)) return
        buatSemuaChannel(ctx)

        val malam = sudahMalam()
        val teks = if (malam) "Nyalakan AC atau lampu sebelum tiba?" else "Nyalakan AC sebelum tiba?"

        val builder = NotificationCompat.Builder(ctx, CHANNEL_DEKAT)
            .setSmallIcon(R.drawable.ic_nav_beranda)
            .setContentTitle("Anda hampir sampai di rumah")
            .setContentText(teks)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(bukaAplikasi(ctx))
            .addAction(0, "Nyalakan AC", aksi(ctx, ACTION_AC, 1))

        if (malam) builder.addAction(0, "Nyalakan Lampu", aksi(ctx, ACTION_LAMPU, 2))

        NotificationManagerCompat.from(ctx).notify(NOTIF_ID, builder.build())
    }

    // ------------------------------------------------------------ peringatan
    private fun judul(tipe: String): String = when (tipe) {
        "GAS" -> "Peringatan Gas"
        "KOMPOR" -> "Peringatan Kompor"
        "SECURITY" -> "Peringatan Keamanan"
        "ANOMALI" -> "Aktivitas Tidak Biasa"
        "PRE_ARRIVAL" -> "Anda Hampir Tiba"
        "OFFLINE" -> "Rumah Tidak Terhubung"
        else -> tipe.ifBlank { "Informasi" }
    }

    private fun ikon(tipe: String): Int = when (tipe) {
        "SECURITY" -> R.drawable.ic_shield
        "PRE_ARRIVAL" -> R.drawable.ic_near
        "OFFLINE" -> R.drawable.ic_cloud
        "GAS", "KOMPOR", "ANOMALI" -> R.drawable.ic_warning
        else -> R.drawable.ic_info
    }

    private fun mendesak(tipe: String): Boolean = tipe in setOf("GAS", "KOMPOR", "SECURITY")

    /** Peringatan dari rumah. Inilah yang tetap masuk walaupun aplikasi sedang ditutup. */
    @SuppressLint("MissingPermission")
    fun tampilkanPeringatan(ctx: Context, tipe: String, pesan: String) {
        if (!bolehTampil(ctx)) return
        buatSemuaChannel(ctx)

        val builder = NotificationCompat.Builder(ctx, CHANNEL_PERINGATAN)
            .setSmallIcon(ikon(tipe))
            .setContentTitle(judul(tipe))
            .setContentText(pesan)
            .setStyle(NotificationCompat.BigTextStyle().bigText(pesan))
            .setPriority(if (mendesak(tipe)) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (mendesak(tipe)) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(bukaAplikasi(ctx))

        // Peringatan berbahaya tidak boleh menimpa satu sama lain
        val id = if (mendesak(tipe)) NOTIF_PERINGATAN_ID + (System.currentTimeMillis() % 1000).toInt()
        else NOTIF_PERINGATAN_ID

        NotificationManagerCompat.from(ctx).notify(id, builder.build())
    }

    // ------------------------------------------------------------ layanan latar
    /** Notifikasi tetap yang menandai bahwa rumah sedang dipantau. */
    fun notifikasiLayanan(ctx: Context, isi: String): android.app.Notification {
        buatSemuaChannel(ctx)
        return NotificationCompat.Builder(ctx, CHANNEL_LAYANAN)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("HomeGuard memantau rumah")
            .setContentText(isi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(bukaAplikasi(ctx))
            .build()
    }
}
