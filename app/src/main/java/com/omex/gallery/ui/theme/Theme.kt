package com.omex.gallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val OmexDarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = ObsidianBg,
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = CyanGlow,
    secondary = NeonPurple,
    onSecondary = TextPrimaryDark,
    tertiary = AmberAccent,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondaryDark,
    error = ErrorRed
)

private val OmexLightColorScheme = lightColorScheme(
    primary = CyanAccent,
    onPrimary = ObsidianBg,
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = CyanGlow,
    secondary = NeonPurple,
    onSecondary = TextPrimaryDark,
    tertiary = AmberAccent,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondaryDark,
    error = ErrorRed
)

@Composable
fun OmexGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) OmexDarkColorScheme else OmexDarkColorScheme // Default to high-contrast gallery dark

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    OmexGalleryTheme(darkTheme = darkTheme, content = content)
}

