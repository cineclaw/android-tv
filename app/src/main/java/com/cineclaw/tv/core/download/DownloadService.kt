package com.cineclaw.tv.core.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class DownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "cineclaw_downloads_channel"
        const val NOTIFICATION_ID = 9118
        const val ACTION_START = "com.cineclaw.tv.action.START_DOWNLOAD_SERVICE"
        const val ACTION_STOP = "com.cineclaw.tv.action.STOP_DOWNLOAD_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val notification = buildNotification("Загрузка в фоне", "Сохранение фильмов на накопитель")
                startForeground(NOTIFICATION_ID, notification)
                startMonitoring()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        val dm = DownloadManager.getInstance(this)

        monitorJob = serviceScope.launch {
            dm.downloadsState.collect { list ->
                val active = list.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
                if (active.isEmpty()) {
                    // All finished or paused
                    val completed = list.any { it.status == DownloadStatus.COMPLETED }
                    val notification = buildNotification(
                        "Загрузка завершена",
                        if (completed) "Фильмы успешно сохранены" else "Нет активных загрузок"
                    )
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                } else {
                    val current = active.first()
                    val notification = buildNotification(
                        "Скачивание: ${current.metadata.title}",
                        "${current.progressPercent}% • ${String.format("%.1f", current.downloadedMb)} / ${String.format("%.1f", current.totalMb)} MB"
                    )
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Загрузки CineClaw",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновая загрузка фильмов на USB-накопитель"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
