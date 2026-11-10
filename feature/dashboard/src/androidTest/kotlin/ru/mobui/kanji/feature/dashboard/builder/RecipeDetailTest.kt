package ru.mobui.kanji.feature.dashboard.builder

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.RecipeName
import ru.mobui.kanji.domain.catalog.Recipe

class RecipeDetailTest {
    @get:Rule val compose = createComposeRule()
    private val word = CatalogWord(-1, "かな", "かな", mapOf("ru" to "кана"))
    private val snapshot = CatalogSnapshot(1, CatalogContent(emptyList(), listOf(word), emptyList(), emptyList(), emptyList(), emptyList()))
    private val material = RecipeMaterial("ma", MaterialType.Words, wordIds = setOf(-1))
    private val recipe = Recipe(
        "a",
        listOf(RecipeName("ru", "Луна")),
        listOf(material),
        taskCount = 20,
        secondsPerSymbol = 30,
    )

    @Test
    fun showsEditorLikeSettingsWithoutMaterialViewer() {
        var edited = false
        var deleted = false
        compose.setContent {
            KanjiTheme {
                RecipeDetailScreen(
                    recipe,
                    RecipeLibraryState(listOf(recipe), snapshot, loading = false, counts = mapOf("a" to 1)),
                    "ru",
                    RecipeDetailActions({}, { edited = true }, { deleted = true }, {}),
                )
            }
        }
        compose.onNodeWithTag("recipe-detail-name").assertIsDisplayed()
        compose.onNodeWithText("Луна").assertExists()
        compose.onNodeWithTag("recipe-detail-count").assertIsDisplayed()
        compose.onNodeWithText("20 заданий", substring = true).assertExists()
        compose.onNodeWithTag("recipe-detail-timer").assertIsDisplayed()
        compose.onNodeWithText("30 с на символ").assertExists()
        compose.onNodeWithTag("recipe-detail-timer-hint").assertDoesNotExist()
        compose.onNodeWithTag("recipe-detail-timer-hint-toggle").assertIsDisplayed().performClick()
        compose.onNodeWithTag("recipe-detail-timer-hint").assertIsDisplayed()
        compose.onNodeWithText("Лимит слова = секунды × число символов для письма").assertExists()
        compose.onNodeWithTag("recipe-material-ma").assertIsDisplayed()
        compose.onNodeWithTag("material-viewer").assertDoesNotExist()
        compose.onNodeWithTag("recipe-detail-edit").performClick()
        assertTrue(edited)
        compose.onNodeWithTag("recipe-detail-menu").performClick()
        compose.onNodeWithTag("recipe-detail-delete").performClick()
        compose.onNodeWithText("Удалить рецепт?").assertExists()
        compose.onNodeWithText("Удалить").performClick()
        assertTrue(deleted)
    }

    @Test
    fun opensSharedWordPreviewFromDetail() {
        compose.setContent {
            KanjiTheme {
                RecipeDetailScreen(
                    recipe,
                    RecipeLibraryState(listOf(recipe), snapshot, loading = false, counts = mapOf("a" to 1)),
                    "ru",
                    RecipeDetailActions({}, {}, {}, {}),
                )
            }
        }
        compose.onNodeWithTag("recipe-detail-preview").performClick()
        compose.onNodeWithText("かな").assertExists()
    }
}
