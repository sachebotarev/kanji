package ru.mobui.kanji.feature.dashboard.builder

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.RecipesRepository
import ru.mobui.kanji.domain.catalog.PersonalDictionaryRepository
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.WordTagAssignment
import ru.mobui.kanji.domain.catalog.validated

internal enum class RecipeLoadError { Unavailable, Missing }
internal data class RecipeEditorState(
    val initial: Recipe? = null, val snapshot: CatalogSnapshot? = null,
    val tags: List<DictionaryTag> = emptyList(), val assignments: List<WordTagAssignment> = emptyList(),
    val loading: Boolean = true, val loadError: RecipeLoadError? = null,
    val saving: Boolean = false, val writeError: Boolean = false, val completed: Boolean = false,
    val completedId: String? = null,
)

/** Scoped to one editor navigation entry; never rebase an open draft onto an observed revision. */
internal class RecipeEditorViewModel(private val saved: SavedStateHandle, private val catalog: CatalogRepository,
    private val personal: PersonalDictionaryRepository, private val recipesRepository: RecipesRepository,
    private val recipeId: String?, private val copyFromId: String? = null,
    private val copyName: (String) -> String = { it }) : ViewModel() {
    private val id: String = recipeId ?: saved.get<String>("new-recipe-id")
        ?: UUID.randomUUID().toString().also { saved["new-recipe-id"] = it }
    private val mutable = MutableStateFlow(
        RecipeEditorState(
            completed = saved["completed"] ?: false,
            completedId = saved["completed-id"],
        ),
    )
    val state = mutable.asStateFlow()
    private var loading: Job? = null
    init {
        require(recipeId == null || copyFromId == null)
        if (!mutable.value.completed) retry()
    }

    @Suppress("TooGenericExceptionCaught")
    fun retry() {
        if (mutable.value.completed || mutable.value.saving) return
        loading?.cancel()
        mutable.update { it.copy(loading = true, loadError = null) }
        loading = viewModelScope.launch {
            try {
                val initial = mutable.value.initial ?: if (copyFromId != null) {
                    saved.get<Bundle>("copy-initial")?.let(::recipeFromBundle)
                        ?: recipesRepository.observeRecipes().first().firstOrNull { it.id == copyFromId }?.let { source ->
                            currentCoroutineContext().ensureActive()
                            source.copy(id = id, revision = 1, isActive = false,
                                names = source.names.map { it.copy(name = copyName(it.name).take(120)) },
                                materials = source.materials.map { it.copy(id = UUID.randomUUID().toString()) })
                                .also { saved["copy-initial"] = recipeBundle(it) }
                        }
                } else if (recipeId == null) {
                    Recipe(id, emptyList(), emptyList())
                } else recipesRepository.observeRecipes().first().firstOrNull { it.id == recipeId }
                currentCoroutineContext().ensureActive()
                if (initial == null) {
                    mutable.update { it.copy(loading = false, loadError = RecipeLoadError.Missing) }
                    return@launch
                }
                combine(
                    personal.observeWords(), recipesRepository.observeTags(), recipesRepository.observeWordTags(),
                ) { _, tags, assignments -> tags to assignments }
                    .collectLatest { (tags, assignments) ->
                        val snapshot = catalog.load()
                        currentCoroutineContext().ensureActive()
                        mutable.update { it.copy(initial = initial, snapshot = snapshot, tags = tags,
                            assignments = assignments, loading = false, loadError = null) }
                    }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                logger.log(Level.SEVERE, "Failed to load recipe $id", error)
                currentCoroutineContext().ensureActive()
                mutable.update { it.copy(loading = false, loadError = RecipeLoadError.Unavailable) }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun save(recipe: Recipe) {
        val current = mutable.value
        if (current.saving || current.completed) return
        if (current.loading || current.initial == null || recipe.id != id) return
        mutable.update { it.copy(saving = true, writeError = false) }
        viewModelScope.launch {
            try {
                recipesRepository.saveRecipe(recipe.validated())
                saved["completed"] = true
                saved["completed-id"] = recipe.id
                mutable.update { it.copy(saving = false, completed = true, completedId = recipe.id) }
            } catch (cancelled: CancellationException) {
                mutable.update { it.copy(saving = false) }
                throw cancelled
            } catch (error: Exception) {
                logger.log(Level.SEVERE, "Failed to save recipe ${recipe.id}", error)
                mutable.update { it.copy(saving = false, writeError = true) }
            }
        }
    }

    private companion object {
        val logger: Logger = Logger.getLogger("ru.mobui.kanji.RecipeEditor")
    }
}
