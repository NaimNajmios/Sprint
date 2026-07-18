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
    primary = BrandPrimary,
    onPrimary = OnPrimary,
    background = DarkSurfaceBase,
    onBackground = OnDarkSurface,
    surface = DarkSurfaceSheet,
    onSurface = OnDarkSurface,
    surfaceVariant = SurfaceHero,
    onSurfaceVariant = OnDarkSurface,
    // We keep default M3 colors for tertiary, error etc as they are not defined in the new spec
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = OnPrimary,
    background = LightSurfaceBase,
    onBackground = OnLightSurface,
    surface = LightSurfaceSheet,
    onSurface = OnLightSurface,
    surfaceVariant = SurfaceHero,
    onSurfaceVariant = OnDarkSurface, // Hero surface text is always light (on dark navy)
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
