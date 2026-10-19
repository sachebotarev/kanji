package ru.mobui.kanji.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF426B53), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEDD8), onPrimaryContainer = Color(0xFF263E36),
    tertiary = Color(0xFF416D99), tertiaryContainer = Color(0xFFEAF1F8),
    secondary = Color(0xFF996452), secondaryContainer = Color(0xFFEDEAE2),
    onSecondaryContainer = Color(0xFF263E36),
    background = Color(0xFFFAF8F1), onBackground = Color(0xFF263E36),
    surface = Color(0xFFFFFDF7), onSurface = Color(0xFF263E36),
    surfaceVariant = Color(0xFFEDEAE2), onSurfaceVariant = Color(0xFF647168),
    outline = Color(0xFF758178), outlineVariant = Color(0xFFB9C2B5),
    error = Color(0xFFA64935), errorContainer = Color(0xFFFFEDE6), onErrorContainer = Color(0xFFA64935),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFADCDB0), onPrimary = Color(0xFF173822),
    primaryContainer = Color(0xFF244F32), onPrimaryContainer = Color(0xFFE8F0E5),
    background = Color(0xFF181E19), onBackground = Color(0xFFE7E9DF),
    surface = Color(0xFF202720), onSurface = Color(0xFFE7E9DF),
    surfaceVariant = Color(0xFF354038), onSurfaceVariant = Color(0xFFB7C3B6),
    outlineVariant = Color(0xFF58665A),
    tertiary = Color(0xFF9CC6EB), tertiaryContainer = Color(0xFF23384D),
    secondary = Color(0xFFD7AF9C),
    secondaryContainer = Color(0xFF354038), onSecondaryContainer = Color(0xFFE7E9DF),
    error = Color(0xFFFFB59F), errorContainer = Color(0xFF613125),
)

@Composable
fun KanjiTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
