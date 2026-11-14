package com.ferdi_laptop.homeguard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.NumberFormat
import java.util.Locale

/**
 * Halaman 3: Statistik.
 * Grafik 24 jam terakhir dan perkiraan biaya listrik hari ini.
 */
class StatistikFragment : Fragment(), HomeRepo.Pendengar {

    private lateinit var tvBiaya: TextView
    private lateinit var tvBiayaKeterangan: TextView
    private lateinit var tvEnergiHari: TextView
    private lateinit var tvDayaKini: TextView
    private lateinit var tvSuhuRata: TextView
    private lateinit var tvKwhTotal: TextView
    private lateinit var tvPeriode: TextView

    private lateinit var grafikSuhu: GrafikView
    private lateinit var grafikLembap: GrafikView
    private lateinit var grafikDaya: GrafikView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?
    ): View = inflater.inflate(R.layout.fragment_statistik, container, false)

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)

        tvBiaya = view.findViewById(R.id.tvBiaya)
        tvBiayaKeterangan = view.findViewById(R.id.tvBiayaKeterangan)
        tvEnergiHari = view.findViewById(R.id.tvEnergiHari)
        tvDayaKini = view.findViewById(R.id.tvDayaKini)
        tvSuhuRata = view.findViewById(R.id.tvSuhuRata)
        tvKwhTotal = view.findViewById(R.id.tvKwhTotal)
        tvPeriode = view.findViewById(R.id.tvPeriode)

        grafikSuhu = view.findViewById(R.id.grafikSuhu)
        grafikLembap = view.findViewById(R.id.grafikLembap)
        grafikDaya = view.findViewById(R.id.grafikDaya)
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

    override fun onDataBerubah() {
        if (isAdded) muat()
    }

    private fun muat() {
        val ctx = requireContext()
        val sampel = SampelStore.ambil(ctx)
        val s = HomeRepo.sensor

        tvPeriode.text =
            if (sampel.isEmpty()) "Belum ada rekaman"
            else "24 jam terakhir, ${sampel.size} rekaman"

        // ---------------------------------------------------- grafik
        grafikSuhu.isi(sampel.map { it.suhu }, " C", R.color.hg_orange, "Belum cukup data suhu")
        grafikLembap.isi(sampel.map { it.lembap }, " %", R.color.hg_sky, "Belum cukup data kelembapan")
        grafikDaya.isi(sampel.map { it.daya }, " W", R.color.hg_violet, "Belum cukup data daya")

        // ---------------------------------------------------- angka ringkas
        tvDayaKini.text = if (s == null || s.daya.isNaN()) "--"
        else String.format(Locale.US, "%.0f W", s.daya)

        tvKwhTotal.text = if (s == null || s.kwh.isNaN()) "--"
        else String.format(Locale.US, "%.3f kWh", s.kwh)

        val suhuValid = sampel.map { it.suhu }.filter { !it.isNaN() }
        tvSuhuRata.text = if (suhuValid.isEmpty()) "--"
        else String.format(Locale.US, "%.1f °C", suhuValid.average())

        // ---------------------------------------------------- biaya hari ini
        val kwhHari = if (s == null) Float.NaN else SampelStore.kwhHariIni(ctx, s.kwh)
        if (kwhHari.isNaN()) {
            tvEnergiHari.text = "--"
            tvBiaya.text = "--"
            tvBiayaKeterangan.text =
                "Perhitungan dimulai setelah meteran terbaca dan berganti hari"
        } else {
            val tarif = Prefs.tarifKwh(ctx)
            tvEnergiHari.text = String.format(Locale.US, "%.3f kWh", kwhHari)
            tvBiaya.text = rupiah(kwhHari * tarif)
            tvBiayaKeterangan.text =
                "Perkiraan dari ${String.format(Locale.US, "%.3f", kwhHari)} kWh dikali tarif " +
                        "${rupiah(tarif)} per kWh. Belum termasuk biaya tetap dan pajak."
        }
    }

    private fun rupiah(nilai: Float): String {
        val f = NumberFormat.getInstance(Locale("id", "ID"))
        f.maximumFractionDigits = 0
        return "Rp " + f.format(nilai.toDouble())
    }
}
