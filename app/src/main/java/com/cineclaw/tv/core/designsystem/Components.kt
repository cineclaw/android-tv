package com.cineclaw.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.input.key.*
import androidx.tv.material3.*
import coil3.compose.AsyncImage

/**
 * 2:3 Poster Cinema Card for Home Shelves and Catalogs.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvCinemaCard(
    title: String,
    posterUrl: String?,
    year: Int? = null,
    rating: Double? = null,
    qualityBadge: String? = null,
    seedsCount: Int? = null,
    modifier: Modifier = Modifier,
    surfaceModifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    StandardCardContainer(
        modifier = modifier.width(160.dp),
        imageCard = { interactionSource ->
            Surface(
                onClick = onClick,
                onLongClick = onLongClick,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(2.dp, EmeraldPrimary),
                        shape = RoundedCornerShape(16.dp)
                    )
                ),
                glow = ClickableSurfaceDefaults.glow(
                    focusedGlow = Glow(
                        elevationColor = EmeraldGlow,
                        elevation = 14.dp
                    )
                ),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = ObsidianSurface,
                    focusedContainerColor = ObsidianSurface
                ),
                interactionSource = interactionSource,
                modifier = Modifier
                    .width(160.dp)
                    .height(240.dp)
                    .then(surfaceModifier)
            ) {
                if (!posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title.take(2),
                            color = TextMuted,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Top Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (rating != null && rating > 0) {
                        TvRatingBadge(rating = rating)
                    } else {
                        Spacer(modifier = Modifier.size(1.dp))
                    }

                    if (!qualityBadge.isNullOrBlank()) {
                        TvBadge(
                            text = qualityBadge,
                            backgroundColor = if (qualityBadge.contains("4K") || qualityBadge.contains("2160")) AmberGlow else EmeraldGlow,
                            textColor = if (qualityBadge.contains("4K") || qualityBadge.contains("2160")) AmberUHD else EmeraldPrimary
                        )
                    }
                }

                // Bottom Gradient & Seeds
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, ScrimBlack)
                            )
                        )
                )

                if (seedsCount != null && seedsCount > 0) {
                    Text(
                        text = "🌱 $seedsCount",
                        color = EmeraldPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    )
                }
            }
        },
        title = {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (year != null && year > 0) {
                    Text(
                        text = year.toString(),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    )
}

/**
 * 16:9 Wide Card for Continue Watching Shelf & Series Episodes.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvWideCard(
    title: String,
    subtitle: String? = null,
    backdropUrl: String?,
    progressPercent: Int = 0,
    timecode: String? = null,
    isNextUp: Boolean = false,
    isPlayed: Boolean = false,
    modifier: Modifier = Modifier,
    surfaceModifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    StandardCardContainer(
        modifier = modifier.width(280.dp),
        imageCard = { interactionSource ->
            Surface(
                onClick = onClick,
                onLongClick = onLongClick,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(2.dp, EmeraldPrimary),
                        shape = RoundedCornerShape(16.dp)
                    )
                ),
                glow = ClickableSurfaceDefaults.glow(
                    focusedGlow = Glow(
                        elevationColor = EmeraldGlow,
                        elevation = 14.dp
                    )
                ),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = ObsidianSurface,
                    focusedContainerColor = ObsidianSurface
                ),
                interactionSource = interactionSource,
                modifier = Modifier
                    .width(280.dp)
                    .height(158.dp)
                    .then(surfaceModifier)
            ) {
                if (!backdropUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = backdropUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(ObsidianSurfaceVariant))
                }

                // Next Up Badge
                if (isNextUp) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldPrimary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "ДАЛЕЕ",
                            color = ObsidianBackground,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Played Badge
                if (isPlayed) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xD9000000))
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "ПРОСМОТРЕНО",
                                color = EmeraldPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Gradient Scrim & Timecode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, ScrimBlack)
                            )
                        )
                )

                if (!timecode.isNullOrBlank()) {
                    Text(
                        text = timecode,
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 8.dp, bottom = 6.dp)
                    )
                }

                // Bottom Progress Bar
                if (progressPercent in 1..99) {
                    LinearProgressIndicator(
                        progress = { progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = EmeraldPrimary,
                        trackColor = Color(0x66000000)
                    )
                }
            }
        },
        title = {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    )
}

/**
 * Rating Badge with Star.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvRatingBadge(rating: Double, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xCC000000))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = AmberUHD,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = String.format("%.1f", rating),
            color = TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Universal Badge / Chip.
 */
@Composable
fun TvBadge(
    text: String,
    backgroundColor: Color = ObsidianSurfaceVariant,
    textColor: Color = TextSecondary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Primary Focusable TV Action Button.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvActionButton(
    text: String,
    icon: ImageVector? = null,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .tvFocusable(
                focusedScale = 1.05f,
                cornerRadius = 12.dp,
                focusedBorderColor = if (isPrimary) EmeraldPrimary else Color.White,
                onFocusChange = { isFocused = it }
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isFocused) {
                    if (isPrimary) EmeraldPrimary else Color(0xFF27272A)
                } else {
                    if (isPrimary) EmeraldDark else ObsidianSurfaceVariant
                }
            )
            .tvClickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused && isPrimary) ObsidianBackground else TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = text,
            color = if (isFocused && isPrimary) ObsidianBackground else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}
