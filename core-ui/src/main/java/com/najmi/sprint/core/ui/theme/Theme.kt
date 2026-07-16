package com.najmi.sprint.core.ui.theme

import android.app.Activity
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
    primary = AlexandriaDarkPrimary,
    onPrimary = AlexandriaDarkOnPrimary,
    primaryContainer = AlexandriaDarkPrimaryContainer,
    secondary = AlexandriaDarkSecondary,
    onSecondary = AlexandriaDarkOnSecondary,
    secondaryContainer = AlexandriaDarkSecondaryContainer,
    tertiary = AlexandriaDarkTertiary,
    onTertiary = AlexandriaDarkOnTertiary,
    tertiaryContainer = AlexandriaDarkTertiaryContainer,
    background = AlexandriaDarkBackground,
    onBackground = AlexandriaDarkOnBackground,
    surface = AlexandriaDarkSurface,
    onSurface = AlexandriaDarkOnSurface,
    surfaceVariant = AlexandriaDarkSurfaceVariant,
    onSurfaceVariant = AlexandriaDarkOnSurfaceVariant,
    error = AlexandriaDarkError,
    onError = AlexandriaDarkOnError,
    errorContainer = AlexandriaDarkErrorContainer,
    outline = AlexandriaDarkOutline,
    outlineVariant = AlexandriaDarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = AlexandriaPrimary,
    onPrimary = AlexandriaOnPrimary,
    primaryContainer = AlexandriaPrimaryContainer,
    secondary = AlexandriaSecondary,
    onSecondary = AlexandriaOnSecondary,
    secondaryContainer = AlexandriaSecondaryContainer,
    tertiary = AlexandriaTertiary,
    onTertiary = AlexandriaOnTertiary,
    tertiaryContainer = AlexandriaTertiaryContainer,
    background = AlexandriaBackground,
    onBackground = AlexandriaOnBackground,
    surface = AlexandriaSurface,
    onSurface = AlexandriaOnSurface,
    surfaceVariant = AlexandriaSurfaceVariant,
    onSurfaceVariant = AlexandriaOnSurfaceVariant,
    error = AlexandriaError,
    onError = AlexandriaOnError,
    errorContainer = AlexandriaErrorContainer,
    outline = AlexandriaOutline,
    outlineVariant = AlexandriaOutlineVariant
)

@Composable
fun SprintTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Enforce Alexandria theme over dynamic Material You colors by default
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
