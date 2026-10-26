package ru.mobui.kanji

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class AppSettingsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun back() = compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }

    @Test fun checkerSettingsAreSharedWithPracticeAndBackFollowsSettingsHierarchy() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("open-app-settings").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("open-app-settings").performClick()
        compose.onNodeWithTag("settings-screen").assertIsDisplayed()
        compose.onNodeWithTag("open-app-checker-settings").performClick()
        compose.onNodeWithTag("reset-checker-settings").performClick()
        compose.onNodeWithTag("enable-shape-off").performScrollTo().performClick().assertIsSelected()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("enable-shape-off").performScrollTo().assertIsSelected()
        back()
        compose.onNodeWithTag("settings-screen").assertIsDisplayed()
        compose.onNodeWithTag("settings-back").performClick()
        compose.onNodeWithTag("dashboard-screen").assertIsDisplayed()
        compose.onNodeWithTag("start-practice").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("check-indicator-Shape").performClick()
        compose.onNodeWithTag("open-checker-settings").performClick()
        compose.onNodeWithTag("enable-shape-off").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("reset-checker-settings").performClick()
    }
}
