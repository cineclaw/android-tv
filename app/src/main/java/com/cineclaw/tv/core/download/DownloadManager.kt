package com.cineclaw.tv.core.download

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap

enum class DownloadStatus {
    QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED
}

@Serializable
data class OfflineMetadata(
    val id: String,
    val tconst: String,
    val title: String,
    val subtitle: String? = null,
    val posterUrl: String? = null,
    val year: Int? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val mediaType: String = "movie",
    val quality: String = "1080p",
    val fileSize: Long = 0L,
    val durationSeconds: Double = 0.0,
    val addedAtMs: Long = System.currentTimeMillis()
)

data class DownloadItem(
    val id: String,
    val metadata: OfflineMetadata,
    val streamUrl: String,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val localFile: File? = null,
    val errorMessage: String? = null
) {
    val progressPercent: Int
        get() = if (totalBytes > 0) ((downloadedBytes.toDouble() / totalBytes) * 100).toInt() else 0

    val downloadedMb: Double
        get() = downloadedBytes / (1024.0 * 1024.0)

    val totalMb: Double
        get() = totalBytes / (1024.0 * 1024.0)
}

data class OfflineMediaItem(
    val file: File,
    val metadata: OfflineMetadata
)

class DownloadManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "DownloadManager"
        private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

        @Volatile
        private var instance: DownloadManager? = null

        fun getInstance(context: Context): DownloadManager {
            return instance ?: synchronized(this) {
                instance ?: DownloadManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val storageManager = StorageManager(context)
    private val httpClient = OkHttpClient.Builder().build()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val activeDownloads = ConcurrentHashMap<String, DownloadItem>()
    private val downloadJobs = ConcurrentHashMap<String, Job>()

    private val _downloadsState = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloadsState: StateFlow<List<DownloadItem>> = _downloadsState.asStateFlow()

    private val _offlineMediaState = MutableStateFlow<List<OfflineMediaItem>>(emptyList())
    val offlineMediaState: StateFlow<List<OfflineMediaItem>> = _offlineMediaState.asStateFlow()

    init {
        refreshCompletedDownloads()
    }

    fun getStorageManager(): StorageManager = storageManager

    /**
     * Enqueues and starts a video download into local/USB storage.
     */
    fun enqueueDownload(
        id: String,
        metadata: OfflineMetadata,
        streamUrl: String
    ) {
        val targetDir = storageManager.getActiveDownloadDirectory()
        val safeFileName = sanitizeFileName("${metadata.title}_${metadata.id}.mkv")
        val localFile = File(targetDir, safeFileName)

        val item = DownloadItem(
            id = id,
            metadata = metadata,
            streamUrl = streamUrl,
            status = DownloadStatus.QUEUED,
            localFile = localFile
        )

        activeDownloads[id] = item
        updateState()

        val job = scope.launch {
            runDownload(id, streamUrl, localFile, metadata)
        }
        downloadJobs[id] = job
    }

    private suspend fun runDownload(
        id: String,
        streamUrl: String,
        targetFile: File,
        metadata: OfflineMetadata
    ) {
        val currentItem = activeDownloads[id] ?: return
        activeDownloads[id] = currentItem.copy(status = DownloadStatus.DOWNLOADING)
        updateState()

        val existingBytes = if (targetFile.exists()) targetFile.length() else 0L

        try {
            val requestBuilder = Request.Builder().url(streamUrl)
            if (existingBytes > 0) {
                requestBuilder.addHeader("Range", "bytes=$existingBytes-")
                Log.i(TAG, "Resuming download $id from byte $existingBytes")
            }

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    throw Exception("HTTP ${response.code}: ${response.message}")
                }

                val body = response.body ?: throw Exception("Empty response body")
                val contentLength = body.contentLength()
                val totalBytes = if (contentLength > 0) existingBytes + contentLength else metadata.fileSize

                activeDownloads[id] = activeDownloads[id]?.copy(
                    totalBytes = totalBytes,
                    downloadedBytes = existingBytes
                ) ?: return@use
                updateState()

                val outputStream = if (existingBytes > 0) {
                    RandomAccessFile(targetFile, "rw").apply { seek(existingBytes) }
                } else {
                    RandomAccessFile(targetFile, "rw")
                }

                body.byteStream().use { inputStream ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var totalRead = existingBytes
                    var lastUpdateMs = System.currentTimeMillis()

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        currentCoroutineContext().ensureActive()
                        outputStream.write(buffer, 0, read)
                        totalRead += read

                        val now = System.currentTimeMillis()
                        if (now - lastUpdateMs > 800) {
                            lastUpdateMs = now
                            activeDownloads[id] = activeDownloads[id]?.copy(
                                downloadedBytes = totalRead,
                                status = DownloadStatus.DOWNLOADING
                            ) ?: break
                            updateState()
                        }
                    }
                }
                outputStream.close()

                // Save companion metadata JSON
                val metaFile = File(targetFile.parentFile, "${targetFile.name}.meta.json")
                val finalMeta = metadata.copy(fileSize = targetFile.length())
                metaFile.writeText(json.encodeToString(OfflineMetadata.serializer(), finalMeta))

                activeDownloads[id] = activeDownloads[id]?.copy(
                    downloadedBytes = targetFile.length(),
                    totalBytes = targetFile.length(),
                    status = DownloadStatus.COMPLETED
                ) ?: return

                updateState()
                refreshCompletedDownloads()
                Log.i(TAG, "Successfully downloaded $id to ${targetFile.absolutePath}")
            }
        } catch (ce: CancellationException) {
            Log.i(TAG, "Download $id paused/cancelled")
            activeDownloads[id]?.let {
                activeDownloads[id] = it.copy(status = DownloadStatus.PAUSED)
            }
            updateState()
        } catch (e: Exception) {
            Log.e(TAG, "Download error for $id: ${e.message}", e)
            activeDownloads[id]?.let {
                activeDownloads[id] = it.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "Ошибка скачивания"
                )
            }
            updateState()
        }
    }

    fun pauseDownload(id: String) {
        downloadJobs[id]?.cancel()
        downloadJobs.remove(id)
        activeDownloads[id]?.let {
            activeDownloads[id] = it.copy(status = DownloadStatus.PAUSED)
            updateState()
        }
    }

    fun resumeDownload(id: String) {
        val item = activeDownloads[id] ?: return
        if (item.status == DownloadStatus.DOWNLOADING) return
        val localFile = item.localFile ?: return

        val job = scope.launch {
            runDownload(id, item.streamUrl, localFile, item.metadata)
        }
        downloadJobs[id] = job
    }

    fun cancelDownload(id: String) {
        downloadJobs[id]?.cancel()
        downloadJobs.remove(id)
        activeDownloads[id]?.localFile?.let {
            if (it.exists()) it.delete()
            val metaFile = File(it.parentFile, "${it.name}.meta.json")
            if (metaFile.exists()) metaFile.delete()
        }
        activeDownloads.remove(id)
        updateState()
        refreshCompletedDownloads()
    }

    fun deleteOfflineMedia(file: File) {
        if (file.exists()) file.delete()
        val metaFile = File(file.parentFile, "${file.name}.meta.json")
        if (metaFile.exists()) metaFile.delete()
        refreshCompletedDownloads()
    }

    /**
     * Scans all connected storage volumes for completed .mkv downloads with metadata.
     */
    fun refreshCompletedDownloads() {
        val list = mutableListOf<OfflineMediaItem>()
        val volumes = storageManager.getAvailableStorageVolumes()

        for (volume in volumes) {
            val dir = volume.directory
            if (!dir.exists() || !dir.isDirectory) continue

            val videoFiles = dir.listFiles { f -> f.extension.equals("mkv", ignoreCase = true) || f.extension.equals("mp4", ignoreCase = true) } ?: emptyArray()

            for (vf in videoFiles) {
                val metaFile = File(dir, "${vf.name}.meta.json")
                val meta = if (metaFile.exists()) {
                    try {
                        json.decodeFromString(OfflineMetadata.serializer(), metaFile.readText())
                    } catch (e: Exception) {
                        fallbackMetadata(vf)
                    }
                } else {
                    fallbackMetadata(vf)
                }
                list.add(OfflineMediaItem(file = vf, metadata = meta))
            }
        }

        _offlineMediaState.value = list.sortedByDescending { it.metadata.addedAtMs }
    }

    private fun fallbackMetadata(file: File): OfflineMetadata {
        return OfflineMetadata(
            id = file.nameWithoutExtension,
            tconst = file.nameWithoutExtension,
            title = file.nameWithoutExtension.replace('_', ' '),
            fileSize = file.length()
        )
    }

    private fun updateState() {
        _downloadsState.value = activeDownloads.values.toList()
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(100)
    }
}
