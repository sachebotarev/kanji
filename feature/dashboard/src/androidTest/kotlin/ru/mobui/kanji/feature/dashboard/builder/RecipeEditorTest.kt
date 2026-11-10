package ru.mobui.kanji.feature.dashboard.builder

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogLink
import ru.mobui.kanji.domain.catalog.CatalogPlacement
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogSource
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.RecipeName
import ru.mobui.kanji.domain.catalog.recipeWords
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class RecipeEditorTest {
    @get:Rule val compose = createComposeRule()
    private val sun = '日'.code
    private val snapshot = CatalogSnapshot(1, CatalogContent(
        listOf(CatalogSource(1, "Book", emptyMap(), sourceType = "book"), CatalogSource(-1, "Personal", emptyMap(), sourceType = "personal")),
        listOf(CatalogWord(1, "日", "ひ", mapOf("ru" to "солнце")), CatalogWord(-2, "かな", "かな", mapOf("ru" to "кана"))),
        listOf(CatalogKanji(sun, "日", 4, null, false, true, false, emptyMap(), emptyList(), emptyList())), emptyList(),
        listOf(CatalogPlacement(1, sun, 1, 1)), listOf(CatalogLink(1, 1, sun), CatalogLink(-1, -2, null))))
    private val initial = Recipe("recipe", listOf(RecipeName("ru", "Солнце"), RecipeName("de", "Sonne")),
        listOf(RecipeMaterial("one", MaterialType.Book, sourceId = 1), RecipeMaterial("two", MaterialType.Words, wordIds = setOf(1))), revision = 7)
    private fun click(tag: String) = compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
    private fun section(index: Int) = compose.onNodeWithTag("recipe-list").performScrollToIndex(index)

    @Test fun addAndEditOnlyUpdateLocalRecipeUnionKeepsIdsUnique() {
        var result: Recipe? = null
        compose.setContent { KanjiTheme { RecipeEditorScreen(initial, snapshot, emptyList(), emptyList(), "ru", { result = it }, {}, {}, {}) } }
        click("recipe-add")
        click("material-type-words")
        click("material-words")
        click("word--2")
        click("word-picker-apply")
        click("material-apply")
        compose.runOnIdle { assertNull(result) }
        click("recipe-preview")
        compose.onNodeWithTag("word-1").assertExists()
        compose.onNodeWithTag("word--2").assertExists()
        click("material-back")
        click("recipe-save")
        compose.runOnIdle {
            assertEquals(3, result!!.materials.size)
            assertEquals(7L, result!!.revision)
            assertEquals(initial.names, result!!.names)
            assertEquals(listOf(1, -2), snapshot.recipeWords(result!!.materials, emptyList()).map { it.id })
        }
    }

    @Test fun editingCancelAndDeleteUndoPreserveOrderAndLatestUndoOnly() {
        var result: Recipe? = null
        compose.setContent { KanjiTheme { RecipeEditorScreen(initial, snapshot, emptyList(), emptyList(), "ru", { result = it }, {}, {}, {}) } }
        click("recipe-material-two")
        click("material-words")
        click("word--2")
        click("word-picker-back")
        click("material-back")
        click("recipe-remove-one")
        click("recipe-remove-two")
        click("recipe-undo")
        click("recipe-save")
        compose.runOnIdle { assertEquals(listOf(initial.materials[1]), result!!.materials) }
    }

    @Test fun validationRejectsBadParametersThenSavesPositiveCountsBeyondPool() {
        var result: Recipe? = null
        compose.setContent { KanjiTheme { RecipeEditorScreen(initial, snapshot, emptyList(), emptyList(), "ru", { result = it }, {}, {}, {}) } }
        section(2)
        click("recipe-count-true")
        compose.onNodeWithTag("recipe-count").performScrollTo().performTextReplacement("0")
        click("recipe-save")
        compose.runOnIdle { assertNull(result) }
        compose.onNodeWithTag("recipe-count").performScrollTo().performTextReplacement("9")
        compose.onNodeWithTag("recipe-timer-true").performScrollTo()
        click("recipe-timer-true")
        compose.onNodeWithTag("recipe-timer").performScrollTo().performTextReplacement("2147483648")
        click("recipe-save")
        compose.runOnIdle { assertNull(result) }
        compose.onNodeWithTag("recipe-timer").performScrollTo().performTextReplacement("12")
        click("recipe-save")
        compose.runOnIdle { assertEquals(9, result!!.taskCount); assertEquals(12, result!!.secondsPerSymbol) }
    }

    @Test fun unsavedDialogCancelLeaveAndSaveWithOtherLanguagesPreserved() {
        var result: Recipe? = null
        var back = 0
        compose.setContent { KanjiTheme { RecipeEditorScreen(initial, snapshot, emptyList(), emptyList(), "ru", { result = it }, { back++ }, {}, {}) } }
        compose.onNodeWithTag("recipe-name").performTextReplacement("Луна")
        click("material-back")
        compose.onNodeWithText("Сбросить изменения?").assertExists()
        compose.onNodeWithText("Все внесённые правки будут потеряны.").assertExists()
        compose.onNodeWithText("Отмена").assertExists()
        compose.onNodeWithText("Выйти").assertExists()
        click("recipe-stay")
        compose.runOnIdle { assertEquals(0, back); assertNull(result) }
        click("recipe-save")
        compose.runOnIdle {
            assertEquals("Луна", result!!.names.single { it.languageTag == "ru" }.name)
            assertTrue(RecipeName("de", "Sonne") in result!!.names)
        }
        compose.onNodeWithTag("recipe-name").performTextReplacement("Марс")
        click("material-back")
        click("recipe-discard")
        compose.runOnIdle { assertEquals(1, back) }
    }

    @Test fun restoreOpenMaterialDraftAndRecipeNameWithoutPublishing() {
        val restoration = StateRestorationTester(compose)
        var result: Recipe? = null
        restoration.setContent { KanjiTheme { RecipeEditorScreen(initial, snapshot, emptyList(), emptyList(), "ru", { result = it }, {}, {}, {}) } }
        compose.onNodeWithTag("recipe-name").performTextReplacement("Новое")
        click("recipe-material-two")
        click("material-words")
        click("word--2")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("word--2").assertIsSelected()
        click("word-picker-apply")
        click("material-apply")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("recipe-name").assertTextEquals("Новое")
        compose.runOnIdle { assertNull(result) }
        click("recipe-save")
        compose.runOnIdle { assertEquals(setOf(1, -2), result!!.materials[1].wordIds) }
    }

    @Test fun collapsedMaterialsStillCountAndUndoSurvivesRestoration() {
        val recipe = initial.copy(materials = (1..5).map { RecipeMaterial("item-$it", MaterialType.Words, wordIds = setOf(1)) })
        val restoration = StateRestorationTester(compose)
        var result: Recipe? = null
        restoration.setContent { KanjiTheme { RecipeEditorScreen(recipe, snapshot, emptyList(), emptyList(), "ru", { result = it }, {}, {}, {}) } }
        compose.onNodeWithTag("recipe-material-item-4").assertDoesNotExist()
        click("recipe-more")
        compose.onNodeWithTag("recipe-material-item-5").assertExists()
        click("recipe-remove-item-2")
        restoration.emulateSavedInstanceStateRestore()
        click("recipe-undo")
        click("recipe-save")
        compose.runOnIdle { assertEquals(recipe.materials, result!!.materials); assertEquals(1, snapshot.recipeWords(result!!.materials, emptyList()).size) }
    }

    @Test fun restoringOriginalNameDoesNotTriggerFalseUnsavedWarning() {
        var back = 0
        compose.setContent { KanjiTheme { RecipeEditorScreen(initial, snapshot, emptyList(), emptyList(), "ru", {}, { back++ }, {}, {}) } }
        compose.onNodeWithTag("recipe-name").performTextReplacement("Другое")
        compose.onNodeWithTag("recipe-name").performTextReplacement("Солнце")
        click("material-back")
        compose.onNodeWithTag("recipe-stay").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, back) }
    }

    @Test fun englishNameDoesNotCopyRussianFallbackOrDropLegacyTranslations() {
        var result: Recipe? = null
        compose.setContent { KanjiTheme { RecipeEditorScreen(initial, snapshot, emptyList(), emptyList(), "en", { result = it }, {}, {}, {}) } }
        compose.onNodeWithTag("recipe-name").assertTextEquals("")
        click("recipe-save")
        compose.runOnIdle { assertNull(result) }
        compose.onNodeWithTag("recipe-name").performTextReplacement("Sun")
        click("recipe-save")
        compose.runOnIdle { assertTrue(result!!.names.containsAll(initial.names)); assertTrue(RecipeName("en", "Sun") in result!!.names) }
    }

    @Test fun emptyPoolBlocksSaveAndBusyOwnerDisablesDuplicateSubmission() {
        var calls = 0
        var busy by mutableStateOf(false)
        val empty = initial.copy(materials = emptyList())
        var original by mutableStateOf(empty)
        compose.setContent { KanjiTheme { RecipeEditorScreen(original, snapshot, emptyList(), emptyList(), "ru", { calls++; busy = true }, {}, {}, {},
            saving = busy) } }
        click("recipe-save")
        compose.onNodeWithTag("recipe-pool-error").assertExists()
        compose.runOnIdle { assertEquals(0, calls); original = initial.copy(id = "other") }
        click("recipe-save")
        compose.onNodeWithTag("recipe-save").assertDoesNotExist()
        compose.onNodeWithTag("recipe-saving").assertExists()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun hintModeSegmentedButtonChangesAndSavesSelectedMode() {
        var result: Recipe? = null
        compose.setContent { KanjiTheme { RecipeEditorScreen(initial, snapshot, emptyList(), emptyList(), "ru", { result = it }, {}, {}, {}) } }
        section(2)
        compose.onNodeWithTag("recipe-hint-reading").assertIsSelected()
        click("recipe-hint-translation")
        compose.onNodeWithTag("recipe-hint-translation").assertIsSelected()
        click("recipe-save")
        compose.runOnIdle { assertEquals(ru.mobui.kanji.domain.catalog.RecipeHintMode.Translation, result!!.hintMode) }
    }

    @Test fun strokeHintModeSegmentedButtonChangesAndSavesSelectedMode() {
        var result: Recipe? = null
        compose.setContent { KanjiTheme { RecipeEditorScreen(initial, snapshot, emptyList(), emptyList(), "ru", { result = it }, {}, {}, {}) } }
        section(2)
        compose.onNodeWithTag("recipe-stroke-hint-next_stroke").assertIsSelected()
        click("recipe-stroke-hint-full_character")
        compose.onNodeWithTag("recipe-stroke-hint-full_character").assertIsSelected()
        click("recipe-save")
        compose.runOnIdle {
            assertEquals(ru.mobui.kanji.domain.catalog.RecipeStrokeHintMode.FullCharacter, result!!.strokeHintMode)
        }
    }

    @Test fun existingRecipeShowsDeleteMenuAndConfirmationDeletesRecipe() {
        var deleted = false
        compose.setContent {
            KanjiTheme {
                RecipeEditorScreen(
                    initial = initial,
                    snapshot = snapshot,
                    words = emptyList(),
                    tags = emptyList(),
                    languageTag = "ru",
                    onSave = {},
                    onBack = {},
                    onManageTags = {},
                    onOpenWords = {},
                    onDelete = { deleted = true }
                )
            }
        }
        compose.onNodeWithTag("recipe-menu").assertExists()
        click("recipe-menu")
        compose.onNodeWithTag("recipe-delete").assertExists()
        click("recipe-delete")
        compose.onNodeWithTag("delete-confirm").assertExists()
        click("delete-confirm")
        compose.runOnIdle {
            assertTrue(deleted)
        }
    }

    @Test fun newRecipeHidesDeleteMenu() {
        val blankRecipe = initial.copy(id = "")
        compose.setContent {
            KanjiTheme {
                RecipeEditorScreen(
                    initial = blankRecipe,
                    snapshot = snapshot,
                    words = emptyList(),
                    tags = emptyList(),
                    languageTag = "ru",
                    onSave = {},
                    onBack = {},
                    onManageTags = {},
                    onOpenWords = {},
                    onDelete = {}
                )
            }
        }
        compose.onNodeWithTag("recipe-menu").assertDoesNotExist()
    }
}
