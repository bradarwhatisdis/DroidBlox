package com.drake.droidblox.ui.screens.integrations

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drake.droidblox.DroidBloxApp
import com.drake.droidblox.data.local.PreferencesManager
import com.drake.droidblox.data.models.AppSettings
import com.drake.droidblox.data.remote.discord.DiscordApi
import com.drake.droidblox.service.root.RootManager
import com.drake.droidblox.service.watcher.ActivityWatcher
import com.drake.droidblox.ui.components.*
import com.drake.droidblox.ui.theme.DiscordBlurple
import com.drake.droidblox.ui.theme.DiscordGreen
import com.drake.droidblox.util.WebViewActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsScreen(
    preferencesManager: PreferencesManager,
    rootManager: RootManager,
    discordApi: DiscordApi,
    activityWatcher: ActivityWatcher?,
    canLaunchRoblox: Boolean,
    onRefreshRootStatus: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var settings by remember { mutableStateOf(AppSettings()) }
    var discordUsername by remember { mutableStateOf<String?>(null) }
    var isRooted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settings = preferencesManager.getSettings()
        isRooted = rootManager.checkRootAccess()
        if (settings.token != null) {
            val result = discordApi.getUsername(settings.token ?: "")
            result.onSuccess { discordUsername = it.username }
            result.onFailure { discordUsername = null }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DroidBlox") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, "Menu")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 80.dp)
        ) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (!isRooted) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, "Warning",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Root access required",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                SectionHeader("Roblox")
                SettingsButton(
                    title = if (canLaunchRoblox) "Launch Roblox" else "Roblox not installed",
                    subtitle = "Start Roblox with DroidBlox features",
                    icon = Icons.Default.PlayArrow,
                    onClick = {
                        launchRoblox(context, activityWatcher, settings.enableActivityTracking)
                    },
                    enabled = canLaunchRoblox
                )

                HorizontalDivider()
                SectionHeader("Activity Tracking")
                SettingsToggle(
                    title = "Enable activity tracking",
                    subtitle = "Track game sessions and show Discord RPC",
                    checked = settings.enableActivityTracking,
                    onCheckedChange = {
                        settings = settings.copy(enableActivityTracking = it)
                        scope.launch { preferencesManager.updateSetting("enableActivityTracking", it) }
                    }
                )
                SettingsToggle(
                    title = "Query server location",
                    subtitle = "Show game server location via notification",
                    checked = settings.showServerLocation,
                    onCheckedChange = {
                        settings = settings.copy(showServerLocation = it)
                        scope.launch { preferencesManager.updateSetting("showServerLocation", it) }
                    }
                )

                HorizontalDivider()
                SectionHeader("Discord")
                if (discordUsername != null) {
                    SettingsButton(
                        title = "Logged in as $discordUsername",
                        subtitle = "Tap to disconnect",
                        icon = Icons.Default.Person,
                        contentColor = DiscordGreen,
                        onClick = {
                            scope.launch {
                                discordApi.logout(settings.token ?: "")
                                preferencesManager.clearToken()
                                discordUsername = null
                                settings = settings.copy(token = null)
                            }
                        }
                    )
                } else {
                    SettingsButton(
                        title = "Login with Discord",
                        subtitle = "Connect Discord for Rich Presence",
                        icon = Icons.Default.Login,
                        contentColor = DiscordBlurple,
                        onClick = {
                            val intent = WebViewActivity.createDiscordLoginIntent(context)
                            context.startActivity(intent)
                        }
                    )
                }

                if (discordUsername != null) {
                    SettingsToggle(
                        title = "Show game activity",
                        subtitle = "Display current game as Discord status",
                        checked = settings.showGameActivity,
                        onCheckedChange = {
                            settings = settings.copy(showGameActivity = it)
                            scope.launch { preferencesManager.updateSetting("showGameActivity", it) }
                        }
                    )
                    SettingsToggle(
                        title = "Allow activity joining",
                        subtitle = "Add join button to Discord RPC",
                        checked = settings.allowActivityJoining,
                        onCheckedChange = {
                            settings = settings.copy(allowActivityJoining = it)
                            scope.launch { preferencesManager.updateSetting("allowActivityJoining", it) }
                        }
                    )
                    SettingsToggle(
                        title = "Show Roblox account",
                        subtitle = "Display avatar in Discord RPC",
                        checked = settings.showRobloxUser,
                        onCheckedChange = {
                            settings = settings.copy(showRobloxUser = it)
                            scope.launch { preferencesManager.updateSetting("showRobloxUser", it) }
                        }
                    )
                }

                HorizontalDivider()
                SectionHeader("DroidBlox")
                SettingsButton(
                    title = "Version 1.0.0",
                    icon = Icons.Default.Info,
                    onClick = { }
                )
            }
        }
    }
}

private fun launchRoblox(
    context: Context,
    activityWatcher: ActivityWatcher?,
    enableTracking: Boolean
) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("roblox://")
            component = ComponentName("com.roblox.client", "com.roblox.client.ActivityProtocolLaunch")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        if (enableTracking) {
            activityWatcher?.start()
        }
    } catch (e: Exception) {
        // Roblox not installed
    }
}
