package com.drake.droidblox.data.models

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val enableActivityTracking: Boolean = true,
    val showServerLocation: Boolean = false,
    val token: String? = null,
    val showGameActivity: Boolean = true,
    val allowActivityJoining: Boolean = false,
    val showRobloxUser: Boolean = false,
    val applyFFlags: Boolean = false
)
