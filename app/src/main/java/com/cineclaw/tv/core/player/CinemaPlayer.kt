package com.cineclaw.tv.core.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.*
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioOffloadSupport
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.mkv.MatroskaExtractor
import com.cineclaw.tv.BuildConfig
import androidx.media3.ui.AspectRatioFrameLayout
import com.cineclaw.tv.core.model.AudioTrack
import com.cineclaw.tv.core.model.SubtitleTrack
import com.cineclaw.tv.core.network.ApiClient
import com.cineclaw.tv.core.network.CineClawApi
import com.cineclaw.tv.core.model.WatchProgressRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isSwitchingQuality: Boolean = false,
    val currentQualityTier: String = "1080p",
    val currentTranscodeProfile: String = BuildConfig.DEFAULT_STREAM_PROFILE,
    val resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    val isUltraWideExpanded: Boolean = false,
    val isLocalOffline: Boolean = false,
    val currentPositionSeconds: Double = 0.0,
    val durationSeconds: Double = 0.0,
    val progressPercent: Int = 0,
    val audioTracks: List<AudioTrack> = emptyList(),
    val selectedAudioIndex: Int = 0,
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val selectedSubtitleIndex: Int = -1, // -1 is off
    val showNextEpisodeBanner: Boolean = false,
    val countdownNextEpisode: Int = 10,
    val errorMessage: String? = null
)

