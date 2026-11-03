package ru.mobui.kanji.feature.catalog.presentation

import ru.mobui.kanji.feature.catalog.browse.BrowseActions
import ru.mobui.kanji.feature.catalog.browse.BrowseNavigation
import ru.mobui.kanji.feature.catalog.browse.BrowseRequest
import ru.mobui.kanji.feature.catalog.browse.CatalogBrowseScreen
import ru.mobui.kanji.feature.catalog.browse.CatalogBrowseViewModel
import ru.mobui.kanji.feature.catalog.components.decodeJlpt
import ru.mobui.kanji.feature.catalog.components.savedValue
import ru.mobui.kanji.feature.catalog.detail.CatalogDetailScreen
import ru.mobui.kanji.feature.catalog.detail.CatalogDetailViewModel
import ru.mobui.kanji.feature.catalog.detail.DetailActions
import ru.mobui.kanji.feature.catalog.detail.DetailNavigation
import ru.mobui.kanji.feature.catalog.detail.DetailRequest
import ru.mobui.kanji.feature.catalog.filter.CatalogFilterScreen
import ru.mobui.kanji.feature.catalog.filter.CatalogFilterViewModel
import ru.mobui.kanji.feature.catalog.filter.FilterActions
import ru.mobui.kanji.feature.catalog.filter.FilterRequest
import ru.mobui.kanji.feature.catalog.filter.FilterResult

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing

@Composable
internal fun CatalogBrowseEntry(
    entry: NavBackStackEntry,
    request: BrowseRequest,
    context: CatalogEntryContext,
    navigate: (BrowseNavigation) -> Unit,
) {
    with(context) {
    val model: CatalogBrowseViewModel =
        viewModel(
            entry,
            key = "browse",
            factory =
                viewModelFactory {
                    initializer {
                        CatalogBrowseViewModel(
                            createSavedStateHandle(),
                            repository,
                            request,
                            language,
                        )
                    }
                },
        )
    val state by model.state.collectAsStateWithLifecycle()
    val search by model.search.collectAsStateWithLifecycle()
    CatalogResultKind.entries.forEach { kind ->
        val result by
            entry.savedStateHandle.getStateFlow<Int?>(kind.key, null).collectAsStateWithLifecycle()
        LaunchedEffect(result) {
            result?.let {
                model.applyFilter(kind.decode(it))
                entry.savedStateHandle[kind.key] = null
            }
        }
    }
    LaunchedEffect(model, language, revision) { model.updateContext(language, revision) }
    CatalogBrowseScreen(
        request,
        state,
        search,
        language,
        BrowseActions(
            back,
            model::retry,
            model::query,
            model::toggleSearch,
            model::tab,
            model::applyFilter,
            navigate,
        ),
    )
    }
}

@Composable
internal fun CatalogDetailEntry(
    entry: NavBackStackEntry,
    request: DetailRequest,
    context: CatalogEntryContext,
    loadDrawing: suspend (Int) -> ReferenceDrawing?,
    navigate: (DetailNavigation) -> Unit,
) {
    with(context) {
    val model: CatalogDetailViewModel =
        viewModel(
            entry,
            key = "detail",
            factory =
                viewModelFactory {
                    initializer {
                        CatalogDetailViewModel(
                            createSavedStateHandle(),
                            repository,
                            request,
                            language,
                            loadDrawing,
                        )
                    }
                },
        )
    val state by model.state.collectAsStateWithLifecycle()
    val search by model.search.state.collectAsStateWithLifecycle()
    LaunchedEffect(model, language, revision) { model.updateContext(language, revision) }
    CatalogDetailScreen(
        request,
        state,
        search,
        language,
        DetailActions(back, model::retry, model.search::query, model.search::toggle, navigate),
    )
    }
}

@Composable
internal fun CatalogFilterEntry(
    entry: NavBackStackEntry,
    request: FilterRequest,
    context: CatalogEntryContext,
    select: (FilterResult) -> Unit,
) {
    with(context) {
    val model: CatalogFilterViewModel =
        viewModel(
            entry,
            key = "filter",
            factory =
                viewModelFactory {
                    initializer {
                        CatalogFilterViewModel(
                            createSavedStateHandle(),
                            repository,
                            request,
                            language,
                        )
                    }
                },
        )
    val state by model.state.collectAsStateWithLifecycle()
    val search by model.search.state.collectAsStateWithLifecycle()
    LaunchedEffect(model, language, revision) { model.updateContext(language, revision) }
    CatalogFilterScreen(
        request,
        state,
        search,
        language,
        FilterActions(back, model::retry, model.search::query, model.search::toggle, select),
    )
    }
}

/** Primitive persistence codec is separate from typed requests, screen state and business rules. */
internal enum class CatalogResultKind(val key: String) {
    Chapter("resultChapter"),
    Kanji("resultKanji"),
    Radical("resultRadical"),
    Strokes("resultStrokes"),
    Jlpt("resultJlpt");

    fun decode(value: Int): FilterResult =
        when (this) {
            Chapter -> FilterResult.Chapter(value.takeIf { it != 0 })
            Kanji -> FilterResult.Kanji(value.takeIf { it != 0 })
            Radical -> FilterResult.Radical(value.takeIf { it != 0 })
            Strokes -> FilterResult.Strokes(value.takeIf { it != 0 })
            Jlpt -> FilterResult.Jlpt(decodeJlpt(value))
        }
}

internal fun FilterResult.encoded(): Pair<String, Int> =
    when (this) {
        is FilterResult.Chapter -> CatalogResultKind.Chapter.key to (value ?: 0)
        is FilterResult.Kanji -> CatalogResultKind.Kanji.key to (value ?: 0)
        is FilterResult.Radical -> CatalogResultKind.Radical.key to (value ?: 0)
        is FilterResult.Strokes -> CatalogResultKind.Strokes.key to (value ?: 0)
        is FilterResult.Jlpt -> CatalogResultKind.Jlpt.key to value.savedValue()
    }

internal data class CatalogEntryContext(
    val repository: CatalogRepository,
    val language: String,
    val back: () -> Unit,
    val revision: Long = 0L,
)
