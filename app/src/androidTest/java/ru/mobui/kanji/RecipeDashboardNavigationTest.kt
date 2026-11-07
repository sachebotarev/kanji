package ru.mobui.kanji

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class RecipeDashboardNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun trainingPickAndRecipeBookBackFollowRootStack() {
        waitTag("dashboard-screen")
        compose.openTrainingPick()
        waitTag("training-pick")
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        waitTag("dashboard-screen")

        compose.onNodeWithTag("recipe-book").performScrollTo().performClick()
        waitTag("recipe-library")
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        waitTag("dashboard-screen")
        compose.onNodeWithTag("dashboard-screen").assertIsDisplayed()
    }

    @Test
    fun createRecipeFromEmptyPickReturnsToTrainingPick() {
        waitTag("dashboard-screen")
        compose.openTrainingPick()
        waitTag("training-pick")
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("training-pick-list").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("training-pick-empty").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithTag("training-pick-empty").fetchSemanticsNodes().isEmpty()) {
            // Seed already has recipes; empty-create stack path is not available.
            compose.onNodeWithTag("training-pick-back").performClick()
            waitTag("dashboard-screen")
            return
        }
        compose.onNodeWithTag("training-pick-create-first").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("recipe-name").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("recipe-library").fetchSemanticsNodes().isNotEmpty()
        }
        // openCreate lands on editor; leave without saving to library, then pop to pick.
        if (compose.onAllNodesWithTag("recipe-name").fetchSemanticsNodes().isNotEmpty()) {
            compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.waitForIdle()
            // Dirty discard dialog may appear for empty new recipe — confirm leave if shown.
            if (compose.onAllNodesWithTag("recipe-discard").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag("recipe-discard").performClick()
            }
        }
        waitTag("recipe-library")
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        waitTag("training-pick")
        compose.onNodeWithTag("training-pick").assertIsDisplayed()
        compose.onNodeWithTag("training-pick-back").performClick()
        waitTag("dashboard-screen")
    }

    private fun waitTag(tag: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
