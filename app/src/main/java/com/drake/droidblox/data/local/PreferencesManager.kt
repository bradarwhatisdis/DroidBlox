package com.drake.droidblox.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.drake.droidblox.data.models.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val ENABLE_ACTIVITY_TRACKING = booleanPreferencesKey("enableActivityTracking")
        val SHOW_SERVER_LOCATION = booleanPreferencesKey("showServerLocation")
        val TOKEN = stringPreferencesKey("token")
        val SHOW_GAME_ACTIVITY = booleanPreferencesKey("showGameActivity")
        val ALLOW_ACTIVITY_JOINING = booleanPreferencesKey("allowActivityJoining")
        val SHOW_ROBLOX_USER = booleanPreferencesKey("showRobloxUser")
        val APPLY_FFLAGS = booleanPreferencesKey("applyFFlags")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            enableActivityTracking = prefs[Keys.ENABLE_ACTIVITY_TRACKING] ?: true,
            showServerLocation = prefs[Keys.SHOW_SERVER_LOCATION] ?: false,
            token = prefs[Keys.TOKEN],
            showGameActivity = prefs[Keys.SHOW_GAME_ACTIVITY] ?: true,
            allowActivityJoining = prefs[Keys.ALLOW_ACTIVITY_JOINING] ?: false,
            showRobloxUser = prefs[Keys.SHOW_ROBLOX_USER] ?: false,
            applyFFlags = prefs[Keys.APPLY_FFLAGS] ?: false
        )
    }

    suspend fun getSettings(): AppSettings = settingsFlow.first()

    suspend fun updateSetting(key: String, value: Any) {
        context.dataStore.edit { prefs ->
            when (key) {
                "enableActivityTracking" -> prefs[Keys.ENABLE_ACTIVITY_TRACKING] = value as Boolean
                "showServerLocation" -> prefs[Keys.SHOW_SERVER_LOCATION] = value as Boolean
                "token" -> prefs[Keys.TOKEN] = value as? String
                "showGameActivity" -> prefs[Keys.SHOW_GAME_ACTIVITY] = value as Boolean
                "allowActivityJoining" -> prefs[Keys.ALLOW_ACTIVITY_JOINING] = value as Boolean
                "showRobloxUser" -> prefs[Keys.SHOW_ROBLOX_USER] = value as Boolean
                "applyFFlags" -> prefs[Keys.APPLY_FFLAGS] = value as Boolean
            }
        }
    }

    suspend fun setToken(token: String?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.TOKEN)
        }
    }
}
