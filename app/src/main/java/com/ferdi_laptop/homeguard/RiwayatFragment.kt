package com.ferdi_laptop.homeguard

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Halaman 2: Riwayat.
 * Semua peringatan yang pernah masuk, tersimpan di HP dan bisa disaring per jenis.
 */
class RiwayatFragment : Fragment(), HomeRepo.Pendengar {

    private lateinit var daftar: RecyclerView
    private lateinit var kosong: View
    private lateinit var tvJumlah: TextView
    private lateinit var grupSaring: ChipGroup

    private val adapter = Adapter()
    private var saringan = "SEMUA"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?
    ): View = inflater.inflate(R.layout.fragment_riwayat, container, false)

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)

        daftar = view.findViewById(R.id.daftarRiwayat)
        kosong = view.findViewById(R.id.kosong)
        tvJumlah = view.findViewById(R.id.tvJumlah)
        grupSaring = view.findViewById(R.id.grupSaring)

        daftar.layoutManager = LinearLayoutManager(requireContext())
        daftar.adapter = adapter

        grupSaring.setOnCheckedStateChangeListener { _, dipilih ->
            saringan = when (dipilih.firstOrNull()) {
                R.id.chipBahaya -> "BAHAYA"
                R.id.chipKeamanan -> "KEAMANAN"
                R.id.chipLainnya -> "LAINNYA"
                else -> "SEMUA"
            }
            muat()
        }

        view.findViewById<MaterialButton>(R.id.btnHapusRiwayat).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Hapus semua riwayat?")
                .setMessage("Catatan peringatan akan dihapus dari HP ini dan tidak bisa dikembalikan.")
                .setPositiveButton("Hapus") { _, _ ->
                    RiwayatStore.kosongkan(requireContext())
                    muat()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        HomeRepo.daftar(this)
        muat()
    }

    override fun onPause() {
        HomeRepo.lepas(this)
        super.onPause()
    }

    /** Dipanggil saat ada peringatan baru masuk selagi halaman ini terbuka. */
    override fun onDataBerubah() {
        if (isAdded) muat()
    }

    private fun muat() {
        val semua = RiwayatStore.ambil(requireContext())
        val tampil =
            if (saringan == "SEMUA") semua
            else semua.filter { TeksPeringatan.golongan(it.tipe) == saringan }

        adapter.ganti(tampil)
        kosong.visibility = if (tampil.isEmpty()) View.VISIBLE else View.GONE
        daftar.visibility = if (tampil.isEmpty()) View.GONE else View.VISIBLE

        tvJumlah.text = when {
            semua.isEmpty() -> "Belum ada catatan"
            saringan == "SEMUA" -> "${semua.size} catatan tersimpan"
            else -> "${tampil.size} dari ${semua.size} catatan"
        }
    }

    // ------------------------------------------------------------ adapter
    private inner class Adapter : RecyclerView.Adapter<Adapter.Pemegang>() {

        private var data: List<RiwayatStore.Catatan> = emptyList()

        fun ganti(baru: List<RiwayatStore.Catatan>) {
            data = baru
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Pemegang {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_riwayat, parent, false)
            return Pemegang(v)
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: Pemegang, position: Int) = holder.pasang(data[position])

        inner class Pemegang(v: View) : RecyclerView.ViewHolder(v) {
            private val kartu: MaterialCardView = v.findViewById(R.id.kartuItem)
            private val wrap: View = v.findViewById(R.id.wrapItem)
            private val ikon: ImageView = v.findViewById(R.id.ivItem)
            private val judul: TextView = v.findViewById(R.id.tvItemJudul)
            private val pesan: TextView = v.findViewById(R.id.tvItemPesan)
            private val waktu: TextView = v.findViewById(R.id.tvItemWaktu)

            fun pasang(c: RiwayatStore.Catatan) {
                val ctx = itemView.context
                judul.text = TeksPeringatan.judul(c.tipe)
                pesan.text = c.pesan
                waktu.text = waktuRamah(c.waktu)
                ikon.setImageResource(TeksPeringatan.ikon(c.tipe))

                val (isi, latar) = when (TeksPeringatan.golongan(c.tipe)) {
                    "BAHAYA" -> R.color.hg_danger to R.color.hg_danger_bg
                    "KEAMANAN" -> R.color.hg_warn to R.color.hg_warn_bg
                    else -> R.color.hg_info to R.color.hg_info_bg
                }
                kartu.setCardBackgroundColor(ContextCompat.getColor(ctx, latar))
                kartu.strokeWidth = 0
                wrap.setBackgroundResource(R.drawable.bg_icon_surface)
                ikon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, isi))
            }
        }
    }

    /** "Hari ini 14:05", "Kemarin 21:30", atau tanggal lengkap untuk yang lebih lama. */
    private fun waktuRamah(waktu: Long): String {
        val jam = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(waktu))
        val hariIni = hari(System.currentTimeMillis())
        return when (hari(waktu)) {
            hariIni -> "Hari ini, $jam"
            hariIni - 1 -> "Kemarin, $jam"
            else -> SimpleDateFormat("d MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(waktu))
        }
    }

    private fun hari(waktu: Long): Long {
        val c = java.util.Calendar.getInstance()
        c.timeInMillis = waktu
        c.set(java.util.Calendar.HOUR_OF_DAY, 0)
        c.set(java.util.Calendar.MINUTE, 0)
        c.set(java.util.Calendar.SECOND, 0)
        c.set(java.util.Calendar.MILLISECOND, 0)
        return c.timeInMillis / 86_400_000L
    }
}
