package ru.mobui.kanji.feature.catalog

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.mobui.kanji.domain.catalog.*
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing

/** Debug-only entry points use the same three state owners and screens as production. */
@Composable
fun CatalogReviewContent(repository: CatalogRepository, scenario: Int, language: String, dark: Boolean, variant: String,
    loadDrawing: suspend (Int) -> ReferenceDrawing?) {
    val source = if (variant.startsWith("tobira-")) 1 else 2
    val selected = variant in setOf("selected-filters", "selected-strokes", "large-filters")
    val filters = if (selected) KanjiFilters(strokes = 4, jlpt = JlptFilter.Exact(JlptLevel.N5)) else KanjiFilters()
    fun saved(prefix: String) = SavedStateHandle(mapOf(
        "$prefix.query" to if (variant.startsWith("empty")) "999" else "",
        "$prefix.open" to variant.startsWith("empty"),
        "index.strokes" to filters.strokes, "index.jlpt" to filters.jlpt.savedValue()))
    val key = "review-$scenario-$language-$variant"
    CatalogTheme(dark) {
        when (scenario) {
            1, 2, 3, 4, 5, 6, 13, 14, 15 -> {
                val request = when (scenario) {
                    1 -> BrowseRequest.Home
                    2 -> BrowseRequest.Source(source)
                    3 -> BrowseRequest.Source(source, initialTab = SourceTab.Kanji)
                    4 -> BrowseRequest.Source(source, initialTab = SourceTab.Words)
                    5 -> BrowseRequest.Source(source, 1, SourceTab.Kanji)
                    6 -> BrowseRequest.Source(source, 1, SourceTab.Words)
                    13 -> BrowseRequest.AllKanji
                    14 -> BrowseRequest.AllRadicals
                    else -> BrowseRequest.RelatedWords(26085, source)
                }
                val prefix = if (request is BrowseRequest.Source) "source.${request.initialTab.name}" else "search"
                val model: CatalogBrowseViewModel = viewModel(key = key, factory = viewModelFactory {
                    initializer { CatalogBrowseViewModel(saved(prefix), repository, request, language) }
                })
                val state by model.state.collectAsStateWithLifecycle()
                val search by model.search.collectAsStateWithLifecycle()
                ReviewFrame(state, scenario, variant) { CatalogBrowseScreen(request, it, search, language,
                    BrowseActions({}, model::retry, model::query, model::toggleSearch, model::tab, model::applyFilter, {})) }
            }
            7, 8, 9 -> {
                val request = when (scenario) {
                    7 -> when (variant) {
                        "word-note" -> DetailRequest.Word(9, null)
                        "large-word" -> DetailRequest.Word(912, null)
                        else -> DetailRequest.Word(109, source)
                    }
                    8 -> DetailRequest.Kanji(26085, source)
                    else -> DetailRequest.Radical(72, source)
                }
                val model: CatalogDetailViewModel = viewModel(key = key, factory = viewModelFactory {
                    initializer { CatalogDetailViewModel(saved("detail.search"), repository, request, language, loadDrawing) }
                })
                val state by model.state.collectAsStateWithLifecycle()
                val search by model.search.state.collectAsStateWithLifecycle()
                ReviewFrame(state, scenario, variant) { CatalogDetailScreen(request, it, search, language,
                    DetailActions({}, model::retry, model.search::query, model.search::toggle, {})) }
            }
            10, 11, 12, 16, 17 -> {
                val request = when (scenario) {
                    10 -> FilterRequest.ChaptersForKanji(source, filters)
                    11 -> FilterRequest.Kanji(source, WordFilters())
                    12 -> FilterRequest.Radical(source, filters)
                    16 -> FilterRequest.KanjiStrokes(source, filters)
                    else -> FilterRequest.Jlpt(null, filters)
                }
                val model: CatalogFilterViewModel = viewModel(key = key, factory = viewModelFactory {
                    initializer { CatalogFilterViewModel(saved("filter.search"), repository, request, language) }
                })
                val state by model.state.collectAsStateWithLifecycle()
                val search by model.search.state.collectAsStateWithLifecycle()
                ReviewFrame(state, scenario, variant) { CatalogFilterScreen(request, it, search, language,
                    FilterActions({}, model::retry, model.search::query, model.search::toggle, {})) }
            }
            else -> error("Unknown catalog review scenario")
        }
    }
}

@Composable
private fun <T> ReviewFrame(state: CatalogLoad<T>, scenario: Int, variant: String, content: @Composable (CatalogLoad<T>) -> Unit) {
    val reviewed = when (variant) {
        "catalog-loading" -> CatalogLoad.Loading
        "catalog-error" -> CatalogLoad.Error
        else -> state
    }
    Box(Modifier.semantics { contentDescription = when (state) {
        CatalogLoad.Loading -> "catalog-review-loading"
        CatalogLoad.Error -> "catalog-review-error"
        is CatalogLoad.Ready -> "catalog-review-ready-$scenario"
    } }) { content(reviewed) }
}
