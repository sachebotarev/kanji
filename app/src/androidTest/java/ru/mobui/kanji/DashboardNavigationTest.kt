package ru.mobui.kanji

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class DashboardNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun startsOnDashboardAndReturnsFromPractice() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("dashboard-screen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("dashboard-screen").assertIsDisplayed()
        compose.onNodeWithTag("start-practice").performClick()
        compose.onNodeWithTag("practice-pad").assertExists()
        compose.waitForIdle()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("dashboard-screen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("dashboard-screen").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("dashboard-screen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("dashboard-screen").assertIsDisplayed()
    }
    @Test fun statisticsDetailsOpenRestoreAndDismissWithoutLeavingDashboard() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("open-word-statistics").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("874").assertDoesNotExist()
        compose.onNodeWithTag("open-word-statistics").performClick()
        compose.onNodeWithTag("word-statistics-dialog").assertIsDisplayed()
        listOf("874", "249", "125").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("word-statistics-dialog").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("close-word-statistics").performClick()
        compose.onNodeWithTag("word-statistics-dialog").assertDoesNotExist()
        compose.onNodeWithTag("open-word-statistics").performClick()
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithTag("word-statistics-dialog").assertDoesNotExist()
        compose.onNodeWithTag("dashboard-screen").assertIsDisplayed()
    }

    @Test fun activityDetailsOpenRestoreAndClose() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("open-activity-details").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("open-activity-details").performClick()
        compose.onNodeWithTag("activity-details-dialog").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("activity-details-dialog").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("close-activity-details").performClick()
        compose.onNodeWithTag("activity-details-dialog").assertDoesNotExist()
        compose.onNodeWithTag("dashboard-screen").assertIsDisplayed()
    }

}
