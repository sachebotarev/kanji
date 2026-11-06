package ru.mobui.kanji.feature.dashboard.builder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.RecipesRepository
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.PersonalDictionaryRepository
import ru.mobui.kanji.domain.catalog.TagDraft
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.RecipeName
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.WordDraft
import ru.mobui.kanji.domain.catalog.WordTagAssignment

class RecipePersistenceTest {
    @get:Rule val compose = createComposeRule()
    private val stores = mutableListOf<ViewModelStore>()
    private val word = CatalogWord(-1, "かな", "かな", mapOf("ru" to "кана"))
    private val content = CatalogContent(emptyList(), listOf(word), emptyList(), emptyList(), emptyList(), emptyList())
    private val snapshot = CatalogSnapshot(1, content)
    private val recipe = Recipe(
        "recipe", listOf(RecipeName("ru", "Тест")),
        listOf(RecipeMaterial("words", MaterialType.Words, wordIds = setOf(-1))),
    )
    private val catalog = object : CatalogRepository { override suspend fun load() = snapshot }
    private inner class Personal : PersonalDictionaryRepository {
        val broken = MutableStateFlow(false)
        override fun observeWords() = broken.map { check(!it); listOf(word) }
        override suspend fun saveWord(id: Int?, draft: WordDraft): Int = error("unused")
        override suspend fun deleteWord(id: Int) = error("unused")
    }
    private inner class Learning : RecipesRepository {
        val recipes = MutableStateFlow(listOf(recipe))
        val tags = MutableStateFlow<List<DictionaryTag>>(emptyList())
        val assignments = MutableStateFlow<List<WordTagAssignment>>(emptyList())
        var gate: CompletableDeferred<Unit>? = null
        var broken = false
        var attempts = 0
        override fun observeRecipes() = recipes
        override fun observeTags() = tags
        override fun observeWordTags() = assignments
        override suspend fun saveRecipe(recipe: Recipe) {
            attempts++
            gate?.await()
            check(!broken)
            val old = recipes.value.firstOrNull { it.id == recipe.id }
            require(old == null || old.revision == recipe.revision)
            recipes.value = recipes.value.filterNot { it.id == recipe.id } +
                recipe.copy(revision = (old?.revision ?: 0) + 1)
        }
        override suspend fun saveTag(id: Long?, draft: TagDraft): Long = error("unused")
        override suspend fun deleteTag(id: Long) = error("unused")
        override suspend fun assignTags(wordId: Int, tagIds: Set<Long>) = error("unused")
        override suspend fun activateRecipe(id: String) = error("unused")
        override suspend fun reorderRecipes(ids: List<String>) = error("unused")
        override suspend fun deleteRecipe(id: String) = error("unused")
    }
    private fun model(
        personal: Personal, recipesRepository: Learning, id: String? = recipe.id,
        saved: SavedStateHandle = SavedStateHandle(), source: CatalogRepository = catalog,
    ): RecipeEditorViewModel {
        lateinit var model: RecipeEditorViewModel
        compose.runOnIdle {
            model = RecipeEditorViewModel(saved, source, personal, recipesRepository, id)
            ViewModelStore().also { it.put("editor", model); stores += it }
        }
        return model
    }
    @After fun clean() { compose.runOnIdle { stores.forEach { it.clear() } } }
    private fun ready(model: RecipeEditorViewModel) = compose.waitUntil(5_000) { !model.state.value.loading }

