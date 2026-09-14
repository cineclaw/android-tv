package com.cineclaw.tv.feature.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.cineclaw.tv.core.designsystem.*
import androidx.activity.compose.BackHandler
import com.cineclaw.tv.core.model.MediaItem
import com.cineclaw.tv.core.model.Shelf

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CatalogScreen(
    catalogType: String, // "movies", "series", "4k"
    shelves: List<Shelf>,
    isLoading: Boolean,
    onSelectMedia: (MediaItem) -> Unit,
    onOpenShelf: (shelfId: String, title: String) -> Unit,
    onBackClick: () -> Unit
) {
    val backButtonFocusRequester = remember { FocusRequester() }

    BackHandler {
        onBackClick()
    }

    val (title, subtitle) = remember(catalogType) {
        when (catalogType) {
            "series" -> Pair("Сериалы", "Популярные онгоинги, сезоны и стриминговые премьеры")
            "4k" -> Pair("4K UHD Кинозал", "Релизы в ультравысоком разрешении 2160p HDR & Dolby Vision")
            else -> Pair("Фильмы", "Каталог полнометражных фильмов, новинок и классики кино")
        }
    }

    LaunchedEffect(Unit) {
        backButtonFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 32.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Header Row
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .focusRequester(backButtonFocusRequester)
                            .tvFocusable(focusedScale = 1.1f, cornerRadius = 20.dp)
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
                            text = title,
                            color = TextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (isLoading && shelves.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Загрузка каталога...", color = TextSecondary, fontSize = 16.sp)
                    }
                }
            }

            // Shelves
            shelves.forEachIndexed { shelfIndex, shelf ->
                item(key = "catalog_shelf_${shelf.id}_$shelfIndex") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusGroup()
                    ) {
                        // Shelf Title & "Ещё →"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = shelf.title,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            var isMoreFocused by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier
                                    .tvFocusable(focusedScale = 1.05f, cornerRadius = 8.dp, onFocusChange = { isMoreFocused = it })
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isMoreFocused) ObsidianSurfaceVariant else Color.Transparent)
                                    .tvClickable { onOpenShelf(shelf.id, shelf.title) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Ещё",
                                    color = if (isMoreFocused) EmeraldPrimary else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = if (isMoreFocused) EmeraldPrimary else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Edge-to-edge LazyRow with safe padding so scale & glow are never clipped
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 12.dp, bottom = 18.dp)
                        ) {
                            itemsIndexed(shelf.items, key = { index, media ->
                                val idPart = media.effectiveTconst.ifBlank { media.id?.toString() ?: media.title }
                                "${shelf.id}_${idPart}_$index"
                            }) { _, media ->
                                val seedBadge = if ((media.seeds ?: 0) > 0) "🌱 ${media.seeds}" else null
                                TvCinemaCard(
                                    title = media.displayTitle,
                                    posterUrl = media.effectivePoster,
                                    year = media.year,
                                    rating = media.rating,
                                    qualityBadge = seedBadge ?: if (catalogType == "4k") "4K UHD" else null,
                                    onClick = { onSelectMedia(media) }
                                )
                            }

                            // Trailing "Показать все" card
                            item {
                                var isCardFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .height(195.dp)
                                        .tvFocusable(
                                            focusedScale = 1.05f,
                                            cornerRadius = 16.dp,
                                            onFocusChange = { isCardFocused = it }
                                        )
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isCardFocused) ObsidianSurfaceVariant else ObsidianSurface)
                                        .border(
                                            width = if (isCardFocused) 2.dp else 1.dp,
                                            color = if (isCardFocused) EmeraldPrimary else Color.White.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .tvClickable { onOpenShelf(shelf.id, shelf.title) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            tint = if (isCardFocused) EmeraldPrimary else TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Все\nрелизы",
                                            color = if (isCardFocused) TextPrimary else TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
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
