package ru.mobui.kanji.feature.catalog

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import ru.mobui.kanji.domain.catalog.*

internal class CatalogFilterViewModel(saved: SavedStateHandle, repository: CatalogRepository,
    val request: FilterRequest, private val language: String, compute: CoroutineDispatcher = Dispatchers.Default)
    : CatalogStateViewModel<String, FilterContent>(repository, compute) {
    val search = CatalogSearchState(saved, "filter.search")
    init { retry() }
    override fun inputs() = search.state.map { it.query }
    override suspend fun content(snapshot: CatalogSnapshot, input: String): FilterContent = when (val target = request) {
        is FilterRequest.ChaptersForKanji -> chapters(snapshot, target.filters.selection(target.sourceId), target.filters.chapter, input, false)
        is FilterRequest.ChaptersForWords -> chapters(snapshot, target.filters.selection(target.sourceId), target.filters.chapter, input, true)
        is FilterRequest.Kanji -> {
            val entries = snapshot.kanji(CatalogSelection(target.sourceId, target.filters.chapter), input, language)
            FilterContent.Kanji(KanjiRows(entries, entries.size, snapshot.numbers(target.sourceId)), target.filters.kanjiId)
        }
        is FilterRequest.Radical -> FilterContent.Radical(snapshot.availableRadicals(target.filters.selection(target.sourceId), input, language), target.filters.radicalId)
        is FilterRequest.KanjiStrokes -> FilterContent.Strokes(snapshot.strokeCounts(target.filters.selection(target.sourceId)), target.filters.strokes, MaterialKind.Kanji)
        is FilterRequest.RadicalStrokes -> FilterContent.Strokes(snapshot.radicalStrokeCounts(), target.selected, MaterialKind.Radical)
        is FilterRequest.Jlpt -> FilterContent.Jlpt(snapshot.jlptCounts(target.filters.selection(target.sourceId)).mapKeys { (level, _) ->
            level?.let { JlptFilter.Exact(it) } ?: JlptFilter.Missing }, target.filters.jlpt)
    }
    private fun chapters(snapshot: CatalogSnapshot, selection: CatalogSelection, selected: Int?, query: String, words: Boolean): FilterContent.Chapters {
        val options = snapshot.availableChapters(selection).filter { query.isBlank() || it.number == query.trim().toIntOrNull() }
        return FilterContent.Chapters(options, selected, snapshot.chapters(requireNotNull(selection.sourceId)).chapterRange(), words)
    }
}
