package com.cineclaw.tv.feature.person

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil3.compose.AsyncImage
import com.cineclaw.tv.core.designsystem.*
import androidx.activity.compose.BackHandler
import com.cineclaw.tv.core.model.PersonCreditItem
import com.cineclaw.tv.core.model.PersonDetailsResponse

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PersonScreen(
    personDetails: PersonDetailsResponse?,
    isLoading: Boolean,
    onSelectMovie: (PersonCreditItem) -> Unit,
    onBackClick: () -> Unit
) {
    val backButtonFocusRequester = remember { FocusRequester() }
    var selectedTab by remember { mutableStateOf("cast") } // "cast" or "crew"
    var sortByRating by remember { mutableStateOf(false) }

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
        if (isLoading || personDetails == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(EmeraldPrimary)
                    )
                    Text(
                        text = "Загрузка персоны...",
                        color = TextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            return@Box
        }

        val rawCredits = if (selectedTab == "cast") personDetails.cast else personDetails.crew
        val sortedCredits = remember(rawCredits, sortByRating) {
            if (sortByRating) {
                rawCredits.sortedByDescending { it.voteAverage ?: 0.0 }
            } else {
                rawCredits.sortedByDescending { it.year ?: 0 }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 36.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header with Back Button
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
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

                    Text(
                        text = "Персона",
                        color = TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Person Info Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Photo
                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianSurface)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    ) {
                        if (!personDetails.effectiveAvatar.isNullOrBlank()) {
                            AsyncImage(
                                model = personDetails.effectiveAvatar,
                                contentDescription = personDetails.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier
                                    .size(64.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }

                    // Metadata & Biography
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = personDetails.name,
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )

                        // Department & Birth Info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            personDetails.knownForDepartment?.let { dept ->
                                val label = when (dept) {
                                    "Acting" -> "Актёрское искусство"
                                    "Directing" -> "Режиссура"
                                    "Writing" -> "Сценарий"
                                    "Production" -> "Продюсирование"
                                    else -> dept
                                }
                                TvBadge(text = label, backgroundColor = EmeraldGlow, textColor = EmeraldPrimary)
                            }
                            personDetails.birthday?.let { bday ->
                                TvBadge(text = bday)
                            }
                        }

                        if (!personDetails.placeOfBirth.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = personDetails.placeOfBirth,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Biography
                        if (personDetails.biography.isNotBlank()) {
                            Text(
                                text = personDetails.biography,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(0.9f)
                            )
                        }
                    }
                }
            }

            // Filmography Controls (Tabs & Sort)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tabs
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (personDetails.cast.isNotEmpty()) {
                            val isActive = selectedTab == "cast"
                            var isFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .tvFocusable(focusedScale = 1.05f, cornerRadius = 8.dp, onFocusChange = { isFocused = it })
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) EmeraldPrimary else if (isFocused) ObsidianSurfaceVariant else ObsidianSurface)
                                    .tvClickable { selectedTab = "cast" }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "В ролях (${personDetails.cast.size})",
                                    color = if (isActive) ObsidianBackground else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (personDetails.crew.isNotEmpty()) {
                            val isActive = selectedTab == "crew"
                            var isFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .tvFocusable(focusedScale = 1.05f, cornerRadius = 8.dp, onFocusChange = { isFocused = it })
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) EmeraldPrimary else if (isFocused) ObsidianSurfaceVariant else ObsidianSurface)
                                    .tvClickable { selectedTab = "crew" }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Съёмочная группа (${personDetails.crew.size})",
                                    color = if (isActive) ObsidianBackground else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Sort Toggle
                    var isSortFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .tvFocusable(focusedScale = 1.05f, cornerRadius = 8.dp, onFocusChange = { isSortFocused = it })
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSortFocused) ObsidianSurfaceVariant else ObsidianSurface)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .tvClickable { sortByRating = !sortByRating }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (sortByRating) Icons.Default.Star else Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (sortByRating) "По рейтингу" else "По новизне",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Filmography Items Horizontal Carousel
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp, end = 48.dp)
                ) {
                    items(sortedCredits, key = { "${it.id}_${it.mediaType}_${it.character}_${it.job}" }) { credit ->
                        TvCinemaCard(
                            title = credit.displayTitle,
                            posterUrl = credit.effectivePoster,
                            year = credit.year,
                            rating = credit.voteAverage,
                            qualityBadge = credit.character?.takeIf { it.isNotBlank() } ?: credit.job,
                            onClick = { onSelectMovie(credit) }
                        )
                    }
                }
            }
        }
    }
}
