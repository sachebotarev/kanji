package ru.mobui.kanji.feature.catalog

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import ru.mobui.kanji.domain.catalog.*

internal sealed interface BrowseInput {
    data object Home : BrowseInput
    data class Source(val request: BrowseRequest.Source, val tab: SourceTab, val kanji: KanjiFilters, val words: WordFilters, val query: String) : BrowseInput
    data class KanjiIndex(val filters: KanjiFilters, val query: String) : BrowseInput
    data class RadicalIndex(val strokes: Int?, val query: String) : BrowseInput
    data class Words(val request: BrowseRequest.RelatedWords, val query: String) : BrowseInput
}

internal class CatalogBrowseViewModel(private val saved: SavedStateHandle, repository: CatalogRepository,
    val request: BrowseRequest, private val language: String, compute: CoroutineDispatcher = Dispatchers.Default)
    : CatalogStateViewModel<BrowseInput, BrowseContent>(repository, compute) {
    private val source = request as? BrowseRequest.Source
    private val currentTab = MutableStateFlow(saved.get<String>("source.tab")?.let { name -> SourceTab.entries.firstOrNull { it.name == name } }
        ?.takeUnless { source?.chapter != null && it == SourceTab.Chapters } ?: source?.initialTab ?: SourceTab.Chapters)
    private val searches = SourceTab.entries.associateWith { CatalogSearchState(saved, "source.${it.name}") }
    private val singleSearch = CatalogSearchState(saved, "search")
    private fun activeSearch() = if (source != null) searches.getValue(currentTab.value) else singleSearch
    private val mutableSearch = MutableStateFlow(activeSearch().state.value)
    val search = mutableSearch.asStateFlow()
    init { retry() }
    fun query(value: String) { activeSearch().query(value); mutableSearch.value = activeSearch().state.value }
    fun toggleSearch() { activeSearch().toggle(); mutableSearch.value = activeSearch().state.value }
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
            is FilterResult.Chapter -> saved[if (forWords) "words.chapter" else "$prefix.chapter"] = result.value ?: source?.chapter
            is FilterResult.Kanji -> if (forWords) saved["words.kanji"] = result.value
            is FilterResult.Radical -> if (!forWords) saved["$prefix.radical"] = result.value
            is FilterResult.Strokes -> if (!forWords) saved[if (request == BrowseRequest.AllRadicals) "radicals.strokes" else "$prefix.strokes"] = result.value
            is FilterResult.Jlpt -> if (request == BrowseRequest.AllKanji) saved["index.jlpt"] = result.value.savedValue()
        }
    }
    override fun inputs(): Flow<BrowseInput> = when (val target = request) {
        BrowseRequest.Home -> flowOf(BrowseInput.Home)
        is BrowseRequest.Source -> combine(currentTab, saved.kanjiFilters("kanji", KanjiFilters(chapter = target.chapter)),
            saved.wordFilters(target.chapter), search) { tab, kanji, words, search -> BrowseInput.Source(target, tab, kanji, words, search.query) }
        BrowseRequest.AllKanji -> combine(saved.kanjiFilters("index"), search) { filters, search -> BrowseInput.KanjiIndex(filters, search.query) }
        BrowseRequest.AllRadicals -> combine(saved.getStateFlow<Int?>("radicals.strokes", null), search) { strokes, search -> BrowseInput.RadicalIndex(strokes, search.query) }
        is BrowseRequest.RelatedWords -> search.map { BrowseInput.Words(target, it.query) }
    }
    override suspend fun content(snapshot: CatalogSnapshot, input: BrowseInput): BrowseContent = when (input) {
        BrowseInput.Home -> BrowseContent.Home(snapshot.sources.map { SourceSummary(it, snapshot.sourceCounts.getValue(it.id)) })
        is BrowseInput.Source -> {
            val target = input.request
            val rows = when (input.tab) {
                SourceTab.Chapters -> {
                    val chapters = snapshot.chapters(target.sourceId)
                    SourceRows.Chapters(chapters.filter { input.query.isBlank() || it.number == input.query.trim().toIntOrNull() }, chapters.chapterRange())
                }
                SourceTab.Kanji -> SourceRows.Kanji(KanjiRows(snapshot.kanji(input.kanji.selection(target.sourceId), input.query, language),
                    snapshot.placements.count { it.sourceId == target.sourceId && (target.chapter == null || it.chapter == target.chapter) },
                    snapshot.numbers(target.sourceId)), input.kanji, input.kanji.radicalId?.let { snapshot.radicalById[it]?.symbol })
                SourceTab.Words -> {
                    val selection = input.words.selection(target.sourceId)
                    SourceRows.Words(WordRows(snapshot.words(selection, input.query, language), snapshot.wordGroups(selection, input.query, language),
                        snapshot.words(CatalogSelection(target.sourceId, target.chapter), "", language).size), input.words,
                        input.words.kanjiId?.let { snapshot.kanjiById[it]?.symbol },
                        input.query.isBlank() && input.words.kanjiId == null && input.words.chapter == target.chapter)
                }
            }
            BrowseContent.Source(target, snapshot.sourceById[target.sourceId], input.tab, rows)
        }
        is BrowseInput.KanjiIndex -> BrowseContent.AllKanji(KanjiRows(snapshot.kanji(input.filters.selection(null), input.query, language),
            snapshot.kanji.count { it.radicalId != null }), input.filters, input.filters.radicalId?.let { snapshot.radicalById[it]?.symbol })
        is BrowseInput.RadicalIndex -> BrowseContent.AllRadicals(snapshot.radicals(input.query, language, input.strokes), snapshot.radicals.size, input.strokes)
        is BrowseInput.Words -> {
            val target = input.request
            val selection = CatalogSelection(sourceId = target.sourceId, kanjiId = target.kanjiId)
            BrowseContent.RelatedWords(snapshot.kanjiById[target.kanjiId]?.symbol.orEmpty(), target.sourceId, target.sourceId?.let { snapshot.sourceById[it] },
                WordRows(snapshot.words(selection, input.query, language), snapshot.wordGroups(selection, input.query, language), snapshot.words(selection, "", language).size))
        }
    }
}
