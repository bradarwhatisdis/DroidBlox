package com.drake.droidblox.data.models

import kotlinx.serialization.Serializable

@Serializable
data class GatewayMessage(
    val op: Int = 0,
    val d: kotlinx.serialization.json.JsonElement? = null,
    val s: Int? = null,
    val t: String? = null
)

@Serializable
data class IdentifyPayload(
    val token: String,
    val properties: ConnectionProperties = ConnectionProperties(),
    val presence: Presence = Presence()
)

@Serializable
data class ResumePayload(
    val token: String,
    val session_id: String,
    val seq: Int
)

@Serializable
data class ConnectionProperties(
    val os: String = "android",
    val browser: String = "DroidBlox",
    val device: String = "DroidBlox"
)

@Serializable
data class Presence(
    val status: String = "online",
    val afk: Boolean = false,
    val since: Long = 0,
    val activities: List<Activity> = emptyList()
)

@Serializable
data class Activity(
    val name: String,
    val type: Int = 0,
    val url: String? = null,
    val timestamps: Timestamps? = null,
    val application_id: String? = DroidBloxAppId,
    val details: String? = null,
    val state: String? = null,
    val assets: ActivityAssets? = null,
    val buttons: List<String>? = null
)

@Serializable
data class Timestamps(
    val start: Long? = null,
    val end: Long? = null
)

@Serializable
data class ActivityAssets(
    val large_image: String? = null,
    val large_text: String? = null,
    val small_image: String? = null,
    val small_text: String? = null
)

@Serializable
data class ActivityButton(
    val label: String,
    val url: String
)

data class RpcState(
    val details: String? = null,
    val state: String? = null,
    val largeImage: String? = null,
    val largeText: String? = null,
    val smallImage: String? = null,
    val smallText: String? = null,
    val startTimestamp: Long? = null,
    val joinSecret: String? = null
)

const val DroidBloxAppId = "1379313837169311825"

object GatewayOp {
    const val DISPATCH = 0
    const val HEARTBEAT = 1
    const val IDENTIFY = 2
    const val PRESENCE_UPDATE = 3
    const val RESUME = 6
    const val RECONNECT = 7
    const val INVALID_SESSION = 9
    const val HELLO = 10
    const val HEARTBEAT_ACK = 11
}
