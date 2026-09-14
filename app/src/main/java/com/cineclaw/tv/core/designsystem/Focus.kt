package com.cineclaw.tv.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.key.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modern 10-foot D-Pad focus modifier:
 * - Scales element smoothly (1.06x) on focus.
 * - Adds glowing emerald border ring and shadow elevation.
 */
@Composable
fun Modifier.tvFocusable(
    focusedScale: Float = 1.06f,
    cornerRadius: Dp = 16.dp,
    focusedBorderColor: Color = EmeraldPrimary,
    glowColor: Color = EmeraldGlow,
    onFocusChange: (Boolean) -> Unit = {}
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "focus_scale"
    )

    return this
        .onFocusChanged {
            isFocused = it.isFocused
            onFocusChange(it.isFocused)
        }
        .focusable()
        .scale(scale)
        .then(
            if (isFocused) {
                Modifier
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(cornerRadius), ambientColor = glowColor, spotColor = glowColor)
                    .border(width = 2.dp, color = focusedBorderColor, shape = RoundedCornerShape(cornerRadius))
            } else {
                Modifier.border(width = 1.dp, color = ObsidianBorder, shape = RoundedCornerShape(cornerRadius))
            }
        )
}

/**
 * Ensures D-Pad Center and Enter keys trigger onClick reliably on Android TV.
 */
fun Modifier.tvClickable(onClick: () -> Unit): Modifier = this
    .onKeyEvent { keyEvent ->
        val code = keyEvent.nativeKeyEvent.keyCode
        if (code == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
            code == android.view.KeyEvent.KEYCODE_ENTER ||
            code == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                onClick()
            }
            true
        } else false
    }
    .clickable { onClick() }
