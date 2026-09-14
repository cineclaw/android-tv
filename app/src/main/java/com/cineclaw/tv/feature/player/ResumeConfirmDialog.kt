package com.cineclaw.tv.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
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
import kotlinx.coroutines.delay

private fun formatTimecode(seconds: Double): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ResumeConfirmDialog(
    title: String,
    subtitle: String? = null,
    resumeSeconds: Double,
    durationSeconds: Double = 0.0,
    onResume: () -> Unit,
    onStartFromBeginning: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    val resumeButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(120)
        try {
            resumeButtonFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val formattedResume = formatTimecode(resumeSeconds)
    val formattedDuration = if (durationSeconds > 0) formatTimecode(durationSeconds) else ""
    val progressPercent = if (durationSeconds > 0) {
        ((resumeSeconds / durationSeconds) * 100.0).toInt().coerceIn(1, 99)
    } else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ObsidianSurface)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.15f))
                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "Продолжить просмотр?",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                if (title.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (!subtitle.isNullOrBlank()) "$title • $subtitle" else title,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Timecode Badge Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianBackground)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Остановлено на",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (formattedDuration.isNotEmpty()) "$formattedResume / $formattedDuration" else formattedResume,
                                color = EmeraldPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (progressPercent != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.15f))
                                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$progressPercent%",
                                    color = EmeraldPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TvActionButton(
                        text = "Продолжить с $formattedResume",
                        icon = Icons.Default.PlayArrow,
                        isPrimary = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(resumeButtonFocusRequester),
                        onClick = onResume
                    )

                    TvActionButton(
                        text = "С начала",
                        icon = Icons.Default.Replay,
                        isPrimary = false,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onStartFromBeginning
                    )
                }
            }
        }
    }
}
