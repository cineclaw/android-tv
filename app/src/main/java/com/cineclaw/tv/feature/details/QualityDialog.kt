package com.cineclaw.tv.feature.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.window.Dialog
import android.view.KeyEvent
import androidx.compose.ui.input.key.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.cineclaw.tv.core.designsystem.*
import com.cineclaw.tv.core.model.QualityGroup
import com.cineclaw.tv.core.model.TorrentRelease

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun QualityDialog(
    qualityGroups: List<QualityGroup>,
    activeTorrentHash: String? = null,
    activeTier: String? = null,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    onSelectTorrent: (TorrentRelease) -> Unit,
    onDismiss: () -> Unit
) {
    // 1. Sort releases inside each group: compatible first, then seeds descending, bitrate, size
    val sortedQualityGroups = remember(qualityGroups) {
        qualityGroups.map { group ->
            group.copy(
                releases = group.releases.sortedWith(
                    compareByDescending<TorrentRelease> { !isHardwareIncompatibleRelease(it) }
                        .thenByDescending { it.seeds }
                        .thenByDescending { it.bitrateMbps }
                        .thenByDescending { it.size }
                )
            )
        }
    }

    // 2. Determine initial open accordion:
    // Match activeTorrentHash -> or activeTier -> or "1080p" -> or first available group
    val initialExpandedTier = remember(sortedQualityGroups, activeTorrentHash, activeTier) {
        if (!activeTorrentHash.isNullOrBlank()) {
            val matchingGroup = sortedQualityGroups.find { group ->
                group.releases.any { it.effectiveHash.equals(activeTorrentHash, ignoreCase = true) }
            }
            if (matchingGroup != null) return@remember matchingGroup.tier
        }

        if (!activeTier.isNullOrBlank()) {
            val normalized = when {
                activeTier.contains("4k", ignoreCase = true) || activeTier.contains("2160", ignoreCase = true) -> "4k"
                activeTier.contains("1080", ignoreCase = true) -> "1080p"
                activeTier.contains("720", ignoreCase = true) -> "720p"
                activeTier.contains("sd", ignoreCase = true) -> "sd"
                else -> activeTier.lowercase()
            }
            val matchingGroup = sortedQualityGroups.find { it.tier.equals(normalized, ignoreCase = true) }
            if (matchingGroup != null) return@remember matchingGroup.tier
        }

        val default1080p = sortedQualityGroups.find { it.tier == "1080p" }
        if (default1080p != null) return@remember default1080p.tier

        sortedQualityGroups.firstOrNull()?.tier
    }

    // Exactly one accordion open at a time
    var expandedTier by remember { mutableStateOf<String?>(initialExpandedTier) }

    // Focus requester for the top item of the expanded accordion
    val topItemRequester = remember { FocusRequester() }

    // On initial mount, focus the top release if available
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        try {
            topItemRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
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
                .width(660.dp)
                .height(520.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ObsidianSurface)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Выбор качества и релиза",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Сортировка по сидам • Нажмите для разворота",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onRefresh != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianBackground)
                                    .tvFocusable(focusedScale = 1.05f, cornerRadius = 10.dp)
                                    .onKeyEvent { keyEvent ->
                                        val code = keyEvent.nativeKeyEvent.keyCode
                                        if (code == KeyEvent.KEYCODE_DPAD_CENTER ||
                                            code == KeyEvent.KEYCODE_ENTER ||
                                            code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                                            if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN && !isRefreshing) {
                                                onRefresh()
                                            }
                                            true
                                        } else false
                                    }
                                    .clickable(enabled = !isRefreshing) { onRefresh() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(
                                        color = EmeraldPrimary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Обновить раздачи",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianBackground)
                                .tvFocusable(focusedScale = 1.05f, cornerRadius = 10.dp)
                                .onKeyEvent { keyEvent ->
                                    val code = keyEvent.nativeKeyEvent.keyCode
                                    if (code == KeyEvent.KEYCODE_DPAD_CENTER ||
                                        code == KeyEvent.KEYCODE_ENTER ||
                                        code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                                        if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                            onDismiss()
                                        }
                                        true
                                    } else false
                                }
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (sortedQualityGroups.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = EmeraldPrimary, strokeWidth = 3.dp)
                            Text(
                                text = "Поиск доступных раздач...",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    // Scrollable Accordion List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        sortedQualityGroups.forEach { group ->
                            val isExpanded = expandedTier == group.tier
                            val hasActive = group.releases.any {
                                (!activeTorrentHash.isNullOrBlank() && it.effectiveHash.equals(activeTorrentHash, ignoreCase = true))
                            }

                            // Accordion Header
                            item(key = "header_${group.tier}") {
                                QualityAccordionHeader(
                                    group = group,
                                    isExpanded = isExpanded,
                                    hasActive = hasActive,
                                    onClick = {
                                        expandedTier = if (isExpanded) null else group.tier
                                    }
                                )
                            }

                            // Accordion Items (only visible when expanded!)
                            if (isExpanded) {
                                itemsIndexed(
                                    items = group.releases,
                                    key = { _, rel -> rel.effectiveHash }
                                ) { index, rel ->
                                    val isSelected = rel.effectiveHash.equals(activeTorrentHash, ignoreCase = true)
                                    val isTopItem = index == 0
                                    QualityReleaseRow(
                                        rel = rel,
                                        isSelected = isSelected,
                                        modifier = (if (isTopItem) Modifier.focusRequester(topItemRequester) else Modifier)
                                            .padding(start = 12.dp, end = 4.dp),
                                        onSelect = {
                                            onSelectTorrent(rel)
                                            onDismiss()
                                        }
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

@Composable
private fun QualityAccordionHeader(
    group: QualityGroup,
    isExpanded: Boolean,
    hasActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable(
                focusedScale = 1.02f,
                cornerRadius = 14.dp,
                onFocusChange = { isFocused = it }
            )
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isFocused) ObsidianSurfaceVariant else if (isExpanded) ObsidianBackground else ObsidianBackground.copy(alpha = 0.6f)
            )
            .border(
                1.dp,
                if (isFocused) EmeraldPrimary else if (isExpanded) EmeraldPrimary.copy(alpha = 0.4f) else ObsidianBorder,
                RoundedCornerShape(14.dp)
            )
            .onKeyEvent { keyEvent ->
                val code = keyEvent.nativeKeyEvent.keyCode
                if (code == KeyEvent.KEYCODE_DPAD_CENTER ||
                    code == KeyEvent.KEYCODE_ENTER ||
                    code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        onClick()
                    }
                    true
                } else false
            }
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TvBadge(
                text = group.badge,
                backgroundColor = if (group.tier == "4k") AmberGlow else EmeraldGlow,
                textColor = if (group.tier == "4k") AmberUHD else EmeraldPrimary
            )
            Text(
                text = group.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (hasActive) {
                TvBadge(
                    text = "Текущее",
                    backgroundColor = EmeraldPrimary.copy(alpha = 0.2f),
                    textColor = EmeraldPrimary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${group.releases.size} ${getPluralVariants(group.releases.size)}",
                color = TextMuted,
                fontSize = 12.sp
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                tint = if (isFocused) EmeraldPrimary else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun QualityReleaseRow(
    rel: TorrentRelease,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable(
                focusedScale = 1.02f,
                cornerRadius = 12.dp,
                onFocusChange = { isFocused = it }
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) EmeraldGlow else if (isFocused) ObsidianSurfaceVariant else ObsidianBackground.copy(alpha = 0.4f)
            )
            .border(
                1.dp,
                if (isSelected) EmeraldPrimary else if (isFocused) EmeraldPrimary.copy(alpha = 0.5f) else ObsidianBorder.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .onKeyEvent { keyEvent ->
                val code = keyEvent.nativeKeyEvent.keyCode
                if (code == KeyEvent.KEYCODE_DPAD_CENTER ||
                    code == KeyEvent.KEYCODE_ENTER ||
                    code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        onSelect()
                    }
                    true
                } else false
            }
            .clickable { onSelect() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = rel.resolution.ifBlank { rel.tier.uppercase() },
                    color = if (rel.tier == "4k") AmberUHD else EmeraldPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (rel.bitrateMbps > 0) {
                    Text(
                        text = rel.bitrateFormatted,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (isSelected) {
                    TvBadge(text = "Текущая", backgroundColor = EmeraldPrimary, textColor = ObsidianBackground)
                }
                if (isHardwareIncompatibleRelease(rel)) {
                    TvBadge(text = "⚠️ 10-bit AVC (не реком.)", backgroundColor = Color(0xFF7F1D1D), textColor = Color(0xFFFCA5A5))
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = rel.sizeFormatted, color = TextSecondary, fontSize = 11.sp)
                Text(text = "•", color = TextMuted, fontSize = 11.sp)
                Text(
                    text = "🌱 ${rel.seeds} сидов",
                    color = if (rel.seeds > 0) EmeraldPrimary else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!rel.audioLabel.isNullOrBlank()) {
                    Text(text = "•", color = TextMuted, fontSize = 11.sp)
                    Text(text = rel.audioLabel, color = CyanSecondary, fontSize = 11.sp)
                }
                if (rel.tracker.isNotBlank()) {
                    Text(text = "•", color = TextMuted, fontSize = 11.sp)
                    Text(text = rel.tracker.uppercase(), color = TextMuted, fontSize = 10.sp)
                }
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Выбрано",
                tint = EmeraldPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun getPluralVariants(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..19 -> "вариантов"
        mod10 == 1 -> "вариант"
        mod10 in 2..4 -> "варианта"
        else -> "вариантов"
    }
}

fun isHardwareIncompatibleRelease(r: TorrentRelease): Boolean {
    val lower = r.title.lowercase()
    val has10Bit = lower.contains("10-bit") || lower.contains("10bit") || lower.contains("hi10p") || lower.contains("high 10")
    if (!has10Bit) return false
    val isModernCodec = lower.contains("h.265") || lower.contains("h265") || lower.contains("x265") ||
            lower.contains("hevc") || lower.contains("av1") || lower.contains("vp9")
    if (isModernCodec) return false
    val isAvc = lower.contains("h.264") || lower.contains("h264") || lower.contains("x264") || lower.contains("avc")
    return isAvc
}
