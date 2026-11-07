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
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class WordMaterialEditorTest {
    @get:Rule val compose = createComposeRule()
    private val words = listOf(
        CatalogWord(1, "日", "ひ", mapOf("ru" to "солнце")),
        CatalogWord(-2, "月がきれいですね。", "つきがきれいですね。", mapOf("ru" to "Какая красивая луна!")),
    )
    private val symbols = listOf(SymbolGridItem('日'.code, "日", "солнце"), SymbolGridItem('月'.code, "月", "луна"))
    private val links = mapOf(1 to setOf('日'.code), -2 to setOf('月'.code))
    private fun initial(ids: Set<Int> = setOf(1)) = RecipeMaterial("material", MaterialType.Words, wordIds = ids)
    private fun click(tag: String) = compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)

    @Test fun wordApplyOnlyUpdatesLocalMaterialAndFinalApplyKeepsItsId() {
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            WordMaterialEditorScreen(initial(), words, symbols, links, "ru", { result = it }, {}, {}, editing = true)
        } }
        click("material-words")
        click("word--2")
        click("word-picker-apply")
        compose.runOnIdle { assertNull(result) }
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(setOf(1, -2)), result) }
    }

    @Test fun cancellingNestedPickerAndWholeFormDoesNotPublish() {
        var open by mutableStateOf(true)
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            if (open) WordMaterialEditorScreen(initial(), words, symbols, links, "ru", { result = it }, { open = false }, {})
            else TextButton({ open = true }, Modifier.testTag("reopen")) { Text("Open") }
        } }
        click("material-words")
        click("word--2")
        click("word-picker-back")
        click("material-words")
        compose.onNodeWithTag("word--2").assertIsNotSelected()
        click("word--2")
        click("word-picker-apply")
        click("material-back")
        compose.runOnIdle { assertNull(result) }
        click("reopen")
        click("material-words")
        compose.onNodeWithTag("word--2").assertIsNotSelected()
    }

    @Test fun emptyMaterialCannotBeAddedAndSelectionRepairsError() {
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            WordMaterialEditorScreen(initial(emptySet()), words, symbols, links, "ru", { result = it }, {}, {})
        } }
        compose.onNodeWithTag("material-preview").assertDoesNotExist()
        click("material-apply")
        compose.onNodeWithTag("material-error").assertExists()
        compose.runOnIdle { assertNull(result) }
        click("material-words")
        click("word--2")
        click("word-picker-apply")
        compose.onNodeWithTag("material-error").assertDoesNotExist()
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(setOf(-2)), result) }
    }

    @Test fun previewContainsOnlyChosenWordsAndDelegatesDetails() {
        var viewed: Int? = null
        compose.setContent { KanjiTheme {
            WordMaterialEditorScreen(initial(setOf(-2)), words, symbols, links, "ru", {}, {}, { viewed = it })
        } }
        click("material-preview")
        compose.onNodeWithTag("word-1").assertDoesNotExist()
        click("word--2")
        compose.runOnIdle { assertEquals(-2, viewed) }
        click("material-back")
        compose.onNodeWithTag("material-apply").assertExists()
    }

    @Test fun restorationKeepsOpenPickerAndBothDraftLevelsWithoutPublishing() {
        val restoration = StateRestorationTester(compose)
        var result: RecipeMaterial? = null
        restoration.setContent { KanjiTheme {
            WordMaterialEditorScreen(initial(), words, symbols, links, "ru", { result = it }, {}, {})
        } }
        click("material-words")
        click("word--2")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("word--2").assertIsSelected()
        compose.runOnIdle { assertNull(result) }
        click("word-picker-apply")
        restoration.emulateSavedInstanceStateRestore()
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(setOf(1, -2)), result) }
    }
}
