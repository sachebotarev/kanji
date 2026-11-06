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

    data class RadicalStrokes(val selected: Int?, val selectedIds: Set<Int> = setOfNotNull(selected)) : FilterRequest

    data class Jlpt(val sourceId: Int?, val filters: KanjiFilters) : FilterRequest
}

internal sealed interface FilterContent {
    data class Chapters(
        val entries: List<CatalogChapter>,
        val selected: Int?,
        val range: IntRange?,
        val showWordCount: Boolean,
        val selectedIds: Set<Int> = setOfNotNull(selected),
    ) : FilterContent

    data class Kanji(val rows: KanjiRows, val selected: Int?, val selectedIds: Set<Int> = setOfNotNull(selected),
        val radicals: List<CatalogRadical> = emptyList()) : FilterContent

    data class Radical(val entries: List<CatalogRadical>, val selected: Int?,
        val selectedIds: Set<Int> = setOfNotNull(selected), val counts: Map<Int, Int> = emptyMap()) : FilterContent

    data class Strokes(val counts: Map<Int, Int>, val selected: Int?, val kind: MaterialKind,
        val selectedIds: Set<Int> = setOfNotNull(selected)) :
        FilterContent

    data class Jlpt(val counts: Map<JlptFilter, Int>, val selected: JlptFilter,
        val selectedIds: Set<JlptFilter> = if (selected == JlptFilter.AnyLevel) emptySet() else setOf(selected)) : FilterContent
}

internal sealed interface FilterResult {
    data class Chapter(val value: Int?, val values: Set<Int> = setOfNotNull(value)) : FilterResult {
        constructor(values: Set<Int>) : this(values.singleOrNull(), values)
    }

    data class Kanji(val value: Int?, val values: Set<Int> = setOfNotNull(value)) : FilterResult {
        constructor(values: Set<Int>) : this(values.singleOrNull(), values)
    }

    data class Radical(val value: Int?, val values: Set<Int> = setOfNotNull(value)) : FilterResult {
        constructor(values: Set<Int>) : this(values.singleOrNull(), values)
    }

    data class Strokes(val value: Int?, val values: Set<Int> = setOfNotNull(value)) : FilterResult {
        constructor(values: Set<Int>) : this(values.singleOrNull(), values)
    }

    data class Jlpt(val value: JlptFilter,
        val values: Set<JlptFilter> = if (value == JlptFilter.AnyLevel) emptySet() else setOf(value)) : FilterResult {
        constructor(values: Set<JlptFilter>) : this(values.singleOrNull() ?: JlptFilter.AnyLevel, values)
    }
}
