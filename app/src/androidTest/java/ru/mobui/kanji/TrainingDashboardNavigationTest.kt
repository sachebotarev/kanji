package ru.mobui.kanji

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class TrainingDashboardNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun createsEditsSelectsAndRestoresCurrentTraining() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("select-training").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("select-training").performScrollTo().performClick()
        compose.onNodeWithTag("create-training").performClick()
        compose.onNodeWithTag("save-training").assertIsNotEnabled()
        compose.onNodeWithTag("training-name").performTextInput("JLPT N5")
        compose.onNodeWithTag("training-words").performTextReplacement("0")
        compose.onNodeWithTag("save-training").assertIsNotEnabled()
        compose.onNodeWithTag("training-words").performTextReplacement("10")
        compose.onNodeWithTag("save-training").performScrollTo().performClick()
        compose.onNodeWithTag("current-training-name").assertTextContains("JLPT N5")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("current-training-name").performScrollTo().assertTextContains("JLPT N5")
        compose.onNodeWithTag("select-training").performClick()
        compose.onNodeWithContentDescription("Изменить тренировку «JLPT N5»").performClick()
        compose.onNodeWithTag("training-name").performTextReplacement("Подготовка к JLPT N5: глаголы и прилагательные")
        compose.onNodeWithTag("training-seconds").performTextReplacement("90")
        compose.onNodeWithTag("save-training").performScrollTo().performClick()
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithTag("current-training-name").assertTextContains("Подготовка к JLPT N5: глаголы и прилагательные")
        compose.onNodeWithTag("training-status").performScrollTo().assertTextEquals("Ещё не тренировались")
        compose.onNodeWithTag("training-history").assertDoesNotExist()
        compose.onNodeWithTag("select-training").performScrollTo().performClick()
        compose.onNodeWithTag("edit-training-default").performClick()
        compose.onNodeWithTag("training-name").performTextReplacement("Другая тренировка")
        compose.onNodeWithTag("save-training").performScrollTo().performClick()
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithTag("current-training-name").assertTextContains("Подготовка к JLPT N5: глаголы и прилагательные")
        compose.onNodeWithTag("select-training").performScrollTo().performClick()
        compose.onNodeWithTag("choose-training-default").performClick()
        compose.onNodeWithTag("current-training-name").assertTextContains("Другая тренировка")
        compose.onNodeWithTag("select-training").performClick()
        compose.onNodeWithTag("training-search").performTextInput("несуществующая")
        compose.onNodeWithTag("training-search-empty").assertIsDisplayed()
        compose.onNodeWithTag("create-training").assertIsDisplayed()
        compose.onNodeWithTag("training-search").performTextReplacement("прилагательные")
        compose.onNodeWithTag("choose-training-default").assertDoesNotExist()
        compose.onNodeWithText("Подготовка к JLPT N5: глаголы и прилагательные").performClick()
        compose.onNodeWithTag("current-training-name").assertTextContains("Подготовка к JLPT N5: глаголы и прилагательные")
        compose.onNodeWithTag("start-practice").performClick()
        compose.onNodeWithTag("practice-pad").assertExists()
    }
}
