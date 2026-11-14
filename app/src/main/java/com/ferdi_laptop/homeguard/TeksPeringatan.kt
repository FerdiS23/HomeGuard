package com.ferdi_laptop.homeguard

/**
 * Penerjemah kode peringatan dari ESP32 menjadi judul, ikon, dan golongan
 * yang dipakai bersama oleh Beranda, Riwayat, dan notifikasi.
 */
object TeksPeringatan {

    fun judul(tipe: String): String = when (tipe) {
        "GAS" -> "Peringatan Gas"
        "KOMPOR" -> "Peringatan Kompor"
        "SECURITY" -> "Peringatan Keamanan"
        "ANOMALI" -> "Aktivitas Tidak Biasa"
        "PRE_ARRIVAL" -> "Anda Hampir Tiba"
        "OFFLINE" -> "Rumah Tidak Terhubung"
        else -> tipe.ifBlank { "Informasi" }
    }

    fun ikon(tipe: String): Int = when (tipe) {
        "SECURITY" -> R.drawable.ic_shield
        "PRE_ARRIVAL" -> R.drawable.ic_near
        "OFFLINE" -> R.drawable.ic_cloud
        "GAS", "KOMPOR", "ANOMALI" -> R.drawable.ic_warning
        else -> R.drawable.ic_check_circle
    }

    /** Golongan untuk saringan di halaman Riwayat. */
    fun golongan(tipe: String): String = when (tipe) {
        "GAS", "KOMPOR" -> "BAHAYA"
        "SECURITY" -> "KEAMANAN"
        else -> "LAINNYA"
    }
}
