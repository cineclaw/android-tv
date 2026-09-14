package com.cineclaw.tv.feature.details

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.tv.material3.*
import coil3.compose.AsyncImage
import com.cineclaw.tv.core.designsystem.*
import com.cineclaw.tv.core.model.*
import com.cineclaw.tv.feature.home.CardAction
import com.cineclaw.tv.feature.home.CardActionDialog

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailsScreen(
    media: MediaItem,
    qualityGroups: List<QualityGroup>,
    seasons: List<Int> = emptyList(),
    episodes: List<EpisodeInfo> = emptyList(),
    cast: List<CastMember> = emptyList(),
    crew: List<CrewMember> = emptyList(),
    videos: List<VideoItem> = emptyList(),
    backdrops: List<String> = emptyList(),
    criticSummary: CriticSummaryResponse? = null,
    isLoadingCritics: Boolean = false,
    isLoadingMetadata: Boolean = false,
    isInWatchlist: Boolean = false,
    seriesProgress: SeriesProgressResponse? = null,
    onPersonClick: (Long) -> Unit = {},
    onPlayClick: (Int?, Int?) -> Unit = { _, _ -> },
    onSelectQualityRelease: (TorrentRelease) -> Unit = {},
    onToggleWatchlist: () -> Unit = {},
    onSelectSeason: (Int) -> Unit = {},
    onMarkWatched: (MarkWatchedRequest) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var showQualityDialog by remember { mutableStateOf(false) }
    var selectedTrailer by remember { mutableStateOf<VideoItem?>(null) }
    var galleryViewerIndex by remember { mutableStateOf<Int?>(null) }
    var selectedSeason by remember { mutableStateOf(seasons.firstOrNull() ?: 1) }
    var selectedEpisodeForAction by remember { mutableStateOf<EpisodeInfo?>(null) }
    var pendingCatchUpEpisode by remember { mutableStateOf<EpisodeInfo?>(null) }
    val scrollState = rememberLazyListState()
    val playButtonFocusRequester = remember { FocusRequester() }

    fun hasUnwatchedPrior(targetSeason: Int, targetEpisode: Int): Boolean {
        if (seriesProgress == null) return false
        for (s in 1 until targetSeason) {
            val sSummary = seriesProgress.seasons[s.toString()]
            if (sSummary != null && !sSummary.isCompleted) return true
        }
        for (e in 1 until targetEpisode) {
            val key = "${targetSeason}_$e"
            val status = seriesProgress.episodes[key]
            if (status?.isCompleted != true) return true
        }
        return false
    }

    LaunchedEffect(seasons) {
        if (seasons.isNotEmpty() && selectedSeason !in seasons) {
            selectedSeason = seasons.first()
        }
    }

    BackHandler {
        if (selectedTrailer != null) {
            selectedTrailer = null
        } else if (galleryViewerIndex != null) {
            galleryViewerIndex = null
        } else if (showQualityDialog) {
            showQualityDialog = false
        } else if (selectedEpisodeForAction != null) {
            selectedEpisodeForAction = null
        } else if (pendingCatchUpEpisode != null) {
            pendingCatchUpEpisode = null
        } else {
            onBackClick()
        }
    }

    LaunchedEffect(Unit) {
        playButtonFocusRequester.requestFocus()
        scrollState.scrollToItem(0, 0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        // Wide Backdrop Header with multi-stop seamless gradient fade-out
        val backdrop = media.effectiveBackdrop ?: media.backdropUrl
        if (!backdrop.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
            ) {
                AsyncImage(
                    model = backdrop,
                    contentDescription = media.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Multi-stop vertical gradient fading completely into ObsidianBackground at bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.35f to ObsidianBackground.copy(alpha = 0.20f),
                                0.70f to ObsidianBackground.copy(alpha = 0.85f),
                                1.0f to ObsidianBackground
                            )
                        )
                )

                // Horizontal cinema vignette on the left
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0.0f to ObsidianBackground,
                                0.35f to ObsidianBackground.copy(alpha = 0.75f),
                                0.75f to Color.Transparent
                            )
                        )
                )

                // Top vignette
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(ObsidianBackground.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                )
            }
        }

        // Main Scrollable Content
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 40.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item {
                Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // Left Poster Card (pure display)
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(300.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(ObsidianSurface)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    ) {
                        if (!media.effectivePoster.isNullOrBlank()) {
                            AsyncImage(
                                model = media.effectivePoster,
                                contentDescription = media.displayTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Right Metadata & Actions
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Badges Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (media.rating != null && media.rating > 0) {
                                TvRatingBadge(rating = media.rating)
                            }
                            if (media.year != null && media.year > 0) {
                                TvBadge(text = media.year.toString())
                            }
                            formatRuntime(media.runtimeMinutes)?.let { rt ->
                                TvBadge(text = rt)
                            }
                            TvBadge(
                                text = if (media.type == "TvSeries" || media.isTv) "Сериал" else "Фильм",
                                backgroundColor = EmeraldGlow,
                                textColor = EmeraldPrimary
                            )
                            if (qualityGroups.any { it.tier == "4k" }) {
                                TvBadge(text = "4K UHD", backgroundColor = AmberGlow, textColor = AmberUHD)
                            }
                            if (isLoadingMetadata) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmeraldGlow)
                                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = EmeraldPrimary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "Загрузка данных...",
                                        color = EmeraldPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Main Title
                        Text(
                            text = media.displayTitle,
                            color = TextPrimary,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black
                        )

                        // Genres
                        if (media.genres.isNotEmpty()) {
                            Text(
                                text = media.genres.joinToString(" • "),
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        // Action Buttons Row
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TvActionButton(
                                text = "Смотреть",
                                icon = Icons.Default.PlayArrow,
                                isPrimary = true,
                                modifier = Modifier.focusRequester(playButtonFocusRequester),
                                onClick = { onPlayClick(if (media.isTv) selectedSeason else null, if (media.isTv) 1 else null) }
                            )

                            TvActionButton(
                                text = "Качество",
                                icon = Icons.Default.HighQuality,
                                isPrimary = false,
                                onClick = { showQualityDialog = true }
                            )

                            TvActionButton(
                                text = if (isInWatchlist) "В списке" else "Буду смотреть",
                                icon = if (isInWatchlist) Icons.Default.Check else Icons.Default.BookmarkBorder,
                                isPrimary = false,
                                onClick = { onToggleWatchlist() }
                            )
                        }

                        // Overview / Synopsis
                        if (!media.overview.isNullOrBlank()) {
                            Text(
                                text = media.overview,
                                color = TextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(0.9f)
                            )
                        } else if (isLoadingMetadata) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth(0.95f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.08f)))
                                Box(modifier = Modifier.fillMaxWidth(0.85f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.08f)))
                                Box(modifier = Modifier.fillMaxWidth(0.65f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.08f)))
                            }
                        }
                    }
                }
            }

            // AI Critics Consensus Section
            item {
                AiCriticsCard(
                    summary = criticSummary,
                    isLoading = isLoadingCritics,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // TV Series Seasons & Episodes Section
            if (media.isTv && seasons.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Сезоны и серии",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Mark current season chip
                            val currentSeasonSummary = seriesProgress?.seasons?.get(selectedSeason.toString())
                            val isCurrentSeasonCompleted = currentSeasonSummary?.isCompleted == true
                            var isMarkSeasonFocused by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .onFocusChanged { isMarkSeasonFocused = it.isFocused }
                                    .tvClickable {
                                        onMarkWatched(
                                            MarkWatchedRequest(
                                                imdbId = media.effectiveTconst,
                                                mode = "season",
                                                title = media.displayTitle,
                                                season = selectedSeason,
                                                completed = !isCurrentSeasonCompleted
                                            )
                                        )
                                    }
                                    .focusable()
                                    .scale(if (isMarkSeasonFocused) 1.05f else 1.0f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrentSeasonCompleted) EmeraldGlow else ObsidianSurface)
                                    .border(
                                        width = if (isMarkSeasonFocused) 2.dp else 1.dp,
                                        color = if (isMarkSeasonFocused) (if (isCurrentSeasonCompleted) Color.White else EmeraldPrimary) else ObsidianBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isCurrentSeasonCompleted) EmeraldPrimary else TextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (isCurrentSeasonCompleted) "Сезон просмотрен" else "Отметить сезон",
                                        color = if (isCurrentSeasonCompleted) EmeraldPrimary else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Seasons Tabs Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            items(seasons) { seasonNum ->
                                val isSeasonActive = seasonNum == selectedSeason
                                var isFocused by remember { mutableStateOf(false) }
                                val sSummary = seriesProgress?.seasons?.get(seasonNum.toString())
                                val seasonBadge = when {
                                    sSummary?.isCompleted == true -> " ✓"
                                    sSummary != null && sSummary.watchedEpisodes > 0 -> " (${sSummary.watchedEpisodes}/${sSummary.totalEpisodes})"
                                    else -> ""
                                }

                                Box(
                                    modifier = Modifier
                                        .onFocusChanged { isFocused = it.isFocused }
                                        .onKeyEvent { keyEvent ->
                                            val code = keyEvent.nativeKeyEvent.keyCode
                                            if (code == KeyEvent.KEYCODE_DPAD_CENTER ||
                                                code == KeyEvent.KEYCODE_ENTER ||
                                                code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                                                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                                    selectedSeason = seasonNum
                                                    onSelectSeason(seasonNum)
                                                }
                                                true
                                            } else false
                                        }
                                        .clickable {
                                            selectedSeason = seasonNum
                                            onSelectSeason(seasonNum)
                                        }
                                        .focusable()
                                        .scale(if (isFocused) 1.06f else 1.0f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSeasonActive) EmeraldPrimary else if (isFocused) ObsidianSurfaceVariant else ObsidianSurface
                                        )
                                        .border(
                                            width = if (isFocused) 2.dp else 1.dp,
                                            color = if (isFocused) (if (isSeasonActive) Color.White else EmeraldPrimary) else ObsidianBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Сезон $seasonNum$seasonBadge",
                                        color = if (isSeasonActive) ObsidianBackground else (if (sSummary?.isCompleted == true) EmeraldPrimary else TextPrimary),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Episodes Horizontal Carousel
                        val seasonEpisodes = episodes.filter { it.seasonNumber == selectedSeason }
                        if (seasonEpisodes.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                            ) {
                                items(seasonEpisodes) { ep ->
                                    val epKey = "${ep.seasonNumber}_${ep.episodeNumber}"
                                    val epStatus = seriesProgress?.episodes?.get(epKey)
                                    val isEpisodePlayed = epStatus?.isCompleted == true || ep.isPlayed

                                    TvWideCard(
                                        title = "${ep.episodeNumber}. ${ep.name}",
                                        subtitle = ep.airDate,
                                        backdropUrl = ep.effectiveStill ?: media.effectiveBackdrop,
                                        progressPercent = if (isEpisodePlayed) 100 else (ep.resumeSeconds / 3000.0 * 100).toInt(),
                                        isPlayed = isEpisodePlayed,
                                        onClick = {
                                            if (hasUnwatchedPrior(ep.seasonNumber, ep.episodeNumber)) {
                                                pendingCatchUpEpisode = ep
                                            } else {
                                                onPlayClick(ep.seasonNumber, ep.episodeNumber)
                                            }
                                        },
                                        onLongClick = {
                                            selectedEpisodeForAction = ep
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Trailers & Videos ("Трейлеры и видео")
            val youtubeVideos = videos.filter { it.isYouTube && it.key.isNotBlank() }
            if (youtubeVideos.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Трейлеры и видео",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(start = 0.dp, end = 32.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            items(youtubeVideos) { video ->
                                TrailerCard(
                                    video = video,
                                    onClick = { selectedTrailer = video }
                                )
                            }
                        }
                    }
                }
            }

            // Film Gallery ("Галерея кадров")
            if (backdrops.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Галерея кадров",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ObsidianSurface)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${backdrops.size}",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(start = 0.dp, end = 32.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            itemsIndexed(backdrops) { index, backdropPath ->
                                GalleryCard(
                                    backdropPath = backdropPath,
                                    onClick = { galleryViewerIndex = index }
                                )
                            }
                        }
                    }
                }
            }

            // Cast ("В главных ролях")
            if (cast.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "В главных ролях",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(start = 0.dp, end = 32.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            items(cast) { member ->
                                CastCard(
                                    member = member,
                                    onClick = { member.id?.let { onPersonClick(it) } }
                                )
                            }
                        }
                    }
                }
            }

            // Crew ("Создатели")
            if (crew.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Создатели",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(start = 0.dp, end = 32.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            items(crew) { member ->
                                CrewCard(
                                    member = member,
                                    onClick = { member.id?.let { onPersonClick(it) } }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quality Dialog
        if (showQualityDialog) {
            QualityDialog(
                qualityGroups = qualityGroups,
                onSelectTorrent = onSelectQualityRelease,
                onDismiss = { showQualityDialog = false }
            )
        }

        // Trailer Player Dialog
        selectedTrailer?.let { trailer ->
            TrailerPlayerDialog(
                video = trailer,
                onDismiss = { selectedTrailer = null }
            )
        }

        // Gallery Viewer Dialog
        galleryViewerIndex?.let { idx ->
            GalleryViewerDialog(
                backdrops = backdrops,
                initialIndex = idx,
                onDismiss = { galleryViewerIndex = null }
            )
        }

        // Episode Long Press Action Dialog
        selectedEpisodeForAction?.let { ep ->
            val epKey = "${ep.seasonNumber}_${ep.episodeNumber}"
            val isEpPlayed = seriesProgress?.episodes?.get(epKey)?.isCompleted == true || ep.isPlayed
            CardActionDialog(
                title = "${ep.episodeNumber}. ${ep.name}",
                subtitle = "Сезон ${ep.seasonNumber}${ep.airDate?.let { " • $it" } ?: ""}",
                imageUrl = ep.effectiveStill ?: media.effectiveBackdrop,
                actions = listOf(
                    CardAction(
                        title = if (isEpPlayed) "Снять отметку о просмотре" else "Отметить как просмотренную",
                        icon = Icons.Default.Check,
                        onClick = {
                            onMarkWatched(
                                MarkWatchedRequest(
                                    imdbId = media.effectiveTconst,
                                    mode = "episode",
                                    title = media.displayTitle,
                                    season = ep.seasonNumber,
                                    episode = ep.episodeNumber,
                                    completed = !isEpPlayed
                                )
                            )
                            selectedEpisodeForAction = null
                        }
                    ),
                    CardAction(
                        title = "Пометить все серии до этой как просмотренные",
                        icon = Icons.Default.Check,
                        onClick = {
                            onMarkWatched(
                                MarkWatchedRequest(
                                    imdbId = media.effectiveTconst,
                                    mode = "up_to",
                                    title = media.displayTitle,
                                    upToSeason = ep.seasonNumber,
                                    upToEpisode = ep.episodeNumber,
                                    completed = true
                                )
                            )
                            selectedEpisodeForAction = null
                        }
                    )
                ),
                onDismiss = { selectedEpisodeForAction = null }
            )
        }

        // Catch-Up Prompt Dialog
        pendingCatchUpEpisode?.let { ep ->
            CardActionDialog(
                title = "Предыдущие серии не просмотрены",
                subtitle = "Вы начинаете просмотр с ${ep.episodeNumber} серии (${ep.seasonNumber} сезон). Пометить все предыдущие серии как просмотренные?",
                imageUrl = ep.effectiveStill ?: media.effectiveBackdrop,
                actions = listOf(
                    CardAction(
                        title = "Пометить предыдущие и смотреть",
                        icon = Icons.Default.Check,
                        onClick = {
                            onMarkWatched(
                                MarkWatchedRequest(
                                    imdbId = media.effectiveTconst,
                                    mode = "up_to",
                                    title = media.displayTitle,
                                    upToSeason = ep.seasonNumber,
                                    upToEpisode = ep.episodeNumber,
                                    completed = true
                                )
                            )
                            val targetSeason = ep.seasonNumber
                            val targetEpisode = ep.episodeNumber
                            pendingCatchUpEpisode = null
                            onPlayClick(targetSeason, targetEpisode)
                        }
                    ),
                    CardAction(
                        title = "Только смотреть",
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            val targetSeason = ep.seasonNumber
                            val targetEpisode = ep.episodeNumber
                            pendingCatchUpEpisode = null
                            onPlayClick(targetSeason, targetEpisode)
                        }
                    )
                ),
                onDismiss = { pendingCatchUpEpisode = null }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CastCard(
    member: CastMember,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, EmeraldPrimary),
                shape = RoundedCornerShape(14.dp)
            ),
            border = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(14.dp)
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = ObsidianSurface,
            focusedContainerColor = ObsidianSurfaceVariant
        ),
        modifier = modifier.width(220.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(ObsidianBackground),
                contentAlignment = Alignment.Center
            ) {
                if (!member.effectiveAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = member.effectiveAvatar,
                        contentDescription = member.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Name & Role
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = member.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!member.character.isNullOrBlank()) {
                    Text(
                        text = member.character,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CrewCard(
    member: CrewMember,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, EmeraldPrimary),
                shape = RoundedCornerShape(14.dp)
            ),
            border = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(14.dp)
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = ObsidianSurface,
            focusedContainerColor = ObsidianSurfaceVariant
        ),
        modifier = modifier.width(220.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(ObsidianBackground),
                contentAlignment = Alignment.Center
            ) {
                if (!member.effectiveAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = member.effectiveAvatar,
                        contentDescription = member.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.MovieFilter,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = member.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val roleText = when (member.job ?: member.department) {
                    "Director" -> "Режиссёр"
                    "Writer", "Screenplay" -> "Сценарист"
                    "Creator" -> "Создатель"
                    "Producer", "Executive Producer" -> "Продюсер"
                    "Original Music Composer" -> "Композитор"
                    "Director of Photography" -> "Оператор"
                    else -> member.job ?: member.department ?: "Создатель"
                }
                Text(
                    text = roleText,
                    color = EmeraldPrimary.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TrailerCard(
    video: VideoItem,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(260.dp)
            .height(146.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .scale(if (isFocused) 1.06f else 1.0f)
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianSurface)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) EmeraldPrimary else ObsidianBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .focusable()
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = video.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient vignette overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.4f to Color.Black.copy(alpha = 0.25f),
                        1.0f to Color.Black.copy(alpha = 0.90f)
                    )
                )
        )

        // Center Play icon
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isFocused) EmeraldPrimary else Color(0xDDCC0000))
                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Воспроизвести",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        // Top Badges (Official / Type)
        if (video.official) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, AmberUHD.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Официальный",
                    color = AmberUHD,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Title
        Text(
            text = video.name,
            color = if (isFocused) Color.White else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun GalleryCard(
    backdropPath: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val imageUrl = resolveImageUrl(backdropPath, "https://image.tmdb.org/t/p/w780")

    Box(
        modifier = Modifier
            .width(260.dp)
            .height(146.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .scale(if (isFocused) 1.06f else 1.0f)
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianSurface)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) EmeraldPrimary else ObsidianBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .focusable()
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Кадр из фильма",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle gradient vignette on focus
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = if (isFocused) 0.05f else 0.30f)
                        )
                    )
                )
        )
    }
}
