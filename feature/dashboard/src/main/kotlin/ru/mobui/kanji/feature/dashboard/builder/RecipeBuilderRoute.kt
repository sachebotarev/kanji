package ru.mobui.kanji.feature.dashboard.builder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import ru.mobui.kanji.core.designsystem.preferredLanguageTag
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.RecipesRepository
import ru.mobui.kanji.domain.catalog.PersonalDictionaryRepository
import ru.mobui.kanji.domain.catalog.name

@Serializable private object RecipeLibrary
@Serializable private data class RecipeDetail(val id: String)
@Serializable private data class RecipeEdit(val id: String? = null, val copyFromId: String? = null)
@Serializable private data class RecipeWord(val id: Int)
@Serializable private object RecipeTags

class RecipeBuilderActions(
    val onBack: () -> Unit,
    val wordContent: @Composable (Int, () -> Unit) -> Unit,
    val tagContent: @Composable (() -> Unit) -> Unit,
)

@Composable
fun RecipeBuilderRoute(
    catalog: CatalogRepository,
    personal: PersonalDictionaryRepository,
    recipesRepository: RecipesRepository,
    actions: RecipeBuilderActions,
    modifier: Modifier = Modifier,
    openCreate: Boolean = false,
) {
    val nav = rememberNavController()
    val language = LocalConfiguration.current.preferredLanguageTag()
    LaunchedEffect(openCreate) {
        if (openCreate) nav.navigate(RecipeEdit())
    }
    NavHost(nav, startDestination = RecipeLibrary, modifier = modifier) {
        composable<RecipeLibrary> {
            val model = libraryModel(catalog, personal, recipesRepository, "library")
            val state by model.state.collectAsStateWithLifecycle()
            val query by model.query.collectAsStateWithLifecycle()
            RecipeLibraryScreen(state, query, language, RecipeLibraryActions(
                actions.onBack, { nav.navigate(RecipeEdit()) }, { nav.navigate(RecipeDetail(it)) },
                model::reorder, model::query, model::retry))
        }
        composable<RecipeDetail> { entry ->
            val id = entry.toRoute<RecipeDetail>().id
            // Share the library ViewModel so detail does not load a second catalog copy.
            val libraryOwner = remember(entry) { nav.getBackStackEntry(RecipeLibrary) }
            val model = libraryModel(catalog, personal, recipesRepository, "library", libraryOwner)
            val state by model.state.collectAsStateWithLifecycle()
            RecipeDetailScreen(
                recipe = state.recipes.firstOrNull { it.id == id },
                state = state,
                language = language,
                actions = RecipeDetailActions(
                    back = { nav.popBackStack() },
                    edit = { nav.navigate(RecipeEdit(id)) },
                    delete = {
                        model.delete(id)
                        nav.popBackStack()
                    },
                    onWord = { nav.navigate(RecipeWord(it)) },
                ),
            )
        }
        composable<RecipeEdit> { entry ->
            val route = entry.toRoute<RecipeEdit>()
            RecipeEditorRoute(route.id, catalog, personal, recipesRepository,
                RecipeEditorActions(
                    onSaved = { nav.popBackStack() },
                    onBack = { nav.popBackStack() },
                    onWord = { nav.navigate(RecipeWord(it)) },
                    onManageTags = { nav.navigate(RecipeTags) },
                ),
                language,
                copyFromId = route.copyFromId,
            )
        }
        composable<RecipeWord> { entry -> actions.wordContent(entry.toRoute<RecipeWord>().id) { nav.popBackStack() } }
        composable<RecipeTags> { actions.tagContent { nav.popBackStack() } }
    }
}

/** Main-screen adapter; the active choice comes from the same persistent repository as the list. */
@Composable
fun RecipeHomeSummary(catalog: CatalogRepository, personal: PersonalDictionaryRepository,
    recipesRepository: RecipesRepository, content: @Composable (ru.mobui.kanji.feature.dashboard.RecipeOverview) -> Unit) {
    val model = libraryModel(catalog, personal, recipesRepository, "recipe-home")
    val state by model.state.collectAsStateWithLifecycle()
    val selected = state.recipes.firstOrNull { it.isActive }
    val language = LocalConfiguration.current.preferredLanguageTag()
    content(ru.mobui.kanji.feature.dashboard.RecipeOverview(selected?.id.orEmpty(),
        selected?.name(language)
            ?: androidx.compose.ui.res.stringResource(ru.mobui.kanji.feature.dashboard.R.string.library_none),
        selected?.taskCount ?: state.counts[selected?.id] ?: 0, selected?.secondsPerWord ?: 0))
}

@Composable
private fun libraryModel(
    catalog: CatalogRepository,
    personal: PersonalDictionaryRepository,
    recipesRepository: RecipesRepository,
    key: String,
    viewModelStoreOwner: ViewModelStoreOwner? = null,
): RecipeLibraryViewModel {
    val factory = viewModelFactory {
        initializer {
            RecipeLibraryViewModel(createSavedStateHandle(), catalog, personal, recipesRepository)
        }
    }
    return if (viewModelStoreOwner != null) {
        viewModel(viewModelStoreOwner = viewModelStoreOwner, key = key, factory = factory)
    } else {
        viewModel(key = key, factory = factory)
    }
}
