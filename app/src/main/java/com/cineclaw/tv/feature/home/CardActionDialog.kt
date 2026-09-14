package com.cineclaw.tv.feature.home

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.*
import coil3.compose.AsyncImage
import com.cineclaw.tv.core.designsystem.*
import kotlinx.coroutines.delay

data class CardAction(
    val title: String,
    val icon: ImageVector,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CardActionDialog(
    title: String,
    subtitle: String? = null,
    imageUrl: String? = null,
    actions: List<CardAction>,
    onDismiss: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    val firstButtonFocusRequester = remember { FocusRequester() }
    var isKeyReleased by remember { mutableStateOf(false) }

    // Fallback: enable clicks after 400ms in case opened without long-press
    LaunchedEffect(Unit) {
        delay(400)
        isKeyReleased = true
    }

    LaunchedEffect(Unit) {
        delay(200)
        try {
            firstButtonFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .onPreviewKeyEvent { event ->
                    val code = event.nativeKeyEvent.keyCode
                    if (code == KeyEvent.KEYCODE_DPAD_CENTER ||
                        code == KeyEvent.KEYCODE_ENTER ||
                        code == KeyEvent.KEYCODE_NUMPAD_ENTER
                    ) {
                        if (!isKeyReleased) {
                            if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                isKeyReleased = true
                            }
                            // Consume both repeating ACTION_DOWN and the trailing ACTION_UP from the initiating long-press
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(480.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
                    .padding(28.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with Poster thumbnail & Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(96.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = title,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!subtitle.isNullOrBlank()) {
                                Text(
                                    text = subtitle,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons List
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        actions.forEachIndexed { index, action ->
                            val requester = if (index == 0) firstButtonFocusRequester else remember { FocusRequester() }
                            val buttonBorderColor = if (action.isDestructive) CrimsonBorder else EmeraldPrimary
                            val buttonGlowColor = if (action.isDestructive) CrimsonGlow else EmeraldGlow
                            val iconTint = if (action.isDestructive) CrimsonText else EmeraldPrimary

                            Surface(
                                onClick = {
                                    if (!isKeyReleased) return@Surface
                                    onDismiss()
                                    action.onClick()
                                },
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
                                border = ClickableSurfaceDefaults.border(
                                    focusedBorder = Border(
                                        border = BorderStroke(2.dp, buttonBorderColor),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                ),
                                glow = ClickableSurfaceDefaults.glow(
                                    focusedGlow = Glow(
                                        elevationColor = buttonGlowColor,
                                        elevation = 12.dp
                                    )
                                ),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = ObsidianSurfaceVariant,
                                    focusedContainerColor = ObsidianSurfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .focusRequester(requester)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = action.icon,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = action.title,
                                        color = if (action.isDestructive) CrimsonText else TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Dismiss "Отмена" Button
                        Surface(
                            onClick = {
                                if (!isKeyReleased) return@Surface
                                onDismiss()
                            },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(
                                    border = BorderStroke(2.dp, ObsidianBorderHighlight),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            ),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.Transparent,
                                focusedContainerColor = ObsidianSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Отмена",
                                    color = TextMuted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
