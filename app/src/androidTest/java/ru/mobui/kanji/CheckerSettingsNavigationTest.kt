package ru.mobui.kanji

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class CheckerSettingsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun opensRealSettingsAndPreservesTogglesWhenReturning() {
        compose.onNodeWithTag("open-checker-settings").performClick()
        compose.onNodeWithTag("reset-checker-settings").assertIsDisplayed()
        compose.onNodeWithTag("check-order").performScrollTo().assertIsOff().performClick().assertIsOn()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("practice-pad").assertExists()
        compose.onNodeWithTag("open-checker-settings").performClick()
        compose.onNodeWithTag("check-order").performScrollTo().assertIsOn()
        compose.onNodeWithTag("reset-checker-settings").performClick()
        compose.onNodeWithTag("check-order").performScrollTo().assertIsOff()
    }
}
