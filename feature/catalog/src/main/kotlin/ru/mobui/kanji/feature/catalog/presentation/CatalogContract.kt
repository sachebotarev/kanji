package ru.mobui.kanji.feature.catalog.presentation

import ru.mobui.kanji.feature.catalog.components.numbers

import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogSelection
import ru.mobui.kanji.domain.catalog.JlptFilter

internal data class KanjiFilters(
    val chapter: Int? = null,
    val radicalId: Int? = null,
    val strokes: Int? = null,
    val jlpt: JlptFilter = JlptFilter.AnyLevel,
    val chapterIds: Set<Int> = setOfNotNull(chapter),
    val radicalIds: Set<Int> = setOfNotNull(radicalId),
    val strokeIds: Set<Int> = setOfNotNull(strokes),
    val jlptFilters: Set<JlptFilter> = if (jlpt == JlptFilter.AnyLevel) emptySet() else setOf(jlpt),
) {
    fun selection(sourceId: Int?) =
        CatalogSelection(sourceId, chapterIds.singleOrNull(), radicalIds.singleOrNull(),
            strokes = strokeIds.singleOrNull(), jlpt = jlptFilters.singleOrNull() ?: JlptFilter.AnyLevel,
            chapterIds = if (chapterIds.size > 1) chapterIds else emptySet(),
            radicalIds = if (radicalIds.size > 1) radicalIds else emptySet(),
            strokeIds = if (strokeIds.size > 1) strokeIds else emptySet(),
            jlptFilters = if (jlptFilters.size > 1) jlptFilters else emptySet())
}

internal data class WordFilters(val chapter: Int? = null, val kanjiId: Int? = null,
    val kanjiIds: Set<Int> = setOfNotNull(kanjiId),
    val chapterIds: Set<Int> = setOfNotNull(chapter)) {
    fun selection(sourceId: Int) = CatalogSelection(sourceId, chapterIds.singleOrNull(), kanjiId = kanjiIds.singleOrNull(),
        kanjiIds = if (kanjiIds.size > 1) kanjiIds else emptySet(),
        chapterIds = if (chapterIds.size > 1) chapterIds else emptySet())
}

internal sealed interface CatalogLoad<out T> {
    data object Loading : CatalogLoad<Nothing>

    data object Error : CatalogLoad<Nothing>

    data class Ready<T>(val content: T) : CatalogLoad<T>
}

internal data class SearchState(val query: String = "", val open: Boolean = false)

internal data class KanjiRows(
    val entries: List<CatalogKanji>,
    val total: Int,
    val numbers: Map<Int, String> = emptyMap(),
)
