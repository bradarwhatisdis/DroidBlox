package com.drake.droidblox.data.models

import kotlinx.serialization.Serializable

typealias FFlagsMap = LinkedHashMap<String, String>

@Serializable
data class FFlagsFile(
    val flags: FFlagsMap = LinkedHashMap()
)
