package ru.mobui.kanji

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.practice.RecognitionSettings
import ru.mobui.kanji.feature.practice.CheckerSettingsScreen

class CheckerHelpTest {
    @get:Rule val compose = createComposeRule()

    @Test fun helpExpandsIndependentlyRestoresAndDoesNotChangeSettings() {
        val restoration = StateRestorationTester(compose)
        val settings = mutableStateOf(RecognitionSettings.touchInput())
        var changes = 0
        restoration.setContent { KanjiTheme {
            CheckerSettingsScreen(settings.value, { settings.value = it; changes++ }, {}, {})
        } }
        compose.onNodeWithTag("help-content-match").assertDoesNotExist()
        compose.onNodeWithTag("help-toggle-match").performScrollTo().performClick()
        compose.onNodeWithTag("help-content-match").assertIsDisplayed()
        compose.onNodeWithTag("help-toggle-distance").performScrollTo().performClick()
        compose.onNodeWithTag("help-content-distance").assertExists()
        compose.onNodeWithTag("help-content-match").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("help-content-match").assertExists()
        compose.onNodeWithTag("help-toggle-match").performScrollTo().performClick()
        compose.onNodeWithTag("help-content-match").assertDoesNotExist()
        compose.onNodeWithTag("parameter-match").performScrollTo().performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(80f) }
        compose.runOnIdle {
            assertEquals(1, changes)
            assertEquals(80f, settings.value.requiredMatchPercent)
        }
        compose.onNodeWithTag("help-toggle-enable-readability").performScrollTo().performClick()
        compose.onNodeWithTag("help-content-enable-readability").assertExists()
        compose.runOnIdle { assertEquals(1, changes) }
    }
}
