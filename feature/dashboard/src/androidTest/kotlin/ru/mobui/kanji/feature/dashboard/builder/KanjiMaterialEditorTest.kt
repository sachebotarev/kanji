package ru.mobui.kanji.feature.dashboard.builder

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
import ru.mobui.kanji.core.wordui.SymbolGridItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class KanjiMaterialEditorTest {
    @get:Rule val compose = createComposeRule()
    private val sun = '日'.code
    private val moon = '月'.code
    private val fire = '火'.code
    private val words = listOf(
        CatalogWord(1, "日", "ひ", mapOf("ru" to "солнце")),
        CatalogWord(2, "月", "つき", mapOf("ru" to "луна")),
        CatalogWord(-3, "日と月", "ひとつき", mapOf("ru" to "Солнце и луна")),
        CatalogWord(-4, "ありがとう", "ありがとう", mapOf("ru" to "спасибо")),
    )
    private val entries = listOf(SymbolGridItem(sun, "日", "солнце"), SymbolGridItem(moon, "月", "луна"),
        SymbolGridItem(fire, "火", "огонь"))
    private val links = mapOf(1 to setOf(sun), 2 to setOf(moon), -3 to setOf(sun, moon))
    private fun initial(ids: Set<Int> = emptySet()) = RecipeMaterial("kanji-material", MaterialType.Kanji, kanjiIds = ids)
    private fun click(tag: String) = compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)

    @Test fun unrestrictedPoolIncludesPersonalKanaWordsAndDeduplicatesById() {
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            KanjiMaterialEditorScreen(initial(), words + words.first(), entries, links, "ru", { result = it }, {}, {})
        } }
        click("material-preview")
        words.forEach { compose.onNodeWithTag("word-${it.id}").assertExists() }
        compose.onAllNodesWithTag("word-1").assertCountEquals(1)
        click("material-back")
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(), result) }
    }

    @Test fun symbolsUseOrAndOnlyFinalApplyPublishesKanjiIds() {
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            KanjiMaterialEditorScreen(initial(setOf(sun)), words, entries, links, "ru", { result = it }, {}, {}, editing = true)
        } }
        click("material-kanji")
        click("kanji-$moon")
        click("symbol-picker-apply")
        compose.runOnIdle { assertNull(result) }
        click("material-preview")
        listOf(1, 2, -3).forEach { compose.onNodeWithTag("word-$it").assertExists() }
        compose.onNodeWithTag("word--4").assertDoesNotExist()
        click("material-back")
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(setOf(sun, moon)), result) }
    }

    @Test fun nestedCancelAndFormCancelDiscardDifferentDraftLevels() {
        var open by mutableStateOf(true)
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            if (open) KanjiMaterialEditorScreen(initial(setOf(sun)), words, entries, links, "ru",
                { result = it }, { open = false }, {})
            else TextButton({ open = true }, Modifier.testTag("reopen")) { Text("Open") }
        } }
        click("material-kanji")
        click("kanji-$moon")
        click("symbol-picker-back")
        click("material-kanji")
        compose.onNodeWithTag("kanji-$moon").assertIsNotSelected()
        click("kanji-$moon")
        click("symbol-picker-apply")
        click("material-back")
        click("reopen")
        click("material-kanji")
        compose.onNodeWithTag("kanji-$moon").assertIsNotSelected()
        compose.runOnIdle { assertNull(result) }
    }

    @Test fun emptyResultIsBlockedButEmptySelectionMeansAllWords() {
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            KanjiMaterialEditorScreen(initial(setOf(fire)), words, entries, links, "ru", { result = it }, {}, {})
        } }
        compose.onNodeWithTag("material-preview").assertDoesNotExist()
        click("material-apply")
        compose.onNodeWithTag("material-error").assertExists()
        compose.runOnIdle { assertNull(result) }
        click("material-kanji")
        click("symbol-picker-clear")
        click("symbol-picker-apply")
        compose.onNodeWithTag("material-error").assertDoesNotExist()
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(), result) }
    }

    @Test fun restoreKeepsOpenSelectionDraftAndAppliedFormDraft() {
        val restoration = StateRestorationTester(compose)
        var result: RecipeMaterial? = null
        restoration.setContent { KanjiTheme {
            KanjiMaterialEditorScreen(initial(setOf(sun)), words, entries, links, "ru", { result = it }, {}, {})
        } }
        click("material-kanji")
        click("kanji-$moon")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("kanji-$moon").assertIsSelected()
        compose.runOnIdle { assertNull(result) }
        click("symbol-picker-apply")
        restoration.emulateSavedInstanceStateRestore()
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(setOf(sun, moon)), result) }
    }

    @Test fun previewDelegatesPersonalWordDetailsAndReturnsToSameDraft() {
        var viewed: Int? = null
        compose.setContent { KanjiTheme {
            KanjiMaterialEditorScreen(initial(setOf(moon)), words, entries, links, "ru", {}, {}, { viewed = it })
        } }
        click("material-preview")
        click("word--3")
        compose.runOnIdle { assertEquals(-3, viewed) }
        click("material-back")
        click("material-kanji")
        compose.onNodeWithTag("kanji-$moon").assertIsSelected()
        compose.onNodeWithTag("kanji-$sun").assertIsNotSelected()
    }
}
