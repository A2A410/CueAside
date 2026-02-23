package foz.cueaside.aa.native_port.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import foz.cueaside.aa.native_port.model.AppTheme
import foz.cueaside.aa.native_port.model.DesignMode
import foz.cueaside.aa.native_port.model.THEMES

@Composable
fun CueAsideTheme(
    themeId: String = "default",
    designMode: DesignMode = DesignMode.MINIMAL,
    content: @Composable () -> Unit
) {
    val appTheme = THEMES.find { it.id == themeId } ?: THEMES.first()

    val colorScheme = remember(appTheme) {
        val bg = Color(android.graphics.Color.parseColor(appTheme.background))
        val acc = Color(android.graphics.Color.parseColor(appTheme.accent))
        val acc2 = Color(android.graphics.Color.parseColor(appTheme.accentSecondary))

        darkColorScheme(
            primary = acc,
            secondary = acc2,
            background = bg,
            surface = bg, // Simplified for now
            onPrimary = Color.Black,
            onSecondary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
