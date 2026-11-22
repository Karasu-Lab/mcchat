package com.karasu256.mcchatnative.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SplashPink = Color(0xFFFFA6D2)
val SplashSky = Color(0xFF9EDCFF)
val SplashInk = Color(0xFF3D0024)

private val LightColors = lightColorScheme(
    primary = Color(0xFFA8336F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD8E9),
    onPrimaryContainer = Color(0xFF3D0024),
    secondary = Color(0xFF00668B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC5E8FF),
    onSecondaryContainer = Color(0xFF001E2E),
    background = Color(0xFFFCFBFC),
    onBackground = Color(0xFF1C1B1D),
    surface = Color(0xFFFCFBFC),
    onSurface = Color(0xFF1C1B1D),
    onSurfaceVariant = Color(0xFF4D4A4E),
    surfaceContainerLow = Color(0xFFF6F4F6),
    surfaceContainer = Color(0xFFF1EFF1),
    surfaceContainerHigh = Color(0xFFEBE9EB),
    surfaceContainerHighest = Color(0xFFE5E3E5),
    outline = Color(0xFF7B787C),
    outlineVariant = Color(0xFFCCC9CD),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFAFD3),
    onPrimary = Color(0xFF5E0F3B),
    primaryContainer = Color(0xFF7F2652),
    onPrimaryContainer = Color(0xFFFFD8E9),
    secondary = Color(0xFF7DD0FF),
    onSecondary = Color(0xFF00344A),
    secondaryContainer = Color(0xFF004C6A),
    onSecondaryContainer = Color(0xFFC5E8FF),
    background = Color(0xFF141315),
    onBackground = Color(0xFFE6E3E6),
    surface = Color(0xFF141315),
    onSurface = Color(0xFFE6E3E6),
    onSurfaceVariant = Color(0xFFC9C5CA),
    surfaceContainerLow = Color(0xFF1C1B1D),
    surfaceContainer = Color(0xFF201F21),
    surfaceContainerHigh = Color(0xFF2B292B),
    surfaceContainerHighest = Color(0xFF363437),
    outline = Color(0xFF938F94),
    outlineVariant = Color(0xFF484649),
)

@Composable
fun MCChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
