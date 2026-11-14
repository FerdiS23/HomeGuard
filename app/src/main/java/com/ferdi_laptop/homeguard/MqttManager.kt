package com.ferdi_laptop.homeguard   // sesuaikan dengan package project Anda

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Satu koneksi MQTT untuk seluruh aplikasi.
 * HOST, PORT, USERNAME, PASSWORD, dan BASE TOPIC dapat dikonfigurasi langsung di variabel di bawah ini
 * atau diisi melalui dialog pengaturan aplikasi.
 */
object MqttManager {

    private const val PREF_MQTT = "mqtt_config_prefs"

    // =========================================================================
    // VARIABEL KONFIGURASI BROKER MQTT
    // Silakan isi variabel di bawah ini jika ingin di-hardcode
    // =========================================================================
    var HOST: String = "ub85e22f.ala.asia-southeast1.emqxsl.com"
    var PORT: Int = 8883
    var USERNAME: String = "KLP1"      // Isi username MQTT di sini jika broker butuh otentikasi
    var PASSWORD: String = "KLP1"      // Isi password MQTT di sini jika broker butuh otentikasi
    var BASE: String = "smarthome/ferdi-pnup-01"

    /**
     * Sambungan terenkripsi. Broker EMQX Cloud pada port 8883 mewajibkan ini.
     * Untuk broker biasa di port 1883, biarkan mati.
     */
    var TLS: Boolean = true

    private var client: Mqtt3AsyncClient? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pending = ConcurrentLinkedQueue<Pair<String, String>>()

    var onMessage: ((topic: String, payload: String) -> Unit)? = null
    var onStatus: ((connected: Boolean) -> Unit)? = null

    fun isConnected(): Boolean = client?.state?.isConnected == true

    /**
     * Memuat konfigurasi MQTT dari SharedPreferences (jika pernah disimpan sebelumnya)
     */
    fun muatKonfigurasi(ctx: Context) {
        val p = ctx.getSharedPreferences(PREF_MQTT, Context.MODE_PRIVATE)
        HOST = p.getString("mqtt_host", HOST) ?: HOST
        PORT = p.getInt("mqtt_port", PORT)
        USERNAME = p.getString("mqtt_username", USERNAME) ?: USERNAME
        PASSWORD = p.getString("mqtt_password", PASSWORD) ?: PASSWORD
        BASE = p.getString("mqtt_base", BASE) ?: BASE
        TLS = p.getBoolean("mqtt_tls", TLS)
    }

    /**
     * Menyimpan konfigurasi MQTT baru dan menyambung ulang ke broker
     */
    fun simpanKonfigurasi(
        ctx: Context,
        host: String,
        port: Int,
        username: String,
        password: String,
        baseTopic: String,
        tls: Boolean = TLS
    ) {
        HOST = host.ifBlank { "broker.emqx.io" }
        PORT = if (port > 0) port else 1883
        USERNAME = username
        PASSWORD = password
        BASE = baseTopic.ifBlank { "smarthome/ferdi-pnup-01" }
        TLS = tls

        ctx.getSharedPreferences(PREF_MQTT, Context.MODE_PRIVATE).edit()
            .putString("mqtt_host", HOST)
            .putInt("mqtt_port", PORT)
            .putString("mqtt_username", USERNAME)
            .putString("mqtt_password", PASSWORD)
            .putString("mqtt_base", BASE)
            .putBoolean("mqtt_tls", TLS)
            .apply()

        disconnectAndReconnect()
    }

    fun disconnectAndReconnect() {
        try {
            client?.disconnect()
        } catch (e: Exception) {
            // abaikan error terputus
        }
        client = null
        connect()
    }

    fun connect(ctx: Context? = null) {
        if (ctx != null) {
            muatKonfigurasi(ctx)
        }

        if (client != null) return

        val builder = MqttClient.builder()
            .useMqttVersion3()
            .identifier("homeguard-" + UUID.randomUUID().toString().take(8))
            .serverHost(HOST)
            .serverPort(PORT)

        // Sambungan terenkripsi, wajib untuk broker di port 8883
        if (TLS) builder.sslWithDefaultConfig()

        // Tambahkan otentikasi username & password jika diisi
        if (USERNAME.isNotBlank()) {
            builder.simpleAuth()
                .username(USERNAME)
                .password(PASSWORD.toByteArray(StandardCharsets.UTF_8))
                .applySimpleAuth()
        }

        val c = builder.automaticReconnectWithDefaultConfig()
            .addConnectedListener {
                client?.let {
                    subscribeAll(it)
                    flushPending(it)
                }
                mainHandler.post { onStatus?.invoke(true) }
            }
            .addDisconnectedListener {
                mainHandler.post { onStatus?.invoke(false) }
            }
            .buildAsync()

        client = c
        c.connect()
    }

    private fun subscribeAll(c: Mqtt3AsyncClient) {
        c.subscribeWith()
            .topicFilter("$BASE/#")
            .qos(MqttQos.AT_MOST_ONCE)
            .callback { pub ->
                val topic = pub.topic.toString()
                val text = String(pub.payloadAsBytes, StandardCharsets.UTF_8)
                mainHandler.post { onMessage?.invoke(topic, text) }
            }
            .send()
    }

    private fun flushPending(c: Mqtt3AsyncClient) {
        while (true) {
            val item = pending.poll() ?: break
            kirimLangsung(c, item.first, item.second)
        }
    }

    private fun kirimLangsung(c: Mqtt3AsyncClient, subTopic: String, message: String) {
        c.publishWith()
            .topic("$BASE/$subTopic")
            .payload(message.toByteArray(StandardCharsets.UTF_8))
            .qos(MqttQos.AT_MOST_ONCE)
            .send()
    }

    /** Contoh: publish("cmd/lampu", "on") -> topik smarthome/ferdi-pnup-01/cmd/lampu */
    fun publish(subTopic: String, message: String) {
        val c = client
        if (c != null && c.state.isConnected) {
            kirimLangsung(c, subTopic, message)
        } else {
            pending.add(subTopic to message)   // kirim nanti saat sudah terhubung
            connect()
        }
    }
}