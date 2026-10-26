package ru.mobui.kanji

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.feature.dashboard.DashboardDemo
import ru.mobui.kanji.feature.dashboard.DashboardScreen
import ru.mobui.kanji.feature.dashboard.TrainingOverview

class TrainingPickerScaleTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectsLastOfHundredAndSearchKeepsCreationAvailable() {
        val trainings = (1..100).map { TrainingOverview(it.toString(), "Training $it") }
        val selected = mutableStateOf("1")
        compose.setContent { KanjiTheme {
            DashboardScreen(DashboardDemo.state, {}, {}, trainings = trainings,
                selectedTrainingId = selected.value, onSelectTraining = { selected.value = it })
        } }
        compose.onNodeWithTag("select-training").performScrollTo().performClick()
        compose.onNodeWithTag("training-list").performScrollToNode(hasTestTag("choose-training-100"))
        compose.onNodeWithTag("create-training").assertIsDisplayed()
        compose.onNodeWithTag("choose-training-100").performClick()
        compose.onNodeWithTag("current-training-name").assertTextContains("Training 100")
        compose.onNodeWithTag("select-training").performScrollTo().performClick()
        compose.onNodeWithTag("training-search").performTextInput("training 100")
        compose.onNodeWithTag("choose-training-100").assertIsSelected()
        compose.onNodeWithTag("choose-training-1").assertDoesNotExist()
        compose.onNodeWithTag("training-search").performTextReplacement("absent")
        compose.onNodeWithTag("training-search-empty").assertIsDisplayed()
        compose.onNodeWithTag("create-training").assertIsDisplayed().performClick()
        compose.onNodeWithTag("training-editor").assertIsDisplayed()
    }
}
