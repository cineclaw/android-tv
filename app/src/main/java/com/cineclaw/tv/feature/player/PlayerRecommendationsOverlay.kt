package com.cineclaw.tv.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.cineclaw.tv.core.designsystem.*
import com.cineclaw.tv.core.model.MediaItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerRecommendationsOverlay(
    movieTitle: String,
    recommendations: List<MediaItem>,
    onPlayRecommendation: (MediaItem) -> Unit,
    onAddToWatchlist: (MediaItem) -> Unit,
    onClose: () -> Unit
) {
    var selectedItem by remember(recommendations) { mutableStateOf(recommendations.firstOrNull()) }
    var addedToWatchlistMap by remember { mutableStateOf(mapOf<String, Boolean>()) }
    val playButtonFocusRequester = remember { FocusRequester() }

    var hasRequestedFocus by remember { mutableStateOf(false) }
    LaunchedEffect(selectedItem) {
        if (!hasRequestedFocus && selectedItem != null) {
            hasRequestedFocus = true
            for (i in 1..5) {
                delay(80)
                try {
                    playButtonFocusRequester.requestFocus()
                    break
                } catch (_: Exception) {}
            }
        }
    }

    BackHandler {
        onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        // Dynamic Backdrop with Dark Cinema Scrim
        val backdropUrl = selectedItem?.effectiveBackdrop ?: selectedItem?.effectivePoster
        if (!backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.25f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xE607090E),
                            Color(0xF207090E),
                            Color(0xFF07090E)
                        )
                    )
                )
        )

        // Main Content Container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Что посмотреть дальше?",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (movieTitle.isNotBlank()) "Рекомендации зрителей к фильму «$movieTitle»" else "Персональные рекомендации",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }

                TvActionButton(
                    text = "Выйти в меню",
                    icon = Icons.Default.Close,
                    isPrimary = false,
                    onClick = onClose
                )
            }

            // 2. Spotlight Section (Currently focused recommendation preview)
            val current = selectedItem
            if (current != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianSurface.copy(alpha = 0.85f))
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vertical Poster Cover
                    val posterUrl = current.effectivePoster
                    if (!posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = posterUrl,
                            contentDescription = current.displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(120.dp)
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                        )
                    }

                    // Metadata & Synopsis & Action Buttons
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Title
                        Text(
                            text = current.displayTitle,
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Badges Row (Year, Rating)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            current.year?.let { y ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ObsidianSurfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = y.toString(),
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            current.rating?.takeIf { it > 0.0 }?.let { r ->
                                TvRatingBadge(rating = r)
                            }
                        }

                        // Plot Overview
                        val overviewText = current.overview?.takeIf { it.isNotBlank() }
                        if (overviewText != null) {
                            Text(
                                text = overviewText,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 18.sp
                            )
                        }

                        // Action Buttons Row
                        val isAdded = addedToWatchlistMap[current.effectiveTconst] == true
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            TvActionButton(
                                text = "Смотреть",
                                icon = Icons.Default.PlayArrow,
                                isPrimary = true,
                                modifier = Modifier.focusRequester(playButtonFocusRequester),
                                onClick = { onPlayRecommendation(current) }
                            )

                            TvActionButton(
                                text = if (isAdded) "В списке" else "Буду смотреть",
                                icon = if (isAdded) Icons.Default.Check else Icons.Default.Bookmark,
                                isPrimary = false,
                                onClick = {
                                    addedToWatchlistMap = addedToWatchlistMap + (current.effectiveTconst to true)
                                    onAddToWatchlist(current)
                                }
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Horizontal Carousel Shelf
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Похожие фильмы (${recommendations.size})",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(end = 48.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    items(recommendations) { item ->
                        TvCinemaCard(
                            title = item.displayTitle,
                            posterUrl = item.effectivePoster,
                            year = item.year,
                            rating = item.rating,
                            surfaceModifier = Modifier.onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    selectedItem = item
                                }
                            },
                            onClick = { onPlayRecommendation(item) }
                        )
                    }
                }
            }
        }
    }
}
