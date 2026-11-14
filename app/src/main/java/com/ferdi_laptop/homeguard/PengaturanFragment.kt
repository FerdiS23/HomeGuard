package com.ferdi_laptop.homeguard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Halaman 4: Pengaturan.
 * Pintu masuk ke Pengaturan Rumah dan Server MQTT, serta saklar keamanan dan notifikasi.
 */
class PengaturanFragment : Fragment() {

    private lateinit var tvRingkasRumah: TextView
    private lateinit var tvRingkasServer: TextView
    private lateinit var tvRingkasKunci: TextView
    private lateinit var tvRingkasBiometrik: TextView
    private lateinit var tvRingkasNotif: TextView
    private lateinit var tvRingkasLayanan: TextView

    private lateinit var switchKunci: MaterialSwitch
    private lateinit var switchBiometrik: MaterialSwitch
    private lateinit var switchNotif: MaterialSwitch
    private lateinit var switchLayanan: MaterialSwitch

    /** Menandai perubahan yang datang dari kode, bukan dari jari pengguna. */
    private var sedangMenyetel = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?
    ): View = inflater.inflate(R.layout.fragment_pengaturan, container, false)

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)
        val ctx = requireContext()

        tvRingkasRumah = view.findViewById(R.id.tvRingkasRumah)
        tvRingkasServer = view.findViewById(R.id.tvRingkasServer)
        tvRingkasKunci = view.findViewById(R.id.tvRingkasKunci)
        tvRingkasBiometrik = view.findViewById(R.id.tvRingkasBiometrik)
        tvRingkasNotif = view.findViewById(R.id.tvRingkasNotif)
        tvRingkasLayanan = view.findViewById(R.id.tvRingkasLayanan)

        switchKunci = view.findViewById(R.id.switchKunci)
        switchBiometrik = view.findViewById(R.id.switchBiometrik)
        switchNotif = view.findViewById(R.id.switchNotif)
        switchLayanan = view.findViewById(R.id.switchLayanan)

        // ------------------------------------------------ halaman lain
        view.findViewById<View>(R.id.barisRumah).setOnClickListener {
            startActivity(Intent(ctx, RumahActivity::class.java))
        }
        view.findViewById<View>(R.id.barisServer).setOnClickListener {
            startActivity(Intent(ctx, ServerActivity::class.java))
        }
        view.findViewById<View>(R.id.barisPanduan).setOnClickListener {
            startActivity(Intent(ctx, PanduanActivity::class.java))
        }
        view.findViewById<View>(R.id.barisHapusData).setOnClickListener {
            MaterialAlertDialogBuilder(ctx)
                .setTitle("Hapus data statistik?")
                .setMessage("Rekaman grafik 24 jam akan dikosongkan. Riwayat peringatan tidak ikut terhapus.")
                .setPositiveButton("Hapus") { _, _ ->
                    SampelStore.kosongkan(ctx)
                    Toast.makeText(ctx, "Data statistik dikosongkan", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // ------------------------------------------------ kunci aplikasi
        switchKunci.setOnCheckedChangeListener { _, aktif ->
            if (sedangMenyetel) return@setOnCheckedChangeListener
            if (aktif) {
                // PIN dibuat di halaman Kunci Aplikasi
                startActivity(Intent(ctx, KunciActivity::class.java).apply {
                    putExtra(KunciActivity.EXTRA_MODE, KunciActivity.MODE_BUAT)
                })
            } else {
                Prefs.hapusPin(ctx)
                perbaruiTampilan()
            }
        }

        switchBiometrik.setOnCheckedChangeListener { _, aktif ->
            if (sedangMenyetel) return@setOnCheckedChangeListener
            if (aktif && !Prefs.kunciAktif(ctx)) {
                Toast.makeText(ctx, "Aktifkan kunci aplikasi terlebih dahulu", Toast.LENGTH_SHORT).show()
                perbaruiTampilan()
                return@setOnCheckedChangeListener
            }
            Prefs.setBiometrik(ctx, aktif)
            perbaruiTampilan()
        }

        // ------------------------------------------------ notifikasi & layanan
        switchNotif.setOnCheckedChangeListener { _, aktif ->
            if (sedangMenyetel) return@setOnCheckedChangeListener
            Prefs.setNotifPeringatan(ctx, aktif)
            perbaruiTampilan()
        }

        switchLayanan.setOnCheckedChangeListener { _, aktif ->
            if (sedangMenyetel) return@setOnCheckedChangeListener
            Prefs.setLayananAktif(ctx, aktif)
            if (aktif) HomeService.mulai(ctx) else HomeService.hentikan(ctx)
            perbaruiTampilan()
        }

        // Mengetuk baris sama dengan menggeser saklarnya
        view.findViewById<View>(R.id.barisKunci).setOnClickListener { switchKunci.performClick() }
        view.findViewById<View>(R.id.barisBiometrik).setOnClickListener {
            if (switchBiometrik.isEnabled) switchBiometrik.performClick()
            else Toast.makeText(ctx, "Aktifkan kunci aplikasi terlebih dahulu", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.barisNotif).setOnClickListener { switchNotif.performClick() }
        view.findViewById<View>(R.id.barisLayanan).setOnClickListener { switchLayanan.performClick() }

        view.findViewById<TextView>(R.id.tvTentang).text = "HomeGuard versi 1.0"
    }

    override fun onResume() {
        super.onResume()
        perbaruiTampilan()
    }

    private fun perbaruiTampilan() {
        val ctx = requireContext()
        sedangMenyetel = true

        val rumah = GeofenceHelper.ambilRumah(ctx)
        tvRingkasRumah.text = if (rumah == null) {
            "${Prefs.namaRumah(ctx)} - lokasi belum diatur"
        } else {
            "${Prefs.namaRumah(ctx)} - radius dekat ${Prefs.radiusDekat(ctx)} m"
        }

        tvRingkasServer.text = buildString {
            append(if (HomeRepo.terhubungBroker) "Terhubung ke " else "Terputus dari ")
            append(MqttManager.HOST)
        }

        val kunci = Prefs.kunciAktif(ctx)
        switchKunci.isChecked = kunci
        tvRingkasKunci.text = if (kunci) "Aktif, PIN diminta saat membuka aplikasi" else "Mati"

        val bio = Prefs.biometrikAktif(ctx)
        switchBiometrik.isChecked = bio && kunci
        switchBiometrik.isEnabled = kunci
        tvRingkasBiometrik.text = when {
            !kunci -> "Aktifkan kunci aplikasi terlebih dahulu"
            bio -> "Aktif, PIN tetap bisa dipakai"
            else -> "Mati"
        }

        val notif = Prefs.notifPeringatan(ctx)
        switchNotif.isChecked = notif
        tvRingkasNotif.text = if (notif) {
            "Gas, kompor, keamanan, dan rumah tidak terhubung"
        } else {
            "Mati, peringatan hanya terlihat di dalam aplikasi"
        }

        val layanan = Prefs.layananAktif(ctx)
        switchLayanan.isChecked = layanan
        tvRingkasLayanan.text = if (layanan) {
            "Aktif, Android menampilkan satu notifikasi tetap"
        } else {
            "Mati, peringatan berhenti saat aplikasi ditutup"
        }

        sedangMenyetel = false
    }
}
