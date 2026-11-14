package com.ferdi_laptop.homeguard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.TextView

/**
 * Halaman 6: Kunci Aplikasi.
 *
 * Tiga keadaan dalam satu halaman:
 *   MODE_BUKA  - meminta PIN atau sidik jari sebelum masuk Beranda
 *   MODE_BUAT  - membuat PIN baru (dua tahap: isi lalu ulangi)
 */
class KunciActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_BUKA = "buka"
        const val MODE_BUAT = "buat"
        private const val PANJANG_PIN = 6
    }

    private lateinit var tvJudul: TextView
    private lateinit var tvPesan: TextView
    private lateinit var wadahPin: TextInputLayout
    private lateinit var etPin: TextInputEditText
    private lateinit var btnUtama: MaterialButton
    private lateinit var btnSidikJari: MaterialButton
    private lateinit var btnBatal: MaterialButton

    private var mode = MODE_BUKA
    private var pinPertama: String? = null   // tahap pertama saat membuat PIN
    private var salah = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kunci)

        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_BUKA

        tvJudul = findViewById(R.id.tvKunciJudul)
        tvPesan = findViewById(R.id.tvKunciPesan)
        wadahPin = findViewById(R.id.wadahPin)
        etPin = findViewById(R.id.etPin)
        btnUtama = findViewById(R.id.btnKunciUtama)
        btnSidikJari = findViewById(R.id.btnSidikJari)
        btnBatal = findViewById(R.id.btnKunciBatal)

        if (mode == MODE_BUAT) siapkanBuat() else siapkanBuka()

        btnUtama.setOnClickListener { lanjut() }
        btnBatal.setOnClickListener { finish() }
        btnSidikJari.setOnClickListener { mintaSidikJari() }

        // Saat membuka aplikasi, tombol kembali tidak boleh melewati kunci
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mode == MODE_BUAT) finish() else finishAffinity()
            }
        })
    }

    // ------------------------------------------------------------ membuka
    private fun siapkanBuka() {
        tvJudul.text = "Masukkan PIN"
        tvPesan.text = "HomeGuard terkunci. Masukkan PIN untuk melanjutkan."
        btnUtama.text = "Buka"

        if (Prefs.biometrikAktif(this) && biometrikSiap()) {
            btnSidikJari.visibility = android.view.View.VISIBLE
            mintaSidikJari()
        }
    }

    private fun biometrikSiap(): Boolean =
        BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS

    private fun mintaSidikJari() {
        if (!biometrikSiap()) {
            Toast.makeText(this, "Sidik jari belum disiapkan di HP ini", Toast.LENGTH_SHORT).show()
            return
        }

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    masuk()
                }
            }
        )

        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Buka HomeGuard")
                .setSubtitle("Gunakan sidik jari atau wajah Anda")
                .setNegativeButtonText("Pakai PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
        )
    }

    // ------------------------------------------------------------ membuat
    private fun siapkanBuat() {
        tvJudul.text = "Buat PIN"
        tvPesan.text = "PIN $PANJANG_PIN angka untuk membuka HomeGuard"
        btnUtama.text = "Lanjut"
        btnBatal.visibility = android.view.View.VISIBLE
    }

    // ------------------------------------------------------------ alur
    private fun lanjut() {
        val pin = etPin.text?.toString().orEmpty()

        if (mode == MODE_BUAT) {
            if (pin.length != PANJANG_PIN) {
                wadahPin.error = "PIN harus $PANJANG_PIN angka"
                return
            }
            wadahPin.error = null

            val pertama = pinPertama
            if (pertama == null) {
                pinPertama = pin
                etPin.setText("")
                tvJudul.text = "Ulangi PIN"
                tvPesan.text = "Masukkan sekali lagi untuk memastikan"
                btnUtama.text = "Simpan"
                return
            }

            if (pertama != pin) {
                pinPertama = null
                etPin.setText("")
                wadahPin.error = "PIN tidak sama, silakan ulangi dari awal"
                tvJudul.text = "Buat PIN"
                tvPesan.text = "PIN $PANJANG_PIN angka untuk membuka HomeGuard"
                btnUtama.text = "Lanjut"
                return
            }

            Prefs.simpanPin(this, pin)
            Toast.makeText(this, "Kunci aplikasi diaktifkan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Mode membuka
        if (Prefs.pinCocok(this, pin)) {
            masuk()
        } else {
            salah += 1
            etPin.setText("")
            wadahPin.error =
                if (salah >= 3) "PIN salah. Periksa kembali, percobaan ke-$salah" else "PIN salah"
        }
    }

    private fun masuk() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
