package com.hulu.etsplus

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

@Composable
internal fun MiuixConfigTheme(
    colorScheme: ColorScheme,
    isDarkMode: Boolean,
    useDynamicColor: Boolean,
    blurEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    val miuixColors = remember(colorScheme, isDarkMode) {
        colorScheme.toMiuixColors(isDarkMode)
    }
    val controller = remember(miuixColors, isDarkMode, useDynamicColor) {
        if (useDynamicColor) {
            ThemeController(
                colorSchemeMode = ColorSchemeMode.MonetSystem,
                keyColor = null,
                colorSpec = ThemeColorSpec.Spec2025,
                paletteStyle = ThemePaletteStyle.TonalSpot,
                isDark = isDarkMode,
            )
        } else {
            ThemeController(
                colorSchemeMode = if (isDarkMode) ColorSchemeMode.Dark else ColorSchemeMode.Light,
                lightColors = miuixColors,
                darkColors = miuixColors,
                colorSpec = ThemeColorSpec.Spec2021,
                paletteStyle = ThemePaletteStyle.TonalSpot,
                isDark = isDarkMode,
            )
        }
    }

    MiuixTheme(controller = controller) {
        CompositionLocalProvider(LocalUiStyle provides UiStyle.Miuix, LocalBlurEnabled provides blurEnabled) {
            content()
        }
    }
}

private fun ColorScheme.toMiuixColors(isDarkMode: Boolean) = when {
    isDarkMode -> miuixDarkColorScheme().copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryVariant = primaryContainer,
        onPrimaryVariant = onPrimaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryVariant = secondaryContainer,
        onSecondaryVariant = onSecondaryContainer,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        tertiaryContainerVariant = tertiaryContainer,
        background = background,
        onBackground = onBackground,
        onBackgroundVariant = onSurfaceVariant,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceSecondary = onSurfaceVariant,
        onSurfaceVariantSummary = onSurfaceVariant,
        onSurfaceVariantActions = primary,
        disabledOnSurface = onSurface.copy(alpha = 0.38f),
        surfaceContainer = surfaceContainer,
        onSurfaceContainer = onSurface,
        onSurfaceContainerVariant = onSurfaceVariant,
        surfaceContainerHigh = surfaceContainerHigh,
        onSurfaceContainerHigh = onSurface,
        surfaceContainerHighest = surfaceContainerHighest,
        onSurfaceContainerHighest = onSurface,
        outline = outline,
        dividerLine = outlineVariant,
        windowDimming = Color.Black.copy(alpha = 0.32f),
        sliderKeyPoint = primary,
        sliderKeyPointForeground = onPrimary,
        sliderBackground = primaryContainer,
    )

    else -> miuixLightColorScheme().copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryVariant = primaryContainer,
        onPrimaryVariant = onPrimaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryVariant = secondaryContainer,
        onSecondaryVariant = onSecondaryContainer,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        tertiaryContainerVariant = tertiaryContainer,
        background = background,
        onBackground = onBackground,
        onBackgroundVariant = onSurfaceVariant,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceSecondary = onSurfaceVariant,
        onSurfaceVariantSummary = onSurfaceVariant,
        onSurfaceVariantActions = primary,
        disabledOnSurface = onSurface.copy(alpha = 0.38f),
        surfaceContainer = surfaceContainer,
        onSurfaceContainer = onSurface,
        onSurfaceContainerVariant = onSurfaceVariant,
        surfaceContainerHigh = surfaceContainerHigh,
        onSurfaceContainerHigh = onSurface,
        surfaceContainerHighest = surfaceContainerHighest,
        onSurfaceContainerHighest = onSurface,
        outline = outline,
        dividerLine = outlineVariant,
        windowDimming = Color.Black.copy(alpha = 0.32f),
        sliderKeyPoint = primary,
        sliderKeyPointForeground = onPrimary,
        sliderBackground = primaryContainer,
    )
}