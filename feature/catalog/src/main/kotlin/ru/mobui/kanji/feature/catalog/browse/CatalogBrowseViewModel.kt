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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
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

    data class RadicalIndex(val strokes: Int?, val query: String, val strokeIds: Set<Int> = setOfNotNull(strokes)) : BrowseInput

    data class Words(val request: BrowseRequest.RelatedWords, val query: String) : BrowseInput
}

internal class CatalogBrowseViewModel(
    private val saved: SavedStateHandle,
    repository: CatalogRepository,
    val request: BrowseRequest,
    language: String,
    compute: CoroutineDispatcher = Dispatchers.Default,
) : CatalogStateViewModel<BrowseInput, BrowseContent>(repository, compute, language) {
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

    private val tabSearches = combine(searches.values.map { it.state }) { states ->
        SourceTab.entries.zip(states.toList()).toMap()
    }
    val search =
        if (source == null) singleSearch.state
        else combine(currentTab, tabSearches) { tab, states -> states.getValue(tab) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, activeSearch().state.value)

    init {
        retry()
    }

    fun query(value: String) {
        activeSearch().query(value)
    }

    fun toggleSearch() {
        activeSearch().toggle()
    }

    fun tab(value: SourceTab) {
        if (source == null || source.chapter != null && value == SourceTab.Chapters) return
        currentTab.value = value
        saved["source.tab"] = value.name
    }

    fun applyFilter(result: FilterResult) {
        val forWords = source != null && currentTab.value == SourceTab.Words
        val prefix = if (source != null) "kanji" else "index"
        when (result) {
            is FilterResult.Chapter -> {
                val key = chapterFilterKey(forWords, prefix)
                val ids = source?.chapter?.let { setOf(it) } ?: result.values
                saved["${key}Ids"] = ids.sorted().toIntArray()
                saved[key] = ids.singleOrNull()
            }
            is FilterResult.Kanji -> if (forWords) {
                saved["words.kanjiIds"] = result.values.sorted().toIntArray()
                saved["words.kanji"] = result.values.singleOrNull()
            }
            is FilterResult.Radical -> if (!forWords) {
                saved["$prefix.radicalIds"] = result.values.sorted().toIntArray()
                saved["$prefix.radical"] = result.values.singleOrNull()
            }
            is FilterResult.Strokes -> if (!forWords) {
                val key = if (request == BrowseRequest.AllRadicals) "radicals.strokes" else "$prefix.strokes"
                saved[key] = result.value
                saved[if (request == BrowseRequest.AllRadicals) "radicals.strokeIds" else "$prefix.strokeIds"] = result.values.sorted().toIntArray()
            }
            is FilterResult.Jlpt -> if (!forWords) {
                saved["$prefix.jlptIds"] = result.values.map { it.savedValue() }.sorted().toIntArray()
                saved["$prefix.jlpt"] = result.value.savedValue()
            }
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
                    tabSearches,
                ) { tab, kanji, words, searches ->
                    BrowseInput.Source(target, tab, kanji, words, searches.getValue(tab).query)
                }
            BrowseRequest.AllKanji ->
                combine(saved.kanjiFilters("index"), search) { filters, search ->
                    BrowseInput.KanjiIndex(filters, search.query)
                }
            BrowseRequest.AllRadicals ->
                combine(saved.getStateFlow<Int?>("radicals.strokes", null),
                    saved.getStateFlow<IntArray?>("radicals.strokeIds", null), search) { strokes, ids, search ->
                    BrowseInput.RadicalIndex(strokes, search.query, ids?.toSet() ?: setOfNotNull(strokes))
                }
            is BrowseRequest.RelatedWords -> search.map { BrowseInput.Words(target, it.query) }
        }

    override suspend fun content(
        snapshot: CatalogSnapshot,
        input: BrowseInput,
        language: String,
    ): BrowseContent = browseContent(snapshot, input, language)

}

private fun chapterFilterKey(forWords: Boolean, prefix: String): String =
    if (forWords) "words.chapter" else "$prefix.chapter"
