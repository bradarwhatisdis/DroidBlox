package com.drake.droidblox.service.watcher

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.regex.Pattern

data class GameJoinInfo(
    val jobId: String? = null,
    val placeId: Long? = null,
    val universeId: Long? = null,
    val serverIp: String? = null
)

data class BloxstrapRpcMessage(
    val command: String = "",
    val data: Map<String, String> = emptyMap()
)

object LogcatParser {

    private val joiningGamePattern = Pattern.compile(
        "! Joining game '(\\w+)' place (\\d+) at (\\d+\\.\\d+\\.\\d+\\.\\d+)"
    )
    private val universeIdPattern = Pattern.compile("universeid:(\\d+)")
    private val udmuxIpPattern = Pattern.compile("UDMUX Address = (\\d+\\.\\d+\\.\\d+\\.\\d+)")
    private val serverIdPattern = Pattern.compile("serverId: (\\d+\\.\\d+\\.\\d+\\.\\d+)")
    private val disconnectPattern = Pattern.compile("Time to disconnect replication data")
    private val bloxstrapRpcPattern = Pattern.compile("\\[BloxstrapRPC\\] (.+)")
    private val leaveGamePattern = Pattern.compile("leaveUGCGameInternal|destroyLuaApp")

    data class ParseResult(
        val gameJoin: GameJoinInfo? = null,
        val universeId: Long? = null,
        val udmuxIp: String? = null,
        val isDisconnected: Boolean = false,
        val isLeftGame: Boolean = false,
        val bloxstrapMessage: BloxstrapRpcMessage? = null
    )

    fun parseLine(line: String): ParseResult? {
        if (line.isBlank()) return null

        val joinMatcher = joiningGamePattern.matcher(line)
        if (joinMatcher.find()) {
            return ParseResult(
                gameJoin = GameJoinInfo(
                    jobId = joinMatcher.group(1),
                    placeId = joinMatcher.group(2).toLongOrNull(),
                    serverIp = joinMatcher.group(3)
                )
            )
        }

        val universeMatcher = universeIdPattern.matcher(line)
        if (universeMatcher.find()) {
            return ParseResult(universeId = universeMatcher.group(1).toLongOrNull())
        }

        val udmuxMatcher = udmuxIpPattern.matcher(line)
        if (udmuxMatcher.find()) {
            return ParseResult(udmuxIp = udmuxMatcher.group(1))
        }

        val serverMatcher = serverIdPattern.matcher(line)
        if (serverMatcher.find()) {
            return ParseResult(gameJoin = GameJoinInfo(serverIp = serverMatcher.group(1)))
        }

        if (disconnectPattern.matcher(line).find()) {
            return ParseResult(isDisconnected = true)
        }

        if (leaveGamePattern.matcher(line).find()) {
            return ParseResult(isLeftGame = true)
        }

        val rpcMatcher = bloxstrapRpcPattern.matcher(line)
        if (rpcMatcher.find()) {
            val jsonStr = rpcMatcher.group(1)
            return try {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val obj = json.parseToJsonElement(jsonStr).jsonObject
                val command = obj["command"]?.jsonPrimitive?.contentOrNull ?: ""
                val dataObj = obj["data"]?.jsonObject
                val data = mutableMapOf<String, String>()
                dataObj?.entries?.forEach { (k, v) -> data[k] = v.jsonPrimitive.contentOrNull ?: "" }
                ParseResult(bloxstrapMessage = BloxstrapRpcMessage(command, data))
            } catch (_: Exception) { null }
        }

        return null
    }

    private val Pattern.find: Boolean get() = throw UnsupportedOperationException()
}
