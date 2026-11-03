package ru.mobui.kanji.feature.catalog.browse

import ru.mobui.kanji.domain.catalog.availableChapters

import ru.mobui.kanji.feature.catalog.components.chapterRange
import ru.mobui.kanji.feature.catalog.components.numbers
import ru.mobui.kanji.feature.catalog.presentation.KanjiRows

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

internal fun browseContent(snapshot: CatalogSnapshot, input: BrowseInput, language: String): BrowseContent =
when (input) {
    BrowseInput.Home ->
        BrowseContent.Home(
            snapshot.sources.map {
                SourceSummary(it, snapshot.sourceCounts.getValue(it.id))
            },
        )
    is BrowseInput.Source -> {
        val rows = when (input.tab) {
            SourceTab.Chapters -> chapterRows(snapshot, input)
            SourceTab.Kanji -> sourceKanjiRows(snapshot, input, language)
            SourceTab.Words -> sourceWordRows(snapshot, input, language)
        }
        BrowseContent.Source(input.request, snapshot.sourceById[input.request.sourceId], input.tab, rows)
    }
    is BrowseInput.KanjiIndex ->
        BrowseContent.AllKanji(
            KanjiRows(
                snapshot.kanji(input.filters.selection(null), input.query, language),
                snapshot.kanji.count { it.radicalId != null },
            ),
            input.filters,
            input.filters.radicalId?.let { snapshot.radicalById[it]?.symbol },
        )
    is BrowseInput.RadicalIndex ->
        BrowseContent.AllRadicals(
            snapshot.radicals(input.query, language, input.strokes),
            snapshot.radicals.size,
            input.strokes,
        )
    is BrowseInput.Words -> {
        val target = input.request
        val selection =
            CatalogSelection(sourceId = target.sourceId, kanjiId = target.kanjiId)
        BrowseContent.RelatedWords(
            snapshot.kanjiById[target.kanjiId]?.symbol.orEmpty(),
            target.sourceId,
            target.sourceId?.let { snapshot.sourceById[it] },
            WordRows(
                snapshot.words(selection, input.query, language),
                snapshot.wordGroups(selection, input.query, language),
                snapshot.words(selection, "", language).size,
            ),
        )
    }
}

private fun chapterRows(snapshot: CatalogSnapshot, input: BrowseInput.Source): SourceRows {
    val target = input.request
    val chapters = snapshot.chapters(target.sourceId)
    return SourceRows.Chapters(
        chapters.filter {
            input.query.isBlank() || it.number == input.query.trim().toIntOrNull()
        },
        chapters.chapterRange(),
    )
}

private fun sourceKanjiRows(
    snapshot: CatalogSnapshot,
    input: BrowseInput.Source,
    language: String,
): SourceRows {
    val target = input.request
    return SourceRows.Kanji(
        KanjiRows(
            snapshot.kanji(
                input.kanji.selection(target.sourceId),
                input.query,
                language,
            ),
            snapshot.placements.count {
                it.sourceId == target.sourceId &&
                    (target.chapter == null || it.chapter == target.chapter)
            },
            snapshot.numbers(target.sourceId),
        ),
        input.kanji,
        input.kanji.radicalId?.let { snapshot.radicalById[it]?.symbol },
        canChangeChapter = input.kanji.chapter == null ||
            snapshot.availableChapters(input.kanji.selection(target.sourceId)).any { it.number != input.kanji.chapter },
    )
}

private fun sourceWordRows(
    snapshot: CatalogSnapshot,
    input: BrowseInput.Source,
    language: String,
): SourceRows {
    val target = input.request
    val selection = input.words.selection(target.sourceId)
    return SourceRows.Words(
        WordRows(
            snapshot.words(selection, input.query, language),
            snapshot.wordGroups(selection, input.query, language),
            snapshot
                .words(
                    CatalogSelection(target.sourceId, target.chapter),
                    "",
                    language,
                )
                .size,
        ),
        input.words,
        input.words.kanjiId?.let { snapshot.kanjiById[it]?.symbol },
        input.query.isBlank() &&
            input.words.kanjiId == null &&
            input.words.chapter == target.chapter,
        canChangeChapter = input.words.chapter == null ||
            snapshot.availableChapters(selection).any { it.number != input.words.chapter },
    )
}
