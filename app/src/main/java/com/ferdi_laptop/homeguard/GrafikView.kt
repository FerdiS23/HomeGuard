package com.ferdi_laptop.homeguard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Grafik garis sederhana yang digambar sendiri, tanpa pustaka tambahan.
 *
 * Dipakai halaman Statistik untuk menampilkan suhu, kelembapan, dan daya
 * selama 24 jam terakhir.
 */
class GrafikView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var nilai: List<Float> = emptyList()
    private var satuan: String = ""
    private var warnaGaris: Int = Color.BLUE
    private var pesanKosong: String = "Belum ada data"

    private val pGaris = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val pIsi = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val pKisi = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val pTeks = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = sp(11f) }
    private val pTitik = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val jalur = Path()
    private val jalurIsi = Path()

    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun sp(v: Float) = v * resources.displayMetrics.scaledDensity
    private fun warna(id: Int) = ContextCompat.getColor(context, id)

    /**
     * Mengisi grafik. Nilai NaN diabaikan, jadi data sensor yang sempat kosong
     * tidak merusak gambar.
     */
    fun isi(data: List<Float>, satuan: String, warnaId: Int, pesanKosong: String = "Belum ada data") {
        this.nilai = data.filter { !it.isNaN() && !it.isInfinite() }
        this.satuan = satuan
        this.warnaGaris = warna(warnaId)
        this.pesanKosong = pesanKosong
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val kiri = dp(4f)
        val kanan = width - dp(46f)       // ruang untuk label nilai di kanan
        val atas = dp(10f)
        val bawah = height - dp(18f)

        pKisi.color = warna(R.color.hg_stroke)
        pTeks.color = warna(R.color.hg_text_muted)

        // Tiga garis bantu mendatar
        for (i in 0..2) {
            val y = atas + (bawah - atas) * i / 2f
            canvas.drawLine(kiri, y, kanan, y, pKisi)
        }

        if (nilai.size < 2) {
            pTeks.textAlign = Paint.Align.CENTER
            canvas.drawText(pesanKosong, width / 2f, height / 2f, pTeks)
            pTeks.textAlign = Paint.Align.LEFT
            return
        }

        var min = nilai.minOrNull() ?: return
        var maks = nilai.maxOrNull() ?: return
        if (maks - min < 0.5f) {          // data hampir datar, beri ruang agar garis terlihat
            val tengah = (maks + min) / 2f
            min = tengah - 0.5f
            maks = tengah + 0.5f
        }
        val rentang = maks - min

        fun x(i: Int) = kiri + (kanan - kiri) * i / (nilai.size - 1).toFloat()
        fun y(v: Float) = bawah - (v - min) / rentang * (bawah - atas)

        jalur.reset()
        jalurIsi.reset()
        nilai.forEachIndexed { i, v ->
            val px = x(i)
            val py = y(v)
            if (i == 0) {
                jalur.moveTo(px, py)
                jalurIsi.moveTo(px, bawah)
                jalurIsi.lineTo(px, py)
            } else {
                jalur.lineTo(px, py)
                jalurIsi.lineTo(px, py)
            }
        }
        jalurIsi.lineTo(x(nilai.size - 1), bawah)
        jalurIsi.close()

        // Bayangan lembut di bawah garis
        pIsi.shader = LinearGradient(
            0f, atas, 0f, bawah,
            Color.argb(60, Color.red(warnaGaris), Color.green(warnaGaris), Color.blue(warnaGaris)),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(jalurIsi, pIsi)

        pGaris.color = warnaGaris
        canvas.drawPath(jalur, pGaris)

        // Titik pada nilai terakhir
        pTitik.color = warnaGaris
        canvas.drawCircle(x(nilai.size - 1), y(nilai.last()), dp(3.5f), pTitik)

        // Label tertinggi dan terendah di sisi kanan
        pTeks.textAlign = Paint.Align.LEFT
        canvas.drawText(format(maks), kanan + dp(6f), atas + sp(4f), pTeks)
        canvas.drawText(format(min), kanan + dp(6f), bawah, pTeks)

        // Keterangan waktu
        canvas.drawText("24 jam lalu", kiri, height - dp(2f), pTeks)
        pTeks.textAlign = Paint.Align.RIGHT
        canvas.drawText("sekarang", kanan, height - dp(2f), pTeks)
        pTeks.textAlign = Paint.Align.LEFT
    }

    private fun format(v: Float): String {
        val angka = if (v >= 100f) String.format(Locale.US, "%.0f", v)
        else String.format(Locale.US, "%.1f", v)
        return angka + satuan
    }
}