    @Test fun saveCompletesOnlyAfterCommitAndIgnoresDuplicateTap() {
        val personal = Personal()
        val recipesRepository = Learning()
        val gate = CompletableDeferred<Unit>()
        recipesRepository.gate = gate
        val model = model(personal, recipesRepository); ready(model)
        compose.runOnIdle { model.save(recipe); model.save(recipe) }
        compose.runOnIdle {
            assertTrue(model.state.value.saving)
            assertFalse(model.state.value.completed)
            assertEquals(1, recipesRepository.attempts)
            gate.complete(Unit)
        }
        compose.waitUntil { model.state.value.completed }
        compose.runOnIdle {
            model.save(recipe)
            assertEquals(1, recipesRepository.attempts)
            assertEquals(2L, recipesRepository.recipes.value.single().revision)
        }
    }
    @Test fun failureDoesNotCompleteAndRetryWritesSameRecipe() {
        val recipesRepository = Learning(); recipesRepository.broken = true
        val model = model(Personal(), recipesRepository); ready(model)
        compose.runOnIdle { model.save(recipe) }
        compose.waitUntil { model.state.value.writeError }
        compose.runOnIdle { assertFalse(model.state.value.completed); recipesRepository.broken = false; model.save(recipe) }
        compose.waitUntil { model.state.value.completed }
        compose.runOnIdle { assertEquals(2, recipesRepository.attempts) }
    }
    @Test fun missingRecipeIsNotSilentlyCreatedAndLoadCanRetry() {
        val personal = Personal(); personal.broken.value = true
        val recipesRepository = Learning()
        val missing = model(Personal(), recipesRepository, "missing"); ready(missing)
        compose.runOnIdle {
            assertEquals(RecipeLoadError.Missing, missing.state.value.loadError)
            missing.save(recipe)
            assertEquals(0, recipesRepository.attempts)
        }
        val model = model(personal, recipesRepository); ready(model)
        compose.runOnIdle {
            assertEquals(RecipeLoadError.Unavailable, model.state.value.loadError)
            personal.broken.value = false
            model.retry()
        }
        ready(model)
        compose.runOnIdle { assertEquals(recipe, model.state.value.initial); assertNull(model.state.value.loadError) }
    }
    @Test fun newIdAndSuccessfulOutcomeSurviveHandleReconstruction() {
        val recipesRepository = Learning(); val saved = SavedStateHandle()
        val model = model(Personal(), recipesRepository, null, saved); ready(model)
        val id = requireNotNull(model.state.value.initial).id
        val restored = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
        val second = model(Personal(), recipesRepository, null, restored); ready(second)
        compose.runOnIdle { assertEquals(id, second.state.value.initial!!.id); second.save(recipe.copy(id = id)) }
        compose.waitUntil { second.state.value.completed }
        val completed = SavedStateHandle(restored.keys().associateWith { restored.get<Any?>(it) })
        val third = model(Personal(), recipesRepository, null, completed)
        compose.runOnIdle {
            assertTrue(third.state.value.completed)
            third.save(recipe.copy(id = id))
            assertEquals(1, recipesRepository.attempts)
        }
    }
    @Test fun observationDoesNotRebaseDraftRevisionAndConflictDoesNotComplete() {
        val recipesRepository = Learning(); val model = model(Personal(), recipesRepository); ready(model)
        compose.runOnIdle { recipesRepository.recipes.value = listOf(recipe.copy(revision = 2)); model.retry() }; ready(model)
        compose.runOnIdle { assertEquals(1L, model.state.value.initial!!.revision); model.save(recipe) }
        compose.waitUntil { model.state.value.writeError }
        compose.runOnIdle {
            assertFalse(model.state.value.completed)
            assertEquals(2L, recipesRepository.recipes.value.single().revision)
        }
    }
    @Test fun cancelledSaveIsNotConvertedIntoSuccessOrWriteError() {
        val recipesRepository = Learning(); recipesRepository.gate = CompletableDeferred()
        val model = model(Personal(), recipesRepository); ready(model)
        compose.runOnIdle { model.save(recipe); stores.last().clear() }
        compose.runOnIdle {
            assertFalse(model.state.value.saving)
            assertFalse(model.state.value.completed)
            assertFalse(model.state.value.writeError)
            recipesRepository.gate!!.complete(Unit)
        }
        compose.runOnIdle { assertEquals(1L, recipesRepository.recipes.value.single().revision) }
    }
    @Test fun cancelledOldLoadCannotOverwriteRetriedSnapshot() {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val newest = CatalogSnapshot(2, content)
        val slow = object : CatalogRepository {
            override suspend fun load(): CatalogSnapshot {
                val call = ++calls
                if (call == 1) withContext(NonCancellable) { gate.await() }
                return if (call == 1) snapshot else newest
            }
        }
        val model = model(Personal(), Learning(), source = slow)
        compose.waitUntil { calls == 1 }
        compose.runOnIdle { model.retry() }
        ready(model)
        compose.runOnIdle { assertEquals(2, model.state.value.snapshot!!.version); gate.complete(Unit) }
        compose.runOnIdle {
            assertEquals(2, model.state.value.snapshot!!.version)
            assertNull(model.state.value.loadError)
        }
    }

    @Test fun routeKeepsDraftAcrossReadFailureAndNavigatesOnlyAfterSuccessfulSave() {
        val personal = Personal(); val recipesRepository = Learning(); var exits = 0
        compose.setContent {
            KanjiTheme {
                RecipeEditorRoute(
                    recipe.id, catalog, personal, recipesRepository, RecipeEditorActions({ _ -> exits++ }, {}, {}, {}), "ru",
                )
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("recipe-name").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithTag("recipe-name").performTextReplacement("Черновик")
        compose.runOnIdle { personal.broken.value = true }
        compose.waitUntil { compose.onAllNodesWithTag("recipe-retry").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithTag("recipe-name").assertTextEquals("Черновик")
        compose.runOnIdle { personal.broken.value = false; recipesRepository.broken = true }
        compose.onNodeWithTag("recipe-retry").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitUntil { compose.onAllNodesWithTag("recipe-retry").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("recipe-save").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitUntil { compose.onAllNodesWithTag("recipe-save-error").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithTag("recipe-name").assertTextEquals("Черновик")
        compose.runOnIdle { assertEquals(0, exits); recipesRepository.broken = false }
        compose.onNodeWithTag("recipe-save").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitUntil { exits == 1 }
        compose.runOnIdle { assertEquals("Черновик", recipesRepository.recipes.value.single().names.single().name) }
    }
}
