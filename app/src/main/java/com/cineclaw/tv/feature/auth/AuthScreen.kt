package com.cineclaw.tv.feature.auth

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.cineclaw.tv.core.designsystem.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AuthScreen(
    currentServerUrl: String,
    pairingCode: String = "XK94P2",
    onConnectManual: (String, String, String) -> Unit,
    onPairSuccess: () -> Unit
) {
    var isManualMode by remember { mutableStateOf(false) }
    var serverUrlInput by remember { mutableStateOf(currentServerUrl) }
    var usernameInput by remember { mutableStateOf("admin") }
    var passwordInput by remember { mutableStateOf("wavemp3") }
    var isLoading by remember { mutableStateOf(false) }

    val qrBitmap = remember(pairingCode, currentServerUrl) {
        val pairingUrl = "${currentServerUrl.trimEnd('/')}/pair?code=$pairingCode"
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
            // Left Column: Branding & Quick Pair Instructions
            Column(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "C", color = ObsidianBackground, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                    Text(text = "CineClaw TV", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Домашний онлайн-кинотеатр на вашем ТВ",
                    color = TextSecondary,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (!isManualMode) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianSurface)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "1. Отсканируйте QR-код или откройте в браузере:",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${currentServerUrl.trimEnd('/')}/pair",
                                color = EmeraldPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
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
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = pairingCode,
                                    color = AmberUHD,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 4.sp
                                )
                            }
                        }
                    }

                    TvActionButton(
                        text = "Ввести логин и пароль вручную",
                        isPrimary = false,
                        onClick = { isManualMode = true }
                    )
                } else {
                    // Manual Credentials Form
                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it },
                        label = { Text("Адрес сервера (IP:порт)", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )

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

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvActionButton(
                            text = "Подключиться",
                            isPrimary = true,
                            onClick = {
                                isLoading = true
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
                        text = "Ожидание подтверждения...",
                        color = TextMuted,
                        fontSize = 12.sp
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
