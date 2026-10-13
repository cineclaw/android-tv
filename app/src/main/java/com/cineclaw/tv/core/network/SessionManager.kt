package com.cineclaw.tv.core.network

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "cineclaw_tv_prefs")

class SessionManager(private val context: Context) {
    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_AUDIO_PASSTHROUGH = booleanPreferencesKey("audio_passthrough")
        private val KEY_AUTO_FRAME_RATE = booleanPreferencesKey("auto_frame_rate")
        private val KEY_PREFERRED_QUALITY = stringPreferencesKey("preferred_quality")
        private val KEY_ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        private val KEY_ACTIVE_PROFILE_NAME = stringPreferencesKey("active_profile_name")
        private val KEY_ACTIVE_PROFILE_IS_KIDS = booleanPreferencesKey("active_profile_is_kids")
    }

    @Volatile
    private var cachedServerUrl: String = "http://192.168.88.19:3000"
    @Volatile
    private var cachedAuthToken: String? = null
    @Volatile
    private var cachedUsername: String? = null
    @Volatile
    private var cachedActiveProfile: String = "default"
    @Volatile
    private var cachedActiveProfileName: String = "Основной"
    @Volatile
    private var cachedIsKidsMode: Boolean = false
    @Volatile
    private var cachedAutoFrameRate: Boolean = true

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    init {
        // Pre-populate synchronously from DataStore so cache is ready immediately at app startup
        runCatching {
            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                val initial = context.dataStore.data.first()
                cachedServerUrl = initial[KEY_SERVER_URL] ?: "http://192.168.88.19:3000"
                cachedAuthToken = initial[KEY_AUTH_TOKEN]
                cachedUsername = initial[KEY_USERNAME]
                cachedActiveProfile = initial[KEY_ACTIVE_PROFILE] ?: "default"
                cachedActiveProfileName = initial[KEY_ACTIVE_PROFILE_NAME] ?: "Основной"
                cachedIsKidsMode = initial[KEY_ACTIVE_PROFILE_IS_KIDS] ?: false
                cachedAutoFrameRate = initial[KEY_AUTO_FRAME_RATE] ?: true
            }
        }

        scope.launch {
            context.dataStore.data.collect { prefs ->
                cachedServerUrl = prefs[KEY_SERVER_URL] ?: "http://192.168.88.19:3000"
                cachedAuthToken = prefs[KEY_AUTH_TOKEN]
                cachedUsername = prefs[KEY_USERNAME]
                cachedActiveProfile = prefs[KEY_ACTIVE_PROFILE] ?: "default"
                cachedActiveProfileName = prefs[KEY_ACTIVE_PROFILE_NAME] ?: "Основной"
                cachedIsKidsMode = prefs[KEY_ACTIVE_PROFILE_IS_KIDS] ?: false
                cachedAutoFrameRate = prefs[KEY_AUTO_FRAME_RATE] ?: true
            }
        }
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

    val autoFrameRate: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_AUTO_FRAME_RATE] ?: true
    }

    val preferredQuality: Flow<String> = context.dataStore.data.map {
        it[KEY_PREFERRED_QUALITY] ?: "auto"
    }

    val activeProfile: Flow<String> = context.dataStore.data.map {
        it[KEY_ACTIVE_PROFILE] ?: "default"
    }

    val activeProfileName: Flow<String> = context.dataStore.data.map {
        it[KEY_ACTIVE_PROFILE_NAME] ?: "Основной"
    }

    val isKidsMode: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_ACTIVE_PROFILE_IS_KIDS] ?: false
    }

    fun getServerUrlDirect(): String = cachedServerUrl

    fun getAuthTokenDirect(): String? {
        cachedAuthToken?.let { return it }
        return runCatching {
            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                val prefs = context.dataStore.data.first()
                val token = prefs[KEY_AUTH_TOKEN]
                cachedAuthToken = token
                token
            }
        }.getOrNull()
    }

    fun getUsernameDirect(): String? = cachedUsername

    fun getActiveProfileDirect(): String = cachedActiveProfile

    fun getActiveProfileNameDirect(): String = cachedActiveProfileName

    fun isKidsModeDirect(): Boolean = cachedIsKidsMode

    fun isAutoFrameRateDirect(): Boolean = cachedAutoFrameRate

    suspend fun setActiveProfile(id: String, name: String, isKids: Boolean) {
        cachedActiveProfile = id
        cachedActiveProfileName = name
        cachedIsKidsMode = isKids
        context.dataStore.edit {
            it[KEY_ACTIVE_PROFILE] = id
            it[KEY_ACTIVE_PROFILE_NAME] = name
            it[KEY_ACTIVE_PROFILE_IS_KIDS] = isKids
        }
    }

    suspend fun getServerUrlSync(): String = serverUrl.first()
    suspend fun getAuthTokenSync(): String? = authToken.first()

    suspend fun saveServerUrl(url: String) {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        clean = clean.trimEnd('/')
        cachedServerUrl = clean
        context.dataStore.edit { it[KEY_SERVER_URL] = clean }
    }

    suspend fun saveSession(token: String, user: String) {
        cachedAuthToken = token
        cachedUsername = user
        context.dataStore.edit {
            it[KEY_AUTH_TOKEN] = token
            it[KEY_USERNAME] = user
        }
    }

    suspend fun clearSession() {
        cachedAuthToken = null
        cachedUsername = null
        context.dataStore.edit {
            it.remove(KEY_AUTH_TOKEN)
            it.remove(KEY_USERNAME)
        }
    }

    fun clearSessionAsync() {
        cachedAuthToken = null
        cachedUsername = null
        scope.launch {
            clearSession()
        }
    }

    suspend fun setAudioPassthrough(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUDIO_PASSTHROUGH] = enabled }
    }

    suspend fun setAutoFrameRate(enabled: Boolean) {
        cachedAutoFrameRate = enabled
        context.dataStore.edit { it[KEY_AUTO_FRAME_RATE] = enabled }
    }

    suspend fun setPreferredQuality(quality: String) {
        context.dataStore.edit { it[KEY_PREFERRED_QUALITY] = quality }
    }
}
