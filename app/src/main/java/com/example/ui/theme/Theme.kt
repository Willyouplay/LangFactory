package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = SpeaksyPurpleLight, // Vibrant accent #708CFD
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E335A),
    onPrimaryContainer = Color(0xFFDAD7FF),
    secondary = SpeaksyPurpleLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF22263C),
    onSecondaryContainer = Color(0xFFDAD7FF),
    tertiary = Color(0xFFFB7185),
    onTertiary = Color.White,
    background = Color(0xFF0F111A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF181B26),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF232738),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF0C0E16),
    surfaceContainerLow = Color(0xFF141722),
    surfaceContainer = Color(0xFF181B26),
    surfaceContainerHigh = Color(0xFF202434),
    surfaceContainerHighest = Color(0xFF282D42),
    outline = Color(0xFF3B4158),
    outlineVariant = Color(0xFF2E3346),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF3B1D22),
    onError = Color.White,
    onErrorContainer = Color(0xFFFCA5A5)
)

private val LightColorScheme = lightColorScheme(
    primary = SpeaksyPurple,
    onPrimary = PrecisionOnPrimary,
    primaryContainer = PrecisionPrimaryContainer,
    onPrimaryContainer = PrecisionOnPrimaryContainer,
    secondary = PrecisionSecondary,
    onSecondary = PrecisionOnSecondary,
    secondaryContainer = PrecisionSecondaryContainer,
    onSecondaryContainer = PrecisionOnSecondaryContainer,
    tertiary = KoreanChannelCrimson,
    onTertiary = Color.White,
    background = SpeaksyBackgroundLight,
    onBackground = PrecisionOnSurface,
    surface = Color.White,
    onSurface = PrecisionOnSurface,
    surfaceVariant = SpeaksyPurpleSoft,
    onSurfaceVariant = PrecisionOnSurfaceVariant,
    surfaceContainerLowest = PrecisionSurfaceContainerLowest,
    surfaceContainerLow = PrecisionSurfaceContainerLow,
    surfaceContainer = PrecisionSurfaceContainer,
    surfaceContainerHigh = PrecisionSurfaceContainerHigh,
    surfaceContainerHighest = PrecisionSurfaceContainerHighest,
    outline = PrecisionOutlineVariant,
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onError = Color.White,
    onErrorContainer = Color(0xFF991B1B)
)

@Composable
fun LangfactoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun SpeaksyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = LangfactoryTheme(darkTheme, dynamicColor, content)
