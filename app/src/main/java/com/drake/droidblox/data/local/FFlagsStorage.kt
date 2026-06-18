package com.drake.droidblox.data.local

import android.content.Context
import com.drake.droidblox.data.models.FFlagsMap
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class FFlagsStorage(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val fflagsFile: File
        get() = File(context.filesDir, "fflags.json")

    fun readFFlags(): FFlagsMap {
        return try {
            if (fflagsFile.exists()) {
                val text = fflagsFile.readText()
                if (text.isBlank()) return LinkedHashMap()
                json.decodeFromString<Map<String, String>>(text).let { map ->
                    val ordered = LinkedHashMap<String, String>()
                    ordered.putAll(map)
                    ordered
                }
            } else LinkedHashMap()
        } catch (e: Exception) {
            LinkedHashMap()
        }
    }

    fun readRawFFlags(): String {
        return try {
            if (fflagsFile.exists()) fflagsFile.readText() else "{}"
        } catch (e: Exception) {
            "{}"
        }
    }

    fun writeFFlag(key: String, value: String) {
        val flags = readFFlags()
        flags[key] = value
        writeToFFlags(flags)
    }

    fun mergeFFlags(updates: Map<String, String>) {
        val flags = readFFlags()
        flags.putAll(updates)
        writeToFFlags(flags)
    }

    fun deleteFFlags(keys: List<String>) {
        val flags = readFFlags()
        keys.forEach { flags.remove(it) }
        writeToFFlags(flags)
    }

    fun writeToFFlags(flags: FFlagsMap) {
        val sorted = LinkedHashMap<String, String>()
        flags.entries.sortedBy { it.key }.forEach { sorted[it.key] = it.value }
        fflagsFile.writeText(json.encodeToString(sorted))
    }

    fun writeToFFlags(text: String): Boolean {
        return try {
            val parsed = text.ifBlank { "{}" }
            json.decodeFromString<Map<String, String>>(parsed)
            fflagsFile.writeText(parsed)
            true
        } catch (e: Exception) {
            false
        }
    }
}
