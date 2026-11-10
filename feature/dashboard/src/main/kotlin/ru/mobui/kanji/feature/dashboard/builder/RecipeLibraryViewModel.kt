package ru.mobui.kanji.feature.dashboard.builder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.mobui.kanji.domain.catalog.recipeWords
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.RecipesRepository
import ru.mobui.kanji.domain.catalog.PersonalDictionaryRepository
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.WordTagAssignment
import ru.mobui.kanji.domain.practice.TrainingsRepository
import ru.mobui.kanji.feature.dashboard.history.findExcellentRecipeIds
import ru.mobui.kanji.feature.dashboard.history.isExcellent

internal data class RecipeLibraryState(
    val recipes: List<Recipe> = emptyList(),
    val snapshot: CatalogSnapshot? = null,
    val tags: List<DictionaryTag> = emptyList(),
    val assignments: List<WordTagAssignment> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val excellentRecipeIds: Set<String> = emptySet(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: Boolean = false,
)

internal class RecipeLibraryViewModel(
    private val saved: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val personal: PersonalDictionaryRepository,
    private val recipesRepository: RecipesRepository,
    private val trainingsRepository: TrainingsRepository? = null,
    private val compute: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val mutable = MutableStateFlow(RecipeLibraryState())
    val state = mutable.asStateFlow()
    private val search = MutableStateFlow(saved.get<String>("query").orEmpty())
    val query = search.asStateFlow()
    private var observation: Job? = null
    init { retry() }
    fun query(value: String) { search.value = value; saved["query"] = value }
    @Suppress("TooGenericExceptionCaught")
    fun retry() {
        observation?.cancel()
        mutable.update { it.copy(loading = it.snapshot == null, error = false) }
        observation = viewModelScope.launch {
            try {
                val historyFlow = trainingsRepository?.observeHistory() ?: flowOf(emptyList())
                combine(
                    recipesRepository.observeRecipes(),
                    recipesRepository.observeTags(),
                    recipesRepository.observeWordTags(),
                    personal.observeWords(),
                    historyFlow,
                ) { recipes, tags, assignments, _, history ->
                    val excellentRecipeIds = findExcellentRecipeIds(recipes, history)
                    RecipeLibraryState(
                        recipes = recipes,
                        tags = tags,
                        assignments = assignments,
                        excellentRecipeIds = excellentRecipeIds,
                    )
                }.collectLatest { loaded ->
                    val snapshot = catalog.load()
                    val counts = withContext(compute) {
                        loaded.recipes.associate { recipe ->
                            recipe.id to snapshot.recipeWords(recipe.materials, loaded.assignments).size
                        }
                    }
                    currentCoroutineContext().ensureActive()
                    mutable.update {
                        loaded.copy(snapshot = snapshot, counts = counts, loading = false, busy = it.busy)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                logger.log(Level.SEVERE, "Failed to load recipes", error)
                currentCoroutineContext().ensureActive()
                mutable.update { it.copy(loading = false, error = true) }
            }
        }
    }
    fun select(id: String) = operation { recipesRepository.activateRecipe(id) }
    fun delete(id: String) = operation { recipesRepository.deleteRecipe(id) }
    fun reorder(ids: List<String>) {
        if (query.value.isNotBlank()) return
        if (ids.toSet() != mutable.value.recipes.map { it.id }.toSet() || ids.distinct().size != ids.size) return
        operation { recipesRepository.reorderRecipes(ids) }
    }
    @Suppress("TooGenericExceptionCaught")
    private fun operation(block: suspend () -> Unit) {
        if (mutable.value.busy || mutable.value.loading) return
        mutable.update { it.copy(busy = true, error = false) }
        viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                logger.log(Level.SEVERE, "Recipe library write failed", error)
                mutable.update { it.copy(error = true) }
            }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }

    private companion object {
        val logger: Logger = Logger.getLogger("ru.mobui.kanji.RecipeLibrary")
    }
}
