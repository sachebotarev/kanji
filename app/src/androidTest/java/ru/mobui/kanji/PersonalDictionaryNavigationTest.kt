package ru.mobui.kanji

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class PersonalDictionaryNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun waitTag(tag: String) {
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun screenshot(name: String) {
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(350) // Allow the platform window/dialog transition to reach its final frame.
        val image =
            requireNotNull(
                InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot(),
            )
        val directory = File(compose.activity.filesDir, "personal-review").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun createsRestoresEditsSearchesAndDeletesManualWord() {
        val repository = (compose.activity.application as KanjiApplication).dictionary
        val written = runBlocking {
            if (repository.observeWords().first().any { it.written == "木漏れ日" })
                "木漏れ日${System.nanoTime()}"
            else "木漏れ日"
        }
        try {
            waitTag("open-dictionary")
            compose.onNodeWithTag("open-dictionary").bringIntoViewIfNeeded().performClick()
            waitTag("add-word")
            screenshot("empty")
            compose.onNodeWithTag("add-word").performClick()
            waitTag("word-editor")
            screenshot("create")
            compose.onNodeWithTag("word-input-written").performTextInput(written)
            compose.onNodeWithTag("word-input-reading").performTextInput("こもれび")
            compose
                .onNodeWithTag("editor-current-translation")
                .bringIntoViewIfNeeded()
                .performTextInput("солнечный свет сквозь листву")
            Espresso.closeSoftKeyboard()
            compose.onNodeWithTag("editor-other-translations").bringIntoViewIfNeeded().performClick()
            waitTag("dictionary-translations")
            compose.onNodeWithTag("translation-add").performClick()
            compose.onNodeWithTag("translation-text").performTextInput("sunlight through leaves")
            compose.onNodeWithTag("translation-confirm").performClick()
            waitTag("translation-en")
            compose.onNodeWithTag("dictionary-back").performClick()
            waitTag("word-editor")
            compose.onNodeWithTag("save-word").bringIntoViewIfNeeded().performClick()
            // New words pop to the list, not the detail card.
            waitTag("add-word")
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText(written).fetchSemanticsNodes().isNotEmpty()
            }
            screenshot("word")
            compose.activityRule.scenario.recreate()
            waitTag("add-word")
            compose.onNodeWithText(written).assertIsDisplayed()
            compose.onNodeWithText(written).performClick()
            waitTag("word-written")
            compose.onNodeWithTag("word-written").assertTextContains(written)
            compose.onNodeWithText("木", useUnmergedTree = true).bringIntoViewIfNeeded().performClick()
            waitTag("kanji-symbol")
            Espresso.pressBack()
            waitTag("word-written")
            compose.onNodeWithTag("edit-word").performClick()
            waitTag("word-input-written")
            compose
                .onNodeWithTag("editor-current-translation")
                .bringIntoViewIfNeeded()
                .performTextReplacement("свет солнца сквозь листву")
            Espresso.closeSoftKeyboard()
            compose.onNodeWithTag("editor-other-translations").bringIntoViewIfNeeded().performClick()
            waitTag("translation-en")
            compose.onNodeWithTag("translation-en").assertIsDisplayed()
            compose.onNodeWithTag("dictionary-back").performClick()
            waitTag("word-editor")
            screenshot("edit")
            compose.onNodeWithTag("save-word").bringIntoViewIfNeeded().performClick()
            waitTag("word-written")
            compose.onNodeWithText("свет солнца сквозь листву").assertIsDisplayed()
            compose.onNodeWithTag("dictionary-back").performClick()
            waitTag("add-word")
            compose.onNodeWithTag("dictionary-search-toggle").performClick()
            waitTag("dictionary-search")
            compose.onNodeWithTag("dictionary-search").performTextInput("コモレビ")
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText(written).fetchSemanticsNodes().isNotEmpty()
            }
            Espresso.closeSoftKeyboard()
            screenshot("list")
            compose.onNodeWithText(written).performClick()
            waitTag("word-actions")
            compose.onNodeWithTag("word-actions").performClick()
            compose.onNodeWithTag("delete-word").performClick()
            screenshot("delete")
            compose.onNodeWithTag("confirm-delete-word").performClick()
            waitTag("add-word")
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText(written).fetchSemanticsNodes().isEmpty()
            }
            // Search field may stay open after delete; reopen if the toggle closed it.
            if (compose.onAllNodesWithTag("dictionary-search").fetchSemanticsNodes().isEmpty()) {
                compose.onNodeWithTag("dictionary-search-toggle").performClick()
            }
            compose.onNodeWithTag("dictionary-search").assertTextContains("コモレビ")
            compose.onNodeWithTag("dictionary-back").performClick()
            waitTag("dashboard-screen")
        } finally {
            runBlocking {
                repository
                    .observeWords()
                    .first()
                    .filter { it.written == written && it.isUserCreated }
                    .forEach { repository.deleteWord(it.id) }
            }
        }
    }
}
