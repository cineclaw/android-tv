package com.cineclaw.tv.feature.auth

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.cineclaw.tv.core.designsystem.*
import com.cineclaw.tv.core.model.DiscoveredServer
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AuthScreen(
    currentServerUrl: String,
    pairingCode: String = remember { (100000..999999).random().toString() },
    onConnectManual: (String, String, String) -> Unit = { _, _, _ -> },
    onPairSuccess: () -> Unit = {},
    errorMessage: String? = null,
    isLoggingIn: Boolean = false,
    onCheckPing: (suspend (String) -> Boolean)? = null,
    onPollPairing: (suspend (String, String) -> Boolean)? = null,
    onInitPairing: (suspend (String, String) -> Boolean)? = null,
    onDiscoverServers: (suspend () -> List<DiscoveredServer>)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var isManualMode by remember { mutableStateOf(false) }
    var serverUrlInput by remember { mutableStateOf(currentServerUrl) }
    var usernameInput by remember { mutableStateOf("admin") }
    var passwordInput by remember { mutableStateOf("wavemp3") }

    var isServerOnline by remember { mutableStateOf<Boolean?>(null) }
    var isCheckingPing by remember { mutableStateOf(false) }

    var discoveredServers by remember { mutableStateOf<List<DiscoveredServer>>(emptyList()) }
    var isDiscovering by remember { mutableStateOf(false) }

    // Run discovery on launch
    LaunchedEffect(Unit) {
        if (onDiscoverServers != null) {
            isDiscovering = true
            val found = onDiscoverServers()
            discoveredServers = found
            isDiscovering = false

            // Auto-select if current server is unreachable or default, but an active server was discovered
            if (found.isNotEmpty()) {
                val best = found.firstOrNull { it.isOnline } ?: found.first()
                if (serverUrlInput.isBlank() || serverUrlInput.contains("127.0.0.1") || isServerOnline == false) {
                    serverUrlInput = best.url
                }
            }
        }
    }

    // Ping check when serverUrlInput changes
    LaunchedEffect(serverUrlInput) {
        if (onCheckPing != null) {
            isCheckingPing = true
            isServerOnline = onCheckPing(serverUrlInput)
            isCheckingPing = false
        }
    }

    // Initialize pairing session on the server
    LaunchedEffect(pairingCode, serverUrlInput) {
        if (onInitPairing != null && serverUrlInput.isNotBlank()) {
            onInitPairing(pairingCode, serverUrlInput)
        }
    }

    // Polling pairing status
    LaunchedEffect(pairingCode, serverUrlInput) {
        if (onPollPairing != null && serverUrlInput.isNotBlank()) {
            while (isActive) {
                delay(2000)
                val paired = onPollPairing(pairingCode, serverUrlInput)
                if (paired) {
                    onPairSuccess()
                    break
                }
            }
        }
    }

    val qrBitmap = remember(pairingCode, serverUrlInput) {
        val pairingUrl = "${serverUrlInput.trimEnd('/')}/pair?code=$pairingCode"
        generateQrBitmap(pairingUrl, 280)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(horizontal = 48.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Branding and LAN Auto-Discovered Servers Bar
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
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "C", color = ObsidianBackground, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    }
                    Column {
                        Text(text = "CineClaw TV", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Домашний кинотеатр", color = TextMuted, fontSize = 13.sp)
                    }
                }

                // Discovered Servers row in header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isDiscovering) {
                        CircularProgressIndicator(
                            color = EmeraldPrimary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(text = "Поиск серверов в сети...", color = TextMuted, fontSize = 12.sp)
                    } else if (discoveredServers.isNotEmpty()) {
                        Text(text = "Серверы в сети:", color = TextMuted, fontSize = 12.sp)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(discoveredServers) { srv ->
                                val isSelected = srv.url.trimEnd('/') == serverUrlInput.trimEnd('/')
                                var isFocused by remember { mutableStateOf(false) }

                                Row(
                                    modifier = Modifier
                                        .tvFocusable(
                                            focusedScale = 1.05f,
                                            cornerRadius = 8.dp,
                                            focusedBorderColor = EmeraldPrimary,
                                            onFocusChange = { isFocused = it }
                                        )
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) EmeraldDark.copy(alpha = 0.6f)
                                            else if (isFocused) ObsidianSurfaceVariant
                                            else ObsidianSurface
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) EmeraldPrimary else ObsidianBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .tvClickable {
                                            serverUrlInput = srv.url
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (srv.isOnline) EmeraldPrimary else CrimsonText)
                                    )
                                    Text(
                                        text = "${srv.name} (${srv.url.removePrefix("http://")})",
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Refresh discovery button
                    TvActionButton(
                        text = "Обновить поиск",
                        icon = Icons.Default.Refresh,
                        isPrimary = false,
                        onClick = {
                            coroutineScope.launch {
                                if (onDiscoverServers != null) {
                                    isDiscovering = true
                                    discoveredServers = onDiscoverServers()
                                    isDiscovering = false
                                }
                            }
                        }
                    )
                }
            }

            // Main Content Body: Split into QR Code / Instructions OR Manual Login
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isManualMode) {
                    // Left: QR Code Pairing Instructions
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "Быстрый вход через QR-код",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Авторизуйтесь со смартфона или компьютера за секунду без ввода пароля пультом:",
                            color = TextSecondary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(ObsidianSurface)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("1", color = ObsidianBackground, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    }
                                    Text(
                                        text = "Наведите камеру телефона на QR-код или откройте в браузере:",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }

                                Text(
                                    text = "${serverUrlInput.trimEnd('/')}/pair",
                                    color = EmeraldPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(start = 34.dp)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("2", color = ObsidianBackground, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    }
                                    Text(
                                        text = "Код подтверждения на экране (заполнится автоматически):",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .padding(start = 34.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ObsidianSurfaceVariant)
                                        .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 24.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = pairingCode,
                                        color = AmberUHD,
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 6.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TvActionButton(
                                text = "Ввести логин и пароль пультом",
                                isPrimary = false,
                                onClick = { isManualMode = true }
                            )

                            // Ping status indicator
                            if (isCheckingPing) {
                                CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else if (isServerOnline != null) {
                                val online = isServerOnline == true
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (online) EmeraldPrimary else CrimsonText)
                                    )
                                    Text(
                                        text = if (online) "Сервер онлайн" else "Сервер недоступен",
                                        color = if (online) EmeraldPrimary else CrimsonText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Right: QR Code Visual
                    Column(
                        modifier = Modifier.weight(0.9f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                                .padding(18.dp)
                        ) {
                            qrBitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "QR-код авторизации",
                                    modifier = Modifier.size(240.dp)
                                )
                            } ?: Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = EmeraldPrimary)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = EmeraldPrimary,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Ожидание подтверждения со смартфона...",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    // Manual Credentials Form Mode
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .padding(end = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Ручной вход в CineClaw",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Server URL
                        OutlinedTextField(
                            value = serverUrlInput,
                            onValueChange = { serverUrlInput = it },
                            label = { Text("Адрес сервера (URL)", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = ObsidianBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Username
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("Имя пользователя", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = ObsidianBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Password
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Пароль", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = ObsidianBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Error message banner
                        if (!errorMessage.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CrimsonGlow)
                                    .border(1.dp, CrimsonBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CrimsonText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = errorMessage,
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            TvActionButton(
                                text = if (isLoggingIn) "Подключение..." else "Войти",
                                icon = Icons.Default.ArrowForward,
                                isPrimary = true,
                                onClick = {
                                    onConnectManual(serverUrlInput, usernameInput, passwordInput)
                                }
                            )
                            TvActionButton(
                                text = "Назад к QR-коду",
                                isPrimary = false,
                                onClick = { isManualMode = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
