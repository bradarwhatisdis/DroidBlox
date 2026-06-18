package com.drake.droidblox.service.rpc

import com.drake.droidblox.data.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*
import okhttp3.*

class DiscordRpcService(
    private val tokenProvider: () -> String?,
    private val onConnectionChanged: (Boolean) -> Unit = {}
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = OkHttpClient.Builder()
        .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var sessionId: String? = null
    private var heartbeatInterval: Long = 41250
    private var lastSeq: Int? = null
    private var heartbeatJob: Job? = null
    private var scope: CoroutineScope? = null
    private var reconnectAttempts = 0

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    fun connect(coroutineScope: CoroutineScope) {
        scope = coroutineScope
        val token = tokenProvider() ?: return
        val url = "wss://gateway.discord.gg/?v=10&encoding=json"
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                _connected.value = true
                onConnectionChanged(true)
                reconnectAttempts = 0
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                if (code == 4000) {
                    reconnectAttempts = Int.MAX_VALUE
                }
                disconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _connected.value = false
                onConnectionChanged(false)
                if (reconnectAttempts < 5) {
                    reconnectAttempts++
                    coroutineScope.launch {
                        delay((reconnectAttempts * 2000L).coerceAtMost(15000))
                        connect(coroutineScope)
                    }
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                _connected.value = false
                onConnectionChanged(false)
                if (reconnectAttempts < 5) {
                    reconnectAttempts++
                    coroutineScope.launch {
                        delay((reconnectAttempts * 2000L).coerceAtMost(15000))
                        connect(coroutineScope)
                    }
                }
            }
        })
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        webSocket?.close(1000, "Disconnected")
        webSocket = null
        _connected.value = false
        onConnectionChanged(false)
    }

    fun setRpc(payload: ChangeRPCPayload) {
        val activity = buildActivity(payload)
        val msg = buildJsonObject {
            put("op", GatewayOp.PRESENCE_UPDATE)
            putJsonObject("d") {
                put("since", 0)
                putJsonArray("activities") { add(activity) }
                put("status", "online")
                put("afk", false)
            }
        }
        webSocket?.send(msg.toString())
    }

    fun removeRpc() {
        val msg = buildJsonObject {
            put("op", GatewayOp.PRESENCE_UPDATE)
            putJsonObject("d") {
                put("since", 0)
                putJsonArray("activities") { }
                put("status", "online")
                put("afk", false)
            }
        }
        webSocket?.send(msg.toString())
    }

    private fun handleMessage(text: String) {
        try {
            val msg = json.decodeFromString<GatewayMessage>(text)
            lastSeq = msg.s
            when (msg.op) {
                GatewayOp.HELLO -> {
                    val data = msg.d?.jsonObject
                    heartbeatInterval = data?.get("heartbeat_interval")?.jsonPrimitive?.long ?: 41250
                    sendIdentify()
                    startHeartbeat()
                }
                GatewayOp.HEARTBEAT_ACK -> { }
                GatewayOp.RECONNECT -> {
                    reconnectAttempts = 0
                    val token = tokenProvider() ?: return
                    if (sessionId != null) {
                        sendResume(token)
                    } else {
                        sendIdentify()
                    }
                }
                GatewayOp.INVALID_SESSION -> {
                    sessionId = null
                    lastSeq = null
                    sendIdentify()
                }
                GatewayOp.DISPATCH -> {
                    val data = msg.d?.jsonObject
                    when (msg.t) {
                        "READY" -> {
                            sessionId = data?.get("session_id")?.jsonPrimitive?.content
                        }
                    }
                }
            }
        } catch (_: Exception) { }
    }

    private fun sendIdentify() {
        val token = tokenProvider() ?: return
        val identify = buildJsonObject {
            put("op", GatewayOp.IDENTIFY)
            putJsonObject("d") {
                put("token", token)
                putJsonObject("properties") {
                    put("os", "android")
                    put("browser", "DroidBlox")
                    put("device", "DroidBlox")
                }
                putJsonObject("presence") {
                    put("status", "online")
                    put("afk", false)
                    put("since", 0)
                    putJsonArray("activities") { }
                }
            }
        }
        webSocket?.send(identify.toString())
    }

    private fun sendResume(token: String) {
        val resume = buildJsonObject {
            put("op", GatewayOp.RESUME)
            putJsonObject("d") {
                put("token", token)
                put("session_id", sessionId ?: "")
                put("seq", lastSeq ?: 0)
            }
        }
        webSocket?.send(resume.toString())
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope?.launch {
            while (isActive) {
                delay(heartbeatInterval)
                val msg = buildJsonObject {
                    put("op", GatewayOp.HEARTBEAT)
                    put("d", lastSeq?.let { JsonPrimitive(it) } ?: JsonNull)
                }
                webSocket?.send(msg.toString())
            }
        }
    }

    private fun buildActivity(payload: ChangeRPCPayload): JsonObject {
        return buildJsonObject {
            put("name", "Roblox")
            put("type", 0)
            put("application_id", DroidBloxAppId)
            payload.details?.let { put("details", it) }
            payload.state?.let { put("state", it) }
            putJsonObject("timestamps") {
                payload.startTimestamp?.let { put("start", it) }
            }
            if (payload.largeImage != null || payload.smallImage != null) {
                putJsonObject("assets") {
                    payload.largeImage?.let { put("large_image", it) }
                    payload.largeText?.let { put("large_text", it) }
                    payload.smallImage?.let { put("small_image", it) }
                    payload.smallText?.let { put("small_text", it) }
                }
            }
            if (payload.buttonLabel != null && payload.buttonUrl != null) {
                putJsonArray("buttons") {
                    add(payload.buttonLabel)
                }
            }
        }
    }
}

data class ChangeRPCPayload(
    val details: String? = null,
    val state: String? = null,
    val largeImage: String? = null,
    val largeText: String? = null,
    val smallImage: String? = null,
    val smallText: String? = null,
    val startTimestamp: Long? = null,
    val buttonLabel: String? = null,
    val buttonUrl: String? = null
)
