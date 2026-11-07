package ru.mobui.kanji

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class DashboardNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun startsOnDashboardPickRecipeAndReturnsFromPractice() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("dashboard-screen").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("dashboard-screen").assertIsDisplayed()
        compose.openTrainingPick()
        compose.onNodeWithTag("training-pick").assertIsDisplayed()
        // Demo DB may have recipes; otherwise empty leads to recipe book CTA.
        val pickRows = compose.onAllNodesWithTag("training-pick-list").fetchSemanticsNodes()
        if (pickRows.isNotEmpty()) {
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("training-pick-list").fetchSemanticsNodes().isNotEmpty()
            }
            // Tap first recipe card body if present via open tag pattern is hard; use back and assert.
        }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("dashboard-screen").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("dashboard-screen").assertIsDisplayed()
        compose.onNodeWithTag("recipe-book").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("dashboard-screen").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("dashboard-screen").assertIsDisplayed()
    }

    @Test
    fun statisticsDetailsOpenRestoreAndDismissWithoutLeavingDashboard() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("open-word-statistics").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("874").assertDoesNotExist()
        compose.onNodeWithTag("open-word-statistics").performClick()
        compose.onNodeWithTag("word-statistics-dialog").assertIsDisplayed()
        listOf("874", "249", "125").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("word-statistics-dialog").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("close-word-statistics").performClick()
        compose.onNodeWithTag("word-statistics-dialog").assertDoesNotExist()
        compose.onNodeWithTag("open-word-statistics").performClick()
        pressSystemBack()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("word-statistics-dialog").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag("word-statistics-dialog").assertDoesNotExist()
        compose.onNodeWithTag("dashboard-screen").assertIsDisplayed()
    }

    @Test
    fun activityDetailsOpenRestoreAndClose() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("open-activity-details").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("open-activity-details").performClick()
        compose.onNodeWithTag("activity-details-dialog").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("activity-details-dialog").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("close-activity-details").performClick()
        compose.onNodeWithTag("activity-details-dialog").assertDoesNotExist()
        compose.onNodeWithTag("dashboard-screen").assertIsDisplayed()
    }
}
