package com.drake.droidblox.data.models

import kotlinx.serialization.Serializable

@Serializable
data class DiscordUser(
    val id: String = "",
    val username: String = "",
    val global_name: String? = null,
    val avatar: String? = null
)

@Serializable
data class ExternalAssetResponse(
    val id: String = "",
    val url: String = ""
)
