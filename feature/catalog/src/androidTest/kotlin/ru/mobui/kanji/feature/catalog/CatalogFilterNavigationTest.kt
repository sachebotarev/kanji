package ru.mobui.kanji.feature.catalog

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.*
import java.util.Locale

class CatalogFilterNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun switchingToWordsDoesNotRestrictChapterChoicesByHiddenStrokeFilter() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("ru")) }
        val localized = context.createConfigurationContext(configuration)
        val data = CatalogSnapshot(1, listOf(CatalogSource(1, "Учебник", emptyMap())),
            listOf(CatalogWord(1, "一", "いち", mapOf("ru" to "один")), CatalogWord(2, "二", "に", mapOf("ru" to "два"))),
            listOf(kanji(1, "一", 1), kanji(2, "二", 2)), emptyList(),
            listOf(CatalogPlacement(1, 1, 1, 3), CatalogPlacement(1, 2, 2, 4)),
            listOf(CatalogLink(1, 1, 1), CatalogLink(1, 2, 2)))
        val repository = object : CatalogRepository { override suspend fun load() = data }
        compose.setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration) {
                KanjiTheme { CatalogRoute(repository, {}, initialSourceId = 1) }
            }
        }
        compose.onNodeWithText("Кандзи").performClick()
        compose.onNodeWithText("Все черты").performClick()
        compose.onNodeWithText("1 черта").performClick()
        compose.onNodeWithTag("kanji-2").assertDoesNotExist()
        compose.onNodeWithText("Слова").performClick()
        compose.onNodeWithTag("word-2").assertIsDisplayed()
        compose.onNodeWithTag("catalog-filter-chapter").performClick()
        compose.onNodeWithTag("chapter-3").assertIsDisplayed()
        compose.onNodeWithTag("chapter-4").assertIsDisplayed()
        val wordCount = localized.resources.getQuantityString(R.plurals.catalog_word_count, 1, 1)
        compose.onNodeWithTag("chapter-3").assert(hasText(wordCount, substring = true))
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithText("Кандзи").performClick()
        compose.onNodeWithContentDescription("Сбросить: 1 черта").assertIsDisplayed()
        compose.onNodeWithTag("kanji-2").assertDoesNotExist()
        compose.onNodeWithTag("catalog-filter-chapter").performClick()
        compose.onNodeWithTag("chapter-3").assert(hasText(wordCount, substring = true).not())
    }

    private fun kanji(id: Int, symbol: String, strokes: Int) = CatalogKanji(id, symbol, strokes, 1,
        false, false, false, emptyMap(), emptyList(), emptyList())

    @Test fun numericFilterResultsKeepJlptAndStrokesIndependent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("ru")) }
        val localized = context.createConfigurationContext(configuration)
        val data = CatalogSnapshot(1, emptyList(), emptyList(),
            listOf(kanji(1, "一", 1).copy(jlptLevel = JlptLevel.N5),
                kanji(2, "二", 2).copy(jlptLevel = JlptLevel.N4)), emptyList(), emptyList(), emptyList())
        val repository = object : CatalogRepository { override suspend fun load() = data }
        compose.setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration) {
                KanjiTheme { CatalogRoute(repository, {}) }
            }
        }
        compose.onNodeWithTag("catalog-all-kanji").performClick()
        compose.onNodeWithText("JLPT").performClick()
        compose.onNodeWithText("JLPT N5").performClick()
        compose.onNodeWithTag("kanji-2").assertDoesNotExist()
        compose.onNodeWithText("Все черты").performClick()
        compose.onNodeWithText("1 черта").performClick()
        compose.onNodeWithContentDescription("Сбросить: JLPT N5").assertIsDisplayed()
        compose.onNodeWithContentDescription("Сбросить: 1 черта").performClick()
        compose.onNodeWithTag("kanji-2").assertDoesNotExist()
        compose.onNodeWithContentDescription("Сбросить: JLPT N5").performClick()
        compose.onNodeWithTag("kanji-2").assertIsDisplayed()
    }
}
