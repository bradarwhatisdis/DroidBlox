package com.drake.droidblox.ui.screens.fflags

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drake.droidblox.data.local.FFlagsStorage
import com.drake.droidblox.data.local.PreferencesManager
import com.drake.droidblox.data.models.AppSettings
import com.drake.droidblox.data.models.FFlagsMap
import com.drake.droidblox.service.root.RootManager
import com.drake.droidblox.ui.components.*
import kotlinx.coroutines.launch

private data class FlagControl(
    val key: String,
    val label: String,
    val subtitle: String = "",
    val type: FlagType = FlagType.TOGGLE,
    val options: List<String> = emptyList()
)

private enum class FlagType { TOGGLE, TEXT, DROPDOWN }

private val fflagControls = listOf(
    FlagControl("FFlagDebugGraphicsPreferVulkan", "Vulkan Rendering", "Use Vulkan instead of OpenGL"),
    FlagControl("FFlagDebugGraphicsDisableOpenGL", "Disable OpenGL", "Force disable OpenGL renderer"),
    FlagControl("FIntRobloxGuiBlurIntensity", "GUI Blur", type = FlagType.DROPDOWN, options = listOf("0", "2", "4", "8", "16")),
    FlagControl("FFlagDebugForceMSAASamples", "MSAA (Anti-Aliasing)", type = FlagType.TEXT),
    FlagControl("FFlagDisablePostFx", "Disable Post-Processing", "Disable bloom/lighting effects"),
    FlagControl("FFlagDebugDisableShadows", "Disable Shadows"),
    FlagControl("FFlagDebugDisableTerrain", "Disable Terrain"),
    FlagControl("FIntRobloxGuiFrameRateCap", "Frame Rate Cap", type = FlagType.TEXT),
    FlagControl("FFlagDebugDisableVR", "Disable VR"),
    FlagControl("FFlagDebugDisableTeleportPrompt", "Disable Teleport Prompt"),
    FlagControl("FFlagDebugDisableFeedback", "Disable Feedback Button"),
    FlagControl("FFlagDebugDisableLanguageSelect", "Disable Language Selector"),
    FlagControl("FFlagDebugForceCameraType", "Camera Type", type = FlagType.DROPDOWN, options = listOf("Classic", "Follow", "Custom")),
    FlagControl("FFlagDebugDisableChatTranslation", "Disable Chat Translation"),
    FlagControl("FStringRobloxGuiChatFontSize", "Chat Font Size", type = FlagType.DROPDOWN, options = listOf("10", "12", "14", "16", "18", "20")),
    FlagControl("FFlagDebugEnableAppStyleLight", "Light Theme", "Use Roblox light theme"),
    FlagControl("FIntRenderShadowIntensity", "Shadow Intensity", type = FlagType.TEXT),
    FlagControl("FFlagDebugDisplayFPS", "Show FPS Counter"),
    FlagControl("FFlagDebugPingBreakdown", "Show Ping Breakdown"),
    FlagControl("FIntRobloxGuiShowPing", "Ping Display", type = FlagType.DROPDOWN, options = listOf("0", "1", "2")),
)

