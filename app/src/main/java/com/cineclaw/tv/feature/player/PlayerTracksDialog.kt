package com.cineclaw.tv.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import android.view.KeyEvent
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.cineclaw.tv.core.designsystem.*
import com.cineclaw.tv.core.model.AudioTrack
import com.cineclaw.tv.core.model.SubtitleTrack
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AudioTracksDialog(
    tracks: List<AudioTrack>,
    selectedIndex: Int,
    onSelectTrack: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val firstItemRequester = remember { FocusRequester() }

    LaunchedEffect(tracks) {
        if (tracks.isNotEmpty()) {
            delay(100)
            try {
                firstItemRequester.requestFocus()
            } catch (_: Exception) {}
        }
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
                .width(560.dp)
                .height(440.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ObsidianSurface)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
                .padding(28.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Аудиодорожки",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (tracks.isNotEmpty()) "${tracks.size} дорожек доступно" else "Поиск дорожек...",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianBackground)
                            .tvFocusable(focusedScale = 1.05f, cornerRadius = 10.dp)
                            .onKeyEvent { keyEvent ->
                                val code = keyEvent.nativeKeyEvent.keyCode
                                if (code == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                                    code == android.view.KeyEvent.KEYCODE_ENTER) {
                                    if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        onDismiss()
                                    }
                                    true
                                } else false
                            },
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

                Spacer(modifier = Modifier.height(18.dp))

                if (tracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Дорожки определяются из потока...",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(tracks, key = { it.index }) { track ->
                            val isSelected = track.index == selectedIndex
                            val isFirst = track.index == tracks.first().index
                            AudioTrackRow(
                                track = track,
                                isSelected = isSelected,
                                modifier = if (isFirst) Modifier.focusRequester(firstItemRequester) else Modifier,
                                onSelect = {
                                    onSelectTrack(track.index)
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

fun formatLangCode(lang: String): String {
    return when (lang.lowercase().trim()) {
        "ru", "rus", "russian" -> "RU"
        "en", "eng", "english" -> "EN"
        "uk", "ukr", "ukrainian" -> "UA"
        "he", "heb", "hebrew" -> "HE"
        "pt", "por", "portuguese" -> "PT"
        "ro", "ron", "rum", "romanian" -> "RO"
        "ja", "jpn", "japanese" -> "JA"
        "ko", "kor", "korean" -> "KO"
        "fr", "fra", "fre", "french" -> "FR"
        "de", "deu", "ger", "german" -> "DE"
        "it", "ita", "italian" -> "IT"
        "es", "spa", "spanish" -> "ES"
        "zh", "zho", "chi", "chinese" -> "ZH"
        "tr", "tur", "turkish" -> "TR"
        else -> lang.take(3).uppercase()
    }
}

@Composable
private fun AudioTrackRow(
    track: AudioTrack,
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
                if (isSelected) EmeraldGlow else if (isFocused) ObsidianSurfaceVariant else Color.Transparent
            )
            .onKeyEvent { keyEvent ->
                val code = keyEvent.nativeKeyEvent.keyCode
                if (code == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                    code == android.view.KeyEvent.KEYCODE_ENTER) {
                    if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                        onSelect()
                    }
                    true
                } else false
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = track.title.ifBlank { "Аудиодорожка ${track.index + 1}" },
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (isSelected) {
                    TvBadge(text = "Активна", backgroundColor = EmeraldPrimary, textColor = ObsidianBackground)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TvBadge(text = formatLangCode(track.language), backgroundColor = ObsidianBackground, textColor = CyanSecondary)
                if (track.codec.isNotBlank()) {
                    Text(text = "•", color = TextMuted, fontSize = 11.sp)
                    Text(text = track.codec.uppercase(), color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (track.channels > 0) {
                    Text(text = "•", color = TextMuted, fontSize = 11.sp)
                    val channelsText = when (track.channels) {
                        6 -> "5.1"
                        8 -> "7.1"
                        2 -> "2.0 Стерео"
                        1 -> "Моно"
                        else -> "${track.channels} ch"
                    }
                    Text(text = channelsText, color = TextMuted, fontSize = 11.sp)
                }
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Выбрана",
                tint = EmeraldPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SubtitleTracksDialog(
    tracks: List<SubtitleTrack>,
    selectedIndex: Int,
    onSelectTrack: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val firstItemRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        try {
            firstItemRequester.requestFocus()
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
                .width(560.dp)
                .height(440.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ObsidianSurface)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
                .padding(28.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.SubtitlesOff,
                                contentDescription = null,
                                tint = AmberUHD,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Субтитры",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (tracks.isNotEmpty()) "${tracks.size} дорожек доступно" else "Субтитры отсутствуют",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianBackground)
                            .tvFocusable(focusedScale = 1.05f, cornerRadius = 10.dp)
                            .onKeyEvent { keyEvent ->
                                val code = keyEvent.nativeKeyEvent.keyCode
                                if (code == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                                    code == android.view.KeyEvent.KEYCODE_ENTER) {
                                    if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        onDismiss()
                                    }
                                    true
                                } else false
                            },
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

                Spacer(modifier = Modifier.height(18.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Option to turn off subtitles
                    item(key = -1) {
                        val isOff = selectedIndex < 0
                        var isFocused by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(firstItemRequester)
                                .tvFocusable(
                                    focusedScale = 1.02f,
                                    cornerRadius = 12.dp,
                                    onFocusChange = { isFocused = it }
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isOff) EmeraldGlow else if (isFocused) ObsidianSurfaceVariant else Color.Transparent
                                )
                                .onKeyEvent { keyEvent ->
                                    val code = keyEvent.nativeKeyEvent.keyCode
                                    if (code == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                                        code == android.view.KeyEvent.KEYCODE_ENTER) {
                                        if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            onSelectTrack(-1)
                                            onDismiss()
                                        }
                                        true
                                    } else false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(
                                    imageVector = Icons.Default.SubtitlesOff,
                                    contentDescription = null,
                                    tint = if (isOff) EmeraldPrimary else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Выключить субтитры",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (isOff) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Выключено",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    items(tracks, key = { it.index }) { track ->
                        val isSelected = track.index == selectedIndex
                        SubtitleTrackRow(
                            track = track,
                            isSelected = isSelected,
                            onSelect = {
                                onSelectTrack(track.index)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleTrackRow(
    track: SubtitleTrack,
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
                if (isSelected) EmeraldGlow else if (isFocused) ObsidianSurfaceVariant else Color.Transparent
            )
            .onKeyEvent { keyEvent ->
                val code = keyEvent.nativeKeyEvent.keyCode
                if (code == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                    code == android.view.KeyEvent.KEYCODE_ENTER) {
                    if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                        onSelect()
                    }
                    true
                } else false
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = track.title.ifBlank { "Субтитры ${track.index + 1}" },
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (isSelected) {
                    TvBadge(text = "Активны", backgroundColor = EmeraldPrimary, textColor = ObsidianBackground)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TvBadge(text = formatLangCode(track.language), backgroundColor = ObsidianBackground, textColor = AmberUHD)
                if (track.codec.isNotBlank()) {
                    Text(text = "•", color = TextMuted, fontSize = 11.sp)
                    val codecClean = track.codec.substringAfterLast("/").substringAfterLast(".")
                    Text(text = codecClean.uppercase(), color = TextSecondary, fontSize = 11.sp)
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
