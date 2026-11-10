package ru.mobui.kanji

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class AppSettingsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun back() = compose.runOnIdle {
        compose.activity.onBackPressedDispatcher.onBackPressed()
    }

    @Test
    fun checkerSettingsAreSharedWithPracticeAndBackFollowsSettingsHierarchy() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("open-app-settings").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("open-app-settings").performClick()
        compose.onNodeWithTag("settings-screen").assertIsDisplayed()
        compose
            .onNodeWithTag("enable-shape-off")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        compose.onNodeWithTag("open-app-checker-settings").performClick()
        compose.onNodeWithTag("checker-settings-menu").assertIsDisplayed()
        back()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("settings-screen").assertIsDisplayed()
        compose.onNodeWithTag("enable-shape-off").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("settings-back").performClick()
        compose.onNodeWithTag("dashboard-screen").assertIsDisplayed()
    }
}
