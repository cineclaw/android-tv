package com.cineclaw.tv

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.cineclaw.tv.core.designsystem.CineClawTVTheme
import com.cineclaw.tv.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    companion object {
        var keyEventInterceptor: ((KeyEvent) -> Boolean)? = null
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (keyEventInterceptor?.invoke(event) == true) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TV screen awake & fullscreen setup
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            CineClawTVTheme {
                AppNavigation()
            }
        }
    }
}
