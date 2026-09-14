package com.cineclaw.tv.core.download

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.io.File

data class StorageVolumeInfo(
    val directory: File,
    val name: String,
    val isRemovable: Boolean,
    val freeSpaceBytes: Long,
    val totalSpaceBytes: Long
) {
    val freeSpaceGb: Double get() = freeSpaceBytes / (1024.0 * 1024.0 * 1024.0)
    val totalSpaceGb: Double get() = totalSpaceBytes / (1024.0 * 1024.0 * 1024.0)
    val usedSpaceGb: Double get() = (totalSpaceBytes - freeSpaceBytes) / (1024.0 * 1024.0 * 1024.0)
    val usedPercent: Int get() = if (totalSpaceBytes > 0) (((totalSpaceBytes - freeSpaceBytes).toDouble() / totalSpaceBytes) * 100).toInt() else 0
}

class StorageManager(private val context: Context) {

    companion object {
        private const val TAG = "StorageManager"
        private const val DOWNLOADS_DIR_NAME = "downloads"
    }

    /**
     * Discovers all available storage locations (internal memory and external USB flash / SSD drives).
     * Uses context.getExternalFilesDirs(null) to bypass Storage Access Framework (SAF) restrictions
     * common on Chinese automotive ROMs (Flyme Auto / LYNK OS).
     */
    fun getAvailableStorageVolumes(): List<StorageVolumeInfo> {
        val volumes = mutableListOf<StorageVolumeInfo>()
        val externalDirs = context.getExternalFilesDirs(null) ?: emptyArray()

        for ((index, dir) in externalDirs.withIndex()) {
            if (dir == null) continue
            try {
                val isRemovable = Environment.isExternalStorageRemovable(dir)
                val stat = StatFs(dir.path)
                val blockSize = stat.blockSizeLong
                val availableBlocks = stat.availableBlocksLong
                val totalBlocks = stat.blockCountLong

                val freeBytes = availableBlocks * blockSize
                val totalBytes = totalBlocks * blockSize

                val displayName = if (isRemovable || index > 0) {
                    val usbName = dir.path.split("/").find { it.contains("-") && it.length == 9 }
                    if (usbName != null) "USB-накопитель ($usbName)" else "Внешний USB-диск"
                } else {
                    "Внутренняя память автомобиля"
                }

                val targetDownloadsDir = File(dir, DOWNLOADS_DIR_NAME).apply { if (!exists()) mkdirs() }

                volumes.add(
                    StorageVolumeInfo(
                        directory = targetDownloadsDir,
                        name = displayName,
                        isRemovable = isRemovable || index > 0,
                        freeSpaceBytes = freeBytes,
                        totalSpaceBytes = totalBytes
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query storage stats for ${dir.path}: ${e.message}")
            }
        }

        // Fallback to internal app files directory if no external dirs found
        if (volumes.isEmpty()) {
            val internal = File(context.filesDir, DOWNLOADS_DIR_NAME).apply { if (!exists()) mkdirs() }
            val stat = StatFs(internal.path)
            volumes.add(
                StorageVolumeInfo(
                    directory = internal,
                    name = "Внутренняя память",
                    isRemovable = false,
                    freeSpaceBytes = stat.availableBlocksLong * stat.blockSizeLong,
                    totalSpaceBytes = stat.blockCountLong * stat.blockSizeLong
                )
            )
        }

        return volumes
    }

    /**
     * Returns the primary target directory for downloading media.
     * Automatically prioritizes external removable USB drive/SSD if plugged in,
     * otherwise falls back to internal storage.
     */
    fun getActiveDownloadDirectory(): File {
        val volumes = getAvailableStorageVolumes()
        val usbVolume = volumes.firstOrNull { it.isRemovable && it.freeSpaceGb >= 1.0 }
        if (usbVolume != null) {
            Log.i(TAG, "Selected USB volume for downloads: ${usbVolume.directory.path} (${String.format("%.1f", usbVolume.freeSpaceGb)} GB free)")
            return usbVolume.directory
        }
        val bestVolume = volumes.maxByOrNull { it.freeSpaceBytes } ?: volumes.first()
        Log.i(TAG, "Selected primary volume for downloads: ${bestVolume.directory.path} (${String.format("%.1f", bestVolume.freeSpaceGb)} GB free)")
        return bestVolume.directory
    }
}
