package ru.mobui.kanji.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Synced from doc/design-system/theme via npm run build → material-theme.json. */
private val LightColors =
    lightColorScheme(
        primary = Color(0xFF1E6A4B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFADF8D0),
        onPrimaryContainer = Color(0xFF002113),
        inversePrimary = Color(0xFF8CD6B0),
        secondary = Color(0xFF585D72),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFDDE1FA),
        onSecondaryContainer = Color(0xFF161B2D),
        tertiary = Color(0xFF80543D),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDBCB),
        onTertiaryContainer = Color(0xFF311303),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD5),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFF5FBF5),
        onBackground = Color(0xFF171D19),
        surface = Color(0xFFF5FBF5),
        onSurface = Color(0xFF171D19),
        surfaceVariant = Color(0xFFD9E6DC),
        onSurfaceVariant = Color(0xFF3E4942),
        surfaceTint = Color(0xFF1E6A4B),
        inverseSurface = Color(0xFF2C322E),
        inverseOnSurface = Color(0xFFEDF2EC),
        outline = Color(0xFF6E7A72),
        outlineVariant = Color(0xFFBDCAC0),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFF5FBF5),
        surfaceDim = Color(0xFFD6DBD5),
        surfaceContainer = Color(0xFFEAEFE9),
        surfaceContainerHigh = Color(0xFFE4EAE4),
        surfaceContainerHighest = Color(0xFFDEE4DE),
        surfaceContainerLow = Color(0xFFF0F5EF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF8CD6B0),
        onPrimary = Color(0xFF003824),
        primaryContainer = Color(0xFF00422B),
        onPrimaryContainer = Color(0xFFA8F2CB),
        inversePrimary = Color(0xFF1F6B4C),
        secondary = Color(0xFFC1C5DE),
        onSecondary = Color(0xFF2B3043),
        secondaryContainer = Color(0xFF41465A),
        onSecondaryContainer = Color(0xFFDDE1FA),
        tertiary = Color(0xFFF4BA9D),
        onTertiary = Color(0xFF4B2713),
        tertiaryContainer = Color(0xFF653D28),
        onTertiaryContainer = Color(0xFFFFDBCB),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690004),
        errorContainer = Color(0xFF930009),
        onErrorContainer = Color(0xFFFFDAD5),
        background = Color(0xFF0F1511),
        onBackground = Color(0xFFDEE4DE),
        surface = Color(0xFF0F1511),
        onSurface = Color(0xFFDEE4DE),
        surfaceVariant = Color(0xFF3E4942),
        onSurfaceVariant = Color(0xFFBDCAC0),
        surfaceTint = Color(0xFF8CD6B0),
        inverseSurface = Color(0xFFDEE4DE),
        inverseOnSurface = Color(0xFF2C322E),
        outline = Color(0xFF87948B),
        outlineVariant = Color(0xFF3E4942),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF353B36),
        surfaceDim = Color(0xFF0F1511),
        surfaceContainer = Color(0xFF1B211D),
        surfaceContainerHigh = Color(0xFF262B27),
        surfaceContainerHighest = Color(0xFF303632),
        surfaceContainerLow = Color(0xFF171D19),
        surfaceContainerLowest = Color(0xFF0A0F0C),
    )

val KanjiShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

private val defaultTypography = Typography()

val KanjiTypography =
    Typography(
        displayLarge =
            defaultTypography.displayLarge.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        displayMedium =
            defaultTypography.displayMedium.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        displaySmall =
            defaultTypography.displaySmall.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        headlineLarge =
            defaultTypography.headlineLarge.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        headlineMedium =
            defaultTypography.headlineMedium.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        headlineSmall =
            defaultTypography.headlineSmall.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        titleLarge =
            defaultTypography.titleLarge.copy(
                fontSize = 22.sp,
                lineHeight = 28.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        titleMedium =
            defaultTypography.titleMedium.copy(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        titleSmall =
            defaultTypography.titleSmall.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        bodyLarge =
            defaultTypography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        bodyMedium =
            defaultTypography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        bodySmall =
            defaultTypography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        labelLarge =
            defaultTypography.labelLarge.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        labelMedium =
            defaultTypography.labelMedium.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        labelSmall =
            defaultTypography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 16.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
    )

@Composable
fun KanjiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = KanjiTypography,
        shapes = KanjiShapes,
        content = content,
    )
}
