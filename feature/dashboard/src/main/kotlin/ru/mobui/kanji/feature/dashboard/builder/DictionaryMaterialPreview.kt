package ru.mobui.kanji.feature.dashboard.builder

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogLink
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogSource
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.TagColor
import ru.mobui.kanji.domain.catalog.TagTranslation
import ru.mobui.kanji.domain.catalog.WordTagAssignment

@Preview(name = "Русский · крупный текст", locale = "ru", fontScale = 1.5f, showBackground = true)
@Preview(name = "English · dark", locale = "en", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun DictionaryMaterialPreview() {
    val id = '日'.code
    val snapshot = CatalogSnapshot(1, CatalogContent(
        listOf(CatalogSource(-1, "Personal", mapOf("ru" to "Мой словарь", "en" to "My dictionary"), sourceType = "personal")),
        listOf(CatalogWord(-1, "日", "ひ", mapOf("ru" to "солнце", "en" to "sun"))),
        listOf(CatalogKanji(id, "日", 4, null, false, true, false,
            mapOf("ru" to listOf("солнце"), "en" to listOf("sun")), emptyList(), emptyList())),
        emptyList(), emptyList(), listOf(CatalogLink(-1, -1, null))))
    val tag = DictionaryTag(1, TagColor.Green, listOf(TagTranslation("ru", "Повторить"), TagTranslation("en", "Review")))
    KanjiTheme {
        DictionaryMaterialEditorScreen(RecipeMaterial("preview", MaterialType.Dictionary, sourceId = -1, tagIds = setOf(1), kanjiIds = setOf(id)),
            snapshot, listOf(tag), listOf(WordTagAssignment(-1, setOf(1))), LocalConfiguration.current.locales[0].language,
            {}, {}, {}, {})
    }
}
