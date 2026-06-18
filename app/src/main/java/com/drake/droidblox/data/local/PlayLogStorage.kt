package com.drake.droidblox.data.local

import android.content.Context
import com.drake.droidblox.data.models.PlaySession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class PlayLogStorage(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val playLogsFile: File
        get() = File(context.filesDir, "playlogs.json")

    fun getPlayLogs(): List<PlaySession> {
        return try {
            if (playLogsFile.exists()) {
                json.decodeFromString(playLogsFile.readText())
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun logPlaySession(session: PlaySession) {
        val logs = getPlayLogs().toMutableList()
        logs.add(0, session)
        if (logs.size > 100) {
            logs.removeAt(logs.lastIndex)
        }
        playLogsFile.writeText(json.encodeToString(logs))
    }

    fun updateLastSession(leftAt: Long, deeplink: String?) {
        val logs = getPlayLogs().toMutableList()
        if (logs.isNotEmpty()) {
            val last = logs[0]
            logs[0] = last.copy(leftAt = leftAt, deeplink = deeplink)
            playLogsFile.writeText(json.encodeToString(logs))
        }
    }
}
