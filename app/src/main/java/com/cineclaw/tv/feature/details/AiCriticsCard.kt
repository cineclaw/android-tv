package com.cineclaw.tv.feature.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.cineclaw.tv.core.designsystem.*
import com.cineclaw.tv.core.model.CriticSummaryResponse
import kotlinx.coroutines.delay

private val PurplePrimary = Color(0xFFA855F7)
private val PurpleGlow = Color(0x33A855F7)
private val PurpleBorder = Color(0x66A855F7)
private val TomatoRed = Color(0xFFEF4444)
private val MetacriticGreen = Color(0xFF10B981)
private val MetacriticYellow = Color(0xFFF59E0B)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AiCriticsCard(
    summary: CriticSummaryResponse?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        AiCriticsSkeleton(modifier = modifier)
    } else if (summary != null && summary.verdict.isNotBlank()) {
        AiCriticsContent(summary = summary, modifier = modifier)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AiCriticsSkeleton(modifier: Modifier = Modifier) {
    val phases = remember {
        listOf(
            "Gemini 2.5 Flash анализирует оценки и рецензии...",
            "Сбор рейтингов Rotten Tomatoes, Metacritic и IMDb...",
            "Анализ отзывов мировой прессы и зрителей...",
            "Формирование консенсуса, плюсов и минусов..."
        )
    }
    var phaseIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            phaseIndex = (phaseIndex + 1) % phases.size
        }
    }

    // Shimmer effect
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.04f),
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.04f)
        ),
        start = androidx.compose.ui.geometry.Offset(translateAnim.value - 300f, 0f),
        end = androidx.compose.ui.geometry.Offset(translateAnim.value, 0f)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianSurface)
            .border(1.dp, PurpleBorder.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(PurpleGlow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PurplePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "ИИ-КОНСЕНСУС КРИТИКОВ",
                    color = PurplePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Pulse "Генерация" badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PurpleGlow)
                    .border(1.dp, PurpleBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary)
                )
                Text(
                    text = "Генерация",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Rotating status phase
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                tint = PurplePrimary.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = phases[phaseIndex],
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Shimmering ghost lines
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush)
            )
        }

        // Ghost badges
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush)
            )
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush)
            )
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

private data class ToneConfig(
    val label: String,
    val color: Color,
    val bg: Color,
    val border: Color
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AiCriticsContent(
    summary: CriticSummaryResponse,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val toneConfig = when (summary.tone.lowercase()) {
        "strongly_positive" -> ToneConfig("Восторженный приём", Color(0xFF34D399), Color(0x2A059669), Color(0x6634D399))
        "positive" -> ToneConfig("Положительный приём", Color(0xFF2DD4BF), Color(0x2A0D9488), Color(0x662DD4BF))
        "mixed" -> ToneConfig("Смешанные отзывы", Color(0xFFFBBF24), Color(0x2AD97706), Color(0x66FBBF24))
        "negative" -> ToneConfig("Сдержанный / Спорный", Color(0xFFFB7185), Color(0x2AE11D48), Color(0x66FB7185))
        else -> ToneConfig("Консенсус критиков", PurplePrimary, PurpleGlow, PurpleBorder)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable(
                focusedScale = 1.01f,
                cornerRadius = 16.dp,
                onFocusChange = { isFocused = it }
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        if (isFocused) ObsidianSurfaceVariant else ObsidianSurface,
                        Color(0xFF0F0B18)
                    )
                )
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) EmeraldPrimary else PurpleBorder.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(PurpleGlow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PurplePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "ИИ-КОНСЕНСУС КРИТИКОВ",
                    color = PurplePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Right header badges: Verdict Tone Badge + Model
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Prominent Tone / Verdict Badge (Web Parity)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(toneConfig.bg)
                        .border(1.dp, toneConfig.border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(toneConfig.color)
                    )
                    Text(
                        text = toneConfig.label,
                        color = toneConfig.color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Model badge
                Text(
                    text = summary.model.replace("google/", ""),
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Scores Badges Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rotten Tomatoes
            summary.scores.rottenTomatoes?.let { rt ->
                val isFresh = rt >= 60
                val rtColor = if (isFresh) Color(0xFFEF4444) else Color(0xFF10B981)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(rtColor.copy(alpha = 0.15f))
                        .border(1.dp, rtColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "🍅", fontSize = 12.sp)
                    Text(
                        text = "$rt%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Metacritic
            summary.scores.metacritic?.let { meta ->
                val metaColor = when {
                    meta >= 60 -> MetacriticGreen
                    meta >= 40 -> MetacriticYellow
                    else -> TomatoRed
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(metaColor.copy(alpha = 0.15f))
                        .border(1.dp, metaColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "M",
                        color = metaColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = meta.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // IMDb
            summary.scores.imdb?.let { imdb ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x22F59E0B))
                        .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = String.format("%.1f", imdb),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!summary.scores.imdbVotes.isNullOrBlank()) {
                        Text(
                            text = "(${summary.scores.imdbVotes})",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Awards
            if (!summary.scores.awards.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x22F59E0B))
                        .border(1.dp, Color(0x44F59E0B), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = summary.scores.awards,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        // Verdict text with quote icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.FormatQuote,
                contentDescription = null,
                tint = PurplePrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = summary.verdict,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
        }

        // Pros & Cons
        if (summary.pros.isNotEmpty() || summary.cons.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pros
                if (summary.pros.isNotEmpty()) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Плюсы",
                            color = EmeraldPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        summary.pros.forEach { pro ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = pro,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Cons
                if (summary.cons.isNotEmpty()) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Оговорки",
                            color = Color(0xFFF43F5E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        summary.cons.forEach { con ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color(0xFFF43F5E),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = con,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Target audience
        if (summary.targetAudience.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.Default.Explore,
                    contentDescription = null,
                    tint = PurplePrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Кому понравится: ${summary.targetAudience}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
