package com.batteryalarm.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

@Composable
fun BatteryAlarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}