package com.ferdi_laptop.homeguard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Riwayat peringatan yang disimpan di HP.
 * Dipakai halaman Riwayat agar peringatan lama tidak hilang saat tertimpa yang baru.
 */
object RiwayatStore {

    private const val NAMA = "homeguard_riwayat"
    private const val KUNCI = "daftar"
    private const val BATAS = 200   // simpan paling banyak 200 catatan terakhir

    data class Catatan(val waktu: Long, val tipe: String, val pesan: String)

    private fun p(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(NAMA, Context.MODE_PRIVATE)

    fun ambil(ctx: Context): List<Catatan> {
        val teks = p(ctx).getString(KUNCI, null) ?: return emptyList()
        return try {
            val arr = JSONArray(teks)
            val hasil = ArrayList<Catatan>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                hasil.add(Catatan(o.optLong("w"), o.optString("t"), o.optString("p")))
            }
            hasil.sortedByDescending { it.waktu }   // terbaru di atas
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun tambah(ctx: Context, tipe: String, pesan: String) {
        val daftar = ambil(ctx).toMutableList()
        daftar.add(0, Catatan(System.currentTimeMillis(), tipe, pesan))
        simpan(ctx, daftar.take(BATAS))
    }

    fun kosongkan(ctx: Context) {
        p(ctx).edit().remove(KUNCI).apply()
    }

    private fun simpan(ctx: Context, daftar: List<Catatan>) {
        val arr = JSONArray()
        daftar.forEach {
            arr.put(JSONObject().put("w", it.waktu).put("t", it.tipe).put("p", it.pesan))
        }
        p(ctx).edit().putString(KUNCI, arr.toString()).apply()
    }
}
