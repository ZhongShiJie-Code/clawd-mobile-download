package com.clawd.mobile.ws

import com.clawd.mobile.data.*
import kotlinx.serialization.json.*
import android.util.Log
import com.clawd.mobile.util.ConnectionLog
import com.clawd.mobile.util.HttpClientProvider
import okhttp3.*
import okio.ByteString
import java.util.concurrent.atomic.AtomicLong

/**
 * WebSocket transport using OkHttp.
 * Delegates shared logic to [AbstractStreamingClient].
 *
 * @param connectionStrategy Optional strategy for URL/auth header construction.
 *        When null, uses default LAN behavior (cfg.streamUrl() / cfg.authHeader()).
 *        When provided (e.g. RelayConnectionStrategy), uses strategy.streamUrl() / strategy.authHeader().
 */
class WsClient(
    prefsStore: PrefsStore,
    private val connectionStrategy: ConnectionStrategy? = null
) : AbstractStreamingClient(prefsStore) {

    override val tag = "WsClient" + if (connectionStrategy != null) ":${connectionStrategy.tag}" else ""
    override val watchdogTimeoutMs = 90_000L

    @Volatile
    private var ws: WebSocket? = null
    /**
     * Identifies the currently requested transport. OkHttp may deliver
     * callbacks for a socket after it was replaced, so stale callbacks must
     * not change the state or schedule another reconnect.
     */
    private val transportGeneration = AtomicLong(0L)
    private var currentUrl: String? = null

    override fun doConnect() {
        val cfg = config ?: return
        if (!canOpenTransport()) return
        doConnectPreamble()
        closeTransport()
        val generation = transportGeneration.get()

        val url = connectionStrategy?.streamUrl(cfg) ?: cfg.streamUrl()
        val authHeader = connectionStrategy?.authHeader(cfg) ?: cfg.authHeader()
        currentUrl = url
        Log.d(tag, "doConnect → $url")
        ConnectionLog.d(tag, "doConnect → $url")

        try {
            val httpClient = HttpClientProvider.getStreamingClient(cfg)
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", authHeader)
                .build()

            val socket = httpClient.newWebSocket(request, object : WebSocketListener() {
                private fun isCurrentTransport(): Boolean =
                    transportGeneration.get() == generation

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (!isCurrentTransport()) {
                        webSocket.cancel()
                        return
                    }
                    Log.d(tag, "onOpen code=${response.code}")
                    ConnectionLog.d(tag, "OkHttp onOpen code=${response.code}")
                    onTransportOpen(response)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (!isCurrentTransport()) return
                    ConnectionLog.d(tag, "OkHttp onMessage len=${text.length}")
                    onTransportMessage(text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    if (!isCurrentTransport()) return
                    // Binary frames — treat as text if possible
                    val text = bytes.utf8()
                    ConnectionLog.d(tag, "OkHttp onMessage(binary) len=${text.length}")
                    onTransportMessage(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    if (!isCurrentTransport()) {
                        webSocket.cancel()
                        return
                    }
                    Log.d(tag, "WS onClosing code=$code reason=$reason")
                    ConnectionLog.d(tag, "OkHttp onClosing code=$code reason=$reason")
                    webSocket.close(1000, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (!isCurrentTransport()) return
                    Log.d(tag, "WS onClosed code=$code reason=$reason")
                    ConnectionLog.d(tag, "OkHttp onClosed code=$code reason=$reason")
                    onTransportClosed()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (!isCurrentTransport()) return
                    Log.e(tag, "WS onFailure: ${t.javaClass.simpleName}: ${t.message}")
                    ConnectionLog.e(tag, "OkHttp onFailure code=${response?.code} err=${t.javaClass.simpleName}: ${t.message}")
                    onTransportFailure(t, response)
                }
            })
            if (transportGeneration.get() == generation) {
                ws = socket
            } else {
                // A newer connect/disconnect won the race while OkHttp was
                // creating this socket. Do not leave the stale socket alive.
                socket.cancel()
            }
        } catch (e: Exception) {
            Log.e(tag, "doConnect failed: ${e.message}")
            ConnectionLog.e(tag, "doConnect exception: ${e.javaClass.simpleName}: ${e.message}")
            onTransportFailure(e, null)
        }
    }

    /**
     * Override to skip saveConfig when using a relay strategy.
     * Relay client should not overwrite the LAN config in prefs.
     */
    override fun connect(config: ConnectionConfig) {
        if (connectionStrategy != null) {
            // Relay mode: don't save config, don't overwrite LAN config
            android.util.Log.d(tag, "connect(relay) → ${connectionStrategy.streamUrl(config)}")
            this.config = config
            reconnectDelay = 1000L
            reconnectAttempts = 0
            doConnect()
        } else {
            // LAN mode: use default behavior (saves config)
            super.connect(config)
        }
    }

    override fun closeTransport() {
        transportGeneration.incrementAndGet()
        val socket = ws
        ws = null
        try {
            socket?.close(1000, "Client disconnect")
        } catch (_: Exception) {}
    }

    override fun cancelTransport() {
        transportGeneration.incrementAndGet()
        val socket = ws
        ws = null
        try {
            socket?.cancel()
        } catch (_: Exception) {}
    }

    /** Send a command via WebSocket (upstream message). WS-specific, not on [StreamingClient].
     *  @return true if sent, false if dropped (not connected or buffer full). */
    fun sendCommand(type: String, payload: JsonObject): Boolean {
        val socket = ws
        if (socket == null || connectionState.value != ConnectionState.CONNECTED) {
            Log.w(tag, "sendCommand skipped: not connected (state=${connectionState.value})")
            return false
        }
        val msg = buildJsonObject {
            put("type", type)
            for ((k, v) in payload) put(k, v)
        }.toString()
        return try {
            socket.send(msg)
        } catch (e: Exception) {
            Log.w(tag, "sendCommand failed: ${e.message}")
            false
        }
    }

    /** @return true if sent, false if not connected or send failed. */
    override fun sendMessage(json: String): Boolean {

        val socket = ws
        if (socket == null || connectionState.value != ConnectionState.CONNECTED) {
            Log.w(tag, "sendMessage skipped: not connected (state=${connectionState.value})")
            return false
        }
        return try {
            socket.send(json)
        } catch (e: Exception) {
            Log.w(tag, "sendMessage failed: ${e.message}")
            false
        }
    }
}
