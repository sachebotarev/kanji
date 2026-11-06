package ru.mobui.kanji.feature.catalog

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.util.Locale
import ru.mobui.kanji.core.designsystem.KanjiTheme
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogLink
import ru.mobui.kanji.domain.catalog.CatalogPlacement
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogSource
import ru.mobui.kanji.domain.catalog.CatalogWord

class CatalogKanjiPickerTest {
    @get:Rule val compose = createComposeRule()

    private fun start(restoration: StateRestorationTester? = null) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("ru")) }
        val localized = context.createConfigurationContext(config)
        val entries = listOf("一", "二", "三", "火").mapIndexed { index, symbol ->
            CatalogKanji(symbol.codePointAt(0), symbol, index + 1, 1, false, false, false,
                mapOf("ru" to listOf("Знак ${index + 1}")), emptyList(), emptyList())
        }
        val snapshot = CatalogSnapshot(1, CatalogContent(
            listOf(CatalogSource(1, "Учебник", emptyMap()), CatalogSource(2, "Другой учебник", emptyMap())),
            entries.mapIndexed { index, item -> CatalogWord(index + 1, item.symbol, "чтение", mapOf("ru" to "Значение ${index + 1}")) },
            entries, emptyList(), entries.mapIndexed { index, item -> CatalogPlacement(if (index == 3) 2 else 1, item.id, index + 1, if (index < 2) 3 else 4) },
            entries.mapIndexed { index, item -> CatalogLink(if (index == 3) 2 else 1, index + 1, item.id) }))
        val repository = object : CatalogRepository { override suspend fun load() = snapshot }
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides config) {
                KanjiTheme { CatalogRoute(repository, {}, initialSourceId = 1) }
            }
        }
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
        compose.onNodeWithText("Слова").performClick()
    }

    @Test fun multipleSelectionAppliesOnceBackCancelsAndChipClears() {
        start()
        compose.onNodeWithTag("catalog-filter-kanji").performClick()
        compose.onNodeWithTag("kanji-28779").assertDoesNotExist()
        compose.onNodeWithTag("kanji-19968").performClick()
        compose.onNodeWithTag("kanji-20108").performClick()
        compose.onNodeWithTag("symbol-picker-apply").assertExists()
        compose.onNodeWithTag("catalog-back").performClick()
        compose.onNodeWithTag("word-3").assertIsDisplayed()
        compose.onNodeWithTag("catalog-filter-kanji").performClick()
        compose.onNodeWithTag("kanji-19968").assertIsNotSelected()
        compose.onNodeWithTag("kanji-19968").performClick()
        compose.onNodeWithTag("kanji-20108").performClick()
        compose.onNodeWithTag("symbol-picker-apply").performClick()
        compose.onNodeWithTag("word-1").assertIsDisplayed()
        compose.onNodeWithTag("word-2").assertIsDisplayed()
        compose.onNodeWithTag("word-3").assertDoesNotExist()
        compose.onNodeWithText("Кандзи 一、二").assertIsDisplayed()
        compose.onNodeWithTag("catalog-filter-chapter").performClick()
        compose.onNodeWithTag("chapter-3").assertIsDisplayed()
        compose.onNodeWithTag("chapter-4").assertDoesNotExist()
        compose.onNodeWithTag("catalog-back").performClick()
        compose.onNodeWithContentDescription("Сбросить: Кандзи 一、二").performClick()
        compose.onNodeWithTag("word-3").assertIsDisplayed()
    }

    @Test fun eligiblePoolHonorsChapterAndClearingKanjiKeepsChapter() {
        start()
        compose.onNodeWithTag("catalog-filter-chapter").performClick()
        compose.onNodeWithTag("chapter-3").performClick()
        compose.onNodeWithTag("chapter-apply").performClick()
        compose.onNodeWithTag("catalog-filter-kanji").performClick()
        compose.onNodeWithTag("kanji-19977").assertDoesNotExist()
        compose.onNodeWithTag("kanji-19968").performClick()
        compose.onNodeWithTag("symbol-picker-apply").performClick()
        compose.onNodeWithTag("word-2").assertDoesNotExist()
        compose.onNodeWithTag("catalog-filter-kanji").performClick()
        compose.onNodeWithTag("symbol-picker-clear").performClick()
        compose.onNodeWithTag("symbol-picker-apply").performClick()
        compose.onNodeWithTag("word-2").assertIsDisplayed()
        compose.onNodeWithTag("word-3").assertDoesNotExist()
    }

    @Test fun chapterSetsStageRestoreAndIntersectKanjiWithoutClearingOtherFilters() {
        val restoration = StateRestorationTester(compose)
        start(restoration)
        compose.onNodeWithTag("catalog-filter-chapter").performClick()
        compose.onNodeWithTag("chapter-3").performClick()
        compose.onNodeWithTag("catalog-back").performClick()
        compose.onNodeWithTag("word-3").assertIsDisplayed()
        compose.onNodeWithTag("catalog-filter-chapter").performClick()
        compose.onNodeWithTag("chapter-3").assertIsNotSelected().performClick()
        compose.onNodeWithTag("chapter-4").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("chapter-3").assertIsSelected()
        compose.onNodeWithTag("chapter-4").assertIsSelected()
        compose.onNodeWithTag("chapter-apply").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Главы: 3、4").assertIsDisplayed()
        compose.onNodeWithTag("catalog-filter-kanji").performClick()
        compose.onNodeWithTag("kanji-19968").assertIsDisplayed()
        compose.onNodeWithTag("kanji-19977").performClick()
        compose.onNodeWithTag("symbol-picker-apply").performClick()
        compose.onNodeWithTag("word-3").assertIsDisplayed()
        compose.onNodeWithTag("word-1").assertDoesNotExist()
        compose.onNodeWithContentDescription("Сбросить: Главы: 3、4").performClick()
        compose.onNodeWithTag("word-3").assertIsDisplayed()
        compose.onNodeWithTag("word-1").assertDoesNotExist()
        compose.onNodeWithTag("catalog-filter-chapter").performClick()
        compose.onNodeWithTag("chapter-3").assertDoesNotExist()
        compose.onNodeWithTag("chapter-4").assertIsNotSelected()
    }

    @Test fun kanjiChapterRoutePreservesBothChaptersAcrossOtherFacetRoutes() {
        start()
        compose.onNodeWithText("Кандзи").performClick()
        compose.onNodeWithTag("catalog-filter-chapter").performClick()
        compose.onNodeWithTag("chapter-3").performClick()
        compose.onNodeWithTag("chapter-4").performClick()
        compose.onNodeWithTag("chapter-apply").performClick()
        compose.onNodeWithTag("kanji-19977").assertIsDisplayed()
        compose.onNodeWithText("Все черты").performClick()
        compose.onNodeWithTag("facet-option-1").performClick()
        compose.onNodeWithTag("facet-apply").performClick()
        compose.onNodeWithText("Главы: 3、4").assertIsDisplayed()
        compose.onNodeWithTag("catalog-filter-chapter").performClick()
        compose.onNodeWithTag("chapter-3").assertIsSelected()
        compose.onNodeWithTag("chapter-4").assertIsSelected()
        compose.onNodeWithTag("catalog-back").performClick()
        compose.onNodeWithContentDescription("Сбросить: 1 черта").performClick()
        compose.onNodeWithTag("kanji-19977").assertIsDisplayed()
    }

    @Test fun draftAndAppliedSelectionSurviveSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        start(restoration)
        compose.onNodeWithTag("catalog-filter-kanji").performClick()
        compose.onNodeWithTag("kanji-19968").performClick()
        compose.onNodeWithTag("kanji-20108").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("kanji-19968").assertIsSelected()
        compose.onNodeWithTag("kanji-20108").assertIsSelected()
        compose.onNodeWithTag("symbol-picker-apply").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("catalog-filter-kanji").performClick()
        compose.onNodeWithTag("kanji-19968").assertIsSelected()
        compose.onNodeWithTag("kanji-20108").assertIsSelected()
    }
}
