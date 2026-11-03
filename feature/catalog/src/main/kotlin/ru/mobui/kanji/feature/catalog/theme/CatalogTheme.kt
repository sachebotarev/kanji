package ru.mobui.kanji.feature.catalog.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.LocaleList
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.feature.catalog.R

/**
 * Catalog theme wrapper delegating directly to the unified application theme [KanjiTheme].
 */
@Composable
internal fun CatalogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    KanjiTheme(darkTheme = darkTheme, content = content)
}

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
    get() = MaterialTheme.colorScheme.primary