@OptIn(UnstableApi::class)
class CinemaPlayer(
    private val context: Context,
    private val apiClient: ApiClient,
    initialQualityTier: String = "1080p",
    private val audioPassthrough: Boolean = false
) {
    val exoPlayer: ExoPlayer
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val _uiState = MutableStateFlow(PlayerUiState(currentQualityTier = initialQualityTier))
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentTconst: String = ""
    private var currentTitle: String = ""
    private var currentMediaType: String = "movie"
    private var currentSeason: Int? = null
    private var currentEpisode: Int? = null
    private var pendingSeekSeconds: Double? = null
    private var initialServerAudioTracks: List<AudioTrack> = emptyList()
    private var initialServerSubtitleTracks: List<SubtitleTrack> = emptyList()
    private var hasAppliedDefaultAudio: Boolean = false
    private var errorRecoveryCount: Int = 0
    var onDecoderFallback: (() -> Unit)? = null

    init {
        val minBuffer = BuildConfig.BUFFER_MIN_MS
        val maxBuffer = BuildConfig.BUFFER_MAX_MS
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBuffer,
                maxBuffer,
                1_500,  // buffer for playback
                2_500   // buffer for playback after rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(15_000, true)
            .build()

        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                val downmixProcessor = createStereoDownmixingProcessor()
                val sinkBuilder = if (audioPassthrough) {
                    DefaultAudioSink.Builder(context)
                        .setAudioCapabilities(AudioCapabilities.getCapabilities(context))
                } else {
                    // Do not pass context to Builder so AudioCapabilitiesReceiver does NOT
                    // dynamically overwrite audio capabilities with TV HDMI passthrough caps!
                    DefaultAudioSink.Builder()
                        .setAudioCapabilities(
                            AudioCapabilities(intArrayOf(android.media.AudioFormat.ENCODING_PCM_16BIT), 2)
                        )
                }

                val baseSink = sinkBuilder
                    .setEnableFloatOutput(false)
                    .setEnableAudioTrackPlaybackParams(false)
                    .setAudioProcessors(arrayOf(downmixProcessor))
                    .setAudioOffloadSupportProvider { _, _ -> AudioOffloadSupport.DEFAULT_UNSUPPORTED }
                    .build()

                return if (audioPassthrough) baseSink else PcmOnlyAudioSink(baseSink)
            }

            override fun buildAudioRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: androidx.media3.exoplayer.mediacodec.MediaCodecSelector,
                enableDecoderFallback: Boolean,
                audioSink: AudioSink,
                eventHandler: android.os.Handler,
                eventListener: androidx.media3.exoplayer.audio.AudioRendererEventListener,
                out: java.util.ArrayList<Renderer>
            ) {
                out.add(
                    PcmMediaCodecAudioRenderer(
                        context = context,
                        codecAdapterFactory = codecAdapterFactory,
                        mediaCodecSelector = mediaCodecSelector,
                        enableDecoderFallback = enableDecoderFallback,
                        eventHandler = eventHandler,
                        eventListener = eventListener,
                        audioSink = audioSink,
                        audioPassthrough = audioPassthrough
                    )
                )
            }
        }.apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            setEnableDecoderFallback(true)
        }

        val okHttpClient = apiClient.getDirectOkHttpClient()
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        val extractorsFactory = ResilientExtractorsFactory()
        val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)
            .setDataSourceFactory(dataSourceFactory)
            .experimentalParseSubtitlesDuringExtraction(false)
            .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(5))

        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _uiState.value = _uiState.value.copy(isBuffering = true)
                    }
                    Player.STATE_READY -> {
                        errorRecoveryCount = 0
                        _uiState.value = _uiState.value.copy(
                            isBuffering = false,
                            isSwitchingQuality = false,
                            errorMessage = null,
                            durationSeconds = (exoPlayer.duration.coerceAtLeast(0) / 1000.0)
                        )
                        pendingSeekSeconds?.let { seekTarget ->
                            pendingSeekSeconds = null
                            val currentMs = exoPlayer.currentPosition
                            val targetMs = (seekTarget * 1000).toLong()
                            if (kotlin.math.abs(currentMs - targetMs) > 2000L) {
                                exoPlayer.seekTo(targetMs)
                            }
                            exoPlayer.play()
                        }
                    }
                    Player.STATE_ENDED -> {
                        _uiState.value = _uiState.value.copy(showNextEpisodeBanner = true)
                        syncWatchProgress(isCompleted = true)
                    }
                    Player.STATE_IDLE -> Unit
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }

            override fun onTracksChanged(tracks: Tracks) {
                val parsedAudio = mutableListOf<AudioTrack>()
                var audioIdx = 0
                var activeAudioIdx = -1
                val parsedSubs = mutableListOf<SubtitleTrack>()
                var subIdx = 0
                var activeSubIdx = -1

                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val rawLang = format.language?.lowercase() ?: "rus"
                            val isSelected = group.isTrackSelected(i)
                            if (isSelected) {
                                activeAudioIdx = audioIdx
                            }

                            // Clean label: prefer container format.label if present and non-generic
                            var label = format.label?.takeIf { it.isNotBlank() }
                            val serverTrack = initialServerAudioTracks.getOrNull(audioIdx)
                                ?: _uiState.value.audioTracks.getOrNull(audioIdx)

                            val isGenericLabel = label.isNullOrBlank() ||
                                label.equals("rus", ignoreCase = true) ||
                                label.equals("ru", ignoreCase = true) ||
                                label.equals("russian", ignoreCase = true) ||
                                label.equals("русский", ignoreCase = true) ||
                                label.equals("eng", ignoreCase = true) ||
                                label.equals("en", ignoreCase = true) ||
                                label.equals("english", ignoreCase = true)

                            if (isGenericLabel && serverTrack != null && serverTrack.title.isNotBlank() && !serverTrack.title.startsWith("Аудио #")) {
                                label = serverTrack.title
                            } else if (label.isNullOrBlank() && serverTrack != null && serverTrack.title.isNotBlank()) {
                                label = serverTrack.title
                            }

                            if (label.isNullOrBlank()) {
                                label = if (rawLang.startsWith("ru")) "Русский" else "Аудиодорожка ${audioIdx + 1}"
                            }

                            val resolvedLang = if ((rawLang == "und" || rawLang.isBlank()) && serverTrack != null && serverTrack.language.isNotBlank()) {
                                serverTrack.language
                            } else {
                                rawLang
                            }

                            val codecName = when {
                                format.sampleMimeType?.contains("eac3", ignoreCase = true) == true -> "E-AC3"
                                format.sampleMimeType?.contains("ac3", ignoreCase = true) == true -> "AC3"
                                format.sampleMimeType?.contains("dts", ignoreCase = true) == true -> "DTS"
                                format.sampleMimeType?.contains("truehd", ignoreCase = true) == true -> "TrueHD"
                                format.sampleMimeType?.contains("aac", ignoreCase = true) == true -> "AAC"
                                format.sampleMimeType?.contains("flac", ignoreCase = true) == true -> "FLAC"
                                format.sampleMimeType?.contains("opus", ignoreCase = true) == true -> "Opus"
                                format.sampleMimeType?.contains("vorbis", ignoreCase = true) == true -> "Vorbis"
                                format.sampleMimeType?.contains("mpeg-L2", ignoreCase = true) == true ||
                                        format.sampleMimeType?.contains("mpegl2", ignoreCase = true) == true -> "MP2"
                                format.sampleMimeType?.contains("mpeg", ignoreCase = true) == true ||
                                        format.sampleMimeType?.contains("mp3", ignoreCase = true) == true -> "MPEG"
                                serverTrack != null && serverTrack.codec.isNotBlank() -> serverTrack.codec
                                else -> "Audio"
                            }

                            val resolvedChannels = if (format.channelCount > 0) format.channelCount else (serverTrack?.channels ?: 2)

                            parsedAudio.add(
                                AudioTrack(
                                    index = audioIdx,
                                    title = label,
                                    language = resolvedLang,
                                    codec = codecName,
                                    channels = resolvedChannels,
                                    isDefault = isSelected
                                )
                            )
                            audioIdx++
                        }
                    } else if (group.type == C.TRACK_TYPE_TEXT) {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val rawLang = format.language?.lowercase() ?: "rus"
                            val isSelected = group.isTrackSelected(i)
                            if (isSelected) {
                                activeSubIdx = subIdx
                            }

                            var label = format.label?.takeIf { it.isNotBlank() }
                            val serverSub = initialServerSubtitleTracks.getOrNull(subIdx)
                                ?: _uiState.value.subtitleTracks.getOrNull(subIdx)

                            val isGenericSubLabel = label.isNullOrBlank() ||
                                label.equals("rus", ignoreCase = true) ||
                                label.equals("ru", ignoreCase = true) ||
                                label.equals("russian", ignoreCase = true) ||
                                label.equals("русские", ignoreCase = true) ||
                                label.equals("eng", ignoreCase = true) ||
                                label.equals("en", ignoreCase = true) ||
                                label.equals("english", ignoreCase = true)

                            if (isGenericSubLabel && serverSub != null && serverSub.title.isNotBlank() && !serverSub.title.startsWith("Субтитры #")) {
                                label = serverSub.title
                            } else if (label.isNullOrBlank() && serverSub != null && serverSub.title.isNotBlank()) {
                                label = serverSub.title
                            }

                            if (label.isNullOrBlank()) {
                                label = if (rawLang.startsWith("ru")) "Русские субтитры" else "Субтитры ${subIdx + 1}"
                            }

                            val resolvedSubLang = if ((rawLang == "und" || rawLang.isBlank()) && serverSub != null && serverSub.language.isNotBlank()) {
                                serverSub.language
                            } else {
                                rawLang
                            }

                            parsedSubs.add(
                                SubtitleTrack(
                                    index = subIdx,
                                    title = label,
                                    language = resolvedSubLang,
                                    codec = format.sampleMimeType ?: "text",
                                    isDefault = isSelected
                                )
                            )
                            subIdx++
                        }
                    }
                }

                Log.d(
                    "CineClaw",
                    "onTracksChanged: audioTracks=${parsedAudio.size}, activeIdx=$activeAudioIdx, subs=${parsedSubs.size}, activeSub=$activeSubIdx"
                )

                _uiState.value = _uiState.value.copy(
                    audioTracks = if (parsedAudio.isNotEmpty()) parsedAudio else _uiState.value.audioTracks,
                    selectedAudioIndex = if (activeAudioIdx >= 0) activeAudioIdx else _uiState.value.selectedAudioIndex,
                    subtitleTracks = if (parsedSubs.isNotEmpty()) parsedSubs else _uiState.value.subtitleTracks,
                    selectedSubtitleIndex = if (activeSubIdx >= 0) activeSubIdx else _uiState.value.selectedSubtitleIndex
                )

                val defaultServerTrack = initialServerAudioTracks.firstOrNull { it.isDefault }
                if (!hasAppliedDefaultAudio && defaultServerTrack != null && parsedAudio.isNotEmpty()) {
                    hasAppliedDefaultAudio = true
                    val targetIdx = parsedAudio.indexOfFirst {
                        it.title.equals(defaultServerTrack.title, ignoreCase = true) ||
                        (it.language.equals(defaultServerTrack.language, ignoreCase = true) && it.index == defaultServerTrack.index)
                    }.takeIf { it >= 0 } ?: defaultServerTrack.index.coerceIn(0, parsedAudio.size - 1)

                    if (targetIdx != activeAudioIdx) {
                        Log.d("CineClaw", "Auto-switching to user's preferred audio track: index $targetIdx (${defaultServerTrack.title})")
                        selectAudioTrack(targetIdx, persist = false)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.w("CineClaw", "onPlayerError caught error: ${error.errorCodeName} (${error.errorCode})", error)
                val isDecoderError = error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
                if (isDecoderError) {
                    if (onDecoderFallback != null) {
                        Log.i("CineClaw", "Hardware decoder failed; triggering auto-fallback to alternative release...")
                        onDecoderFallback?.invoke()
                        return
                    }
                    val rootCause = error.cause?.message ?: error.localizedMessage ?: ""
                    val msg = if (rootCause.contains("c2.mtk.avc.decoder") || rootCause.contains("avc1.F4") || rootCause.contains("NO_EXCEEDS_CAPABILITIES")) {
                        "Формат видео (H.264 10-bit) не поддерживается ТВ. Выберите раздачу H.265 (HEVC) или обычный 8-bit AVC."
                    } else {
                        "Ошибка декодера ТВ: ${error.localizedMessage}. Выберите другую раздачу в меню качества."
                    }
                    _uiState.value = _uiState.value.copy(
                        isBuffering = false,
                        isSwitchingQuality = false,
                        errorMessage = msg
                    )
                    return
                }

                if (errorRecoveryCount < 3) {
                    errorRecoveryCount++
                    val currentPos = exoPlayer.currentPosition
                    Log.i("CineClaw", "Auto-recovering playback error ($errorRecoveryCount/3) by skipping past corrupted packet at $currentPos")
                    if (currentPos > 0) {
                        exoPlayer.seekTo(currentPos + 1000L)
                    }
                    exoPlayer.prepare()
                    exoPlayer.play()
                    return
                }
                _uiState.value = _uiState.value.copy(
                    isBuffering = false,
                    isSwitchingQuality = false,
                    errorMessage = "Ошибка потока: ${error.localizedMessage}"
                )
            }
        })
    }

    fun setErrorMessage(msg: String) {
        _uiState.value = _uiState.value.copy(
            isBuffering = false,
            isSwitchingQuality = false,
            errorMessage = msg
        )
    }

    fun prepare(
        streamUrl: String,
        tconst: String,
        title: String = "",
        season: Int? = null,
        episode: Int? = null,
        initialPositionMs: Long = 0L,
        audioTracks: List<AudioTrack> = emptyList(),
        subtitleTracks: List<SubtitleTrack> = emptyList(),
        audioPassthrough: Boolean = false,
        quality: String = "1080p"
    ) {
        currentTconst = tconst
        currentTitle = title
        currentMediaType = if (season != null && season > 0) "tv" else "movie"
        currentSeason = season
        currentEpisode = episode
        initialServerAudioTracks = audioTracks
        initialServerSubtitleTracks = subtitleTracks
        hasAppliedDefaultAudio = false
        if (initialPositionMs > 0) {
            pendingSeekSeconds = initialPositionMs / 1000.0
        }

        val trackParams = exoPlayer.trackSelectionParameters.buildUpon()
            .setAudioOffloadPreferences(
                TrackSelectionParameters.AudioOffloadPreferences.Builder()
                    .setAudioOffloadMode(TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED)
                    .setIsGaplessSupportRequired(false)
                    .build()
            )
            .setPreferredAudioLanguage("rus")
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        exoPlayer.trackSelectionParameters = trackParams

        val mediaItem = MediaItem.fromUri(streamUrl)
        exoPlayer.setMediaItem(mediaItem)
        if (initialPositionMs > 0) {
            exoPlayer.seekTo(initialPositionMs)
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        val initialSelectedAudioIdx = audioTracks.indexOfFirst { it.isDefault }
            .takeIf { it >= 0 }
            ?: audioTracks.indexOfFirst {
                it.language.startsWith("ru", ignoreCase = true)
            }.takeIf { it >= 0 } ?: 0

        _uiState.value = _uiState.value.copy(
            audioTracks = audioTracks,
            selectedAudioIndex = initialSelectedAudioIdx,
            subtitleTracks = subtitleTracks,
            currentQualityTier = quality
        )

        startProgressTicker()
    }

    fun initializePlayer(
        streamUrl: String,
        tconst: String,
        initialPositionSeconds: Double = 0.0,
        audioTracks: List<AudioTrack> = emptyList(),
        subtitleTracks: List<SubtitleTrack> = emptyList(),
        season: Int? = null,
        episode: Int? = null,
        audioPassthrough: Boolean = false
    ): ExoPlayer {
        prepare(
            streamUrl = streamUrl,
            tconst = tconst,
            season = season,
            episode = episode,
            initialPositionMs = (initialPositionSeconds * 1000).toLong(),
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            audioPassthrough = audioPassthrough
        )
        return exoPlayer
    }

    /**
     * Seamless Quality Switch: preserves exact seconds, holds frame, updates stream without 0:00 reset.
     */
    fun switchQuality(
        newStreamUrl: String,
        tierName: String,
        audioTracks: List<AudioTrack> = emptyList(),
        subtitleTracks: List<SubtitleTrack> = emptyList()
    ) {
        val player = exoPlayer
        val currentSec = player.currentPosition / 1000.0

        pendingSeekSeconds = currentSec
        if (audioTracks.isNotEmpty()) initialServerAudioTracks = audioTracks
        if (subtitleTracks.isNotEmpty()) initialServerSubtitleTracks = subtitleTracks

        _uiState.value = _uiState.value.copy(
            isSwitchingQuality = true,
            currentQualityTier = tierName,
            currentPositionSeconds = currentSec,
            audioTracks = if (audioTracks.isNotEmpty()) audioTracks else _uiState.value.audioTracks,
            subtitleTracks = if (subtitleTracks.isNotEmpty()) subtitleTracks else _uiState.value.subtitleTracks
        )

        // Instant progress sync to DB before switch
        syncWatchProgress(isCompleted = false)

        val newMediaItem = MediaItem.fromUri(newStreamUrl)
        player.setMediaItem(newMediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    /**
     * Switch between transcode profiles (direct, 1080p, 720p, 480p, 360p) on the fly
     * seamlessly retaining exact playback timestamp.
     */
    fun switchTranscodeProfile(
        profile: String,
        directStreamUrl: String,
        baseUrl: String,
        mediaSourceId: String?,
        fileIdx: Int = 0
    ) {
        val targetUrl = if (profile == "direct" || mediaSourceId.isNullOrBlank()) {
            directStreamUrl
        } else {
            val cleanBase = baseUrl.trimEnd('/')
            val activeAudio = _uiState.value.selectedAudioIndex
            val audioParam = if (activeAudio >= 0) "&audio=$activeAudio" else ""
            val startSec = (_uiState.value.currentPositionSeconds).toInt()
            "$cleanBase/api/stream/transcode/$mediaSourceId/master.m3u8?profile=$profile&file_idx=$fileIdx$audioParam&start=$startSec"
        }

        _uiState.value = _uiState.value.copy(currentTranscodeProfile = profile)
        switchQuality(
            newStreamUrl = targetUrl,
            tierName = if (profile == "direct") _uiState.value.currentQualityTier else profile.uppercase(),
            audioTracks = _uiState.value.audioTracks,
            subtitleTracks = _uiState.value.subtitleTracks
        )
    }

    fun toggleResizeMode() {
        val nextMode = when (_uiState.value.resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM // 21:9 CinemaScope
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        _uiState.value = _uiState.value.copy(resizeMode = nextMode)
    }

    fun setResizeMode(mode: Int) {
        _uiState.value = _uiState.value.copy(resizeMode = mode)
    }

    fun toggleUltraWideExpanded() {
        _uiState.value = _uiState.value.copy(isUltraWideExpanded = !_uiState.value.isUltraWideExpanded)
    }

    fun setUltraWideExpanded(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isUltraWideExpanded = expanded)
    }

    fun seekRelative(secondsDelta: Int) {
        val player = exoPlayer
        val dur = player.duration
        val maxMs = if (dur > 0L) dur else Long.MAX_VALUE
        val targetMs = (player.currentPosition + secondsDelta * 1000L).coerceIn(0L, maxMs)
        player.seekTo(targetMs)
        _uiState.value = _uiState.value.copy(currentPositionSeconds = targetMs / 1000.0)
    }

    fun togglePlayPause() {
        val player = exoPlayer
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionSeconds: Double) {
        val player = exoPlayer
        val rawMs = (positionSeconds * 1000).toLong()
        val dur = player.duration
        val maxMs = if (dur > 0L) dur else Long.MAX_VALUE
        val posMs = rawMs.coerceIn(0L, maxMs)
        player.seekTo(posMs)
        _uiState.value = _uiState.value.copy(currentPositionSeconds = posMs / 1000.0)
    }

    fun selectAudioTrack(targetIndex: Int, persist: Boolean = true) {
        val audioPairs = mutableListOf<Pair<Tracks.Group, Int>>()
        for (group in exoPlayer.currentTracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (trackIndex in 0 until group.length) {
                    audioPairs.add(group to trackIndex)
                }
            }
        }
        val target = audioPairs.getOrNull(targetIndex) ?: return
        _uiState.value = _uiState.value.copy(selectedAudioIndex = targetIndex)

        try {
            val isSubtitleDisabled = _uiState.value.selectedSubtitleIndex < 0
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setAudioOffloadPreferences(
                    TrackSelectionParameters.AudioOffloadPreferences.Builder()
                        .setAudioOffloadMode(TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED)
                        .setIsGaplessSupportRequired(false)
                        .build()
                )
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                .setOverrideForType(TrackSelectionOverride(target.first.mediaTrackGroup, target.second))
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, isSubtitleDisabled)
                .build()
            Log.d(
                "CineClaw",
                "selectAudioTrack: switched to index $targetIndex (${target.first.getTrackFormat(target.second).label})"
            )
        } catch (e: Exception) {
            Log.e("CineClaw", "selectAudioTrack error switching to index $targetIndex", e)
        }

        if (persist) {
            val trackTitle = _uiState.value.audioTracks.getOrNull(targetIndex)?.title ?: ""
            if (currentTconst.isNotBlank() && trackTitle.isNotBlank()) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        apiClient.getApi().setAudioPreference(
                            com.cineclaw.tv.core.model.AudioPreferenceRequest(
                                imdbId = currentTconst,
                                audioTitle = trackTitle,
                                audioIndex = targetIndex
                            )
                        )
                        Log.d("CineClaw", "Persisted audio preference: $trackTitle (idx $targetIndex) for $currentTconst")
                    } catch (e: Exception) {
                        Log.w("CineClaw", "Failed to persist audio preference", e)
                    }
                }
            }
        }
    }

    fun selectSubtitleTrack(targetIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedSubtitleIndex = targetIndex)

        if (targetIndex < 0) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .build()
            Log.d("CineClaw", "selectSubtitleTrack: disabled subtitles")
            return
        }

        val textPairs = mutableListOf<Pair<Tracks.Group, Int>>()
        for (group in exoPlayer.currentTracks.groups) {
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (trackIndex in 0 until group.length) {
                    textPairs.add(group to trackIndex)
                }
            }
        }
        val target = textPairs.getOrNull(targetIndex) ?: return
        try {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setOverrideForType(TrackSelectionOverride(target.first.mediaTrackGroup, target.second))
                .build()
            Log.d(
                "CineClaw",
                "selectSubtitleTrack: switched to index $targetIndex (${target.first.getTrackFormat(target.second).label})"
            )
        } catch (e: Exception) {
            Log.e("CineClaw", "selectSubtitleTrack error switching to index $targetIndex", e)
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            while (isActive) {
                delay(1000)
                val player = exoPlayer
                if (player.playbackState == Player.STATE_READY) {
                    val posSec = player.currentPosition / 1000.0
                    val durSec = (player.duration.coerceAtLeast(1) / 1000.0)
                    val percent = ((posSec / durSec) * 100).toInt().coerceIn(0, 100)

                    _uiState.value = _uiState.value.copy(
                        currentPositionSeconds = posSec,
                        durationSeconds = durSec,
                        progressPercent = percent
                    )

                    // Sync to SQLite every 5 seconds
                    if (posSec.toInt() % 5 == 0 && posSec > 1.0) {
                        syncWatchProgress(isCompleted = percent >= 92)
                    }

                    // Next episode auto-countdown near the end of series
                    if (percent >= 96 && !_uiState.value.showNextEpisodeBanner && currentEpisode != null) {
                        _uiState.value = _uiState.value.copy(showNextEpisodeBanner = true)
                    }
                }
            }
        }
    }

    private fun syncWatchProgress(isCompleted: Boolean) {
        val player = exoPlayer
        val posSec = player.currentPosition / 1000.0
        val durSec = (player.duration.coerceAtLeast(1) / 1000.0)
        val percent = if (durSec > 0) (posSec / durSec) * 100.0 else 0.0

        coroutineScope.launch(Dispatchers.IO) {
            try {
                apiClient.getApi().reportProgress(
                    WatchProgressRequest(
                        tconst = currentTconst,
                        title = currentTitle,
                        mediaType = currentMediaType,
                        season = currentSeason ?: 0,
                        episode = currentEpisode ?: 0,
                        positionSeconds = posSec,
                        durationSeconds = durSec,
                        playbackPercent = percent,
                        isCompleted = isCompleted
                    )
                )
            } catch (e: Exception) {
                // Silently ignore network hiccup during streaming
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        syncWatchProgress(isCompleted = false)
        exoPlayer.release()
        coroutineScope.cancel()
    }

    companion object {
        fun createStereoDownmixingProcessor(): ChannelMixingAudioProcessor {
            val processor = ChannelMixingAudioProcessor()
            for (ch in 1..16) {
                processor.putChannelMixingMatrix(createDownmixMatrix(ch))
            }
            return processor
        }

        private fun createDownmixMatrix(channels: Int): ChannelMixingMatrix {
            val coefficients = FloatArray(channels * 2)
            when (channels) {
                1 -> { // Mono
                    coefficients[0] = 1.0f
                    coefficients[1] = 1.0f
                }
                2 -> { // Stereo pass-through (identity)
                    coefficients[0] = 1.0f; coefficients[1] = 0.0f
                    coefficients[2] = 0.0f; coefficients[3] = 1.0f
                }
                3 -> { // 3.0: L, R, C
                    coefficients[0] = 1.0f; coefficients[1] = 0.0f
                    coefficients[2] = 0.0f; coefficients[3] = 1.0f
                    coefficients[4] = 0.7071f; coefficients[5] = 0.7071f
                }
                4 -> { // Quad: L, R, Ls, Rs
                    coefficients[0] = 1.0f; coefficients[1] = 0.0f
                    coefficients[2] = 0.0f; coefficients[3] = 1.0f
                    coefficients[4] = 0.7071f; coefficients[5] = 0.0f
                    coefficients[6] = 0.0f; coefficients[7] = 0.7071f
                }
                5 -> { // 5.0: L, R, C, Ls, Rs
                    coefficients[0] = 1.0f; coefficients[1] = 0.0f
                    coefficients[2] = 0.0f; coefficients[3] = 1.0f
                    coefficients[4] = 0.7071f; coefficients[5] = 0.7071f
                    coefficients[6] = 0.7071f; coefficients[7] = 0.0f
                    coefficients[8] = 0.0f; coefficients[9] = 0.7071f
                }
                6 -> { // 5.1: L, R, C, LFE, Ls, Rs (ITU-R BS.775)
                    coefficients[0] = 1.0f; coefficients[1] = 0.0f
                    coefficients[2] = 0.0f; coefficients[3] = 1.0f
                    coefficients[4] = 0.7071f; coefficients[5] = 0.7071f
                    coefficients[6] = 0.0f; coefficients[7] = 0.0f // LFE omitted for TV speakers protection
                    coefficients[8] = 0.7071f; coefficients[9] = 0.0f
                    coefficients[10] = 0.0f; coefficients[11] = 0.7071f
                }
                7 -> { // 6.1: L, R, C, LFE, Cs, Ls, Rs
                    coefficients[0] = 1.0f; coefficients[1] = 0.0f
                    coefficients[2] = 0.0f; coefficients[3] = 1.0f
                    coefficients[4] = 0.7071f; coefficients[5] = 0.7071f
                    coefficients[6] = 0.0f; coefficients[7] = 0.0f
                    coefficients[8] = 0.5f; coefficients[9] = 0.5f
                    coefficients[10] = 0.7071f; coefficients[11] = 0.0f
                    coefficients[12] = 0.0f; coefficients[13] = 0.7071f
                }
                8 -> { // 7.1: L, R, C, LFE, Ls, Rs, Rls, Rrs
                    coefficients[0] = 1.0f; coefficients[1] = 0.0f
                    coefficients[2] = 0.0f; coefficients[3] = 1.0f
                    coefficients[4] = 0.7071f; coefficients[5] = 0.7071f
                    coefficients[6] = 0.0f; coefficients[7] = 0.0f
                    coefficients[8] = 0.7071f; coefficients[9] = 0.0f
                    coefficients[10] = 0.0f; coefficients[11] = 0.7071f
                    coefficients[12] = 0.7071f; coefficients[13] = 0.0f
                    coefficients[14] = 0.0f; coefficients[15] = 0.7071f
                }
                else -> {
                    // Fallback for N channels: alternate channels across L and R
                    for (i in 0 until channels) {
                        if (i % 2 == 0) {
                            coefficients[i * 2] = 0.7071f
                        } else {
                            coefficients[i * 2 + 1] = 0.7071f
                        }
                    }
                }
            }
            return ChannelMixingMatrix(channels, 2, coefficients)
        }
    }
}

@OptIn(UnstableApi::class)
class PcmOnlyAudioSink(private val delegate: AudioSink) : ForwardingAudioSink(delegate) {
    override fun supportsFormat(format: Format): Boolean {
        val mime = format.sampleMimeType ?: return false
        return MimeTypes.AUDIO_RAW == mime
    }

    override fun getFormatSupport(format: Format): Int {
        val mime = format.sampleMimeType ?: return AudioSink.SINK_FORMAT_UNSUPPORTED
        if (MimeTypes.AUDIO_RAW == mime) {
            return delegate.getFormatSupport(format)
        }
        return AudioSink.SINK_FORMAT_UNSUPPORTED
    }

    override fun getFormatOffloadSupport(format: Format): AudioOffloadSupport {
        return AudioOffloadSupport.DEFAULT_UNSUPPORTED
    }
}

@OptIn(UnstableApi::class)
class PcmMediaCodecAudioRenderer(
    context: Context,
    codecAdapterFactory: androidx.media3.exoplayer.mediacodec.MediaCodecAdapter.Factory,
    mediaCodecSelector: androidx.media3.exoplayer.mediacodec.MediaCodecSelector,
    enableDecoderFallback: Boolean,
    eventHandler: android.os.Handler,
    eventListener: androidx.media3.exoplayer.audio.AudioRendererEventListener,
    audioSink: AudioSink,
    private val audioPassthrough: Boolean
) : MediaCodecAudioRenderer(
    context,
    codecAdapterFactory,
    mediaCodecSelector,
    enableDecoderFallback,
    eventHandler,
    eventListener,
    audioSink
) {
    override fun shouldUseBypass(format: Format): Boolean {
        if (!audioPassthrough) return false
        return super.shouldUseBypass(format)
    }
}

@OptIn(UnstableApi::class)
class ResilientExtractorsFactory : ExtractorsFactory {
    private val defaultFactory = DefaultExtractorsFactory()
        .setMatroskaExtractorFlags(
            MatroskaExtractor.FLAG_EMIT_RAW_SUBTITLE_DATA
        )
        .setTextTrackTranscodingEnabled(false)

    override fun createExtractors(): Array<Extractor> = defaultFactory.createExtractors()

    override fun createExtractors(uri: Uri, responseHeaders: Map<String, List<String>>): Array<Extractor> =
        defaultFactory.createExtractors(uri, responseHeaders)
}
