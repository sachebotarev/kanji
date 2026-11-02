package ru.mobui.kanji.feature.catalog

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.JlptFilter
import ru.mobui.kanji.domain.catalog.JlptLevel
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing

/** Debug-only entry points use the same three state owners and screens as production. */
@Composable
fun CatalogReviewContent(
    repository: CatalogRepository,
    options: CatalogReviewOptions,
    loadDrawing: suspend (Int) -> ReferenceDrawing?,
) {
    with(options) {
    val source = if (variant.startsWith("tobira-")) 1 else 2
    val selected = variant in setOf("selected-filters", "selected-strokes", "large-filters")
    val filters =
        if (selected) KanjiFilters(strokes = 4, jlpt = JlptFilter.Exact(JlptLevel.N5))
        else KanjiFilters()
    @SuppressLint("VisibleForTests")
    fun saved(prefix: String) =
        SavedStateHandle().apply {
            set("$prefix.query", if (variant.startsWith("empty")) "999" else "")
            set("$prefix.open", variant.startsWith("empty"))
            set("index.strokes", filters.strokes)
            set("index.jlpt", filters.jlpt.savedValue())
        }
    val key = "review-$scenario-$language-$variant"
    CatalogTheme(dark) {
        when (scenario) {
            1,
            2,
            SOURCE_KANJI_SCENARIO,
            SOURCE_WORDS_SCENARIO,
            CHAPTER_KANJI_SCENARIO,
            CHAPTER_WORDS_SCENARIO,
            ALL_KANJI_SCENARIO,
            ALL_RADICALS_SCENARIO,
            RELATED_WORDS_SCENARIO -> {
                val request =
                    when (scenario) {
                        1 -> BrowseRequest.Home
                        2 -> BrowseRequest.Source(source)
                        SOURCE_KANJI_SCENARIO ->
                            BrowseRequest.Source(source, initialTab = SourceTab.Kanji)
                        SOURCE_WORDS_SCENARIO ->
                            BrowseRequest.Source(source, initialTab = SourceTab.Words)
                        CHAPTER_KANJI_SCENARIO -> BrowseRequest.Source(source, 1, SourceTab.Kanji)
                        CHAPTER_WORDS_SCENARIO -> BrowseRequest.Source(source, 1, SourceTab.Words)
                        ALL_KANJI_SCENARIO -> BrowseRequest.AllKanji
                        ALL_RADICALS_SCENARIO -> BrowseRequest.AllRadicals
                        else -> BrowseRequest.RelatedWords(SUN_KANJI_CODE_POINT, source)
                    }
                val prefix =
                    if (request is BrowseRequest.Source) "source.${request.initialTab.name}"
                    else "search"
                val model: CatalogBrowseViewModel =
                    viewModel(
                        key = key,
                        factory =
                            viewModelFactory {
                                initializer {
                                    CatalogBrowseViewModel(
                                        saved(prefix),
                                        repository,
                                        request,
                                        language,
                                    )
                                }
                            },
                    )
                val state by model.state.collectAsStateWithLifecycle()
                val search by model.search.collectAsStateWithLifecycle()
                ReviewFrame(state, scenario, variant) {
                    CatalogBrowseScreen(
                        request,
                        it,
                        search,
                        language,
                        BrowseActions(
                            {},
                            model::retry,
                            model::query,
                            model::toggleSearch,
                            model::tab,
                            model::applyFilter,
                            {},
                        ),
                    )
                }
            }
            WORD_DETAILS_SCENARIO,
            KANJI_DETAILS_SCENARIO,
            RADICAL_DETAILS_SCENARIO -> {
                val request =
                    when (scenario) {
                        WORD_DETAILS_SCENARIO ->
                            when (variant) {
                                "word-note" -> DetailRequest.Word(RADICAL_DETAILS_SCENARIO, null)
                                "large-word" -> DetailRequest.Word(LONG_EXAMPLE_WORD_ID, null)
                                else -> DetailRequest.Word(EXAMPLE_WORD_ID, source)
                            }
                        KANJI_DETAILS_SCENARIO -> DetailRequest.Kanji(SUN_KANJI_CODE_POINT, source)
                        else -> DetailRequest.Radical(SUN_RADICAL_ID, source)
                    }
                val model: CatalogDetailViewModel =
                    viewModel(
                        key = key,
                        factory =
                            viewModelFactory {
                                initializer {
                                    CatalogDetailViewModel(
                                        saved("detail.search"),
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
                ReviewFrame(state, scenario, variant) {
                    CatalogDetailScreen(
                        request,
                        it,
                        search,
                        language,
                        DetailActions(
                            {},
                            model::retry,
                            model.search::query,
                            model.search::toggle,
                            {},
                        ),
                    )
                }
            }
            CHAPTER_FILTER_SCENARIO,
            KANJI_FILTER_SCENARIO,
            RADICAL_FILTER_SCENARIO,
            STROKE_FILTER_SCENARIO,
            JLPT_FILTER_SCENARIO -> {
                val request =
                    when (scenario) {
                        CHAPTER_FILTER_SCENARIO -> FilterRequest.ChaptersForKanji(source, filters)
                        KANJI_FILTER_SCENARIO -> FilterRequest.Kanji(source, WordFilters())
                        RADICAL_FILTER_SCENARIO -> FilterRequest.Radical(source, filters)
                        STROKE_FILTER_SCENARIO -> FilterRequest.KanjiStrokes(source, filters)
                        else -> FilterRequest.Jlpt(null, filters)
                    }
                val model: CatalogFilterViewModel =
                    viewModel(
                        key = key,
                        factory =
                            viewModelFactory {
                                initializer {
                                    CatalogFilterViewModel(
                                        saved("filter.search"),
                                        repository,
                                        request,
                                        language,
                                    )
                                }
                            },
                    )
                val state by model.state.collectAsStateWithLifecycle()
                val search by model.search.state.collectAsStateWithLifecycle()
                ReviewFrame(state, scenario, variant) {
                    CatalogFilterScreen(
                        request,
                        it,
                        search,
                        language,
                        FilterActions(
                            {},
                            model::retry,
                            model.search::query,
                            model.search::toggle,
                            {},
                        ),
                    )
                }
            }
            else -> error("Unknown catalog review scenario")
        }
    }

    }
}

@Composable
private fun <T> ReviewFrame(
    state: CatalogLoad<T>,
    scenario: Int,
    variant: String,
    content: @Composable (CatalogLoad<T>) -> Unit,
) {
    val reviewed =
        when (variant) {
            "catalog-loading" -> CatalogLoad.Loading
            "catalog-error" -> CatalogLoad.Error
            else -> state
        }
    Box(
        Modifier.semantics {
            contentDescription =
                when (state) {
                    CatalogLoad.Loading -> "catalog-review-loading"
                    CatalogLoad.Error -> "catalog-review-error"
                    is CatalogLoad.Ready -> "catalog-review-ready-$scenario"
                }
        },
    ) {
        content(reviewed)
    }
}

private const val SOURCE_KANJI_SCENARIO = 3
private const val SOURCE_WORDS_SCENARIO = 4
private const val CHAPTER_KANJI_SCENARIO = 5
private const val CHAPTER_WORDS_SCENARIO = 6
private const val ALL_KANJI_SCENARIO = 13
private const val ALL_RADICALS_SCENARIO = 14
private const val RELATED_WORDS_SCENARIO = 15
private const val SUN_KANJI_CODE_POINT = 26085
private const val WORD_DETAILS_SCENARIO = 7
private const val KANJI_DETAILS_SCENARIO = 8
private const val RADICAL_DETAILS_SCENARIO = 9
private const val LONG_EXAMPLE_WORD_ID = 912
private const val EXAMPLE_WORD_ID = 109
private const val SUN_RADICAL_ID = 72
private const val CHAPTER_FILTER_SCENARIO = 10
private const val KANJI_FILTER_SCENARIO = 11
private const val RADICAL_FILTER_SCENARIO = 12
private const val STROKE_FILTER_SCENARIO = 16
private const val JLPT_FILTER_SCENARIO = 17
