package com.cineclaw.tv.feature.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import androidx.activity.compose.BackHandler
import com.cineclaw.tv.core.designsystem.*
import com.cineclaw.tv.core.model.MediaItem

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WatchlistScreen(
    items: List<MediaItem>,
    isLoading: Boolean,
    onSelectMedia: (MediaItem) -> Unit,
    onRemoveMedia: (MediaItem) -> Unit,
    onBackClick: () -> Unit
) {
    val backButtonFocusRequester = remember { FocusRequester() }

    BackHandler {
        onBackClick()
    }

    LaunchedEffect(Unit) {
        backButtonFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .focusRequester(backButtonFocusRequester)
                        .tvFocusable(
                            focusedScale = 1.1f,
                            cornerRadius = 20.dp
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .tvClickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Назад",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Буду смотреть",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (items.isNotEmpty()) "${items.size} фильмов и сериалов" else "Ваша персональная медиатека",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Загрузка списка...", color = TextSecondary, fontSize = 16.sp)
                }
            } else if (items.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(ObsidianSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Ваш список пока пуст",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Добавляйте понравившиеся фильмы и сериалы кнопкой «В список» на странице фильма",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(360.dp)
                        )
                    }
                }
            } else {
                // Grid of watchlist items
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    itemsIndexed(items, key = { index, item -> "${item.effectiveTconst}_$index" }) { _, item ->
                        TvCinemaCard(
                            title = item.displayTitle,
                            posterUrl = item.effectivePoster,
                            year = item.year,
                            rating = item.rating,
                            qualityBadge = if (item.isTv) "Сериал" else "Фильм",
                            onClick = { onSelectMedia(item) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
