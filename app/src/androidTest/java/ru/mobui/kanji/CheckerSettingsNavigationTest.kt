package ru.mobui.kanji

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class CheckerSettingsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun practiceDoesNotOfferModelDownload() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("start-practice").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("start-practice").performClick()
        compose.onNodeWithTag("practice-pad").assertExists()
        compose.onNodeWithTag("japanese-model-dialog").assertDoesNotExist()
    }

    @Test
    fun opensRealSettingsAndPreservesTogglesWhenReturning() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("start-practice").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("start-practice").performClick()
        compose.onNodeWithTag("check-indicator-Shape").performClick()
        compose.onNodeWithTag("open-checker-settings").performClick()
        compose.onNodeWithTag("reset-checker-settings").assertIsDisplayed()
        compose
            .onNodeWithTag("enable-writing-off")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("practice-pad").assertExists()
        compose.onNodeWithTag("check-indicator-Shape").performClick()
        compose.onNodeWithTag("open-checker-settings").performClick()
        compose.onNodeWithTag("enable-writing-off").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("reset-checker-settings").performClick()
        compose.onNodeWithTag("enable-writing-off").performScrollTo().assertIsNotSelected()
    }
}
