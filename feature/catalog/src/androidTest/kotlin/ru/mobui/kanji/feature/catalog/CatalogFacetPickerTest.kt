package ru.mobui.kanji.feature.catalog

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import java.util.Locale
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogPlacement
import ru.mobui.kanji.domain.catalog.CatalogRadical
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogSource
import ru.mobui.kanji.domain.catalog.JlptLevel

class CatalogFacetPickerTest {
    @get:Rule val compose = createComposeRule()

    private fun start(restoration: StateRestorationTester? = null, source: Int = 0) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("ru")) }
        val localized = context.createConfigurationContext(config)
        fun kanji(id: Int, symbol: String, strokes: Int, radical: Int, level: JlptLevel?) =
            CatalogKanji(id, symbol, strokes, radical, false, false, false, mapOf("ru" to listOf(symbol)), emptyList(), emptyList(), jlptLevel = level)
        val data = CatalogSnapshot(1, CatalogContent(
            listOf(CatalogSource(1, "first", mapOf("ru" to "Первый")), CatalogSource(2, "second", mapOf("ru" to "Второй"))), emptyList(),
            listOf(kanji(101, "一", 1, 1, JlptLevel.N5), kanji(102, "二", 2, 2, JlptLevel.N4),
                kanji(103, "三", 3, 3, null), kanji(104, "四", 4, 4, JlptLevel.N1)),
            listOf(CatalogRadical(1, "一", 1, mapOf("ru" to "Один")), CatalogRadical(2, "二", 2, mapOf("ru" to "Два")),
                CatalogRadical(3, "口", 3, mapOf("ru" to "Рот")), CatalogRadical(4, "日", 4, mapOf("ru" to "Солнце"))),
            listOf(CatalogPlacement(1, 101, 1, 1), CatalogPlacement(1, 102, 2, 2), CatalogPlacement(1, 103, 3, 2), CatalogPlacement(2, 104, 1, 1)),
                emptyList()))
        val repo = object : CatalogRepository { override suspend fun load() = data }
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides config) {
                KanjiTheme { CatalogRoute(repo, {}, initialSourceId = source) }
            }
        }
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
        if (source == 0) compose.onNodeWithTag("catalog-all-kanji").performClick()
        else compose.onNodeWithText("Кандзи").performClick()
    }

    private fun click(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (tag.startsWith("catalog-filter-")) {
            compose.onAllNodes(hasAnyAncestor(hasTestTag(tag)) and hasClickAction()).onFirst()
                .performSemanticsAction(SemanticsActions.OnClick)
        } else node.performSemanticsAction(SemanticsActions.OnClick)
    }

    @Test fun radicalsCancelRestoreHiddenSearchAndRetainZeroResultSelectedValues() {
        val restoration = StateRestorationTester(compose)
        start(restoration)
        click("catalog-filter-radical")
        click("kanji-1")
        click("catalog-back")
        compose.onNodeWithTag("kanji-104").assertIsDisplayed()
        click("catalog-filter-radical")
        compose.onNodeWithTag("kanji-1").assertIsNotSelected()
        click("kanji-1")
        click("catalog-search-toggle")
        compose.onNodeWithTag("facet-search").performTextInput("ДВА")
        compose.onNodeWithTag("kanji-1").assertDoesNotExist()
        click("kanji-2")
        restoration.emulateSavedInstanceStateRestore()
        click("facet-apply")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("kanji-101").assertIsDisplayed()
        compose.onNodeWithTag("kanji-102").assertIsDisplayed()
        compose.onNodeWithTag("kanji-103").assertDoesNotExist()
        compose.onNodeWithText("Ключ 一、二").assertIsDisplayed()
        click("catalog-filter-strokes")
        compose.onNodeWithTag("facet-option-3").assertDoesNotExist()
        click("facet-option-1")
        click("facet-apply")
        click("catalog-filter-radical")
        compose.onNodeWithTag("kanji-1").assertIsSelected()
        compose.onNodeWithTag("kanji-2").assertIsSelected()
        compose.onNodeWithTag("kanji-3").assertDoesNotExist()
        click("facet-clear")
        click("facet-apply")
        compose.onNodeWithTag("kanji-101").assertIsDisplayed()
        compose.onNodeWithTag("kanji-102").assertDoesNotExist()
    }

    @Test fun strokesAndJlptSetsIntersectAndClearIndependentlyIncludingNoData() {
        val restoration = StateRestorationTester(compose)
        start(restoration)
        click("catalog-filter-strokes")
        click("facet-option-1")
        click("catalog-search-toggle")
        compose.onNodeWithTag("facet-search").performTextInput("２")
        click("facet-option-2")
        click("facet-apply")
        click("catalog-filter-jlpt")
        compose.onNodeWithTag("facet-option-1").assertExists() // Zero count remains available.
        click("facet-option-5")
        click("facet-option-4")
        click("facet-option--1")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("facet-option-5").assertIsSelected()
        compose.onNodeWithTag("facet-option-4").assertIsSelected()
        compose.onNodeWithTag("facet-option--1").assertIsSelected()
        click("facet-apply")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("kanji-101").assertIsDisplayed()
        compose.onNodeWithTag("kanji-102").assertIsDisplayed()
        compose.onNodeWithTag("kanji-103").assertDoesNotExist()
        compose.onNodeWithText("JLPT N5、JLPT N4 +1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Сбросить: 1 черта、2 черты").performClick()
        compose.onNodeWithTag("kanji-103").assertIsDisplayed()
        compose.onNodeWithTag("kanji-104").assertDoesNotExist()
        click("catalog-filter-jlpt")
        click("facet-clear")
        click("catalog-back") // Clear is cancelled too.
        compose.onNodeWithTag("kanji-104").assertDoesNotExist()
        click("catalog-filter-jlpt")
        click("facet-clear")
        click("facet-apply")
        compose.onNodeWithTag("kanji-104").assertIsDisplayed()
    }

    @Test fun chaptersAndOtherFacetSetsTravelThroughTypedRoutesInTextbookScope() {
        val restoration = StateRestorationTester(compose)
        start(restoration, source = 1)
        click("catalog-filter-chapter")
        click("chapter-1")
        click("chapter-2")
        click("chapter-apply")
        click("catalog-filter-radical")
        compose.onNodeWithTag("kanji-4").assertDoesNotExist()
        click("kanji-1")
        click("kanji-2")
        click("facet-apply")
        click("catalog-filter-strokes")
        click("facet-option-1")
        click("facet-option-2")
        click("facet-apply")
        restoration.emulateSavedInstanceStateRestore()
        click("catalog-filter-chapter")
        compose.onNodeWithTag("chapter-1").assertIsSelected()
        compose.onNodeWithTag("chapter-2").assertIsSelected()
        click("chapter-apply")
        click("catalog-filter-radical")
        compose.onNodeWithTag("kanji-1").assertIsSelected()
        compose.onNodeWithTag("kanji-2").assertIsSelected()
        click("facet-apply")
        compose.onNodeWithTag("kanji-101").assertIsDisplayed()
        compose.onNodeWithTag("kanji-102").assertIsDisplayed()
        compose.onNodeWithTag("kanji-103").assertDoesNotExist()
        compose.onNodeWithTag("kanji-104").assertDoesNotExist()
    }
}
