package ru.mobui.kanji.feature.dictionary

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import ru.mobui.kanji.core.wordui.SymbolFacet
import ru.mobui.kanji.core.wordui.SymbolFacetOption
import ru.mobui.kanji.core.wordui.SymbolGridItem
import ru.mobui.kanji.core.wordui.SymbolPickerScreen
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogWord
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class SymbolPickerTest {
    @get:Rule val compose = createComposeRule()
    private val items = listOf(SymbolGridItem(1, "日", "солнце"), SymbolGridItem(2, "月", "луна", "月, луна LUNA"))

    @Test fun selectionIsStagedAndBackDiscardsIt() {
        var open by mutableStateOf(true)
        var applied = setOf(1)
        compose.setContent {
            KanjiTheme {
                if (open) SymbolPickerScreen(items, applied, { applied = it; open = false }, header = {
                    TextButton({ open = false }, Modifier.testTag("back")) { Text("Back") }
                }) else TextButton({ open = true }, Modifier.testTag("reopen")) { Text("Open") }
            }
        }
        compose.onNodeWithTag("kanji-2").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(setOf(1), applied) }
        compose.onNodeWithTag("back").performClick()
        compose.onNodeWithTag("reopen").performClick()
        compose.onNodeWithTag("kanji-2").assertIsNotSelected().performClick()
        compose.onNodeWithTag("symbol-picker-apply").performClick()
        compose.runOnIdle { assertEquals(setOf(1, 2), applied) }
    }

    @Test fun normalizedSearchDoesNotLoseHiddenChoiceAndClearIsExplicit() {
        var result: Set<Int>? = null
        compose.setContent { KanjiTheme {
            SymbolPickerScreen(items, setOf(1), { result = it }, header = { toggle ->
                TextButton(toggle, Modifier.testTag("search-toggle")) { Text("Search") }
            })
        } }
        compose.onNodeWithTag("search-toggle").performClick()
        compose.onNodeWithTag("symbol-picker-search").performTextReplacement("ＬＵＮＡ")
        compose.onNodeWithTag("kanji-1").assertDoesNotExist()
        compose.onNodeWithTag("kanji-2").assertExists()
        compose.onNodeWithTag("symbol-picker-search").performTextReplacement("ЛУНА")
        compose.onNodeWithTag("kanji-2").performClick()
        compose.onNodeWithTag("symbol-picker-apply").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        compose.runOnIdle { assertEquals(setOf(1, 2), result) }
        compose.onNodeWithTag("symbol-picker-clear").performClick()
        compose.onNodeWithTag("symbol-picker-apply").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        compose.runOnIdle { assertEquals(emptySet<Int>(), result) }
    }

    @Test fun restoresStagedSelectionWithoutPublishing() {
        val restoration = StateRestorationTester(compose)
        var result: Set<Int>? = null
        restoration.setContent { KanjiTheme { SymbolPickerScreen(items, emptySet(), { result = it }, header = {}) } }
        compose.onNodeWithTag("kanji-1").performClick()
        compose.onNodeWithTag("kanji-2").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("kanji-1").assertIsSelected()
        compose.onNodeWithTag("kanji-2").assertIsSelected()
        assertNull(result)
        compose.onNodeWithTag("symbol-picker-apply").performClick()
        compose.runOnIdle { assertEquals(setOf(1, 2), result) }
    }

    @Test fun facetsStageCancelCombineAndKeepHiddenSelection() {
        var applied: Set<Int>? = null
        val entries = items + SymbolGridItem(3, "火", "огонь")
        val facets = listOf(
            SymbolFacet("strokes", "Strokes", listOf(SymbolFacetOption(4, "4"), SymbolFacetOption(5, "5"))) { if (it.id == 3) 5 else 4 },
            SymbolFacet("jlpt", "JLPT", listOf(SymbolFacetOption(5, "N5"), SymbolFacetOption(4, "N4")), true) { if (it.id == 2) 4 else 5 },
        )
        compose.setContent { KanjiTheme {
            SymbolPickerScreen(entries, setOf(3), { applied = it }, header = {}, facets = facets,
                facetHeader = { _, back, _ -> TextButton(back, Modifier.testTag("facet-back")) { Text("Back") } })
        } }
        compose.onNodeWithText("Strokes").performClick()
        compose.onNodeWithTag("facet-option-4").performClick()
        compose.onNodeWithTag("facet-back").performClick()
        compose.onNodeWithTag("kanji-3").assertExists().assertIsSelected()
        compose.onNodeWithText("Strokes").performClick()
        compose.onNodeWithTag("facet-option-4").assertIsNotSelected().performClick()
        compose.onNodeWithTag("facet-apply").performClick()
        compose.onNodeWithTag("kanji-3").assertDoesNotExist()
        compose.onNodeWithTag("kanji-1").assertExists()
        compose.onNodeWithTag("kanji-2").assertExists()
        compose.onNodeWithText("JLPT").performClick()
        compose.onNodeWithTag("facet-option-5").performClick()
        compose.onNodeWithTag("facet-apply").performClick()
        compose.onNodeWithTag("kanji-2").assertDoesNotExist()
        compose.onNodeWithTag("kanji-1").assertExists()
        compose.onNodeWithText("Strokes", substring = true).performClick()
        compose.onNodeWithTag("facet-option-5").performClick()
        compose.onNodeWithTag("facet-apply").performClick()
        compose.onNodeWithTag("kanji-3").assertIsSelected()
        compose.onNodeWithTag("kanji-2").assertDoesNotExist()
        compose.onNodeWithText("Strokes · 4, 5").assertExists().performClick()
        compose.onNodeWithTag("facet-clear").performClick()
        compose.onNodeWithTag("facet-apply").performClick()
        compose.onNodeWithTag("kanji-2").assertDoesNotExist()
        compose.onNodeWithTag("kanji-1").assertExists()
        compose.onNodeWithTag("symbol-picker-apply").performClick()
        compose.runOnIdle { assertEquals(setOf(3), applied) }
    }

    @Test fun restoresNestedFacetDraftAndCancelDoesNotApplyIt() {
        val restoration = StateRestorationTester(compose)
        val facet = SymbolFacet("strokes", "Strokes", listOf(SymbolFacetOption(4, "4"), SymbolFacetOption(5, "5"))) { if (it.id == 1) 4 else 5 }
        restoration.setContent { KanjiTheme {
            SymbolPickerScreen(items, emptySet(), {}, header = {}, facets = listOf(facet),
                facetHeader = { _, back, _ -> TextButton(back, Modifier.testTag("facet-back")) { Text("Back") } })
        } }
        compose.onNodeWithText("Strokes").performClick()
        compose.onNodeWithTag("facet-option-4").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("facet-option-4").assertIsSelected()
        compose.onNodeWithTag("facet-back").performClick()
        compose.onNodeWithTag("kanji-2").assertExists()
    }

    @Test fun radicalGridUsesSymbolsAndSearchPreservesHiddenChoices() {
        val facet = SymbolFacet("radical", "Radical", listOf(
            SymbolFacetOption(10, "sun", "日", "日"), SymbolFacetOption(20, "moon", "月", "月"))) { if (it.id == 1) 10 else 20 }
        compose.setContent { KanjiTheme {
            SymbolPickerScreen(items, emptySet(), {}, header = {}, facets = listOf(facet),
                facetHeader = { _, _, search -> TextButton(search, Modifier.testTag("facet-search-toggle")) { Text("Search") } })
        } }
        compose.onNodeWithText("Radical").performClick()
        compose.onNodeWithTag("kanji-10").performClick().assertIsSelected()
        compose.onNodeWithTag("facet-search-toggle").performClick()
        compose.onNodeWithTag("facet-search").performTextReplacement("ＭＯＯＮ")
        compose.onNodeWithTag("kanji-10").assertDoesNotExist()
        compose.onNodeWithTag("kanji-20").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick).assertIsSelected()
        compose.onNodeWithTag("facet-apply").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        assertTrue(compose.onRoot().printToString(), compose.onAllNodesWithText("Radical · 日, 月").fetchSemanticsNodes().size == 1)
        compose.onNodeWithTag("kanji-1").assertExists()
        compose.onNodeWithTag("kanji-2").assertExists()
    }

    @Test fun dictionaryPoolContainsOnlySignsInPersonalEntries() {
        fun kanji(id: Int, symbol: String) = CatalogKanji(id, symbol, 4, null, false, false, false, mapOf("ru" to listOf(symbol)), emptyList(), emptyList())
        val snapshot = CatalogSnapshot(1, CatalogContent(emptyList(), emptyList(), listOf(kanji('日'.code, "日"), kanji('月'.code, "月"), kanji('火'.code, "火")),
            emptyList(), emptyList(), emptyList()))
        val state = DictionaryState(loading = false, snapshot = snapshot, words = listOf(CatalogWord(-1, "日月", "", mapOf("ru" to "день и месяц"),
            isUserCreated = true)))
        compose.setContent { KanjiTheme { DictionaryKanjiPicker(state, "ru", {}, {}) } }
        compose.onNodeWithTag("kanji-${'日'.code}").assertExists()
        compose.onNodeWithTag("kanji-${'月'.code}").assertExists()
        compose.onNodeWithTag("kanji-${'火'.code}").assertDoesNotExist()
    }
}
