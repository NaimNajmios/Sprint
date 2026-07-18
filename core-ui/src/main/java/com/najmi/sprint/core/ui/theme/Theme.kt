package com.najmi.sprint.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color.White,
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    background = DarkSurfaceBase,
    onBackground = OnDarkSurface,
    surface = DarkSurfaceSheet,
    onSurface = OnDarkSurface
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = OnPrimary,
    background = LightSurfaceBase,
    onBackground = OnLightSurface,
    surface = LightSurfaceSheet,
    onSurface = OnLightSurface
    // Keep other defaults
)

@Composable
fun SprintTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We enforce the Daily Ledger theme over dynamic colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
