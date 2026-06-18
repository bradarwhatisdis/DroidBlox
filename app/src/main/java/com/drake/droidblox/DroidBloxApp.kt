package com.drake.droidblox

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class DroidBloxApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "DroidBlox",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Server location and app notifications"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .build()
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "com.drake.droidblox"
        const val DISCORD_APP_ID = "1379313837169311825"
        const val DISCORD_REDIRECT_URI = "https://droidblox.app/callback"
        const val ROBLOX_PACKAGE = "com.roblox.client"
        const val CHEVSTRAP_PACKAGE = "com.chevstrap.rbx"

        lateinit var instance: DroidBloxApp
            private set
    }
}
