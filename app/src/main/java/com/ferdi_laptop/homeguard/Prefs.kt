package com.ferdi_laptop.homeguard

import android.content.Context
import java.security.MessageDigest

/**
 * Semua pengaturan aplikasi dalam satu tempat.
 * Dipakai oleh halaman Pengaturan, Pengaturan Rumah, dan Kunci Aplikasi.
 */
object Prefs {

    private const val NAMA = "homeguard_prefs"

    // Kunci penyimpanan
    private const val K_PANDUAN_SELESAI = "panduan_selesai"
    private const val K_PIN = "pin_hash"
    private const val K_KUNCI_AKTIF = "kunci_aktif"
    private const val K_BIOMETRIK = "biometrik_aktif"
    private const val K_AMBANG_GAS = "ambang_gas"
    private const val K_RADIUS_RUMAH = "radius_rumah"
    private const val K_RADIUS_DEKAT = "radius_dekat"
    private const val K_NAMA_RUMAH = "nama_rumah"
    private const val K_TARIF = "tarif_kwh"
    private const val K_LAYANAN = "layanan_aktif"
    private const val K_NOTIF_PERINGATAN = "notif_peringatan"
    private const val K_AMBANG_OFFLINE = "ambang_offline_detik"

    // Nilai bawaan
    const val AMBANG_GAS_BAWAAN = 2000
    const val RADIUS_RUMAH_BAWAAN = 150
    const val RADIUS_DEKAT_BAWAAN = 1000
    const val TARIF_BAWAAN = 1444.7f
    const val AMBANG_OFFLINE_BAWAAN = 120

    private fun p(ctx: Context) = ctx.applicationContext.getSharedPreferences(NAMA, Context.MODE_PRIVATE)

    // ---------------------------------------------------------- panduan awal
    fun panduanSelesai(ctx: Context): Boolean = p(ctx).getBoolean(K_PANDUAN_SELESAI, false)

    fun tandaiPanduanSelesai(ctx: Context) {
        p(ctx).edit().putBoolean(K_PANDUAN_SELESAI, true).apply()
    }

    // ---------------------------------------------------------- kunci aplikasi
    /** PIN disimpan sebagai sidik SHA-256, bukan teks asli. */
    private fun acak(pin: String): String {
        val b = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return b.joinToString("") { "%02x".format(it) }
    }

    fun adaPin(ctx: Context): Boolean = !p(ctx).getString(K_PIN, null).isNullOrBlank()

    fun simpanPin(ctx: Context, pin: String) {
        p(ctx).edit().putString(K_PIN, acak(pin)).putBoolean(K_KUNCI_AKTIF, true).apply()
    }

    fun pinCocok(ctx: Context, pin: String): Boolean = p(ctx).getString(K_PIN, null) == acak(pin)

    fun hapusPin(ctx: Context) {
        p(ctx).edit().remove(K_PIN).putBoolean(K_KUNCI_AKTIF, false).putBoolean(K_BIOMETRIK, false).apply()
    }

    fun kunciAktif(ctx: Context): Boolean = p(ctx).getBoolean(K_KUNCI_AKTIF, false) && adaPin(ctx)

    fun biometrikAktif(ctx: Context): Boolean = p(ctx).getBoolean(K_BIOMETRIK, false)

    fun setBiometrik(ctx: Context, aktif: Boolean) {
        p(ctx).edit().putBoolean(K_BIOMETRIK, aktif).apply()
    }

    // ---------------------------------------------------------- rumah & sensor
    fun ambangGas(ctx: Context): Int = p(ctx).getInt(K_AMBANG_GAS, AMBANG_GAS_BAWAAN)

    fun setAmbangGas(ctx: Context, nilai: Int) {
        p(ctx).edit().putInt(K_AMBANG_GAS, nilai.coerceIn(100, 10000)).apply()
    }

    fun radiusRumah(ctx: Context): Int = p(ctx).getInt(K_RADIUS_RUMAH, RADIUS_RUMAH_BAWAAN)

    fun setRadiusRumah(ctx: Context, meter: Int) {
        p(ctx).edit().putInt(K_RADIUS_RUMAH, meter.coerceIn(100, 2000)).apply()
    }

    fun radiusDekat(ctx: Context): Int = p(ctx).getInt(K_RADIUS_DEKAT, RADIUS_DEKAT_BAWAAN)

    fun setRadiusDekat(ctx: Context, meter: Int) {
        p(ctx).edit().putInt(K_RADIUS_DEKAT, meter.coerceIn(200, 10000)).apply()
    }

    fun namaRumah(ctx: Context): String = p(ctx).getString(K_NAMA_RUMAH, "Rumah Saya") ?: "Rumah Saya"

    fun setNamaRumah(ctx: Context, nama: String) {
        p(ctx).edit().putString(K_NAMA_RUMAH, nama.ifBlank { "Rumah Saya" }).apply()
    }

    fun tarifKwh(ctx: Context): Float = p(ctx).getFloat(K_TARIF, TARIF_BAWAAN)

    fun setTarifKwh(ctx: Context, tarif: Float) {
        p(ctx).edit().putFloat(K_TARIF, if (tarif > 0f) tarif else TARIF_BAWAAN).apply()
    }

    // ---------------------------------------------------------- layanan & notifikasi
    fun layananAktif(ctx: Context): Boolean = p(ctx).getBoolean(K_LAYANAN, true)

    fun setLayananAktif(ctx: Context, aktif: Boolean) {
        p(ctx).edit().putBoolean(K_LAYANAN, aktif).apply()
    }

    fun notifPeringatan(ctx: Context): Boolean = p(ctx).getBoolean(K_NOTIF_PERINGATAN, true)

    fun setNotifPeringatan(ctx: Context, aktif: Boolean) {
        p(ctx).edit().putBoolean(K_NOTIF_PERINGATAN, aktif).apply()
    }

    /** Berapa detik tanpa data sampai rumah dianggap tidak terhubung. */
    fun ambangOffline(ctx: Context): Int = p(ctx).getInt(K_AMBANG_OFFLINE, AMBANG_OFFLINE_BAWAAN)

    fun setAmbangOffline(ctx: Context, detik: Int) {
        p(ctx).edit().putInt(K_AMBANG_OFFLINE, detik.coerceIn(30, 1800)).apply()
    }
}
