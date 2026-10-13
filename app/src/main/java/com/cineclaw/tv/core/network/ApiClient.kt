package com.cineclaw.tv.core.network

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.net.URI
import java.util.concurrent.TimeUnit

class ApiClient(private val sessionManager: SessionManager) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }

    // Clean client without interceptors for pings and internal auth verification
    val cleanOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // Dedicated client for Coil image loading: NO session-wiping interceptor, NO token leakage to external CDNs
    val imageOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    // Dedicated client for ExoPlayer video streaming: adds auth token for CineClaw streams, NEVER clears session on stream errors
    val streamingOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // No read timeout for video streams
            .addInterceptor { chain ->
                val request = chain.request()
                val requestBuilder = request.newBuilder()
                val serverHost = runCatching { URI(sessionManager.getServerUrlDirect()).host }.getOrNull()
                val reqHost = request.url.host
                val isCineClawHost = serverHost != null && (reqHost.equals(serverHost, ignoreCase = true) || reqHost == "127.0.0.1" || reqHost == "localhost")

                if (isCineClawHost && request.header("Authorization") == null) {
                    sessionManager.getAuthTokenDirect()?.let { token ->
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }
                }
                if (isCineClawHost && request.header("X-Profile") == null) {
                    val profile = sessionManager.getActiveProfileDirect()
                    if (profile.isNotBlank()) {
                        requestBuilder.addHeader("X-Profile", profile)
                    }
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    // Primary Retrofit client with strict host guards and /api/auth/verify verification
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val requestBuilder = request.newBuilder()
                val serverHost = runCatching { URI(sessionManager.getServerUrlDirect()).host }.getOrNull()
                val reqHost = request.url.host
                val isCineClawHost = serverHost != null && (reqHost.equals(serverHost, ignoreCase = true) || reqHost == "127.0.0.1" || reqHost == "localhost")
                var sentToken: String? = null

                if (isCineClawHost && request.header("Authorization") == null) {
                    sessionManager.getAuthTokenDirect()?.let { token ->
                        sentToken = token
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }
                }
                if (isCineClawHost && request.header("X-Profile") == null) {
                    val profile = sessionManager.getActiveProfileDirect()
                    if (profile.isNotBlank()) {
                        requestBuilder.addHeader("X-Profile", profile)
                    }
                }

                val response = chain.proceed(requestBuilder.build())
                val path = request.url.encodedPath

                // Only react to 401 if:
                // 1. Request was actually sent to CineClaw server
                // 2. Request had an Authorization token sent
                // 3. Path is not an authentication endpoint
                if (response.code == 401 && isCineClawHost && sentToken != null &&
                    !path.contains("/api/auth/login") && !path.contains("/api/auth/pair") && !path.contains("/api/auth/verify")) {

                    // Double check with /api/auth/verify to avoid false-positive logouts from transient gateway glitches
                    val isDefinitelyInvalid = runCatching {
                        val verifyReq = okhttp3.Request.Builder()
                            .url("${sessionManager.getServerUrlDirect().trimEnd('/')}/api/auth/verify")
                            .addHeader("Authorization", "Bearer $sentToken")
                            .get()
                            .build()
                        cleanOkHttpClient.newCall(verifyReq).execute().use { it.code == 401 }
                    }.getOrDefault(false)

                    if (isDefinitelyInvalid) {
                        android.util.Log.w("CineClaw", "Auth token rejected by /api/auth/verify, clearing session")
                        sessionManager.clearSessionAsync()
                    }
                }
                response
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    suspend fun getApi(): CineClawApi {
        val rawBase = sessionManager.getServerUrlSync().trimEnd('/')
        com.cineclaw.tv.core.model.ApiConfig.baseUrl = rawBase
        val baseUrl = "$rawBase/"
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CineClawApi::class.java)
    }

    suspend fun pingServer(targetUrl: String? = null): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val raw = (targetUrl ?: sessionManager.getServerUrlSync()).trim()
        var url = if (!raw.startsWith("http://") && !raw.startsWith("https://")) "http://$raw" else raw
        url = url.trimEnd('/')
        try {
            val req = okhttp3.Request.Builder()
                .url(url)
                .head()
                .build()
            val client = cleanOkHttpClient.newBuilder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build()
            client.newCall(req).execute().use { resp ->
                resp.code in 200..401
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun loginWithResult(username: String, password: String, targetUrl: String? = null): Pair<Boolean, String?> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (!targetUrl.isNullOrBlank()) {
                    sessionManager.saveServerUrl(targetUrl)
                }
                val resp = getApi().login(com.cineclaw.tv.core.model.LoginRequest(username = username, password = password))
                if (resp.success && !resp.token.isNullOrBlank()) {
                    sessionManager.saveSession(resp.token, resp.username ?: username)
                    Pair(true, null)
                } else {
                    Pair(false, "Неверный логин или пароль")
                }
            } catch (e: retrofit2.HttpException) {
                val msg = if (e.code() == 401) "Неверный логин или пароль" else "Ошибка сервера (HTTP ${e.code()})"
                Pair(false, msg)
            } catch (e: Exception) {
                android.util.Log.e("CineClaw", "Login failed: ${e.message}", e)
                Pair(false, "Не удалось подключиться: ${e.localizedMessage ?: "сервер недоступен"}")
            }
        }

    suspend fun login(username: String, password: String): Boolean {
        return loginWithResult(username, password).first
    }

    suspend fun initPairing(code: String, deviceName: String = "Android TV", targetUrl: String? = null): Boolean =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (!targetUrl.isNullOrBlank()) {
                    sessionManager.saveServerUrl(targetUrl)
                }
                val resp = getApi().initPairing(com.cineclaw.tv.core.model.PairInitRequest(code = code, deviceName = deviceName))
                resp.success
            } catch (e: Exception) {
                android.util.Log.w("CineClaw", "initPairing failed: ${e.message}")
                false
            }
        }

    suspend fun checkPairingStatus(code: String, targetUrl: String? = null): com.cineclaw.tv.core.model.PairingStatusResponse? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (!targetUrl.isNullOrBlank()) {
                    sessionManager.saveServerUrl(targetUrl)
                }
                val resp = getApi().checkPairingStatus(code)
                if (resp.paired && !resp.token.isNullOrBlank()) {
                    sessionManager.saveSession(resp.token, resp.username ?: "admin")
                }
                resp
            } catch (e: Exception) {
                null
            }
        }
}
