package com.cineclaw.tv.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.cineclaw.tv.core.designsystem.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentServerUrl: String,
    isAudioPassthrough: Boolean,
    preferredQuality: String,
    onSaveServerUrl: (String) -> Unit,
    onToggleAudioPassthrough: (Boolean) -> Unit,
    onSetPreferredQuality: (String) -> Unit,
    onLogout: () -> Unit
) {
    var serverUrlInput by remember { mutableStateOf(currentServerUrl) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(48.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(0.7f),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(text = "Настройки приложения", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }

            // Server URL Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Сервер CineClaw", color = EmeraldPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it },
                        label = { Text("Адрес сервера", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TvActionButton(
                        text = "Сохранить адрес",
                        isPrimary = true,
                        onClick = { onSaveServerUrl(serverUrlInput) }
                    )
                }
            }

            // Audio Passthrough Toggle
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Аудио & Звук", color = EmeraldPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    TvSettingsToggleRow(
                        title = "Сквозной вывод звука (HDMI Passthrough)",
                        description = "Прямая передача Dolby Digital AC3, E-AC3, TrueHD и DTS на ресивер/саундбар",
                        isChecked = isAudioPassthrough,
                        onToggle = { onToggleAudioPassthrough(!isAudioPassthrough) }
                    )
                }
            }

            // Streaming Quality Preference
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Качество видео по умолчанию", color = EmeraldPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for ((tier, label) in listOf("auto" to "Авто (Лучшее)", "4k" to "4K UHD", "1080p" to "1080p FHD")) {
                            QualityPill(
                                label = label,
                                isSelected = preferredQuality == tier,
                                onSelect = { onSetPreferredQuality(tier) }
                            )
                        }
                    }
                }
            }

            // Logout / Disconnect
            item {
                Spacer(modifier = Modifier.height(12.dp))
                TvActionButton(
                    text = "Отключить ТВ от сервера",
                    isPrimary = false,
                    onClick = onLogout
                )
            }
        }
    }
}

@Composable
fun TvSettingsToggleRow(
    title: String,
    description: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(cornerRadius = 14.dp, onFocusChange = { isFocused = it })
            .clip(RoundedCornerShape(14.dp))
            .background(if (isFocused) ObsidianSurfaceVariant else ObsidianSurface)
            .clickable { onToggle() }
            .focusable()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = description, color = TextSecondary, fontSize = 11.sp)
        }

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isChecked) EmeraldPrimary else ObsidianBackground),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Включено", tint = ObsidianBackground, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun QualityPill(
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .tvFocusable(cornerRadius = 10.dp, onFocusChange = { isFocused = it })
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) EmeraldPrimary else if (isFocused) ObsidianSurfaceVariant else ObsidianSurface)
            .clickable { onSelect() }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) ObsidianBackground else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

