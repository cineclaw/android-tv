package com.cineclaw.tv.feature.player

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.cineclaw.tv.core.designsystem.*
import com.cineclaw.tv.core.model.EpisodeInfo
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodesDialog(
    title: String,
    seasons: List<Int>,
    currentSeason: Int?,
    currentEpisode: Int?,
    episodes: List<EpisodeInfo>,
    onSelectSeason: (Int) -> Unit,
    onSelectEpisode: (season: Int, episode: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialSeason = currentSeason ?: seasons.firstOrNull() ?: 1
    var selectedSeason by remember(initialSeason) { mutableIntStateOf(initialSeason) }
    val initialFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        try {
            initialFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        BackHandler { onDismiss() }

        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .width(920.dp)
                .height(520.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ObsidianSurface)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
                .padding(28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldGlow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Выбор серии",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (title.isNotBlank()) {
                                Text(
                                    text = title,
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Close Button
                    var isCloseFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .tvFocusable(focusedScale = 1.1f, cornerRadius = 12.dp, onFocusChange = { isCloseFocused = it })
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCloseFocused) ObsidianSurfaceVariant else Color.Transparent)
                            .tvClickable { onDismiss() }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = if (isCloseFocused) TextPrimary else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Seasons Row (if multi-season)
                if (seasons.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(seasons) { sNum ->
                            val isSeasonActive = sNum == selectedSeason
                            var isFocused by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .onKeyEvent { keyEvent ->
                                        val code = keyEvent.nativeKeyEvent.keyCode
                                        if (code == KeyEvent.KEYCODE_DPAD_CENTER ||
                                            code == KeyEvent.KEYCODE_ENTER ||
                                            code == KeyEvent.KEYCODE_NUMPAD_ENTER
                                        ) {
                                            if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                                selectedSeason = sNum
                                                onSelectSeason(sNum)
                                            }
                                            true
                                        } else false
                                    }
                                    .clickable {
                                        selectedSeason = sNum
                                        onSelectSeason(sNum)
                                    }
                                    .focusable()
                                    .scale(if (isFocused) 1.06f else 1.0f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSeasonActive) EmeraldPrimary else if (isFocused) ObsidianSurfaceVariant else ObsidianBackground
                                    )
                                    .border(
                                        width = if (isFocused) 2.dp else 1.dp,
                                        color = if (isFocused) (if (isSeasonActive) Color.White else EmeraldPrimary) else ObsidianBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Сезон $sNum",
                                    color = if (isSeasonActive) ObsidianBackground else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Episodes Grid / Carousel
                val seasonEpisodes = episodes.filter { it.seasonNumber == selectedSeason }
                if (seasonEpisodes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Загрузка списка серий сезона $selectedSeason...",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp)
                    ) {
                        items(seasonEpisodes) { ep ->
                            val isCurrentPlaying = ep.seasonNumber == currentSeason && ep.episodeNumber == currentEpisode
                            var isCardFocused by remember { mutableStateOf(false) }

                            val cardModifier = if (isCurrentPlaying) {
                                Modifier.focusRequester(initialFocusRequester)
                            } else Modifier

                            Box(
                                modifier = cardModifier
                                    .width(220.dp)
                                    .fillMaxHeight()
                                    .tvFocusable(
                                        focusedScale = 1.04f,
                                        cornerRadius = 16.dp,
                                        onFocusChange = { isCardFocused = it }
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isCardFocused) ObsidianSurfaceVariant else ObsidianBackground)
                                    .border(
                                        width = if (isCardFocused) 2.dp else if (isCurrentPlaying) 1.5.dp else 1.dp,
                                        color = when {
                                            isCardFocused -> EmeraldPrimary
                                            isCurrentPlaying -> EmeraldPrimary.copy(alpha = 0.8f)
                                            else -> ObsidianBorder
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .tvClickable {
                                        onSelectEpisode(ep.seasonNumber, ep.episodeNumber)
                                    }
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Preview Still (16:9 ratio)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(115.dp)
                                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                            .background(Color(0xFF1E2430))
                                    ) {
                                        val still = ep.effectiveStill
                                        if (!still.isNullOrBlank()) {
                                            AsyncImage(
                                                model = still,
                                                contentDescription = ep.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        }

                                        // Playing badge
                                        if (isCurrentPlaying) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(6.dp)
                                            ) {
                                                TvBadge(
                                                    text = "● Играет",
                                                    backgroundColor = EmeraldPrimary,
                                                    textColor = ObsidianBackground
                                                )
                                            }
                                        } else if (ep.isPlayed) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                            ) {
                                                TvBadge(
                                                    text = "✓ Просмотрено",
                                                    backgroundColor = Color(0x99000000),
                                                    textColor = TextSecondary
                                                )
                                            }
                                        }
                                    }

                                    // Episode Details
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Серия ${ep.episodeNumber}",
                                            color = EmeraldPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            text = ep.name.ifBlank { "Серия ${ep.episodeNumber}" },
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 16.sp
                                        )

                                        if (!ep.airDate.isNullOrBlank()) {
                                            Text(
                                                text = ep.airDate,
                                                color = TextMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
