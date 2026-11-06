package ru.mobui.kanji

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test

class RecipeDashboardNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun createsEditsSelectsAndRestoresCurrentTraining() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("select-recipe").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("select-recipe").performScrollTo().performClick()
        compose.onNodeWithTag("create-recipe").performClick()
        compose.onNodeWithTag("save-recipe").assertIsNotEnabled()
        compose.onNodeWithTag("recipe-name").performTextInput("JLPT N5")
        compose.onNodeWithTag("recipe-words").performTextReplacement("0")
        compose.onNodeWithTag("save-recipe").assertIsNotEnabled()
        compose.onNodeWithTag("recipe-words").performTextReplacement("10")
        compose.onNodeWithTag("save-recipe").performScrollTo().performClick()
        compose.onNodeWithTag("current-recipe-name").assertTextContains("JLPT N5")
        compose.activityRule.scenario.recreate()
        compose
            .onNodeWithTag("current-recipe-name")
            .performScrollTo()
            .assertTextContains("JLPT N5")
        compose.onNodeWithTag("select-recipe").performClick()
        compose.onNodeWithContentDescription("Изменить рецепт «JLPT N5»").performClick()
        compose
            .onNodeWithTag("recipe-name")
            .performTextReplacement("Подготовка к JLPT N5: глаголы и прилагательные")
        compose.onNodeWithTag("recipe-seconds").performTextReplacement("90")
        compose.onNodeWithTag("save-recipe").performScrollTo().performClick()
        waitForTrainingPicker()
        pressSystemBack()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("recipe-picker").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithTag("current-recipe-name").fetchSemanticsNodes().isNotEmpty()
        }
        compose
            .onNodeWithTag("current-recipe-name")
            .assertTextContains("Подготовка к JLPT N5: глаголы и прилагательные")
        compose
            .onNodeWithTag("training-status")
            .performScrollTo()
            .assertTextEquals("Ещё не тренировались")
        compose.onNodeWithTag("training-history").assertDoesNotExist()
        compose.onNodeWithTag("select-recipe").performScrollTo().performClick()
        compose.onNodeWithTag("edit-recipe-default").performClick()
        compose.onNodeWithTag("recipe-name").performTextReplacement("Другая рецепт")
        compose.onNodeWithTag("save-recipe").performScrollTo().performClick()
        waitForTrainingPicker()
        pressSystemBack()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("recipe-picker").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithTag("current-recipe-name").fetchSemanticsNodes().isNotEmpty()
        }
        compose
            .onNodeWithTag("current-recipe-name")
            .assertTextContains("Подготовка к JLPT N5: глаголы и прилагательные")
        compose.onNodeWithTag("select-recipe").performScrollTo().performClick()
        compose.onNodeWithTag("choose-recipe-default").performClick()
        compose.onNodeWithTag("current-recipe-name").assertTextContains("Другая рецепт")
        compose.onNodeWithTag("select-recipe").performClick()
        compose.onNodeWithTag("recipe-search").performTextInput("несуществующая")
        compose.onNodeWithTag("recipe-search-empty").assertIsDisplayed()
        compose.onNodeWithTag("create-recipe").assertIsDisplayed()
        compose.onNodeWithTag("recipe-search").performTextReplacement("прилагательные")
        compose.onNodeWithTag("choose-recipe-default").assertDoesNotExist()
        compose.onNodeWithText("Подготовка к JLPT N5: глаголы и прилагательные").performClick()
        compose
            .onNodeWithTag("current-recipe-name")
            .assertTextContains("Подготовка к JLPT N5: глаголы и прилагательные")
        compose.onNodeWithTag("start-practice").performClick()
        compose.onNodeWithTag("practice-pad").assertExists()
    }

    private fun waitForTrainingPicker() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("recipe-editor").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithTag("recipe-picker").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
