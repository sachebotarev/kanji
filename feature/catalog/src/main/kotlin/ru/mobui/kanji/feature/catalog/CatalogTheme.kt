package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.sp

/** Scoped to the catalog: changing its palette does not restyle other features. */
@Composable
internal fun CatalogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = catalogColors(MaterialTheme.colorScheme, darkTheme)
    val typography = catalogTypography(MaterialTheme.typography)
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}

private fun catalogColors(
    base: androidx.compose.material3.ColorScheme,
    dark: Boolean,
): androidx.compose.material3.ColorScheme =
    if (dark)
        base.copy(
            background = Color(color = 0xFF232C24),
            surface = Color(color = 0xFF232C24),
            surfaceContainerLow = Color(color = 0xFF29332B),
            surfaceContainerHigh = Color(color = 0xFF354334),
            surfaceContainerHighest = Color(color = 0xFF40513C),
            surfaceContainer = Color(color = 0xFF2B372D),
            onBackground = Color(color = 0xFFF0EFE2),
            onSurface = Color(color = 0xFFF0EFE2),
            onSurfaceVariant = Color(color = 0xFFC1CCBC),
            primary = Color(color = 0xFFB2CEA8),
            onPrimary = Color(color = 0xFF19351C),
            primaryContainer = Color(color = 0xFF244F32),
            secondaryContainer = Color(color = 0xFF2B372D),
            onSecondaryContainer = Color(color = 0xFFF0EFE2),
            error = Color(color = 0xFFFFB59F),
            outline = Color(color = 0xFF63735F),
        )
    else
        base.copy(
            background = Color(color = 0xFFFAF8F1),
            surface = Color(color = 0xFFFAF8F1),
            surfaceContainerLow = Color(color = 0xFFF1F2EB),
            surfaceContainerHigh = Color(color = 0xFFE1E7D9),
            surfaceContainerHighest = Color(color = 0xFFD2DDC9),
            surfaceContainer = Color(color = 0xFFEEF0E7),
            onBackground = Color(color = 0xFF263E36),
            onSurface = Color(color = 0xFF263E36),
            onSurfaceVariant = Color(color = 0xFF4A5C52),
            primary = Color(color = 0xFF315F47),
            onPrimary = Color.White,
            primaryContainer = Color(color = 0xFFDDEDD8),
            secondaryContainer = Color(color = 0xFFEEF0E7),
            onSecondaryContainer = Color(color = 0xFF263E36),
            error = Color(color = 0xFFA64935),
            outline = Color(color = 0xFFA8B3A4),
        )

private fun catalogTypography(
    typography: androidx.compose.material3.Typography,
): androidx.compose.material3.Typography =
    typography.copy(
        bodyMedium =
            typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
                platformStyle =
                    androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
            ),
        bodySmall =
            typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp,
                platformStyle =
                    androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
            ),
        bodyLarge =
            typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
                platformStyle =
                    androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
            ),
        titleMedium =
            typography.titleMedium.copy(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
                platformStyle =
                    androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
            ),
        titleLarge =
            typography.titleLarge.copy(
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
                platformStyle =
                    androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
            ),
        labelLarge =
            typography.labelLarge.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
                platformStyle =
                    androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
            ),
    )

internal val CatalogJapaneseFont = FontFamily(Font(R.font.catalog_japanese))
internal val CatalogJapaneseStyle =
    TextStyle(
        fontFamily = CatalogJapaneseFont,
        localeList = LocaleList("ja-JP"),
        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle =
            androidx.compose.ui.text.style.LineHeightStyle(
                alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
                mode = androidx.compose.ui.text.style.LineHeightStyle.Mode.Tight,
            ),
    )
internal val CatalogJapaneseColor: Color
    @Composable
    get() =
        if (MaterialTheme.colorScheme.background.luminance() < .5f) Color(0xFFE4D4AE)
        else Color(0xFF526348)
