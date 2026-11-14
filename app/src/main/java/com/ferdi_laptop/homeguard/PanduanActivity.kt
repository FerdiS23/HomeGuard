package com.ferdi_laptop.homeguard

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton

/**
 * Halaman 5: Panduan awal.
 * Tiga layar singkat yang menjelaskan isi aplikasi. Tampil sekali saat pertama dibuka,
 * dan bisa dibuka lagi dari halaman Pengaturan.
 */
class PanduanActivity : AppCompatActivity() {

    private class Langkah(
        val ikon: Int,
        val warna: Int,
        val judul: String,
        val isi: String
    )

    private val langkah = listOf(
        Langkah(
            R.drawable.ic_nav_beranda, R.color.hg_info,
            "Pantau rumah sekilas",
            "Beranda menampilkan suhu, kelembapan, gas, pintu, dan pemakaian listrik. " +
                    "Kartu berubah warna saat ada yang perlu diperhatikan."
        ),
        Langkah(
            R.drawable.ic_light, R.color.hg_yellow,
            "Kendalikan perangkat",
            "Ketuk kartu untuk menyalakan lampu, stop kontak, atau AC. " +
                    "Skenario cepat mengatur beberapa perangkat sekaligus."
        ),
        Langkah(
            R.drawable.ic_shield, R.color.hg_ok,
            "Peringatan tetap sampai",
            "Peringatan gas, kompor, dan keamanan dikirim sebagai notifikasi walaupun " +
                    "aplikasi ditutup, lalu tersimpan di halaman Riwayat."
        )
    )

    private lateinit var pager: ViewPager2
    private lateinit var indikator: LinearLayout
    private lateinit var btnLanjut: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panduan)

        pager = findViewById(R.id.halamanPanduan)
        indikator = findViewById(R.id.indikator)
        btnLanjut = findViewById(R.id.btnLanjut)

        pager.adapter = Adapter()
        buatIndikator()

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                perbaruiIndikator(position)
                btnLanjut.text = if (position == langkah.lastIndex) "Mulai" else "Lanjut"
            }
        })

        btnLanjut.setOnClickListener {
            if (pager.currentItem < langkah.lastIndex) pager.currentItem += 1 else selesai()
        }
        findViewById<MaterialButton>(R.id.btnLewati).setOnClickListener { selesai() }
    }

    private fun selesai() {
        val pertamaKali = !Prefs.panduanSelesai(this)
        Prefs.tandaiPanduanSelesai(this)

        // Dibuka ulang dari Pengaturan cukup ditutup saja
        if (pertamaKali) startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // ------------------------------------------------------------ indikator
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private fun buatIndikator() {
        indikator.removeAllViews()
        repeat(langkah.size) {
            val titik = View(this)
            val lp = LinearLayout.LayoutParams(dp(8), dp(8))
            lp.marginStart = dp(4)
            lp.marginEnd = dp(4)
            titik.layoutParams = lp
            titik.setBackgroundResource(R.drawable.bg_dot)
            indikator.addView(titik)
        }
        perbaruiIndikator(0)
    }

    private fun perbaruiIndikator(aktif: Int) {
        for (i in 0 until indikator.childCount) {
            val warna = if (i == aktif) R.color.hg_primary else R.color.hg_stroke
            indikator.getChildAt(i).backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, warna))
        }
    }

    // ------------------------------------------------------------ adapter
    private inner class Adapter : RecyclerView.Adapter<Adapter.Pemegang>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Pemegang =
            Pemegang(LayoutInflater.from(parent.context).inflate(R.layout.item_panduan, parent, false))

        override fun getItemCount() = langkah.size

        override fun onBindViewHolder(holder: Pemegang, position: Int) = holder.pasang(langkah[position])

        inner class Pemegang(v: View) : RecyclerView.ViewHolder(v) {
            private val ikon: ImageView = v.findViewById(R.id.ivPanduan)
            private val judul: TextView = v.findViewById(R.id.tvPanduanJudul)
            private val keterangan: TextView = v.findViewById(R.id.tvPanduanIsi)

            fun pasang(l: Langkah) {
                ikon.setImageResource(l.ikon)
                ikon.imageTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(itemView.context, l.warna))
                judul.text = l.judul
                keterangan.text = l.isi
            }
        }
    }
}
