package com.batteryalarm.app.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

private val LightColors = lightColorScheme(
    primary = Volt,
    onPrimary = Ice,
    primaryContainer = VoltLight,
    onPrimaryContainer = Ink,
    secondary = VoltDark,
    onSecondary = Ice,
    secondaryContainer = Mint,
    onSecondaryContainer = Ink,
    tertiary = AlarmAmber,
    onTertiary = Ink,
    background = Color(0xFFF4FAF6),
    onBackground = Ink,
    surface = Color(0xFFFFFFFF),
    onSurface = Ink,
    surfaceVariant = Crest,
    onSurfaceVariant = InkSoft,
    outline = Mist
)

private val DarkColors = darkColorScheme(
    primary = VoltLight,
    onPrimary = Ink,
    primaryContainer = ForestMid,
    onPrimaryContainer = Mint,
    secondary = Volt,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF12352A),
    onSecondaryContainer = Mint,
    tertiary = AlarmAmber,
    onTertiary = Ink,
    background = Ink,
    onBackground = Ice,
    surface = Color(0xFF111D17),
    onSurface = Ice,
    surfaceVariant = Color(0xFF1B2A22),
    onSurfaceVariant = Mint,
    outline = Mist
)

/**
 * Paleta interpolada entre el tema claro y el oscuro.
 * [progress] va de 0 (claro) a 1 (oscuro).
 */
private fun buildColorScheme(progress: Float): ColorScheme {
    fun blend(light: Color, dark: Color): Color = lerp(light, dark, progress)

    val baseScheme = if (progress >= 0.5f) darkColorScheme() else lightColorScheme()
    return baseScheme.copy(
        primary = blend(LightColors.primary, DarkColors.primary),
        onPrimary = blend(LightColors.onPrimary, DarkColors.onPrimary),
        primaryContainer = blend(LightColors.primaryContainer, DarkColors.primaryContainer),
        onPrimaryContainer = blend(LightColors.onPrimaryContainer, DarkColors.onPrimaryContainer),
        secondary = blend(LightColors.secondary, DarkColors.secondary),
        onSecondary = blend(LightColors.onSecondary, DarkColors.onSecondary),
        secondaryContainer = blend(LightColors.secondaryContainer, DarkColors.secondaryContainer),
        onSecondaryContainer = blend(LightColors.onSecondaryContainer, DarkColors.onSecondaryContainer),
        tertiary = blend(LightColors.tertiary, DarkColors.tertiary),
        onTertiary = blend(LightColors.onTertiary, DarkColors.onTertiary),
        background = blend(LightColors.background, DarkColors.background),
        onBackground = blend(LightColors.onBackground, DarkColors.onBackground),
        surface = blend(LightColors.surface, DarkColors.surface),
        onSurface = blend(LightColors.onSurface, DarkColors.onSurface),
        surfaceVariant = blend(LightColors.surfaceVariant, DarkColors.surfaceVariant),
        onSurfaceVariant = blend(LightColors.onSurfaceVariant, DarkColors.onSurfaceVariant),
        outline = blend(LightColors.outline, DarkColors.outline)
    )
}

@Composable
fun BatteryAlarmTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    // Anima la transición entre claro y oscuro interpolando cada color.
    val progress by animateFloatAsState(
        targetValue = if (darkTheme) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "themeTransition"
    )
    MaterialTheme(
        colorScheme = buildColorScheme(progress),
        typography = Typography,
        content = content
    )
}