package com.cineclaw.tv

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.cineclaw.tv.core.network.ApiClient
import com.cineclaw.tv.core.network.SessionManager

class CineClawApp : Application(), SingletonImageLoader.Factory {
    lateinit var sessionManager: SessionManager
        private set
    lateinit var apiClient: ApiClient
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionManager = SessionManager(this)
        apiClient = ApiClient(sessionManager)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { apiClient.getDirectOkHttpClient() }))
            }
            .build()
    }

    companion object {
        lateinit var instance: CineClawApp
            private set
    }
}
