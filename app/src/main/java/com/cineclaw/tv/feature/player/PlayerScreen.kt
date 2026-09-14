package com.cineclaw.tv.feature.player

import android.view.KeyEvent
import android.view.ViewGroup
import com.cineclaw.tv.MainActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.cineclaw.tv.BuildConfig
import com.cineclaw.tv.core.designsystem.*
import com.cineclaw.tv.core.model.EpisodeInfo
import com.cineclaw.tv.core.model.QualityGroup
import com.cineclaw.tv.core.model.TorrentRelease
import com.cineclaw.tv.core.player.CinemaPlayer
import com.cineclaw.tv.core.player.PlayerUiState
import com.cineclaw.tv.feature.details.QualityDialog
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    title: String,
    subtitle: String? = null,
    cinemaPlayer: CinemaPlayer,
    exoPlayer: ExoPlayer,
    qualityGroups: List<QualityGroup>,
    season: Int? = null,
    episode: Int? = null,
    seasons: List<Int> = emptyList(),
    episodes: List<EpisodeInfo> = emptyList(),
    mediaSourceId: String? = null,
    directStreamUrl: String? = null,
    baseUrl: String = "",
    onSelectQualityRelease: (TorrentRelease) -> Unit,
    onNextEpisodeClick: () -> Unit = {},
    onSelectEpisode: (season: Int, episode: Int) -> Unit = { _, _ -> },
    onFetchSeasonEpisodes: (season: Int) -> Unit = {},
    onToggleUltraWide: (() -> Unit)? = null,
    onClosePlayer: () -> Unit
) {
    val uiState by cinemaPlayer.uiState.collectAsState()
    var showControls by remember { mutableStateOf(true) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showTranscodeDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showEpisodesDialog by remember { mutableStateOf(false) }
    var isScrubberFocused by remember { mutableStateOf(false) }
    var seekBubbleText by remember { mutableStateOf<String?>(null) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val rootFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val scrubberFocusRequester = remember { FocusRequester() }

    // Intercept Back button: dismiss OSD if visible, else exit player
    BackHandler(enabled = true) {
        if (showControls) {
            showControls = false
        } else {
            onClosePlayer()
        }
    }

    // Auto-hide controls timer (15 seconds of inactivity, suspended when paused, scrubbing, or when any dialog is open)
    LaunchedEffect(showControls, uiState.isPlaying, lastInteraction, showQualityDialog, showAudioDialog, showSubtitleDialog, showEpisodesDialog, isScrubberFocused) {
        if (showControls && uiState.isPlaying && !showQualityDialog && !showAudioDialog && !showSubtitleDialog && !showEpisodesDialog && !isScrubberFocused) {
            delay(15000)
            showControls = false
        }
    }

    // Global hardware key event interceptor (catches TV remote clicks even if Compose focus was lost)
    DisposableEffect(showControls, isScrubberFocused, showQualityDialog, showAudioDialog, showSubtitleDialog, showEpisodesDialog) {
        val interceptor: (KeyEvent) -> Boolean = { keyEvent ->
            val code = keyEvent.keyCode
            val isDown = keyEvent.action == KeyEvent.ACTION_DOWN
            if (isDown) {
                lastInteraction = System.currentTimeMillis()
            }
            if (!showControls) {
                when (code) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (isDown) {
                            showControls = true
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (isDown) {
                            lastInteraction = System.currentTimeMillis()
                            cinemaPlayer.seekRelative(-10)
                            seekBubbleText = "-10с"
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (isDown) {
                            lastInteraction = System.currentTimeMillis()
                            cinemaPlayer.seekRelative(10)
                            seekBubbleText = "+10с"
                        }
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    KeyEvent.KEYCODE_MEDIA_PLAY,
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        if (isDown) {
                            lastInteraction = System.currentTimeMillis()
                            cinemaPlayer.togglePlayPause()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        if (isDown) {
                            onClosePlayer()
                        }
                        true
                    }
                    else -> false
                }
            } else {
                if (code == KeyEvent.KEYCODE_BACK) {
                    if (!showQualityDialog && !showAudioDialog && !showSubtitleDialog && !showEpisodesDialog) {
                        if (isDown) {
                            showControls = false
                        }
                        true
                    } else false
                } else false
            }
        }

        MainActivity.keyEventInterceptor = interceptor
        onDispose {
            if (MainActivity.keyEventInterceptor === interceptor) {
                MainActivity.keyEventInterceptor = null
            }
        }
    }

    // Focus management when toggling controls visibility
    LaunchedEffect(showControls) {
        if (showControls) {
            lastInteraction = System.currentTimeMillis()
            delay(50)
            try {
                playPauseFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusable()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        lastInteraction = System.currentTimeMillis()
                        showControls = !showControls
                    },
                    onDoubleTap = { offset ->
                        lastInteraction = System.currentTimeMillis()
                        val screenWidth = size.width
                        if (offset.x < screenWidth / 2) {
                            cinemaPlayer.seekRelative(-10)
                            seekBubbleText = "-10с"
                        } else {
                            cinemaPlayer.seekRelative(10)
                            seekBubbleText = "+10с"
                        }
                    }
                )
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    lastInteraction = System.currentTimeMillis()
                    if (!showControls) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                                showControls = true
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                cinemaPlayer.seekRelative(-10)
                                seekBubbleText = "-10с"
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                cinemaPlayer.seekRelative(10)
                                seekBubbleText = "+10с"
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                cinemaPlayer.togglePlayPause()
                                true
                            }
                            KeyEvent.KEYCODE_BACK -> {
                                onClosePlayer()
                                true
                            }
                            else -> false
                        }
                    } else false // Allow child focusables (scrubber, buttons) to process DPAD events!
                } else false
            }
    ) {
        // Native Media3 Surface
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    isFocusable = false
                    isFocusableInTouchMode = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    resizeMode = uiState.resizeMode
                }
            },
            update = { playerView ->
                playerView.resizeMode = uiState.resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        val isStreamStarting = (uiState.isBuffering || !uiState.isPlaying) &&
                uiState.currentPositionSeconds <= 1.0 &&
                uiState.errorMessage == null &&
                !uiState.isSwitchingQuality

        // Cinema Streaming Startup Preloader (displayed on cold start / peer discovery)
        if (isStreamStarting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD9000000)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 36.dp, vertical = 28.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = EmeraldPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Подготовка видеопотока...",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val streamDesc = if (!subtitle.isNullOrBlank()) "$title • $subtitle" else title
                            Text(
                                text = streamDesc,
                                color = EmeraldPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Поиск пиров в сети TorrServer и запуск воспроизведения",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else if (uiState.isBuffering && !uiState.isSwitchingQuality) {
            // Center Buffering Spinner (during regular playback after start)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = EmeraldPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Seamless Quality Switching Transition Overlay (Holds current frame & shows target tier)
        if (uiState.isSwitchingQuality) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, EmeraldPrimary, RoundedCornerShape(20.dp))
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = EmeraldPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = "Смена качества:", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                TvBadge(text = uiState.currentQualityTier.uppercase(), backgroundColor = EmeraldPrimary, textColor = ObsidianBackground)
                            }
                            Text(
                                text = "Возобновление с ${formatSeconds(uiState.currentPositionSeconds)}",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Error Overlay
        uiState.errorMessage?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(20.dp))
                        .padding(horizontal = 28.dp, vertical = 20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "Ошибка воспроизведения", color = Color(0xFFEF4444), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = error, color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }

        // Relative Seek Bubble Toast
        seekBubbleText?.let { bubble ->
            LaunchedEffect(bubble) {
                delay(1200)
                seekBubbleText = null
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(text = bubble, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Next Episode Auto-Countdown Banner
        if (uiState.showNextEpisodeBanner) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 48.dp, bottom = 120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, EmeraldPrimary, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column {
                        Text(text = "Следующая серия", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Нажмите для воспроизведения", color = TextSecondary, fontSize = 11.sp)
                    }
                    TvActionButton(
                        text = "Смотреть сейчас",
                        icon = Icons.Default.PlayArrow,
                        isPrimary = true,
                        onClick = onNextEpisodeClick
                    )
                }
            }
        }

        // Fullscreen 10-foot OSD Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xCC000000), Color.Transparent, Color(0xEE000000))
                        )
                    )
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp, vertical = 24.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Touch Close Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianCard)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                                .clickable { onClosePlayer() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(text = title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            if (!subtitle.isNullOrBlank()) {
                                Text(text = subtitle, color = EmeraldPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lynk & Co 900 Full-Width / Half-Screen Expansion Button
                        if (BuildConfig.IS_AUTOMOTIVE) {
                            val isExpanded = uiState.isUltraWideExpanded
                            TvActionButton(
                                text = if (isExpanded) "В пол-экрана" else "На весь экран",
                                icon = if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                isPrimary = isExpanded,
                                onClick = {
                                    lastInteraction = System.currentTimeMillis()
                                    cinemaPlayer.toggleUltraWideExpanded()
                                    onToggleUltraWide?.invoke()
                                }
                            )
                        }

                        val tier = uiState.currentQualityTier.lowercase()
                        val is4k = tier.contains("4k") || tier.contains("2160")
                        TvBadge(
                            text = uiState.currentQualityTier.uppercase(),
                            backgroundColor = if (is4k) AmberGlow else EmeraldGlow,
                            textColor = if (is4k) AmberUHD else EmeraldPrimary
                        )

                        // Transcoding Profile Badge / Quick Button
                        val profileText = when (uiState.currentTranscodeProfile) {
                            "direct" -> "DIRECT STREAM"
                            else -> "TRANSCODE ${uiState.currentTranscodeProfile.uppercase()}"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    lastInteraction = System.currentTimeMillis()
                                    showTranscodeDialog = true
                                }
                        ) {
                            TvBadge(
                                text = profileText,
                                backgroundColor = if (uiState.currentTranscodeProfile == "direct") EmeraldGlow else AmberGlow,
                                textColor = if (uiState.currentTranscodeProfile == "direct") EmeraldPrimary else AmberUHD
                            )
                        }
                    }
                }

                // Bottom Timeline & Actions Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp, vertical = 28.dp)
                        .align(Alignment.BottomCenter)
                        .focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Interactive TV Timeline Scrubber
                    TvTimelineScrubber(
                        currentPositionSeconds = uiState.currentPositionSeconds,
                        durationSeconds = uiState.durationSeconds,
                        onSeek = { targetSec ->
                            cinemaPlayer.seekTo(targetSec)
                        },
                        onFocusChange = { focused ->
                            isScrubberFocused = focused
                            lastInteraction = System.currentTimeMillis()
                        },
                        onInteraction = {
                            lastInteraction = System.currentTimeMillis()
                        },
                        onNavigateDown = {
                            try {
                                playPauseFocusRequester.requestFocus()
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier
                            .focusRequester(scrubberFocusRequester)
                            .focusProperties {
                                down = playPauseFocusRequester
                            }
                    )

                    val buttonUpModifier = Modifier.focusProperties {
                        up = scrubberFocusRequester
                    }

                    // Action Buttons Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                    lastInteraction = System.currentTimeMillis()
                                    try {
                                        scrubberFocusRequester.requestFocus()
                                    } catch (_: Exception) {}
                                    true
                                } else false
                            },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play / Pause Button (Default Focus)
                        TvActionButton(
                            text = if (uiState.isPlaying) "Пауза" else "Смотреть",
                            icon = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            isPrimary = true,
                            modifier = Modifier
                                .focusRequester(playPauseFocusRequester)
                                .then(buttonUpModifier),
                            onClick = {
                                lastInteraction = System.currentTimeMillis()
                                cinemaPlayer.togglePlayPause()
                            }
                        )

                        // Relative -10s
                        TvActionButton(
                            text = "-10с",
                            icon = Icons.Default.Replay10,
                            isPrimary = false,
                            modifier = buttonUpModifier,
                            onClick = {
                                lastInteraction = System.currentTimeMillis()
                                cinemaPlayer.seekRelative(-10)
                            }
                        )

                        // Relative +10s
                        TvActionButton(
                            text = "+10с",
                            icon = Icons.Default.Forward10,
                            isPrimary = false,
                            modifier = buttonUpModifier,
                            onClick = {
                                lastInteraction = System.currentTimeMillis()
                                cinemaPlayer.seekRelative(10)
                            }
                        )

                        // Quality Switcher
                        TvActionButton(
                            text = uiState.currentQualityTier.uppercase(),
                            icon = Icons.Default.HighQuality,
                            isPrimary = false,
                            modifier = buttonUpModifier,
                            onClick = {
                                lastInteraction = System.currentTimeMillis()
                                showQualityDialog = true
                            }
                        )

                        // Audio Track Switcher
                        val audioSummary = if (uiState.audioTracks.isNotEmpty()) {
                            val activeAudio = uiState.audioTracks.getOrNull(uiState.selectedAudioIndex)
                            val lang = activeAudio?.language?.let { formatLangCode(it) }
                            if (!lang.isNullOrBlank()) "Аудио ($lang)" else "Аудио (${uiState.audioTracks.size})"
                        } else {
                            "Аудио"
                        }
                        TvActionButton(
                            text = audioSummary,
                            icon = Icons.Default.VolumeUp,
                            isPrimary = false,
                            modifier = buttonUpModifier,
                            onClick = {
                                lastInteraction = System.currentTimeMillis()
                                showAudioDialog = true
                            }
                        )

                        // Subtitle Track Switcher
                        val subSummary = if (uiState.selectedSubtitleIndex >= 0) {
                            val activeSub = uiState.subtitleTracks.getOrNull(uiState.selectedSubtitleIndex)
                            val lang = activeSub?.language?.let { formatLangCode(it) } ?: "ВКЛ"
                            "Субтитры ($lang)"
                        } else {
                            "Субтитры"
                        }
                        TvActionButton(
                            text = subSummary,
                            icon = Icons.Default.Subtitles,
                            isPrimary = false,
                            modifier = buttonUpModifier,
                            onClick = {
                                lastInteraction = System.currentTimeMillis()
                                showSubtitleDialog = true
                            }
                        )

                        // If TV Series: Next Episode & Episodes Drawer
                        if (season != null && episode != null) {
                            TvActionButton(
                                text = "След. серия",
                                icon = Icons.Default.SkipNext,
                                isPrimary = false,
                                modifier = buttonUpModifier,
                                onClick = {
                                    lastInteraction = System.currentTimeMillis()
                                    onNextEpisodeClick()
                                }
                            )

                            TvActionButton(
                                text = "Серии",
                                icon = Icons.Default.Tv,
                                isPrimary = false,
                                modifier = buttonUpModifier,
                                onClick = {
                                    lastInteraction = System.currentTimeMillis()
                                    showEpisodesDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // Quality Switcher Dialog
        if (showQualityDialog) {
            QualityDialog(
                qualityGroups = qualityGroups,
                activeTier = uiState.currentQualityTier,
                onSelectTorrent = { rel ->
                    onSelectQualityRelease(rel)
                },
                onDismiss = { showQualityDialog = false }
            )
        }

        // Audio Tracks Dialog
        if (showAudioDialog) {
            AudioTracksDialog(
                tracks = uiState.audioTracks,
                selectedIndex = uiState.selectedAudioIndex,
                onSelectTrack = { index ->
                    cinemaPlayer.selectAudioTrack(index)
                },
                onDismiss = { showAudioDialog = false }
            )
        }

        // Subtitle Tracks Dialog
        if (showSubtitleDialog) {
            SubtitleTracksDialog(
                tracks = uiState.subtitleTracks,
                selectedIndex = uiState.selectedSubtitleIndex,
                onSelectTrack = { index ->
                    cinemaPlayer.selectSubtitleTrack(index)
                },
                onDismiss = { showSubtitleDialog = false }
            )
        }

        // Episodes Selector Dialog
        if (showEpisodesDialog) {
            EpisodesDialog(
                title = title,
                seasons = seasons,
                currentSeason = season,
                currentEpisode = episode,
                episodes = episodes,
                onSelectSeason = { newSeason ->
                    onFetchSeasonEpisodes(newSeason)
                },
                onSelectEpisode = { targetSeason, targetEpisode ->
                    showEpisodesDialog = false
                    showControls = false
                    onSelectEpisode(targetSeason, targetEpisode)
                },
                onDismiss = { showEpisodesDialog = false }
            )
        }
    }
}

/**
 * Focusable, D-pad scrubbable TV timeline.
 * LEFT / RIGHT: scrubs time ±15s (or ±30s for long media).
 * CENTER / ENTER or focus loss: commits seek.
 * BACK: resets pending scrub to current position.
 */
@Composable
private fun TvTimelineScrubber(
    currentPositionSeconds: Double,
    durationSeconds: Double,
    onSeek: (Double) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    onInteraction: () -> Unit = {},
    onNavigateDown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var scrubSeconds by remember { mutableDoubleStateOf(currentPositionSeconds) }
    var hasUserScrubbed by remember { mutableStateOf(false) }

    // Keep in sync with playback when not actively scrubbing
    LaunchedEffect(currentPositionSeconds, isFocused, hasUserScrubbed) {
        if (!isFocused || !hasUserScrubbed) {
            scrubSeconds = currentPositionSeconds
        }
    }

    val displaySeconds = if (isFocused && hasUserScrubbed) scrubSeconds else currentPositionSeconds
    val safeDuration = durationSeconds.coerceAtLeast(1.0)
    val progress = (displaySeconds / safeDuration).toFloat().coerceIn(0f, 1f)
    val deltaSeconds = if (hasUserScrubbed) (scrubSeconds - currentPositionSeconds).toInt() else 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (!isFocused && focusState.isFocused) {
                    scrubSeconds = currentPositionSeconds
                    hasUserScrubbed = false
                } else if (isFocused && !focusState.isFocused) {
                    // Commit scrubbed position ONLY when the user actively adjusted the time
                    if (hasUserScrubbed) {
                        onSeek(scrubSeconds)
                        hasUserScrubbed = false
                    }
                }
                isFocused = focusState.isFocused
                onFocusChange(focusState.isFocused)
            }
            .focusable()
            .onKeyEvent { keyEvent ->
                val code = keyEvent.nativeKeyEvent.keyCode
                if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        onInteraction()
                        if (hasUserScrubbed) {
                            onSeek(scrubSeconds)
                            hasUserScrubbed = false
                        }
                    }
                    true
                } else if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    onInteraction()
                    val step = if (durationSeconds > 5400) 30.0 else 15.0
                    when (code) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            hasUserScrubbed = true
                            scrubSeconds = (scrubSeconds - step).coerceAtLeast(0.0)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            hasUserScrubbed = true
                            scrubSeconds = (scrubSeconds + step).coerceAtMost(durationSeconds)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (hasUserScrubbed) {
                                onSeek(scrubSeconds)
                                hasUserScrubbed = false
                            }
                            onNavigateDown()
                            true
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            if (isFocused && hasUserScrubbed) {
                                hasUserScrubbed = false
                                scrubSeconds = currentPositionSeconds
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Track & Progress Bar with focus thumb
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Background inactive track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isFocused) 8.dp else 5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x44FFFFFF))
            )

            // Progress bar fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress.coerceIn(0.001f, 1f))
                    .height(if (isFocused) 8.dp else 5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(listOf(EmeraldDark, EmeraldPrimary))
                    )
            )

            // Scrubber thumb when focused
            if (isFocused) {
                Box(
                    modifier = Modifier.fillMaxWidth(fraction = progress.coerceIn(0.001f, 1f))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }
        }

        // Timecodes and scrubbing indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${formatSeconds(displaySeconds)} / ${formatSeconds(durationSeconds)}",
                    color = if (isFocused) EmeraldPrimary else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (isFocused && hasUserScrubbed && deltaSeconds != 0) {
                    val sign = if (deltaSeconds > 0) "+" else ""
                    TvBadge(
                        text = "$sign${deltaSeconds}с",
                        backgroundColor = if (deltaSeconds > 0) EmeraldGlow else AmberGlow,
                        textColor = if (deltaSeconds > 0) EmeraldPrimary else AmberUHD
                    )
                }
            }

            if (isFocused) {
                Text(
                    text = "◄ ► Перемотка   |   OK Применить",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun formatSeconds(sec: Double): String {
    val totalSec = sec.toInt()
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        String.format("%02d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}
