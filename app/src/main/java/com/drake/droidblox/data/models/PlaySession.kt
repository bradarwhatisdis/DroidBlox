package com.drake.droidblox.data.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaySession(
    val universeId: Long,
    val playedAt: Long,
    val leftAt: Long? = null,
    val deeplink: String? = null
)
