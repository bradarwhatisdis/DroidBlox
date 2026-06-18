package com.drake.droidblox.ui.screens.playlogs

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.drake.droidblox.data.local.PlayLogStorage
import com.drake.droidblox.data.models.PlaySession
import com.drake.droidblox.data.models.ThumbnailRequest
import com.drake.droidblox.data.remote.roblox.RobloxApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayLogsScreen(
    playLogStorage: PlayLogStorage,
    robloxApi: RobloxApi,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf(playLogStorage.getPlayLogs()) }
    var gameNames by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var gameThumbnails by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

    LaunchedEffect(sessions) {
        val universeIds = sessions.map { it.universeId }.distinct()
        if (universeIds.isNotEmpty()) {
            val gamesResult = robloxApi.getGameInfo(universeIds)
            gamesResult.onSuccess { games ->
                gameNames = games.associate { it.id to it.name }
            }
            val thumbsResult = robloxApi.getThumbnails(
                universeIds.map { ThumbnailRequest(targetId = it) }
            )
            thumbsResult.onSuccess { items ->
                gameThumbnails = items.mapIndexed { i, item ->
                    universeIds.getOrNull(i)!! to item.imageUrl
                }.filter { it.second.isNotBlank() }.toMap()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Play Logs") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, "Menu")
                    }
                }
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No play logs yet", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(sessions) { session ->
                    PlayLogCard(
                        session = session,
                        gameName = gameNames[session.universeId] ?: "Unknown Game",
                        thumbnailUrl = gameThumbnails[session.universeId],
                        onRejoin = {
                            val deeplink = session.deeplink
                            if (deeplink != null) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(deeplink)
                                        component = ComponentName("com.roblox.client",
                                            "com.roblox.client.ActivityProtocolLaunch")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) { }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayLogCard(
    session: PlaySession,
    gameName: String,
    thumbnailUrl: String?,
    onRejoin: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val isExpired = session.leftAt != null &&
        (System.currentTimeMillis() - session.leftAt) > 24 * 60 * 60 * 1000L

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            if (thumbnailUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(thumbnailUrl),
                    contentDescription = gameName,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gameName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Played at ${dateFormat.format(Date(session.playedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.deeplink != null) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onRejoin,
                        enabled = !isExpired,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(
                            if (isExpired) "Expired" else "Rejoin",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
