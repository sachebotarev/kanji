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
) {
    fun selection(sourceId: Int?) =
        CatalogSelection(sourceId, chapter, radicalId, strokes = strokes, jlpt = jlpt)
}

internal data class WordFilters(val chapter: Int? = null, val kanjiId: Int? = null) {
    fun selection(sourceId: Int) = CatalogSelection(sourceId, chapter, kanjiId = kanjiId)
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
