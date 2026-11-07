package ru.mobui.kanji

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.RecipeName

private const val LAUNCHABLE_RECIPE_ID = "android-test-launchable-recipe"

private fun hasOpenTag() =
    SemanticsMatcher("training-pick-open-*") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("training-pick-open-") == true
    }

/** Ensures at least one recipe with writable catalog kanji exists (device DB may be empty). */
internal fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.ensureLaunchableRecipe() {
    val app = activity.application as KanjiApplication
    runBlocking {
        val existing = app.recipesRepository.observeRecipes().first()
        if (existing.any { it.id == LAUNCHABLE_RECIPE_ID }) return@runBlocking
        val snapshot = app.catalog.load()
        val word =
            snapshot.words.firstOrNull { word ->
                word.written.codePoints().toArray().any { code -> snapshot.kanjiById.containsKey(code) }
            } ?: error("Catalog has no writable words for practice launch")
        app.recipesRepository.saveRecipe(
            Recipe(
                id = LAUNCHABLE_RECIPE_ID,
                names =
                    listOf(
                        RecipeName("ru", "Тестовый рецепт"),
                        RecipeName("en", "Test recipe"),
                    ),
                materials =
                    listOf(
                        RecipeMaterial(
                            id = "android-test-words",
                            type = MaterialType.Words,
                            wordIds = setOf(word.id),
                        ),
                    ),
                taskCount = 1,
                isActive = true,
            ),
        )
    }
}

internal fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.openTrainingPick() {
    waitUntil(10_000) {
        onAllNodesWithTag("start-practice").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithTag("start-practice").performClick()
    waitUntil(10_000) {
        onAllNodesWithTag("training-pick").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithTag("unfinished-choice-dialog").fetchSemanticsNodes().isNotEmpty()
    }
    if (onAllNodesWithTag("unfinished-choice-dialog").fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithTag("start-new-training").performClick()
        waitUntil(10_000) {
            onAllNodesWithTag("training-pick").fetchSemanticsNodes().isNotEmpty()
        }
    }
}

/** @return true when PRACTICE pad is visible. */
internal fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>.openPracticePad(): Boolean {
    ensureLaunchableRecipe()
    openTrainingPick()
    waitUntil(10_000) {
        onAllNodesWithTag("training-pick-list").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithTag("training-pick-empty").fetchSemanticsNodes().isNotEmpty()
    }
    if (onAllNodesWithTag("training-pick-list").fetchSemanticsNodes().isEmpty()) return false
    // Prefer the seeded recipe; otherwise first openable row.
    val openNodes = onAllNodes(hasOpenTag(), useUnmergedTree = true)
    if (openNodes.fetchSemanticsNodes().isEmpty()) return false
    val preferred =
        onAllNodes(hasOpenTag(), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .indexOfFirst {
                it.config.getOrNull(SemanticsProperties.TestTag) ==
                    "training-pick-open-$LAUNCHABLE_RECIPE_ID"
            }
    if (preferred >= 0) {
        onAllNodes(hasOpenTag(), useUnmergedTree = true)[preferred].performClick()
    } else {
        onAllNodes(hasOpenTag(), useUnmergedTree = true).onFirst().performClick()
    }
    waitUntil(15_000) {
        onAllNodesWithTag("practice-pad").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithTag("training-pick-no-tasks").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithText("Нельзя начать тренировку").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithText("Can't start training").fetchSemanticsNodes().isNotEmpty()
    }
    return onAllNodesWithTag("practice-pad").fetchSemanticsNodes().isNotEmpty()
}
