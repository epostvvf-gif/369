package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CustomFlameOrange,        // Accent actions glow in flame orange
    onPrimary = Color.White,
    secondary = CosmicPrimary,          // Deep corporate branding blue
    onSecondary = Color.White,
    tertiary = AquaticWaveBlue,         // Waves blue for scans, cloud and search tags
    onTertiary = Color.Black,
    background = CharcoalDarkBg,        // Pure charcoal black outer canvas
    surface = DeepSurfaceDark,          // Layered high-density card indigo black
    onBackground = TextWhite,
    onSurface = TextWhite,
    surfaceVariant = Color(0xFF1D263B),
    onSurfaceVariant = TextGray,
    error = Color(0xFFFF5555)
)

private val LightColorScheme = lightColorScheme(
    primary = BrightSlatePrimaryLight,
    onPrimary = Color.White,
    secondary = CustomFlameOrange,
    onSecondary = Color.White,
    tertiary = AquaticWaveBlue,
    background = DeepSurfaceLight,
    surface = Color.White,
    onBackground = Color(0xFF111726),
    onSurface = Color(0xFF111726),
    surfaceVariant = Color(0xFFE4E9F3),
    onSurfaceVariant = Color(0xFF555F75),
    error = Color(0xFFD32F2F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamic color to false so our brand logo colors are preserved perfectly!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
        content = content
    )
}
