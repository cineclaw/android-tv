package com.cineclaw.tv.feature.auth

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    onPollPairing: (suspend (String) -> Boolean)? = null
) {
    var isManualMode by remember { mutableStateOf(false) }
    var serverUrlInput by remember { mutableStateOf(currentServerUrl) }
    var usernameInput by remember { mutableStateOf("admin") }
    var passwordInput by remember { mutableStateOf("wavemp3") }

    var isServerOnline by remember { mutableStateOf<Boolean?>(null) }
    var isCheckingPing by remember { mutableStateOf(false) }

    // Ping check when serverUrlInput changes
    LaunchedEffect(serverUrlInput) {
        if (onCheckPing != null) {
            isCheckingPing = true
            isServerOnline = onCheckPing(serverUrlInput)
            isCheckingPing = false
        }
    }

    // Polling pairing status
    LaunchedEffect(pairingCode, serverUrlInput) {
        if (onPollPairing != null) {
            while (isActive) {
                delay(2500)
                val paired = onPollPairing(pairingCode)
                if (paired) {
                    onPairSuccess()
                    break
                }
            }
        }
    }

    val qrBitmap = remember(pairingCode, serverUrlInput) {
        val pairingUrl = "${serverUrlInput.trimEnd('/')}/pair?code=$pairingCode"
        generateQrBitmap(pairingUrl, 260)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Branding & Login Form / Instructions
            Column(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "C", color = ObsidianBackground, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    }
                    Text(text = "CineClaw TV", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Подключение к домашнему серверу CineClaw",
                    color = TextSecondary,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (!isManualMode) {
                    // QR Pairing Mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianSurface)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "1. Отсканируйте QR-код камерой смартфона или перейдите по адресу:",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${serverUrlInput.trimEnd('/')}/pair",
                                color = EmeraldPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "2. Введите код подтверждения:",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianSurfaceVariant)
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = pairingCode,
                                    color = AmberUHD,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 4.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvActionButton(
                            text = "Ввести логин и пароль вручную",
                            isPrimary = true,
                            onClick = { isManualMode = true }
                        )
                    }
                } else {
                    // Manual Credentials Mode
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Quick Presets Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "Выбор сервера:", color = TextMuted, fontSize = 12.sp)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianSurfaceVariant)
                                    .clickable { serverUrlInput = "http://192.168.88.19:3000" }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = "NAS (192.168.88.19)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianSurfaceVariant)
                                    .clickable { serverUrlInput = "http://127.0.0.1:3000" }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = "Local (127.0.0.1)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Ping Indicator
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
                                        text = if (online) "В сети" else "Недоступен",
                                        color = if (online) EmeraldPrimary else CrimsonText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

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
                            modifier = Modifier.fillMaxWidth(0.9f)
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
                            modifier = Modifier.fillMaxWidth(0.9f)
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
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )

                        // Error message banner
                        if (!errorMessage.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            TvActionButton(
                                text = if (isLoggingIn) "Подключение..." else "Войти в CineClaw",
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

            // Right Column: QR Code Display
            if (!isManualMode) {
                Column(
                    modifier = Modifier.weight(0.8f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(16.dp)
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

                    Text(
                        text = "Ожидание подтверждения с телефона...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
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
