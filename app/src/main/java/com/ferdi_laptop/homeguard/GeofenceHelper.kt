package com.ferdi_laptop.homeguard   // sesuaikan dengan package project Anda

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * Dua area geofence mengelilingi rumah:
 *  - "dekat"  (radius besar) : masuk -> status "near" + notifikasi tanya AC/lampu
 *  - "rumah"  (radius kecil) : keluar -> "away", masuk -> "home"
 */
object GeofenceHelper {

    const val ID_RUMAH = "rumah"
    const val ID_DEKAT = "dekat"

    /** Radius diambil dari halaman Pengaturan Rumah, bukan ditanam di kode. */
    fun radiusRumah(ctx: Context): Float = Prefs.radiusRumah(ctx).toFloat()

    fun radiusDekat(ctx: Context): Float = Prefs.radiusDekat(ctx).toFloat()

    private const val PREF = "homeguard_prefs"

    fun simpanRumah(ctx: Context, lat: Double, lng: Double) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("lat", lat.toString())
            .putString("lng", lng.toString())
            .apply()
    }

    fun ambilRumah(ctx: Context): Pair<Double, Double>? {
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val lat = p.getString("lat", null)?.toDoubleOrNull() ?: return null
        val lng = p.getString("lng", null)?.toDoubleOrNull() ?: return null
        return Pair(lat, lng)
    }

    fun adaIzinLokasi(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    private fun pendingIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, GeofenceReceiver::class.java)
        // Geofence butuh PendingIntent MUTABLE di Android 12+
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        return PendingIntent.getBroadcast(ctx, 0, intent, flags)
    }

    @SuppressLint("MissingPermission")
    fun daftarkan(ctx: Context, hasil: (berhasil: Boolean, pesan: String) -> Unit) {
        val rumah = ambilRumah(ctx)
        if (rumah == null) { hasil(false, "Lokasi rumah belum disimpan"); return }
        if (!adaIzinLokasi(ctx)) { hasil(false, "Izin lokasi belum diberikan"); return }

        val (lat, lng) = rumah

        val geoRumah = Geofence.Builder()
            .setRequestId(ID_RUMAH)
            .setCircularRegion(lat, lng, radiusRumah(ctx))
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geoDekat = Geofence.Builder()
            .setRequestId(ID_DEKAT)
            .setCircularRegion(lat, lng, radiusDekat(ctx))
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0)   // jangan memicu apa pun hanya karena sudah berada di dalam area
            .addGeofences(listOf(geoRumah, geoDekat))
            .build()

        val client = LocationServices.getGeofencingClient(ctx)
        val pi = pendingIntent(ctx)

        // hapus geofence lama dulu agar tidak dobel
        client.removeGeofences(pi).addOnCompleteListener {
            client.addGeofences(request, pi)
                .addOnSuccessListener {
                    hasil(true, "Aktif: notifikasi saat masuk radius ${radiusDekat(ctx).toInt()} m dari rumah")
                }
                .addOnFailureListener { e ->
                    hasil(false, "Gagal mengaktifkan geofence: ${e.message}")
                }
        }
    }
}