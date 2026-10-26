package ru.mobui.kanji

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File

class PersonalDictionaryNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun waitTag(tag: String) {
        compose.waitUntil(30_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun screenshot(name: String) {
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(350) // Allow the platform window/dialog transition to reach its final frame.
        val image = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val directory = File(compose.activity.filesDir, "personal-review").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun createsRestoresEditsSearchesAndDeletesManualWord() {
        val repository = (compose.activity.application as KanjiApplication).dictionary
        val written = runBlocking {
            if (repository.observeWords().first().any { it.written == "木漏れ日" }) "木漏れ日${System.nanoTime()}" else "木漏れ日"
        }
        try {
            waitTag("open-dictionary")
            compose.onNodeWithTag("open-dictionary").performScrollTo().performClick()
            waitTag("add-word")
            screenshot("empty")
            compose.onNodeWithTag("add-word").performClick()
            waitTag("word-editor")
            screenshot("create")
            compose.onNodeWithTag("word-input-written").performTextInput(written)
            compose.onNodeWithTag("word-input-reading").performTextInput("こもれび")
            compose.onNodeWithTag("word-input-translation-0").performScrollTo().performTextInput("солнечный свет сквозь листву")
            Espresso.closeSoftKeyboard()
            compose.onNodeWithTag("add-translation").performScrollTo().performClick()
            compose.onNodeWithTag("word-input-translation-1").performScrollTo().performTextInput("sunlight through leaves")
            Espresso.closeSoftKeyboard()
            compose.activityRule.scenario.recreate()
            waitTag("word-input-written")
            compose.onNodeWithTag("word-input-written").assertTextContains(written)
            compose.onNodeWithTag("dictionary-back").performClick()
            compose.onNodeWithText("Сохранить изменения?").assertIsDisplayed()
            compose.onNodeWithText("Продолжить редактирование").performClick()
            compose.onNodeWithTag("save-word").performScrollTo().performClick()
            waitTag("word-written")
            compose.onNodeWithTag("word-written").assertTextContains(written)
            screenshot("word")
            compose.onNodeWithText("木", useUnmergedTree = true).performScrollTo().performClick()
            waitTag("kanji-symbol")
            Espresso.pressBack()
            waitTag("word-written")
            compose.onNodeWithTag("edit-word").performClick()
            waitTag("word-input-written")
            compose.onNodeWithTag("word-input-translation-0").performScrollTo().performTextReplacement("свет солнца сквозь листву")
            Espresso.closeSoftKeyboard()
            compose.onNodeWithTag("word-input-translation-1").performScrollTo().assertTextContains("sunlight through leaves")
            compose.onNodeWithTag("word-input-written").performScrollTo()
            screenshot("edit")
            compose.onNodeWithTag("save-word").performScrollTo().performClick()
            waitTag("word-written")
            compose.onNodeWithText("свет солнца сквозь листву").assertIsDisplayed()
            compose.onNodeWithTag("dictionary-back").performClick()
            waitTag("dictionary-search")
            compose.onNodeWithTag("dictionary-search").performTextInput("コモレビ")
            compose.waitUntil(10_000) { compose.onAllNodesWithText(written).fetchSemanticsNodes().isNotEmpty() }
            Espresso.closeSoftKeyboard()
            screenshot("list")
            compose.onNodeWithText(written).performClick()
            waitTag("word-actions")
            compose.onNodeWithTag("word-actions").performClick()
            compose.onNodeWithTag("delete-word").performClick()
            screenshot("delete")
            compose.onNodeWithTag("confirm-delete-word").performClick()
            waitTag("dictionary-search")
            compose.waitUntil(10_000) { compose.onAllNodesWithText(written).fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("dictionary-search").assertTextContains("コモレビ")
            compose.onNodeWithTag("dictionary-back").performClick()
            waitTag("dashboard-screen")
        } finally {
            runBlocking { repository.observeWords().first().filter { it.written == written && it.isUserCreated }.forEach { repository.deleteWord(it.id) } }
        }
    }
}
