package com.cineclaw.tv.feature.details

import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.cineclaw.tv.CineClawApp
import com.cineclaw.tv.core.designsystem.*
import com.cineclaw.tv.core.model.VideoItem
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay

@Composable
fun TrailerPlayerDialog(
    video: VideoItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var ytPlayerRef by remember { mutableStateOf<YouTubePlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentSecond by remember { mutableFloatStateOf(0f) }
    var videoDuration by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var resolvedStreamUrl by remember { mutableStateOf(video.streamUrl ?: "") }
    var showHud by remember { mutableStateOf(true) }
    var hudInteractionCount by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    // ExoPlayer instance for Kinopoisk HLS playback
    val exoPlayer = remember(video.isKinopoisk) {
        if (video.isKinopoisk) {
            ExoPlayer.Builder(context).build().apply {
                playWhenReady = true
            }
        } else null
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    val dur = exoPlayer?.duration ?: 0L
                    if (dur > 0) videoDuration = (dur / 1000f)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                errorMessage = "Ошибка воспроизведения: ${error.message ?: error.errorCodeName}"
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
        }
    }

    // Kinopoisk stream URL resolution
    LaunchedEffect(video) {
        if (video.isKinopoisk && resolvedStreamUrl.isBlank()) {
            isBuffering = true
            try {
                val api = CineClawApp.instance.apiClient.getApi()
                val resp = api.getTrailerStream(video.key)
                if (resp.streamUrl.isNotBlank()) {
                    resolvedStreamUrl = resp.streamUrl
                } else {
                    errorMessage = "Не удалось получить поток трейлера Kinopoisk"
                    isBuffering = false
                }
            } catch (e: Exception) {
                android.util.Log.e("CineClaw", "getTrailerStream failed", e)
                errorMessage = "Ошибка загрузки трейлера (${e.message ?: "Сеть"})"
                isBuffering = false
            }
        }
    }

    // Attach stream to ExoPlayer once resolved
    LaunchedEffect(exoPlayer, resolvedStreamUrl) {
        if (exoPlayer != null && resolvedStreamUrl.isNotBlank()) {
            val mediaItem = MediaItem.fromUri(resolvedStreamUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    // Continuous progress tracking for ExoPlayer
    LaunchedEffect(exoPlayer, isPlaying) {
        while (exoPlayer != null && isPlaying) {
            currentSecond = (exoPlayer.currentPosition / 1000f).coerceAtLeast(0f)
            val dur = exoPlayer.duration
            if (dur > 0) videoDuration = (dur / 1000f).coerceAtLeast(0f)
            delay(250)
        }
    }

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(hudInteractionCount, isPlaying) {
        if (isPlaying) {
            showHud = true
            delay(4000)
            showHud = false
        } else {
            showHud = true
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(200f)
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                val code = keyEvent.nativeKeyEvent.keyCode
                val action = keyEvent.nativeKeyEvent.action

                if (action == KeyEvent.ACTION_DOWN) {
                    hudInteractionCount++
                    when (code) {
                        KeyEvent.KEYCODE_BACK -> {
                            onDismiss()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (video.isKinopoisk) {
                                if (isPlaying) exoPlayer?.pause() else exoPlayer?.play()
                            } else {
                                if (isPlaying) ytPlayerRef?.pause() else ytPlayerRef?.play()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            if (video.isKinopoisk) exoPlayer?.play() else ytPlayerRef?.play()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            if (video.isKinopoisk) exoPlayer?.pause() else ytPlayerRef?.pause()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            if (video.isKinopoisk) {
                                exoPlayer?.let { player ->
                                    val newPos = (player.currentPosition - 10000L).coerceAtLeast(0L)
                                    player.seekTo(newPos)
                                    currentSecond = newPos / 1000f
                                }
                            } else {
                                val newTime = (currentSecond - 10f).coerceAtLeast(0f)
                                ytPlayerRef?.seekTo(newTime)
                                currentSecond = newTime
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT,
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            if (video.isKinopoisk) {
                                exoPlayer?.let { player ->
                                    val maxPos = if (player.duration > 0) player.duration else Long.MAX_VALUE
                                    val newPos = (player.currentPosition + 10000L).coerceAtMost(maxPos)
                                    player.seekTo(newPos)
                                    currentSecond = newPos / 1000f
                                }
                            } else {
                                val maxTime = if (videoDuration > 0f) videoDuration else Float.MAX_VALUE
                                val newTime = (currentSecond + 10f).coerceAtMost(maxTime)
                                ytPlayerRef?.seekTo(newTime)
                                currentSecond = newTime
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            showHud = !showHud
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Player Surface: ExoPlayer for Kinopoisk HLS, YouTubePlayerView for YouTube
        if (video.isKinopoisk) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    try {
                        CookieManager.getInstance().setAcceptCookie(true)
                    } catch (_: Exception) {}

                    YouTubePlayerView(ctx).apply {
                        enableAutomaticInitialization = false
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        fun enableThirdParty(view: View) {
                            if (view is WebView) {
                                try {
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(view, true)
                                } catch (_: Exception) {}
                            } else if (view is ViewGroup) {
                                for (i in 0 until view.childCount) {
                                    enableThirdParty(view.getChildAt(i))
                                }
                            }
                        }
                        enableThirdParty(this)

                        val options = IFramePlayerOptions.Builder(ctx)
                            .controls(0)
                            .autoplay(1)
                            .rel(0)
                            .ivLoadPolicy(3)
                            .ccLoadPolicy(0)
                            .fullscreen(0)
                            .build()

                        initialize(
                            object : AbstractYouTubePlayerListener() {
                                override fun onReady(youTubePlayer: YouTubePlayer) {
                                    android.util.Log.i("CineClawYT", "YouTubePlayer ready, playing: ${video.key}")
                                    ytPlayerRef = youTubePlayer
                                    youTubePlayer.play()
                                }

                                override fun onStateChange(
                                    youTubePlayer: YouTubePlayer,
                                    state: PlayerConstants.PlayerState
                                ) {
                                    android.util.Log.i("CineClawYT", "PlayerState: $state")
                                    isPlaying = state == PlayerConstants.PlayerState.PLAYING
                                    isBuffering = state == PlayerConstants.PlayerState.BUFFERING
                                }

                                override fun onCurrentSecond(
                                    youTubePlayer: YouTubePlayer,
                                    second: Float
                                ) {
                                    currentSecond = second
                                }

                                override fun onVideoDuration(
                                    youTubePlayer: YouTubePlayer,
                                    duration: Float
                                ) {
                                    videoDuration = duration
                                }

                                override fun onError(
                                    youTubePlayer: YouTubePlayer,
                                    error: PlayerConstants.PlayerError
                                ) {
                                    android.util.Log.e("CineClawYT", "YouTube error: $error")
                                    isBuffering = false
                                    errorMessage = when (error) {
                                        PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER ->
                                            "Воспроизведение запрещено правообладателем для встраивания"
                                        PlayerConstants.PlayerError.VIDEO_NOT_FOUND ->
                                            "Видео не найдено"
                                        else ->
                                            "Ошибка загрузки видео ($error)"
                                    }
                                }
                            },
                            false,
                            options,
                            video.key
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { view ->
                    try {
                        view.release()
                    } catch (_: Exception) {}
                }
            )
        }

        // Buffering indicator
        if (isBuffering && errorMessage == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = if (video.isKinopoisk) Color(0xFFFF7733) else EmeraldPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Error Banner
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.90f))
                    .zIndex(50f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 48.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Open in YouTube app button (if YouTube trailer)
                        if (video.isYouTube) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE50914))
                                    .clickable {
                                        try {
                                            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${video.key}"))
                                            context.startActivity(appIntent)
                                        } catch (_: Exception) {
                                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.key}"))
                                            context.startActivity(webIntent)
                                        }
                                    }
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Открыть в приложении YouTube",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Close button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianSurface)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .clickable { onDismiss() }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Закрыть",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Auto-hiding Cinema TV HUD Overlay
        AnimatedVisibility(
            visible = showHud && errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 40.dp, vertical = 28.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Source badge + Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (video.isKinopoisk) Color(0xCCFF5500) else Color(0xFFE50914))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (video.isKinopoisk) "Кинопоиск • Дубляж" else "YouTube",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = video.name,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Right: Dismiss button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianSurface.copy(alpha = 0.8f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .clickable { onDismiss() }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Закрыть",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Center: Paused indicator
                if (!isPlaying && !isBuffering) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            .padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Пауза",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Bottom HUD: Progress bar + Remote Hints Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .padding(horizontal = 40.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Progress Bar & Timecodes
                    if (videoDuration > 0f) {
                        val progress = (currentSecond / videoDuration).coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (video.isKinopoisk) Color(0xFFFF7733) else EmeraldPrimary,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTime(currentSecond.toLong()),
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = formatTime(videoDuration.toLong()),
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Remote Hints
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrailerHintBadge("OK: Пауза / Пуск")
                        Spacer(modifier = Modifier.width(20.dp))
                        TrailerHintBadge("◄ / ► 10 сек")
                        Spacer(modifier = Modifier.width(20.dp))
                        TrailerHintBadge("Назад: Выход")
                    }
                }
            }
        }
    }
}

@Composable
private fun TrailerHintBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

