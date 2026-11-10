package ru.mobui.kanji.feature.dictionary

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.CatalogWord
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import ru.mobui.kanji.core.wordui.SymbolGridItem
import ru.mobui.kanji.core.wordui.WordPickerScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** Shared UI hosted here using the existing instrumented test stack, without a repository. */
class WordPickerTest {
    @get:Rule val compose = createComposeRule()
    private val sun = '日'.code
    private val moon = '月'.code
    private val words = listOf(
        CatalogWord(1, "日", "ひ", mapOf("ru" to "солнце", "en" to "Sun")),
        CatalogWord(2, "月", "つき", mapOf("ru" to "Луна LUNA")),
        CatalogWord(-3, "月がきれいですね。", "つきがきれいですね。", mapOf("ru" to "Какая красивая луна!")),
    )
    private val symbols = listOf(SymbolGridItem(sun, "日", "солнце"),
        SymbolGridItem(moon, "月", "луна"), SymbolGridItem('火'.code, "火", "огонь"))
    private val links = mapOf(1 to setOf(sun), 2 to setOf(moon), -3 to setOf(moon))
    private fun click(tag: String) = compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
    private fun openKanji() = compose.onNodeWithText("Кандзи", substring = true)
        .performSemanticsAction(SemanticsActions.OnClick)

    @Test fun selectionAndClearShareOneAccessibleRowAtLargeFont() {
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current.density
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(density, fontScale = 1.5f)) {
                KanjiTheme { WordPickerScreen(words, symbols, links, "ru", setOf(1), {}, {}) }
            }
        }
        val selected = compose.onNodeWithTag("word-picker-selected").getBoundsInRoot()
        val clear = compose.onNodeWithTag("word-picker-clear").getBoundsInRoot()
        assertEquals((selected.top.value + selected.bottom.value) / 2,
            (clear.top.value + clear.bottom.value) / 2, 1f)
        assertTrue(selected.bottom.value - selected.top.value >= 48f)
        assertTrue(clear.bottom.value - clear.top.value >= 48f)
        compose.onNodeWithTag("word-picker-all").assertDoesNotExist()
        click("word-picker-selected")
        compose.onNodeWithTag("word-picker-selected").assertDoesNotExist()
        compose.onNodeWithTag("word-picker-all").assertIsSelected()
        click("word-picker-all")
        compose.onNodeWithTag("word-2").assertExists()
    }

    @Test fun backDiscardsAndApplyPublishesOnlyExplicitWordIds() {
        var open by mutableStateOf(true)
        var result = setOf(1)
        compose.setContent { KanjiTheme {
            if (open) WordPickerScreen(words, symbols, links, "ru", result,
                { result = it; open = false }, { open = false })
            else TextButton({ open = true }, Modifier.testTag("reopen")) { Text("Open") }
        } }
        click("word-2")
        compose.onNodeWithTag("word-2").assertIsSelected()
        compose.runOnIdle { assertEquals(setOf(1), result) }
        click("word-picker-back")
        click("reopen")
        compose.onNodeWithTag("word-2").assertIsNotSelected()
        click("word--3")
        click("word-picker-apply")
        compose.runOnIdle { assertEquals(setOf(1, -3), result) }
    }

    @Test fun searchPreservesHiddenSelectionAndEmptyApplyIsAnEmptySet() {
        var result: Set<Int>? = null
        compose.setContent { KanjiTheme { WordPickerScreen(words, symbols, links, "en", setOf(1), { result = it }, {}) } }
        click("word-picker-search-toggle")
        compose.onNodeWithTag("word-picker-search").performTextReplacement("ＬＵＮＡ")
        compose.onNodeWithTag("word-1").assertDoesNotExist()
        compose.onNodeWithTag("word-2").assertExists()
        click("word-2")
        click("word-picker-apply")
        compose.runOnIdle { assertEquals(setOf(1, 2), result) }
        compose.onNodeWithTag("word-picker-search").performTextReplacement("ツキ")
        compose.onNodeWithTag("word--3").assertExists()
        compose.onNodeWithTag("word-picker-search").performTextReplacement("no result")
        click("word-picker-clear")
        compose.onNodeWithTag("word-picker-search").assertTextContains("no result")
        compose.onNodeWithTag("word-picker-clear").assertIsNotEnabled()
        click("word-picker-apply")
        compose.runOnIdle { assertEquals(emptySet<Int>(), result) }
    }

    @Test fun kanjiFilterStagesCancelsAndNeverRewritesChosenWords() {
        var result: Set<Int>? = null
        compose.setContent { KanjiTheme { WordPickerScreen(words, symbols, links, "ru", setOf(1), { result = it }, {}) } }
        openKanji()
        compose.onNodeWithTag("kanji-${'火'.code}").assertDoesNotExist()
        click("kanji-$moon")
        click("symbol-picker-back")
        compose.onNodeWithTag("word-1").assertIsSelected()
        openKanji()
        compose.onNodeWithTag("kanji-$moon").assertIsNotSelected()
        click("kanji-$moon")
        click("symbol-picker-apply")
        compose.onNodeWithTag("word-1").assertDoesNotExist()
        click("word-2")
        click("word-picker-selected")
        compose.onNodeWithTag("word--3").assertDoesNotExist()
        compose.onNodeWithTag("word-2").assertIsSelected()
        click("word-picker-apply")
        compose.runOnIdle { assertEquals(setOf(1, 2), result) }
        openKanji()
        click("kanji-$sun")
        click("symbol-picker-apply")
        compose.onNodeWithTag("word-1").assertIsSelected()
        compose.onNodeWithTag("word-2").assertIsSelected()
        click("word-picker-all")
        compose.onNodeWithTag("word--3").assertIsNotSelected()
    }

    @Test fun restoresWordDraftQueryAndNestedKanjiDraftWithoutPublishing() {
        val restore = StateRestorationTester(compose)
        var result: Set<Int>? = null
        restore.setContent { KanjiTheme { WordPickerScreen(words, symbols, links, "ru", emptySet(), { result = it }, {}) } }
        click("word-2")
        click("word-picker-search-toggle")
        compose.onNodeWithTag("word-picker-search").performTextReplacement("луна")
        openKanji()
        click("kanji-$moon")
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("kanji-$moon").assertIsSelected()
        assertNull(result)
        click("symbol-picker-apply")
        compose.onNodeWithTag("word-picker-search").assertTextContains("луна")
        compose.onNodeWithTag("word-2").assertIsSelected()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("word-1").assertDoesNotExist()
        compose.onNodeWithTag("word-2").assertIsSelected()
        click("word-picker-apply")
        compose.runOnIdle { assertEquals(setOf(2), result) }
    }

    @Test fun applyIntersectsOnlyCandidateScopeNotSearchOrFilters() {
        var pool by mutableStateOf(words)
        var result: Set<Int>? = null
        compose.setContent { KanjiTheme { WordPickerScreen(pool, symbols, links, "ru", setOf(1, -3, 999), { result = it }, {}) } }
        compose.runOnIdle { pool = words.filter { it.id != -3 } }
        click("word-picker-selected")
        compose.onNodeWithTag("word-1").assertIsSelected()
        compose.onNodeWithTag("word--3").assertDoesNotExist()
        click("word-picker-apply")
        compose.runOnIdle { assertEquals(setOf(1), result) }
    }
}
