package com.drake.droidblox.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Integrations : Screen("integrations", "Integrations", Icons.Default.Home)
    data object FFlags : Screen("fflags", "Fast Flags", Icons.Default.Flag)
    data object FFlagsEditor : Screen("fflags_editor", "FFlags Editor", Icons.Default.Code)
    data object PlayLogs : Screen("play_logs", "Play Logs", Icons.Default.History)
    data object About : Screen("about", "About", Icons.Default.Info)
}

val drawerScreens = listOf(
    Screen.Integrations,
    Screen.FFlags,
    Screen.PlayLogs,
    Screen.About
)
