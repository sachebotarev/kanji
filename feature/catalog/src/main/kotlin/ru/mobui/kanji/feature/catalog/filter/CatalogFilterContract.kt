package ru.mobui.kanji.feature.catalog.filter

import ru.mobui.kanji.feature.catalog.presentation.KanjiFilters
import ru.mobui.kanji.feature.catalog.presentation.KanjiRows
import ru.mobui.kanji.feature.catalog.presentation.MaterialKind
import ru.mobui.kanji.feature.catalog.presentation.WordFilters

import ru.mobui.kanji.domain.catalog.CatalogChapter
import ru.mobui.kanji.domain.catalog.CatalogRadical
import ru.mobui.kanji.domain.catalog.JlptFilter

internal sealed interface FilterRequest {
    data class ChaptersForKanji(val sourceId: Int, val filters: KanjiFilters) : FilterRequest

    data class ChaptersForWords(val sourceId: Int, val filters: WordFilters) : FilterRequest

    data class Kanji(val sourceId: Int, val filters: WordFilters) : FilterRequest

    data class Radical(val sourceId: Int?, val filters: KanjiFilters) : FilterRequest

    data class KanjiStrokes(val sourceId: Int?, val filters: KanjiFilters) : FilterRequest

    data class RadicalStrokes(val selected: Int?) : FilterRequest

    data class Jlpt(val sourceId: Int?, val filters: KanjiFilters) : FilterRequest
}

internal sealed interface FilterContent {
    data class Chapters(
        val entries: List<CatalogChapter>,
        val selected: Int?,
        val range: IntRange?,
        val showWordCount: Boolean,
    ) : FilterContent

    data class Kanji(val rows: KanjiRows, val selected: Int?) : FilterContent

    data class Radical(val entries: List<CatalogRadical>, val selected: Int?) : FilterContent

    data class Strokes(val counts: Map<Int, Int>, val selected: Int?, val kind: MaterialKind) :
        FilterContent

    data class Jlpt(val counts: Map<JlptFilter, Int>, val selected: JlptFilter) : FilterContent
}

internal sealed interface FilterResult {
    data class Chapter(val value: Int?) : FilterResult

    data class Kanji(val value: Int?) : FilterResult

    data class Radical(val value: Int?) : FilterResult

    data class Strokes(val value: Int?) : FilterResult

    data class Jlpt(val value: JlptFilter) : FilterResult
}
