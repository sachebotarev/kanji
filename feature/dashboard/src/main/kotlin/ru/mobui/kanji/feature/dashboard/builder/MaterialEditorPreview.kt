package ru.mobui.kanji.feature.dashboard.builder

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogLink
import ru.mobui.kanji.domain.catalog.CatalogPlacement
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogSource
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.RecipeMaterial

@Preview(name = "Русский · крупный текст", locale = "ru", fontScale = 1.5f, showBackground = true)
@Preview(name = "English · dark", locale = "en", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun MaterialEditorPreview() {
    val sun = '日'.code
    val snapshot = CatalogSnapshot(1, CatalogContent(
        listOf(CatalogSource(1, "日本語", mapOf("ru" to "Учебник японского языка", "en" to "Japanese textbook"), sourceType = "textbook"),
            CatalogSource(-1, "Personal", mapOf("ru" to "Мой словарь", "en" to "My dictionary"), sourceType = "personal")),
        listOf(CatalogWord(1, "日", "ひ", mapOf("ru" to "солнце", "en" to "sun")),
            CatalogWord(-1, "こんにちは", "こんにちは", mapOf("ru" to "здравствуйте", "en" to "hello"))),
        listOf(CatalogKanji(sun, "日", 4, null, false, true, false,
            mapOf("ru" to listOf("солнце"), "en" to listOf("sun")), emptyList(), emptyList())),
        emptyList(), listOf(CatalogPlacement(1, sun, 1, 1)), listOf(CatalogLink(1, 1, sun), CatalogLink(-1, -1, null))))
    KanjiTheme {
        MaterialEditorScreen(RecipeMaterial("preview", MaterialType.Book, sourceId = 1), snapshot, emptyList(), emptyList(),
            LocalConfiguration.current.locales[0].language, {}, {}, {}, {})
    }
}
