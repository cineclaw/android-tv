package com.cineclaw.tv.core.network

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class ApiClient(private val sessionManager: SessionManager) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val requestBuilder = request.newBuilder()
                sessionManager.getAuthTokenDirect()?.let { token ->
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                val response = chain.proceed(requestBuilder.build())
                val path = request.url.encodedPath
                if (response.code == 401 && !path.contains("/api/auth/login") && !path.contains("/api/auth/pair")) {
                    sessionManager.clearSessionAsync()
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
            val client = okHttpClient.newBuilder()
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

    suspend fun checkPairingStatus(code: String): com.cineclaw.tv.core.model.PairingStatusResponse? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val resp = getApi().checkPairingStatus(code)
                if (resp.paired && !resp.token.isNullOrBlank()) {
                    sessionManager.saveSession(resp.token, resp.username ?: "admin")
                }
                resp
            } catch (e: Exception) {
                null
            }
        }

    fun getDirectOkHttpClient(): OkHttpClient = okHttpClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
}
