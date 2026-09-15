package com.example.smartcityassistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SmartCityLightColorScheme = lightColorScheme(
    primary = PrimaryNavy,
    onPrimary = Color.White,
    secondary = SecondaryBlue,
    onSecondary = Color.White,
    background = BackgroundGray,
    onBackground = MainText,
    surface = Color.White,
    onSurface = MainText,
    surfaceVariant = StatusBlue,
    onSurfaceVariant = SecondaryText,
    outlineVariant = DividerColor,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun SmartCityAssistantTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SmartCityLightColorScheme,
        typography = Typography,
        content = content
    )
}
