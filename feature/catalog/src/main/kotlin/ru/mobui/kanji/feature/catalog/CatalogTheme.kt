package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.sp

/** Scoped to the catalog: changing its palette does not restyle other features. */
@Composable
internal fun CatalogTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val dark = darkTheme
    val base = MaterialTheme.colorScheme
    val colors = if (dark) base.copy(
        background = Color(0xFF232C24), surface = Color(0xFF232C24),
        surfaceContainerLow = Color(0xFF29332B), surfaceContainerHigh = Color(0xFF354334),
        surfaceContainerHighest = Color(0xFF40513C), surfaceContainer = Color(0xFF2B372D),
        onBackground = Color(0xFFF0EFE2), onSurface = Color(0xFFF0EFE2), onSurfaceVariant = Color(0xFFC1CCBC),
        primary = Color(0xFFB2CEA8), onPrimary = Color(0xFF19351C), primaryContainer = Color(0xFF244F32),
        secondaryContainer = Color(0xFF2B372D), onSecondaryContainer = Color(0xFFF0EFE2),
        error = Color(0xFFFFB59F), outline = Color(0xFF63735F),
    ) else base.copy(
        background = Color(0xFFFAF8F1), surface = Color(0xFFFAF8F1),
        surfaceContainerLow = Color(0xFFF1F2EB), surfaceContainerHigh = Color(0xFFE1E7D9),
        surfaceContainerHighest = Color(0xFFD2DDC9), surfaceContainer = Color(0xFFEEF0E7),
        onBackground = Color(0xFF263E36), onSurface = Color(0xFF263E36), onSurfaceVariant = Color(0xFF4A5C52),
        primary = Color(0xFF315F47), onPrimary = Color.White, primaryContainer = Color(0xFFDDEDD8),
        secondaryContainer = Color(0xFFEEF0E7), onSecondaryContainer = Color(0xFF263E36),
        error = Color(0xFFA64935), outline = Color(0xFFA8B3A4),
    )
    val typography = MaterialTheme.typography
    MaterialTheme(colorScheme = colors, typography = typography.copy(
        bodyMedium = typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp, platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)),
        bodySmall = typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp, platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)),
        bodyLarge = typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.sp, platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)),
        titleMedium = typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.sp, platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)),
        titleLarge = typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp, platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)),
        labelLarge = typography.labelLarge.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp, platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)),
    ), content = content)
}

internal val CatalogJapaneseFont = FontFamily(Font(R.font.catalog_japanese))
internal val CatalogJapaneseStyle = TextStyle(fontFamily = CatalogJapaneseFont, localeList = LocaleList("ja-JP"),
    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
        alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
        trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
        mode = androidx.compose.ui.text.style.LineHeightStyle.Mode.Tight))
internal val CatalogJapaneseColor: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < .5f) Color(0xFFE4D4AE) else Color(0xFF526348)
