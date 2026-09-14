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
                val requestBuilder = chain.request().newBuilder()
                kotlinx.coroutines.runBlocking {
                    sessionManager.getAuthTokenSync()?.let { token ->
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }
                }
                chain.proceed(requestBuilder.build())
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

    suspend fun login(username: String, password: String): Boolean {
        return try {
            val resp = getApi().login(com.cineclaw.tv.core.model.LoginRequest(username = username, password = password))
            if (resp.success && !resp.token.isNullOrBlank()) {
                sessionManager.saveSession(resp.token, resp.username ?: username)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("CineClaw", "Login failed: ${e.message}", e)
            false
        }
    }

    fun getDirectOkHttpClient(): OkHttpClient = okHttpClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
}
