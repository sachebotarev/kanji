package ru.mobui.kanji

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class LibrarySectionsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeEntriesOpenDistinctRootsAndRestoreReferenceNavigation() {
        waitTag("open-catalog")
        compose.onNodeWithTag("open-catalog").bringIntoViewIfNeeded().performClick()
        waitTag("source-1")
        compose.onNodeWithTag("catalog-all-kanji").assertDoesNotExist()
        compose.onNodeWithTag("catalog-back").performClick()
        waitTag("open-references")
        compose.onNodeWithTag("open-references").bringIntoViewIfNeeded().performClick()
        waitTag("catalog-all-kanji")
        compose.onNodeWithTag("source-1").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        waitTag("catalog-all-kanji")
        compose.onNodeWithTag("source-1").assertDoesNotExist()
        compose.onNodeWithTag("catalog-all-kanji").performClick()
        waitTag("kanji-grid")
        compose.onNodeWithTag("catalog-back").performClick()
        waitTag("catalog-all-radicals")
        compose.onNodeWithTag("catalog-all-radicals").performClick()
        waitTag("radical-grid")
        compose.onNodeWithTag("catalog-back").performClick()
        waitTag("catalog-all-kanji")
        compose.onNodeWithTag("catalog-back").performClick()
        waitTag("open-dictionary")
        compose.onNodeWithTag("open-dictionary").bringIntoViewIfNeeded().performClick()
        waitTag("dictionary-screen")
        compose.onNodeWithTag("dictionary-back").performClick()
        waitTag("dashboard-screen")
    }

    private fun waitTag(tag: String) {
        compose.waitUntil(30_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }
}
