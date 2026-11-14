package com.ferdi_laptop.homeguard

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText

/**
 * Halaman 7: Pengaturan Server MQTT.
 * Menggantikan dialog kecil yang dulu tersembunyi di balik status koneksi.
 */
class ServerActivity : AppCompatActivity(), HomeRepo.Pendengar {

    private lateinit var etHost: TextInputEditText
    private lateinit var etPort: TextInputEditText
    private lateinit var etUser: TextInputEditText
    private lateinit var etPass: TextInputEditText
    private lateinit var etBase: TextInputEditText
    private lateinit var switchTls: MaterialSwitch

    private lateinit var kartuStatus: MaterialCardView
    private lateinit var ivStatus: ImageView
    private lateinit var tvStatusJudul: TextView
    private lateinit var tvStatusIsi: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        etHost = findViewById(R.id.etHost)
        etPort = findViewById(R.id.etPort)
        etUser = findViewById(R.id.etUser)
        etPass = findViewById(R.id.etPass)
        etBase = findViewById(R.id.etBase)
        switchTls = findViewById(R.id.switchTls)

        kartuStatus = findViewById(R.id.kartuStatus)
        ivStatus = findViewById(R.id.ivStatus)
        tvStatusJudul = findViewById(R.id.tvStatusJudul)
        tvStatusIsi = findViewById(R.id.tvStatusIsi)

        isiNilaiSekarang()

        // Port 8883 hampir selalu berarti sambungan terenkripsi
        etPort.setOnFocusChangeListener { _, fokus ->
            if (!fokus && etPort.text?.toString() == "8883") switchTls.isChecked = true
        }

        findViewById<View>(R.id.btnKembali).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnSimpanServer).setOnClickListener { simpan() }
    }

    override fun onResume() {
        super.onResume()
        HomeRepo.daftar(this)
        onDataBerubah()
    }

    override fun onPause() {
        HomeRepo.lepas(this)
        super.onPause()
    }

    private fun isiNilaiSekarang() {
        etHost.setText(MqttManager.HOST)
        etPort.setText(MqttManager.PORT.toString())
        etUser.setText(MqttManager.USERNAME)
        etPass.setText(MqttManager.PASSWORD)
        etBase.setText(MqttManager.BASE)
        switchTls.isChecked = MqttManager.TLS
    }

    private fun simpan() {
        val host = etHost.text?.toString()?.trim().orEmpty()
        val port = etPort.text?.toString()?.trim()?.toIntOrNull() ?: 1883
        val user = etUser.text?.toString()?.trim().orEmpty()
        val pass = etPass.text?.toString()?.trim().orEmpty()
        val base = etBase.text?.toString()?.trim().orEmpty()

        MqttManager.simpanKonfigurasi(this, host, port, user, pass, base, switchTls.isChecked)
        isiNilaiSekarang()
        Toast.makeText(this, "Pengaturan disimpan, menyambung ulang", Toast.LENGTH_SHORT).show()
    }

    override fun onDataBerubah() {
        val tersambung = HomeRepo.terhubungBroker
        val (isi, latar) = if (tersambung) {
            R.color.hg_ok to R.color.hg_ok_bg
        } else {
            R.color.hg_warn to R.color.hg_warn_bg
        }

        kartuStatus.setCardBackgroundColor(ContextCompat.getColor(this, latar))
        ivStatus.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, isi))
        tvStatusJudul.text = if (tersambung) "Terhubung" else "Belum terhubung"
        tvStatusIsi.text = if (tersambung) {
            "Mendengarkan topik ${MqttManager.BASE}"
        } else {
            "Periksa alamat, port, dan sambungan internet"
        }
    }
}
