package com.drake.droidblox

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.drake.droidblox.data.local.FFlagsStorage
import com.drake.droidblox.data.local.PlayLogStorage
import com.drake.droidblox.data.local.PreferencesManager
import com.drake.droidblox.data.remote.discord.DiscordApi
import com.drake.droidblox.data.remote.geolocation.IpGeolocation
import com.drake.droidblox.data.remote.roblox.RobloxApi
import com.drake.droidblox.service.notification.NotificationHelper
import com.drake.droidblox.service.root.RootManager
import com.drake.droidblox.service.rpc.DiscordRpcService
import com.drake.droidblox.service.watcher.ActivityWatcher
import com.drake.droidblox.ui.navigation.Screen
import com.drake.droidblox.ui.navigation.drawerScreens
import com.drake.droidblox.ui.screens.about.AboutScreen
import com.drake.droidblox.ui.screens.fflags.FFlagsScreen
import com.drake.droidblox.ui.screens.fflagseditor.FFlagsEditorScreen
import com.drake.droidblox.ui.screens.integrations.IntegrationsScreen
import com.drake.droidblox.ui.screens.playlogs.PlayLogsScreen
import com.drake.droidblox.ui.theme.DroidBloxTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var rootManager: RootManager
    private lateinit var discordApi: DiscordApi
    private lateinit var robloxApi: RobloxApi
    private lateinit var ipGeolocation: IpGeolocation
    private lateinit var fflagsStorage: FFlagsStorage
    private lateinit var playLogStorage: PlayLogStorage
    private lateinit var notificationHelper: NotificationHelper
    private var rpcService: DiscordRpcService? = null
    private var activityWatcher: ActivityWatcher? = null

    private var canLaunchRoblox = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesManager = PreferencesManager(applicationContext)
        rootManager = RootManager()
        discordApi = DiscordApi { preferencesManager.getSettings().token }
        robloxApi = RobloxApi()
        ipGeolocation = IpGeolocation()
        fflagsStorage = FFlagsStorage(applicationContext)
        playLogStorage = PlayLogStorage(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)

        canLaunchRoblox = try {
            packageManager.getPackageInfo("com.roblox.client", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

        // Start Discord RPC if token exists
        lifecycleScope.launch {
            val settings = preferencesManager.getSettings()
            if (settings.token != null) {
                rpcService = DiscordRpcService(
                    tokenProvider = { settings.token }
                ).also { it.connect(this) }
            }
        }

        // Create activity watcher
        activityWatcher = ActivityWatcher(
            context = applicationContext,
            rootManager = rootManager,
            discordApi = discordApi,
            robloxApi = robloxApi,
            ipGeolocation = ipGeolocation,
            rpcService = rpcService,
            settingsProvider = { preferencesManager.getSettings() }
        )

        setContent {
            DroidBloxTheme {
                DroidBloxApp(
                    preferencesManager = preferencesManager,
                    rootManager = rootManager,
                    discordApi = discordApi,
                    robloxApi = robloxApi,
                    fflagsStorage = fflagsStorage,
                    playLogStorage = playLogStorage,
                    activityWatcher = activityWatcher,
                    canLaunchRoblox = canLaunchRoblox
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityWatcher?.destroy()
        rpcService?.disconnect()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroidBloxApp(
    preferencesManager: PreferencesManager,
    rootManager: RootManager,
    discordApi: DiscordApi,
    robloxApi: RobloxApi,
    fflagsStorage: FFlagsStorage,
    playLogStorage: PlayLogStorage,
    activityWatcher: ActivityWatcher?,
    canLaunchRoblox: Boolean
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Integrations) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "DroidBlox",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))

                drawerScreens.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, screen.title) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                Spacer(Modifier.weight(1f))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Text(
                    text = "v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) {
        when (currentScreen) {
            Screen.Integrations -> IntegrationsScreen(
                preferencesManager = preferencesManager,
                rootManager = rootManager,
                discordApi = discordApi,
                activityWatcher = activityWatcher,
                canLaunchRoblox = canLaunchRoblox,
                onRefreshRootStatus = { },
                onOpenDrawer = { scope.launch { drawerState.open() } }
            )
            Screen.FFlags -> FFlagsScreen(
                fflagsStorage = fflagsStorage,
                preferencesManager = preferencesManager,
                rootManager = rootManager,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onOpenEditor = { currentScreen = Screen.FFlagsEditor }
            )
            Screen.FFlagsEditor -> FFlagsEditorScreen(
                fflagsStorage = fflagsStorage,
                onBack = { currentScreen = Screen.FFlags }
            )
            Screen.PlayLogs -> PlayLogsScreen(
                playLogStorage = playLogStorage,
                robloxApi = robloxApi,
                onOpenDrawer = { scope.launch { drawerState.open() } }
            )
            Screen.About -> AboutScreen(
                onOpenDrawer = { scope.launch { drawerState.open() } }
            )
        }
    }
}
