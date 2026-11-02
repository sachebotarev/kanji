package ru.mobui.kanji.feature.catalog

import androidx.compose.runtime.*
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing

@Composable
internal fun CatalogBrowseEntry(entry: NavBackStackEntry, request: BrowseRequest, language: String,
    repository: CatalogRepository, back: () -> Unit, navigate: (BrowseNavigation) -> Unit) {
    val model: CatalogBrowseViewModel = viewModel(entry, key = "browse-$language", factory = viewModelFactory {
        initializer { CatalogBrowseViewModel(createSavedStateHandle(), repository, request, language) }
    })
    val state by model.state.collectAsStateWithLifecycle()
    val search by model.search.collectAsStateWithLifecycle()
    CatalogResultKind.entries.forEach { kind ->
        val result by entry.savedStateHandle.getStateFlow<Int?>(kind.key, null).collectAsStateWithLifecycle()
        LaunchedEffect(result) { result?.let { model.applyFilter(kind.decode(it)); entry.savedStateHandle[kind.key] = null } }
    }
    CatalogBrowseScreen(request, state, search, language,
        BrowseActions(back, model::retry, model::query, model::toggleSearch, model::tab, model::applyFilter, navigate))
}

@Composable
internal fun CatalogDetailEntry(entry: NavBackStackEntry, request: DetailRequest, language: String,
    repository: CatalogRepository, loadDrawing: suspend (Int) -> ReferenceDrawing?, back: () -> Unit, navigate: (DetailNavigation) -> Unit) {
    val model: CatalogDetailViewModel = viewModel(entry, key = "detail-$language", factory = viewModelFactory {
        initializer { CatalogDetailViewModel(createSavedStateHandle(), repository, request, language, loadDrawing) }
    })
    val state by model.state.collectAsStateWithLifecycle()
    val search by model.search.state.collectAsStateWithLifecycle()
    CatalogDetailScreen(request, state, search, language, DetailActions(back, model::retry, model.search::query, model.search::toggle, navigate))
}

@Composable
internal fun CatalogFilterEntry(entry: NavBackStackEntry, request: FilterRequest, language: String,
    repository: CatalogRepository, back: () -> Unit, select: (FilterResult) -> Unit) {
    val model: CatalogFilterViewModel = viewModel(entry, key = "filter-$language", factory = viewModelFactory {
        initializer { CatalogFilterViewModel(createSavedStateHandle(), repository, request, language) }
    })
    val state by model.state.collectAsStateWithLifecycle()
    val search by model.search.state.collectAsStateWithLifecycle()
    CatalogFilterScreen(request, state, search, language, FilterActions(back, model::retry, model.search::query, model.search::toggle, select))
}

/** Primitive persistence codec is separate from typed requests, screen state and business rules. */
internal enum class CatalogResultKind(val key: String) {
    Chapter("resultChapter"), Kanji("resultKanji"), Radical("resultRadical"), Strokes("resultStrokes"), Jlpt("resultJlpt");
    fun decode(value: Int): FilterResult = when (this) {
        Chapter -> FilterResult.Chapter(value.takeIf { it != 0 })
        Kanji -> FilterResult.Kanji(value.takeIf { it != 0 })
        Radical -> FilterResult.Radical(value.takeIf { it != 0 })
        Strokes -> FilterResult.Strokes(value.takeIf { it != 0 })
        Jlpt -> FilterResult.Jlpt(decodeJlpt(value))
    }
}
internal fun FilterResult.encoded(): Pair<String, Int> = when (this) {
    is FilterResult.Chapter -> CatalogResultKind.Chapter.key to (value ?: 0)
    is FilterResult.Kanji -> CatalogResultKind.Kanji.key to (value ?: 0)
    is FilterResult.Radical -> CatalogResultKind.Radical.key to (value ?: 0)
    is FilterResult.Strokes -> CatalogResultKind.Strokes.key to (value ?: 0)
    is FilterResult.Jlpt -> CatalogResultKind.Jlpt.key to value.savedValue()
}
