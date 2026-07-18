package com.najmi.sprint.core.ui.theme

import androidx.compose.ui.graphics.Color

val BrandPrimary = Color(0xFFC8FF00) // Chartreuse

// Light Theme
val LightSurfaceBase = Color(0xFFF7F7F5)
val LightSurfaceSheet = Color(0xFFFFFFFF)

// Dark Theme
val DarkSurfaceBase = Color(0xFF121316)
val DarkSurfaceSheet = Color(0xFF1C1D21)

// Shared across both themes
val SurfaceHero = Color(0xFF161A2C) // Deep navy-indigo
val SurfaceHeroGlow = Color(0xFF1E2440) // Faint radial glow used in icons/backgrounds

// Semantic map for M3 ColorScheme (simplified mapping, you can use custom LocalProviders if needed)
val OnDarkSurface = Color(0xFFFFFFFF)
val OnLightSurface = Color(0xFF1C1D21)
val OnPrimary = Color(0xFF161A2C) // Dark contrast on chartreuse
