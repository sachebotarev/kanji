package ru.mobui.kanji.feature.catalog.filter

import ru.mobui.kanji.feature.catalog.presentation.CatalogStateViewModel

import ru.mobui.kanji.feature.catalog.components.CatalogSearchState
import ru.mobui.kanji.feature.catalog.components.chapterRange
import ru.mobui.kanji.feature.catalog.components.numbers
import ru.mobui.kanji.feature.catalog.presentation.KanjiRows
import ru.mobui.kanji.feature.catalog.presentation.MaterialKind

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSelection
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.JlptFilter
import ru.mobui.kanji.domain.catalog.availableChapters
import ru.mobui.kanji.domain.catalog.availableRadicals
import ru.mobui.kanji.domain.catalog.jlptCounts
import ru.mobui.kanji.domain.catalog.radicalStrokeCounts
import ru.mobui.kanji.domain.catalog.strokeCounts

internal class CatalogFilterViewModel(
    saved: SavedStateHandle,
    repository: CatalogRepository,
    val request: FilterRequest,
    language: String,
    compute: CoroutineDispatcher = Dispatchers.Default,
) : CatalogStateViewModel<String, FilterContent>(repository, compute, language) {
    val search = CatalogSearchState(saved, "filter.search")

    init {
        retry()
    }

    override fun inputs() = search.state.map { it.query }

    override suspend fun content(
        snapshot: CatalogSnapshot,
        input: String,
        language: String,
    ): FilterContent =
        when (val target = request) {
            is FilterRequest.ChaptersForKanji ->
                chapters(
                    snapshot,
                    target.filters.selection(target.sourceId),
                    target.filters.chapterIds,
                    false,
                )
            is FilterRequest.ChaptersForWords ->
                chapters(
                    snapshot,
                    target.filters.selection(target.sourceId),
                    target.filters.chapterIds,
                    true,
                )
            is FilterRequest.Kanji -> {
                val entries =
                    snapshot.kanji(
                        target.filters.selection(target.sourceId).copy(kanjiId = null, kanjiIds = emptySet()),
                        "",
                        language,
                    )
                FilterContent.Kanji(
                    KanjiRows(entries, entries.size, snapshot.numbers(target.sourceId)),
                    target.filters.kanjiId,
                    target.filters.kanjiIds,
                    snapshot.radicals,
                )
            }
            is FilterRequest.Radical -> {
                val selection = target.filters.selection(target.sourceId)
                val options = (snapshot.availableRadicals(selection, "", language) +
                    snapshot.radicals.filter { it.id in target.filters.radicalIds }).distinctBy { it.id }
                val counts = snapshot.kanji(selection.copy(radicalId = null, radicalIds = emptySet()), "", language)
                    .mapNotNull { it.radicalId }.groupingBy { it }.eachCount()
                FilterContent.Radical(options, target.filters.radicalId, target.filters.radicalIds, counts)
            }
            is FilterRequest.KanjiStrokes ->
                FilterContent.Strokes(
                    target.filters.strokeIds.associateWith { 0 } + snapshot.strokeCounts(target.filters.selection(target.sourceId)),
                    target.filters.strokes,
                    MaterialKind.Kanji,
                    target.filters.strokeIds,
                )
            is FilterRequest.RadicalStrokes ->
                FilterContent.Strokes(
                    snapshot.radicalStrokeCounts(),
                    target.selected,
                    MaterialKind.Radical,
                    target.selectedIds,
                )
            is FilterRequest.Jlpt ->
                FilterContent.Jlpt(
                    snapshot.jlptCounts(target.filters.selection(target.sourceId)).mapKeys {
                        (level, _) ->
                        level?.let { JlptFilter.Exact(it) } ?: JlptFilter.Missing
                    },
                    target.filters.jlpt,
                    target.filters.jlptFilters,
                )
        }

    private fun chapters(
        snapshot: CatalogSnapshot,
        selection: CatalogSelection,
        selected: Set<Int>,
        words: Boolean,
    ): FilterContent.Chapters {
        val all = snapshot.chapters(requireNotNull(selection.sourceId))
        val options = (snapshot.availableChapters(selection) + all.filter { it.number in selected })
            .distinctBy { it.number }.sortedBy { it.number }
        return FilterContent.Chapters(
            options, selected.singleOrNull(), all.chapterRange(), words, selected,
        )
    }
}
