package com.drake.droidblox.data.models

import kotlinx.serialization.Serializable

@Serializable
data class GameInfo(
    val id: Long = 0,
    val name: String = "Unknown",
    val creator: Creator? = null,
    val placeId: Long? = null
)

@Serializable
data class Creator(
    val id: Long = 0,
    val name: String = "Unknown"
)

@Serializable
data class ThumbnailRequest(
    val requestId: String = "0",
    val type: String = "GameIcon",
    val targetId: Long,
    val format: String = "Png",
    val size: String = "128x128"
)

@Serializable
data class ThumbnailResponse(
    val data: List<ThumbnailItem> = emptyList()
)

@Serializable
data class ThumbnailItem(
    val imageUrl: String = "",
    val state: String = ""
)

@Serializable
data class RobloxUser(
    val id: Long = 0,
    val name: String = "",
    val displayName: String = ""
)
