package com.sawan4art.noor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.sawan4art.noor.data.ThemeMode

/**
 * ألوان «نور» الخاصة فوق نظام Material 3 — بطاقة الهيرو والحلقة الذهبية
 * تحتاج درجات لا يغطيها الـ scheme القياسي، فتُمرَّر عبر LocalNoorColors.
 */
@Immutable
data class NoorColors(
    val heroTop: Color,
    val heroBottom: Color,
    val heroContent: Color,
    val heroMuted: Color,
) {
    val heroGradient: Brush
        get() = Brush.verticalGradient(listOf(heroTop, heroBottom))
}

val LocalNoorColors = staticCompositionLocalOf {
    NoorColors(
        heroTop = DayHeroTop,
        heroBottom = DayHeroBottom,
        heroContent = DayHeroContent,
        heroMuted = DayHeroMuted,
    )
}

private val DayColors = lightColorScheme(
    primary = DayPrimary,
    onPrimary = DayOnPrimary,
    primaryContainer = DayPrimaryContainer,
    onPrimaryContainer = DayOnPrimaryContainer,
    secondary = DaySecondary,
    onSecondary = DayOnSecondary,
    secondaryContainer = DaySecondaryContainer,
    onSecondaryContainer = DayOnSecondaryContainer,
    background = DayBackground,
    onBackground = DayOnBackground,
    surface = DaySurface,
    onSurface = DayOnBackground,
    surfaceVariant = DaySurfaceVariant,
    onSurfaceVariant = Color(0xFF4A564F),
    outline = DayOutline,
)

private val NightColors = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = NightPrimaryContainer,
    onPrimaryContainer = NightOnPrimaryContainer,
    secondary = NightSecondary,
    onSecondary = NightOnSecondary,
    secondaryContainer = NightSecondaryContainer,
    onSecondaryContainer = NightOnSecondaryContainer,
    background = NightBackground,
    onBackground = NightOnBackground,
    surface = NightSurface,
    onSurface = NightOnBackground,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = Color(0xFFA7B5AD),
    outline = NightOutline,
)

@Composable
fun NoorTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dark) NightColors else DayColors
    val noorColors = if (dark) {
        NoorColors(NightHeroTop, NightHeroBottom, NightHeroContent, NightHeroMuted)
    } else {
        NoorColors(DayHeroTop, DayHeroBottom, DayHeroContent, DayHeroMuted)
    }

    CompositionLocalProvider(LocalNoorColors provides noorColors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = NoorTypography,
            content = content,
        )
    }
}
