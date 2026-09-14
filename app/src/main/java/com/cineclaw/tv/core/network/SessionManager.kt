package com.cineclaw.tv.core.network

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "cineclaw_tv_prefs")

class SessionManager(private val context: Context) {
    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_AUDIO_PASSTHROUGH = booleanPreferencesKey("audio_passthrough")
        private val KEY_PREFERRED_QUALITY = stringPreferencesKey("preferred_quality")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map {
        it[KEY_SERVER_URL] ?: "http://192.168.88.19:3000"
    }

    val authToken: Flow<String?> = context.dataStore.data.map {
        it[KEY_AUTH_TOKEN]
    }

    val username: Flow<String?> = context.dataStore.data.map {
        it[KEY_USERNAME]
    }

    val audioPassthrough: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_AUDIO_PASSTHROUGH] ?: false
    }

    val preferredQuality: Flow<String> = context.dataStore.data.map {
        it[KEY_PREFERRED_QUALITY] ?: "auto"
    }

    suspend fun getServerUrlSync(): String = serverUrl.first()
    suspend fun getAuthTokenSync(): String? = authToken.first()

    suspend fun saveServerUrl(url: String) {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        clean = clean.trimEnd('/')
        context.dataStore.edit { it[KEY_SERVER_URL] = clean }
    }

    suspend fun saveSession(token: String, user: String) {
        context.dataStore.edit {
            it[KEY_AUTH_TOKEN] = token
            it[KEY_USERNAME] = user
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(KEY_AUTH_TOKEN)
            it.remove(KEY_USERNAME)
        }
    }

    suspend fun setAudioPassthrough(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUDIO_PASSTHROUGH] = enabled }
    }

    suspend fun setPreferredQuality(quality: String) {
        context.dataStore.edit { it[KEY_PREFERRED_QUALITY] = quality }
    }
}