// Presets: Disable Roblox telemetry
private val telemetryFlags = mapOf(
    "FFlagDebugDisableTelemetry" to "true",
    "FFlagDebugDisableTelemetryEphemeral" to "true",
    "FFlagDebugDisableTelemetryV2" to "true",
    "FFlagDebugDisableTelemetryPoint" to "true",
    "FFlagDebugDisableTelemetrySetting" to "true",
    "FFlagDebugDisableTelemetryStats" to "true",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FFlagsScreen(
    fflagsStorage: FFlagsStorage,
    preferencesManager: PreferencesManager,
    rootManager: RootManager,
    onOpenDrawer: () -> Unit,
    onOpenEditor: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(AppSettings()) }
    var fflags by remember { mutableStateOf<FFlagsMap>(LinkedHashMap()) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var applyMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        settings = preferencesManager.getSettings()
        fflags = fflagsStorage.readFFlags()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fast Flags") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenEditor) {
                        Icon(Icons.Default.Code, "JSON Editor")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val success = rootManager.applyFFlags(fflagsStorage.readRawFFlags())
                            applyMessage = if (success) "FFlags applied successfully"
                            else "Failed to apply FFlags"
                            showApplyDialog = true
                        }
                    }) {
                        Icon(Icons.Default.Upload, "Apply to Roblox")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsToggle(
                title = "Allow DroidBlox to manage FFlags",
                subtitle = "Enable FFlag management UI",
                checked = settings.applyFFlags,
                onCheckedChange = {
                    settings = settings.copy(applyFFlags = it)
                    scope.launch { preferencesManager.updateSetting("applyFFlags", it) }
                }
            )

            if (settings.applyFFlags) {
                HorizontalDivider()
                SectionHeader("Presets")
                SettingsButton(
                    title = "Disable Roblox Telemetry",
                    subtitle = "Apply 6 telemetry-disabling flags",
                    icon = Icons.Default.Block,
                    onClick = {
                        fflagsStorage.mergeFFlags(telemetryFlags)
                        fflags = fflagsStorage.readFFlags()
                    }
                )
                SettingsButton(
                    title = "Clear all FFlags",
                    icon = Icons.Default.DeleteSweep,
                    onClick = {
                        val keys = fflags.keys.toList()
                        fflagsStorage.deleteFFlags(keys)
                        fflags = fflagsStorage.readFFlags()
                    }
                )

                HorizontalDivider()
                SectionHeader("Graphics")
                SettingsToggle(
                    title = "Vulkan Rendering",
                    subtitle = "Use Vulkan instead of OpenGL",
                    checked = fflags["FFlagDebugGraphicsPreferVulkan"] == "true",
                    onCheckedChange = {
                        if (it) fflagsStorage.writeFFlag("FFlagDebugGraphicsPreferVulkan", "true")
                        else fflagsStorage.deleteFFlags(listOf("FFlagDebugGraphicsPreferVulkan"))
                        fflags = fflagsStorage.readFFlags()
                    }
                )
                SettingsToggle(
                    title = "Disable Post-Processing",
                    checked = fflags["FFlagDisablePostFx"] == "true",
                    onCheckedChange = {
                        if (it) fflagsStorage.writeFFlag("FFlagDisablePostFx", "true")
                        else fflagsStorage.deleteFFlags(listOf("FFlagDisablePostFx"))
                        fflags = fflagsStorage.readFFlags()
                    }
                )
                SettingsToggle(
                    title = "Disable Shadows",
                    checked = fflags["FFlagDebugDisableShadows"] == "true",
                    onCheckedChange = {
                        if (it) fflagsStorage.writeFFlag("FFlagDebugDisableShadows", "true")
                        else fflagsStorage.deleteFFlags(listOf("FFlagDebugDisableShadows"))
                        fflags = fflagsStorage.readFFlags()
                    }
                )
                SettingsToggle(
                    title = "Disable Terrain",
                    checked = fflags["FFlagDebugDisableTerrain"] == "true",
                    onCheckedChange = {
                        if (it) fflagsStorage.writeFFlag("FFlagDebugDisableTerrain", "true")
                        else fflagsStorage.deleteFFlags(listOf("FFlagDebugDisableTerrain"))
                        fflags = fflagsStorage.readFFlags()
                    }
                )
                SettingsTextField(
                    title = "Frame Rate Cap",
                    value = fflags["FIntRobloxGuiFrameRateCap"] ?: "",
                    onValueChange = {
                        if (it.isNotBlank()) fflagsStorage.writeFFlag("FIntRobloxGuiFrameRateCap", it)
                        else fflagsStorage.deleteFFlags(listOf("FIntRobloxGuiFrameRateCap"))
                        fflags = fflagsStorage.readFFlags()
                    }
                )
                SettingsDropdown(
                    title = "GUI Blur",
                    selectedOption = fflags["FIntRobloxGuiBlurIntensity"] ?: "0",
                    options = listOf("0", "2", "4", "8", "16"),
                    onOptionSelected = {
                        fflagsStorage.writeFFlag("FIntRobloxGuiBlurIntensity", it)
                        fflags = fflagsStorage.readFFlags()
                    }
                )

                HorizontalDivider()
                SectionHeader("UI")
                SettingsToggle(
                    title = "Disable VR",
                    checked = fflags["FFlagDebugDisableVR"] == "true",
                    onCheckedChange = {
                        if (it) fflagsStorage.writeFFlag("FFlagDebugDisableVR", "true")
                        else fflagsStorage.deleteFFlags(listOf("FFlagDebugDisableVR"))
                        fflags = fflagsStorage.readFFlags()
                    }
                )
                SettingsToggle(
                    title = "Disable Feedback Button",
                    checked = fflags["FFlagDebugDisableFeedback"] == "true",
                    onCheckedChange = {
                        if (it) fflagsStorage.writeFFlag("FFlagDebugDisableFeedback", "true")
                        else fflagsStorage.deleteFFlags(listOf("FFlagDebugDisableFeedback"))
                        fflags = fflagsStorage.readFFlags()
                    }
                )
                SettingsToggle(
                    title = "Disable Language Selector",
                    checked = fflags["FFlagDebugDisableLanguageSelect"] == "true",
                    onCheckedChange = {
                        if (it) fflagsStorage.writeFFlag("FFlagDebugDisableLanguageSelect", "true")
                        else fflagsStorage.deleteFFlags(listOf("FFlagDebugDisableLanguageSelect"))
                        fflags = fflagsStorage.readFFlags()
                    }
                )
                SettingsToggle(
                    title = "Disable Chat Translation",
                    checked = fflags["FFlagDebugDisableChatTranslation"] == "true",
                    onCheckedChange = {
                        if (it) fflagsStorage.writeFFlag("FFlagDebugDisableChatTranslation", "true")
                        else fflagsStorage.deleteFFlags(listOf("FFlagDebugDisableChatTranslation"))
                        fflags = fflagsStorage.readFFlags()
                    }
                )

                HorizontalDivider()
                SectionHeader("Debug")
                SettingsToggle(
                    title = "Show FPS Counter",
                    checked = fflags["FFlagDebugDisplayFPS"] == "true",
                    onCheckedChange = {
                        if (it) fflagsStorage.writeFFlag("FFlagDebugDisplayFPS", "true")
                        else fflagsStorage.deleteFFlags(listOf("FFlagDebugDisplayFPS"))
                        fflags = fflagsStorage.readFFlags()
                    }
                )
                SettingsToggle(
                    title = "Show Ping Breakdown",
                    checked = fflags["FFlagDebugPingBreakdown"] == "true",
                    onCheckedChange = {
                        if (it) fflagsStorage.writeFFlag("FFlagDebugPingBreakdown", "true")
                        else fflagsStorage.deleteFFlags(listOf("FFlagDebugPingBreakdown"))
                        fflags = fflagsStorage.readFFlags()
                    }
                )
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            title = { Text("FFlags") },
            text = { Text(applyMessage) },
            confirmButton = {
                TextButton(onClick = { showApplyDialog = false }) { Text("OK") }
            }
        )
    }
}
