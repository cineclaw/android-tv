package com.cineclaw.tv.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    currentUsername: String = "admin",
    onSaveServerUrl: (String) -> Unit = {},
    onToggleAudioPassthrough: (Boolean) -> Unit = {},
    onSetPreferredQuality: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    onChangeServer: () -> Unit = onLogout,
    onPingCheck: (suspend (String) -> Boolean)? = null
) {
    var isServerOnline by remember { mutableStateOf<Boolean?>(null) }
    var isCheckingPing by remember { mutableStateOf(false) }
    var showChangeServerDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Run initial ping check
    LaunchedEffect(currentServerUrl) {
        if (onPingCheck != null) {
            isCheckingPing = true
            isServerOnline = onPingCheck(currentServerUrl)
            isCheckingPing = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(horizontal = 54.dp, vertical = 36.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(0.78f),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Настройки",
                        color = TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Управление подключением к серверу CineClaw и параметрами воспроизведения",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                }
            }

            // Section: Server & Account Card
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Сервер и авторизация",
                        color = TextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(ObsidianSurface)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
                            .padding(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            // Current Server URL + Ping Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(EmeraldGlow),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "S",
                                            color = EmeraldPrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Текущий адрес сервера",
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = currentServerUrl,
                                            color = TextPrimary,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // Ping Status Pill
                                if (isCheckingPing) {
                                    CircularProgressIndicator(
                                        color = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else if (isServerOnline != null) {
                                    val online = isServerOnline == true
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (online) EmeraldGlow else CrimsonGlow)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (online) EmeraldPrimary else CrimsonText)
                                        )
                                        Text(
                                            text = if (online) "На связи" else "Не отвечает",
                                            color = if (online) EmeraldPrimary else CrimsonText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(ObsidianBorder)
                            )

                            // Active User
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ObsidianSurfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Активная учетная запись",
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = currentUsername,
                                            color = TextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "● Авторизован",
                                    color = EmeraldPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(ObsidianBorder)
                            )

                            // Actions: Change Server & Logout
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                TvActionButton(
                                    text = "Сменить сервер",
                                    icon = Icons.Default.Refresh,
                                    isPrimary = true,
                                    onClick = { showChangeServerDialog = true }
                                )

                                TvActionButton(
                                    text = "Выйти из аккаунта",
                                    isPrimary = false,
                                    onClick = { showLogoutDialog = true }
                                )
                            }
                        }
                    }
                }
            }

            // Section: Streaming Quality Preference
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Качество видео по умолчанию",
                        color = TextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Используется при быстром старте фильмов и сериалов («Смотреть»)",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for ((tier, label) in listOf(
                            "4k" to "4K UHD",
                            "1080p" to "1080p FHD",
                            "720p" to "720p HD",
                            "auto" to "Авто (Лучшее)"
                        )) {
                            QualityPill(
                                label = label,
                                isSelected = preferredQuality == tier,
                                onSelect = { onSetPreferredQuality(tier) }
                            )
                        }
                    }
                }
            }

            // Section: Audio Passthrough Toggle
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Аудио & Звук",
                        color = TextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TvSettingsToggleRow(
                        title = "Сквозной вывод звука (HDMI Passthrough)",
                        description = "Прямая передача Dolby Digital AC3, E-AC3, TrueHD и DTS на AV-ресивер без преобразования в PCM",
                        isChecked = isAudioPassthrough,
                        onToggle = { onToggleAudioPassthrough(!isAudioPassthrough) }
                    )
                }
            }

            // Section: System Info Card
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "О системе",
                        color = TextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianSurface)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SystemInfoRow("Клиент", "CineClaw Android TV (v1.2.0)")
                            SystemInfoRow("Медиа-плеер", "Media3 ExoPlayer (Hardware MediaCodec)")
                            SystemInfoRow("Торрент-движок", "TorrServer Turbo (Порт 8092)")
                            SystemInfoRow("Авторизация", "HMAC-SHA256 Token")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Confirmation Dialog: Change Server
        if (showChangeServerDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScrimBlack)
                    .clickable { showChangeServerDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(480.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .clickable(enabled = false) {}
                        .padding(28.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Сменить сервер CineClaw?",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Текущая сессия будет завершена. Откроется стартовый экран с выбором сервера (NAS 192.168.88.19, Local или свой адрес) и авторизацией.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TvActionButton(
                                text = "Сменить сервер",
                                isPrimary = true,
                                onClick = {
                                    showChangeServerDialog = false
                                    onChangeServer()
                                }
                            )

                            TvActionButton(
                                text = "Отмена",
                                isPrimary = false,
                                onClick = { showChangeServerDialog = false }
                            )
                        }
                    }
                }
            }
        }

        // Confirmation Dialog: Logout
        if (showLogoutDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScrimBlack)
                    .clickable { showLogoutDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(480.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, ObsidianBorderHighlight, RoundedCornerShape(20.dp))
                        .clickable(enabled = false) {}
                        .padding(28.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Выйти из аккаунта?",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Вы действительно хотите выйти из профиля «$currentUsername»?",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TvActionButton(
                                text = "Выйти",
                                isPrimary = true,
                                onClick = {
                                    showLogoutDialog = false
                                    onLogout()
                                }
                            )

                            TvActionButton(
                                text = "Отмена",
                                isPrimary = false,
                                onClick = { showLogoutDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 13.sp)
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
