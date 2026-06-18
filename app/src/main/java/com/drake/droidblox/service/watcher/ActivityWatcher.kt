package com.drake.droidblox.service.watcher

import android.content.Context
import com.drake.droidblox.data.local.PlayLogStorage
import com.drake.droidblox.data.models.*
import com.drake.droidblox.data.remote.discord.DiscordApi
import com.drake.droidblox.data.remote.geolocation.IpGeolocation
import com.drake.droidblox.data.remote.roblox.RobloxApi
import com.drake.droidblox.service.notification.NotificationHelper
import com.drake.droidblox.service.root.RootManager
import com.drake.droidblox.service.rpc.ChangeRPCPayload
import com.drake.droidblox.service.rpc.DiscordRpcService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader

class ActivityWatcher(
    private val context: Context,
    private val rootManager: RootManager,
    private val discordApi: DiscordApi,
    private val robloxApi: RobloxApi,
    private val ipGeolocation: IpGeolocation,
    private val rpcService: DiscordRpcService?,
    private val settingsProvider: suspend () -> AppSettings
) {
    private val playLogStorage = PlayLogStorage(context)
    private val notificationHelper = NotificationHelper(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var watcherJob: Job? = null
    private var monitoringProcess: java.lang.Process? = null

    private var currentUniverseId: Long? = null
    private var currentPlaceId: Long? = null
    private var currentJobId: String? = null
    private var currentServerIp: String? = null
    private var sessionStartTime: Long? = null
    private var udmuxIp: String? = null

    private val _isWatching = MutableStateFlow(false)
    val isWatching: StateFlow<Boolean> = _isWatching.asStateFlow()

    fun start() {
        if (_isWatching.value) return
        _isWatching.value = true
        watcherJob = scope.launch {
            // Wait for Roblox to start
            var pid: Int? = null
            for (i in 0..30) {
                pid = rootManager.getRobloxPid()
                if (pid != null) break
                delay(1000)
            }
            if (pid == null) {
                _isWatching.value = false
                return@launch
            }
            monitorLogcat(pid)
        }
    }

    fun stop() {
        watcherJob?.cancel()
        monitoringProcess?.destroy()
        monitoringProcess = null
        _isWatching.value = false
        resetSession()
    }

    private suspend fun monitorLogcat(pid: Int) {
        val result = rootManager.execSh("logcat --pid=$pid -v raw")
        if (!result.success || result.stdout.isBlank()) return

        monitoringProcess = try {
            val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", "logcat --pid=$pid -v raw"))
            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            scope.launch(Dispatchers.IO) {
                var line: String? = null
                while (isActive && reader.readLine().also { line = it } != null) {
                    handleLogLine(line ?: "")
                }
            }
            proc
        } catch (_: Exception) { null }
    }

    private suspend fun handleLogLine(line: String) {
        val result = LogcatParser.parseLine(line) ?: return

        when {
            result.gameJoin != null -> {
                val info = result.gameJoin
                currentJobId = info.jobId ?: currentJobId
                currentPlaceId = info.placeId ?: currentPlaceId
                currentServerIp = info.serverIp ?: currentServerIp
                sessionStartTime = System.currentTimeMillis()
                if (info.serverIp != null) checkServerLocation(info.serverIp)
            }
            result.universeId != null -> {
                currentUniverseId = result.universeId
                onGameIdentified()
            }
            result.udmuxIp != null -> {
                udmuxIp = result.udmuxIp
                checkServerLocation(result.udmuxIp)
            }
            result.isDisconnected -> onDisconnected()
            result.isLeftGame -> onGameLeft()
            result.bloxstrapMessage != null -> {
                handleBloxstrapRpc(result.bloxstrapMessage)
            }
        }
    }

    private suspend fun onGameIdentified() {
        val uid = currentUniverseId ?: return
        val settings = settingsProvider()
        if (!settings.showGameActivity && settings.token == null) return

        val gameResult = robloxApi.getGameInfo(listOf(uid))
        if (gameResult.isFailure) return
        val game = gameResult.getOrNull()?.firstOrNull() ?: return

        val placeId = currentPlaceId ?: game.placeId ?: return
        val joinUrl = if (currentJobId != null)
            "https://www.roblox.com/games/start?placeId=$placeId&gameInstanceId=${currentJobId}"
        else null

        if (settings.token != null && settings.showGameActivity) {
            // Upload thumbnails to Discord media proxy
            val thumbnailsResult = robloxApi.getThumbnails(listOf(
                ThumbnailRequest(targetId = uid),
                ThumbnailRequest(targetId = game.creator?.id ?: uid, type = "AvatarHeadShot")
            ))
            val thumbnails = thumbnailsResult.getOrNull() ?: emptyList()
            val imageUrls = thumbnails.map { it.imageUrl }

            val proxyUrls = if (imageUrls.isNotEmpty() && settings.token != null) {
                discordApi.getMediaProxyUrls(settings.token, imageUrls).getOrNull() ?: imageUrls
            } else imageUrls

            var largeImage = proxyUrls.firstOrNull()
            var smallImage = proxyUrls.getOrNull(1)
            var largeText = game.name
            var smallText = game.creator?.name

            rpcService?.setRpc(ChangeRPCPayload(
                details = "Playing ${game.name}",
                state = "by ${game.creator?.name ?: "Unknown"}",
                largeImage = largeImage,
                largeText = largeText,
                smallImage = smallImage,
                smallText = smallText,
                startTimestamp = sessionStartTime,
                buttonLabel = if (joinUrl != null && settings.allowActivityJoining) "Join Server" else "See Game Page",
                buttonUrl = joinUrl ?: "https://www.roblox.com/games/$placeId"
            ))
        }
    }

    private suspend fun checkServerLocation(ip: String) {
        if (!settingsProvider().showServerLocation) return
        try {
            val isDc = ipGeolocation.isDatacenter(ip)
            if (isDc) {
                notificationHelper.showServerLocationNotification(ip, "Datacenter")
                return
            }
            val location = ipGeolocation.lookupLocation(ip)
            if (location != null) {
                notificationHelper.showServerLocationNotification(ip, location)
            }
        } catch (_: Exception) { }
    }

    private fun onDisconnected() {
        logPlaySession()
        rpcService?.removeRpc()
        resetSession()
    }

    private fun onGameLeft() {
        logPlaySession()
        rpcService?.removeRpc()
        resetSession()
    }

    private suspend fun handleBloxstrapRpc(msg: BloxstrapRpcMessage) {
        when (msg.command) {
            "SetRichPresence" -> {
                val settings = settingsProvider()
                if (settings.token == null) return
                rpcService?.setRpc(ChangeRPCPayload(
                    details = msg.data["details"],
                    state = msg.data["state"],
                    largeImage = msg.data["largeImage"],
                    largeText = msg.data["largeText"],
                    smallImage = msg.data["smallImage"],
                    smallText = msg.data["smallText"],
                    startTimestamp = sessionStartTime
                ))
            }
            "SetLaunchData" -> {
                // Handle custom join data if needed
            }
        }
    }

    private fun logPlaySession() {
        val uid = currentUniverseId ?: return
        val start = sessionStartTime ?: return
        val deeplink = currentJobId?.let { jobId ->
            currentPlaceId?.let { "https://www.roblox.com/games/start?placeId=$it&gameInstanceId=$jobId" }
        }
        val session = PlaySession(
            universeId = uid,
            playedAt = start,
            leftAt = System.currentTimeMillis(),
            deeplink = deeplink
        )
        playLogStorage.logPlaySession(session)
    }

    private fun resetSession() {
        currentUniverseId = null
        currentPlaceId = null
        currentJobId = null
        currentServerIp = null
        sessionStartTime = null
        udmuxIp = null
    }

    fun destroy() {
        stop()
        scope.cancel()
    }
}
