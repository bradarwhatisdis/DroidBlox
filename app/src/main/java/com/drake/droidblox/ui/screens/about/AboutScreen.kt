package com.drake.droidblox.ui.screens.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.drake.droidblox.ui.components.SettingsButton
import com.drake.droidblox.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onOpenDrawer: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Image(
                painter = rememberAsyncImagePainter(
                    "https://raw.githubusercontent.com/bradarwhatisdis/DroidBlox/main/assets/icon.png"
                ),
                contentDescription = "DroidBlox",
                modifier = Modifier
                    .size(96.dp)
                    .clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "DroidBlox",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "v1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "A Roblox bootstrapper for Android\nwith Discord Rich Presence integration",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))

            SectionHeader("Links")

            SettingsButton(
                title = "License (GPLv2)",
                icon = Icons.Default.Description,
                onClick = { openUrl(context, "https://www.gnu.org/licenses/old-licenses/gpl-2.0.html") }
            )
            SettingsButton(
                title = "GitHub Repository",
                icon = Icons.Default.Code,
                onClick = { openUrl(context, "https://github.com/bradarwhatisdis/DroidBlox") }
            )
            SettingsButton(
                title = "Discord Server",
                icon = Icons.Default.Forum,
                onClick = { openUrl(context, "https://discord.gg/droidblox") }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SectionHeader("Credits")

            Text(
                text = "Built with Kotlin & Jetpack Compose\nOriginal Python version by meowstrapper",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) { }
}
