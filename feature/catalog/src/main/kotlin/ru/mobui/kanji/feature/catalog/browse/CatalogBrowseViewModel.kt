package ru.mobui.kanji.feature.catalog.browse

import ru.mobui.kanji.feature.catalog.presentation.CatalogStateViewModel

import ru.mobui.kanji.feature.catalog.components.CatalogSearchState
import ru.mobui.kanji.feature.catalog.components.kanjiFilters
import ru.mobui.kanji.feature.catalog.components.savedValue
import ru.mobui.kanji.feature.catalog.components.wordFilters
import ru.mobui.kanji.feature.catalog.filter.FilterResult
import ru.mobui.kanji.feature.catalog.presentation.KanjiFilters
import ru.mobui.kanji.feature.catalog.presentation.WordFilters

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSelection
import ru.mobui.kanji.domain.catalog.CatalogSnapshot

internal sealed interface BrowseInput {
    data object Home : BrowseInput

    data class Source(
        val request: BrowseRequest.Source,
        val tab: SourceTab,
        val kanji: KanjiFilters,
        val words: WordFilters,
        val query: String,
    ) : BrowseInput

    data class KanjiIndex(val filters: KanjiFilters, val query: String) : BrowseInput

    data class RadicalIndex(val strokes: Int?, val query: String) : BrowseInput

    data class Words(val request: BrowseRequest.RelatedWords, val query: String) : BrowseInput
}

internal class CatalogBrowseViewModel(
    private val saved: SavedStateHandle,
    repository: CatalogRepository,
    val request: BrowseRequest,
    private val language: String,
    compute: CoroutineDispatcher = Dispatchers.Default,
) : CatalogStateViewModel<BrowseInput, BrowseContent>(repository, compute) {
    private val source = request as? BrowseRequest.Source
    private val currentTab =
        MutableStateFlow(
            saved
                .get<String>("source.tab")
                ?.let { name -> SourceTab.entries.firstOrNull { it.name == name } }
                ?.takeUnless { source?.chapter != null && it == SourceTab.Chapters }
                ?: source?.initialTab
                ?: SourceTab.Chapters,
        )
    private val searches =
        SourceTab.entries.associateWith { CatalogSearchState(saved, "source.${it.name}") }
    private val singleSearch = CatalogSearchState(saved, "search")

    private fun activeSearch() =
        if (source != null) searches.getValue(currentTab.value) else singleSearch

    private val mutableSearch = MutableStateFlow(activeSearch().state.value)
    val search = mutableSearch.asStateFlow()

    init {
        retry()
    }

    fun query(value: String) {
        activeSearch().query(value)
        mutableSearch.value = activeSearch().state.value
    }

    fun toggleSearch() {
        activeSearch().toggle()
        mutableSearch.value = activeSearch().state.value
    }

    fun tab(value: SourceTab) {
        if (source == null || source.chapter != null && value == SourceTab.Chapters) return
        mutableSearch.value = searches.getValue(value).state.value
        currentTab.value = value
        saved["source.tab"] = value.name
    }

    fun applyFilter(result: FilterResult) {
        val forWords = source != null && currentTab.value == SourceTab.Words
        val prefix = if (source != null) "kanji" else "index"
        when (result) {
            is FilterResult.Chapter ->
                saved[chapterFilterKey(forWords, prefix)] =
                    result.value ?: source?.chapter
            is FilterResult.Kanji -> if (forWords) saved["words.kanji"] = result.value
            is FilterResult.Radical -> if (!forWords) saved["$prefix.radical"] = result.value
            is FilterResult.Strokes ->
                if (!forWords)
                    saved[
                        if (request == BrowseRequest.AllRadicals) "radicals.strokes"
                        else "$prefix.strokes"] = result.value
            is FilterResult.Jlpt ->
                if (request == BrowseRequest.AllKanji)
                    saved["index.jlpt"] = result.value.savedValue()
        }
    }

    override fun inputs(): Flow<BrowseInput> =
        when (val target = request) {
            BrowseRequest.Home -> flowOf(BrowseInput.Home)
            is BrowseRequest.Source ->
                combine(
                    currentTab,
                    saved.kanjiFilters("kanji", KanjiFilters(chapter = target.chapter)),
                    saved.wordFilters(target.chapter),
                    search,
                ) { tab, kanji, words, search ->
                    BrowseInput.Source(target, tab, kanji, words, search.query)
                }
            BrowseRequest.AllKanji ->
                combine(saved.kanjiFilters("index"), search) { filters, search ->
                    BrowseInput.KanjiIndex(filters, search.query)
                }
            BrowseRequest.AllRadicals ->
                combine(saved.getStateFlow<Int?>("radicals.strokes", null), search) {
                    strokes,
                    search ->
                    BrowseInput.RadicalIndex(strokes, search.query)
                }
            is BrowseRequest.RelatedWords -> search.map { BrowseInput.Words(target, it.query) }
        }

    override suspend fun content(snapshot: CatalogSnapshot, input: BrowseInput): BrowseContent =
        browseContent(snapshot, input, language)

}

private fun chapterFilterKey(forWords: Boolean, prefix: String): String =
    if (forWords) "words.chapter" else "$prefix.chapter"
