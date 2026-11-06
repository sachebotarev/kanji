package ru.mobui.kanji.feature.catalog

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.core.wordui.ChapterPickerItem
import ru.mobui.kanji.core.wordui.ChapterPickerScreen

class ChapterPickerTest {
    @get:Rule val compose = createComposeRule()
    private val entries = listOf(
        ChapterPickerItem(1, "Глава 1", "2 кандзи", "日 月"),
        ChapterPickerItem(2, "Глава 2", "1 кандзи", "木"),
        ChapterPickerItem(3, "Глава 3", "1 кандзи", "山"),
    )

    @Test fun choicesAreDraftUntilApplyAndBackDiscards() {
        var result: Set<Int>? = null
        val open = mutableStateOf(true)
        compose.setContent {
            KanjiTheme {
                if (open.value) ChapterPickerScreen(entries, setOf(1), { result = it }, { open.value = false })
            }
        }
        compose.onNodeWithTag("chapter-2").performClick()
        compose.runOnIdle { assertNull(result) }
        compose.onNodeWithTag("symbol-picker-back").performClick()
        compose.runOnIdle { open.value = true }
        compose.onNodeWithTag("chapter-1").assertIsSelected()
        compose.onNodeWithTag("chapter-2").performClick()
        compose.onNodeWithTag("chapter-apply").performClick()
        compose.runOnIdle { assertEquals(setOf(1, 2), result) }
    }

    @Test fun fullwidthNumberSearchKeepsHiddenChoiceAndClearAllowsEmpty() {
        var result: Set<Int>? = null
        compose.setContent { KanjiTheme { ChapterPickerScreen(entries, setOf(1), { result = it }, {}) } }
        compose.onNodeWithTag("symbol-picker-search-toggle").performClick()
        compose.onNodeWithTag("chapter-search").performTextReplacement("２")
        compose.onNodeWithTag("chapter-1").assertDoesNotExist()
        compose.onNodeWithTag("chapter-2").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithTag("chapter-apply").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { assertEquals(setOf(1, 2), result) }
        compose.onNodeWithTag("chapter-clear").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithTag("chapter-apply").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { assertEquals(emptySet<Int>(), result) }
    }

    @Test fun draftRestoresAndCannotPublishOutsideCandidatePool() {
        var result: Set<Int>? = null
        val restore = StateRestorationTester(compose)
        restore.setContent { KanjiTheme { ChapterPickerScreen(entries, setOf(1, 99), { result = it }, {}) } }
        compose.onNodeWithTag("chapter-2").performClick()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("chapter-2").assertIsSelected()
        compose.runOnIdle { assertNull(result) }
        compose.onNodeWithTag("chapter-apply").performClick()
        compose.runOnIdle { assertEquals(setOf(1, 2), result) }
    }
}
