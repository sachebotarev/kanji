package ru.mobui.kanji

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.RecipeName
import ru.mobui.kanji.feature.dashboard.TrainingPickScreen

class RecipePickerScaleTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun pickListScrollsAndSearchFiltersRecipes() {
        val recipes =
            (1..100).map {
                Recipe(
                    id = it.toString(),
                    names = listOf(RecipeName("ru", "Рецепт $it"), RecipeName("en", "Recipe $it")),
                    materials = emptyList(),
                )
            }
        var picked: String? = null
        compose.setContent {
            var query by remember { mutableStateOf("") }
            KanjiTheme {
                TrainingPickScreen(
                    recipes = recipes,
                    loading = false,
                    query = query,
                    language = "ru",
                    onBack = {},
                    onQuery = { query = it },
                    onPick = { picked = it },
                    onCreateFirstRecipe = {},
                )
            }
        }
        compose
            .onNodeWithTag("training-pick-list")
            .performScrollToNode(hasTestTag("training-pick-100"))
        compose.onNodeWithTag("training-pick-open-100").performClick()
        assertEquals("100", picked)
        compose.onNodeWithTag("training-pick-search-toggle").performClick()
        compose.onNodeWithTag("training-pick-search").performTextInput("Рецепт 100")
        compose.onNodeWithTag("training-pick-100").assertIsDisplayed()
        compose.onNodeWithTag("training-pick-1").assertDoesNotExist()
        compose.onNodeWithTag("training-pick-search").performTextReplacement("absent")
        compose.onNodeWithTag("training-pick-100").assertDoesNotExist()
    }
}
