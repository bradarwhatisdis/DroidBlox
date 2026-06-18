package com.drake.droidblox.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.drake.droidblox.DroidBloxApp
import com.drake.droidblox.data.local.PreferencesManager
import kotlinx.coroutines.launch

class WebViewActivity : ComponentActivity() {

    companion object {
        private const val DISCORD_AUTH_URL = "https://discord.com/api/oauth2/authorize" +
            "?client_id=1379313837169311825" +
            "&response_type=token" +
            "&redirect_uri=https://droidblox.app/callback" +
            "&scope=identify+rpc+activities.write"

        fun createDiscordLoginIntent(context: Context): Intent {
            return Intent(context, WebViewActivity::class.java)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefsManager = PreferencesManager(applicationContext)

        setContent {
            var loading by remember { mutableStateOf(true) }

            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Discord Login") })
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.userAgentString = settings.userAgentString
                                        .replace("; wv", "")
                                        .replace("Mozilla", "Mozilla")

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            loading = true

                                            if (url?.startsWith("https://droidblox.app/callback") == true) {
                                                val fragment = url.split("#").getOrNull(1) ?: return
                                                val params = fragment.split("&")
                                                    .map { it.split("=", limit = 2) }
                                                    .associate { it[0] to it.getOrElse(1) { "" } }
                                                val token = params["access_token"]
                                                if (token != null) {
                                                    kotlinx.coroutines.MainScope().launch {
                                                        prefsManager.setToken(token)
                                                        finish()
                                                    }
                                                }
                                            }
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            loading = false
                                        }
                                    }
                                    loadUrl(DISCORD_AUTH_URL)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .wrapContentSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
