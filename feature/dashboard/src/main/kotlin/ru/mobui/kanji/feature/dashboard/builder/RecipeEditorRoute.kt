package ru.mobui.kanji.feature.dashboard.builder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.RecipesRepository
import ru.mobui.kanji.domain.catalog.PersonalDictionaryRepository
import ru.mobui.kanji.feature.dashboard.R
import androidx.compose.runtime.setValue

/** Call inside a dedicated navigation entry: its ViewModelStore/SavedStateRegistry own one draft. */
@Composable
fun RecipeEditorRoute(
    recipeId: String?,
    catalog: CatalogRepository,
    personal: PersonalDictionaryRepository,
    recipesRepository: RecipesRepository,
    actions: RecipeEditorActions,
    language: String = "ru",
    modifier: Modifier = Modifier,
    copyFromId: String? = null,
) {
    val copyPattern = stringResource(R.string.library_copy_name)
    val model: RecipeEditorViewModel = viewModel(
        key = "recipe-editor-${recipeId ?: copyFromId?.let { "copy-$it" } ?: "new"}",
        factory = viewModelFactory {
            initializer { RecipeEditorViewModel(createSavedStateHandle(), catalog, personal, recipesRepository, recipeId,
                copyFromId, { name -> copyPattern.format(name) }) }
        },
    )
    val state by model.state.collectAsStateWithLifecycle()
    val savedCallback by rememberUpdatedState(actions.onSaved)
    LaunchedEffect(state.completed, state.completedId) {
        val id = state.completedId
        if (state.completed && id != null) savedCallback(id)
    }
    BackHandler(onBack = actions.onBack)
    val initial = state.initial
    val snapshot = state.snapshot
    Column(modifier.fillMaxSize()) {
        if (initial != null && snapshot != null) {
            // Do not remove the editor (and its local draft) on a later observation failure.
            if (state.loadError != null) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.recipe_load_failed), Modifier.weight(1f))
                    TextButton(model::retry, Modifier.testTag("recipe-retry"), enabled = !state.saving) {
                        Text(stringResource(R.string.recipe_retry))
                    }
                }
            }
            RecipeEditorScreen(
                initial, snapshot, state.tags, state.assignments, language, model::save, actions.onBack,
                actions.onWord, actions.onManageTags, Modifier.weight(1f),
                saving = state.saving || state.loading || state.completed,
                creating = recipeId == null,
                saveError = if (state.writeError) stringResource(R.string.recipe_write_failed) else null,
            )
        } else {
            MaterialHeader(stringResource(R.string.recipe_title), actions.onBack)
            if (state.loading) CircularProgressIndicator(Modifier.padding(24.dp).testTag("recipe-loading"))
            else {
                val message = if (state.loadError == RecipeLoadError.Missing) R.string.recipe_missing
                    else R.string.recipe_load_failed
                Text(stringResource(message), Modifier.padding(24.dp))
                TextButton(model::retry, Modifier.testTag("recipe-retry")) {
                    Text(stringResource(R.string.recipe_retry))
                }
            }
        }
    }
}
