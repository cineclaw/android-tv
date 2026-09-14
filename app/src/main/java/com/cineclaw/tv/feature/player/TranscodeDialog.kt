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
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.cineclaw.tv.core.designsystem.*

data class TranscodeOption(
    val id: String,
    val title: String,
    val bitrate: String,
    val subtitle: String,
    val icon: ImageVector,
    val isCarRecommended: Boolean = false
)

private val transcodeOptions = listOf(
    TranscodeOption(
        id = "direct",
        title = "Исходный поток",
        bitrate = "Direct MKV / HLS",
        subtitle = "Без сжатия (для домашнего Wi-Fi / быстрых сетей)",
        icon = Icons.Default.ElectricBolt
    ),
    TranscodeOption(
        id = "1080p",
        title = "1080p Full HD",
        bitrate = "6.0 Мбит/с",
        subtitle = "Высокая четкость для стабильного 5G / 4G+",
        icon = Icons.Default.HighQuality
    ),
    TranscodeOption(
        id = "720p",
        title = "720p HD Авто",
        bitrate = "3.0 Мбит/с",
        subtitle = "Оптимально для автомобиля в движении (LTE)",
        icon = Icons.Default.DirectionsCar,
        isCarRecommended = true
    ),
    TranscodeOption(
        id = "480p",
        title = "480p SD Эконом",
        bitrate = "1.3 Мбит/с",
        subtitle = "Плавный просмотр при слабом сотовом сигнале",
        icon = Icons.Default.NetworkCell
    ),
    TranscodeOption(
        id = "360p",
        title = "360p Мобильный",
        bitrate = "0.7 Мбит/с",
        subtitle = "Минимальный битрейт для роуминга и тоннелей",
        icon = Icons.Default.Speed
    )
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TranscodeDialog(
    currentProfile: String,
    onSelectProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(24.dp))
                .background(ObsidianBase)
                .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Транскодирование на лету",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Адаптация битрейта под мобильный интернет и условия связи",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianCard)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Options List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transcodeOptions) { option ->
                        val isSelected = option.id.equals(currentProfile, ignoreCase = true)
                        var isFocused by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) EmeraldGlow else if (isFocused) ObsidianElevated else ObsidianSurface)
                                .border(
                                    width = if (isSelected || isFocused) 1.5.dp else 1.dp,
                                    color = if (isSelected || isFocused) EmeraldPrimary else ObsidianBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    onSelectProfile(option.id)
                                    onDismiss()
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) EmeraldPrimary else TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = option.title,
                                                color = if (isSelected) EmeraldPrimary else TextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (option.isCarRecommended) {
                                                TvBadge(
                                                    text = "АВТО ЛТЕ",
                                                    backgroundColor = EmeraldGlow,
                                                    textColor = EmeraldPrimary
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${option.bitrate} • ${option.subtitle}",
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Выбрано",
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(22.dp)
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
