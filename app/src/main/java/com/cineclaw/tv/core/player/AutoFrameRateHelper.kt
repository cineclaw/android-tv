package com.cineclaw.tv.core.player

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.Display
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
object AutoFrameRateHelper {
    private const val TAG = "AutoFrameRate"

    /**
     * Resolves the best matching Display.Mode for the given video frame rate and resolution.
     * Prefers modes with the same physical resolution as current display mode (e.g. 4K stays 4K)
     * and a refresh rate that matches the video fps or integer multiple (23.976 -> 23.976 / 24, 24 -> 24 / 48 / 120, etc).
     */
    fun matchDisplayMode(activity: Activity, format: Format?): Display.Mode? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        val targetFps = format?.frameRate ?: return null
        if (targetFps <= 0f) return null

        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        } ?: return null

        val currentMode = display.mode
        val supportedModes = display.supportedModes

        Log.i(
            TAG,
            "Matching AFR for video: ${format.width}x${format.height}@$targetFps fps. Current display: ${currentMode.physicalWidth}x${currentMode.physicalHeight}@${currentMode.refreshRate}Hz"
        )

        // 1. Prefer modes matching current display physical resolution (avoid unnecessary downscaling, let TV hardware scale)
        val sameResModes = supportedModes.filter {
            it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight
        }
        val candidates = if (sameResModes.isNotEmpty()) sameResModes else supportedModes.toList()

        // 2. Score candidate modes by refresh rate compatibility
        val bestMode = candidates.minByOrNull { mode ->
            rateDiffScore(mode.refreshRate, targetFps)
        }

        if (bestMode != null && rateDiffScore(bestMode.refreshRate, targetFps) < 1.0f) {
            return bestMode
        }

        return null
    }

    private fun rateDiffScore(refreshRate: Float, targetFps: Float): Float {
        // Direct exact match (e.g. 23.976 vs 23.976 or 24.0 vs 24.0)
        val directDiff = abs(refreshRate - targetFps)
        if (directDiff < 0.05f) return directDiff

        // Close match (e.g. 23.976 on 24.0Hz or vice versa)
        if (directDiff < 0.1f) return directDiff + 0.1f

        // Multiples: 24fps on 48Hz, 72Hz, 120Hz; 25fps on 50Hz, 100Hz; 30fps on 60Hz
        val ratio = refreshRate / targetFps
        val roundedRatio = ratio.roundToInt().toFloat()
        if (roundedRatio in 2.0f..5.0f) {
            val multDiff = abs(refreshRate - targetFps * roundedRatio)
            if (multDiff < 0.05f) {
                return 0.2f + (roundedRatio * 0.05f)
            }
        }

        // 59.94Hz for 23.976fps
        if (abs(targetFps - 23.976f) < 0.05f && abs(refreshRate - 59.94f) < 0.05f) {
            return 0.5f
        }

        return 1000f + directDiff
    }

    fun applyAutoFrameRate(activity: Activity, format: Format?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val matchingMode = matchDisplayMode(activity, format) ?: return

        val currentModeId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display?.mode?.modeId
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay?.mode?.modeId
        }

        if (currentModeId != matchingMode.modeId) {
            Log.i(
                TAG,
                "Switching display mode to ID ${matchingMode.modeId} (${matchingMode.physicalWidth}x${matchingMode.physicalHeight}@${matchingMode.refreshRate}Hz) for targetFps=${format?.frameRate}"
            )
            val params = activity.window.attributes
            params.preferredDisplayModeId = matchingMode.modeId
            activity.window.attributes = params
        }
    }

    fun restoreDefaultDisplayMode(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val params = activity.window.attributes
        if (params.preferredDisplayModeId != 0) {
            Log.i(TAG, "Restoring default display mode (preferredDisplayModeId = 0)")
            params.preferredDisplayModeId = 0
            activity.window.attributes = params
        }
    }
}
