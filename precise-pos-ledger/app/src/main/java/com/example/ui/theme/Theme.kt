package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HighContrastLightColorScheme = lightColorScheme(
    primary = AmberPrimary,
    onPrimary = AmberOnPrimary,
    primaryContainer = AmberPrimaryContainer,
    onPrimaryContainer = AmberOnPrimaryContainer,
    secondary = HighContrastTextSecondary,
    onSecondary = Color.White,
    secondaryContainer = SlateSecondaryContainer,
    onSecondaryContainer = HighContrastTextPrimary,
    tertiary = VelocityGreen,
    onTertiary = Color.White,
    tertiaryContainer = VelocityGreenContainer,
    onTertiaryContainer = VelocityGreenOnContainer,
    error = AlertRed,
    onError = Color.White,
    errorContainer = AlertRedContainer,
    onErrorContainer = AlertOnErrorContainer,
    background = HighContrastScreenBg,
    onBackground = HighContrastTextPrimary,
    surface = HighContrastCardBg,
    onSurface = HighContrastTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = HighContrastTextSecondary,
    outline = HighContrastBorder,
    outlineVariant = OutlineBorder,
    inverseSurface = SlateInverseSurface,
    inverseOnSurface = SlateInverseOnSurface
)

@Composable
fun CounterFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // For POS Cash Registers & Food Stations, extreme daylight contrast is mandatory
    MaterialTheme(
        colorScheme = HighContrastLightColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun highContrastTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = HighContrastTextPrimary,
    unfocusedTextColor = HighContrastTextPrimary,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color(0xFFF1F5F9),
    focusedBorderColor = AmberPrimary,
    unfocusedBorderColor = HighContrastBorder,
    focusedLabelColor = AmberPrimary,
    unfocusedLabelColor = HighContrastTextSecondary,
    cursorColor = AmberPrimary
)
