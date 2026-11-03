package ru.mobui.kanji.feature.catalog.components

import ru.mobui.kanji.feature.catalog.presentation.KanjiFilters
import ru.mobui.kanji.feature.catalog.presentation.SearchState
import ru.mobui.kanji.feature.catalog.presentation.WordFilters

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.last
import ru.mobui.kanji.domain.catalog.CatalogChapter
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.JlptFilter
import ru.mobui.kanji.domain.catalog.JlptLevel

/** Text edits are acknowledged synchronously, independently of asynchronous result computation. */
internal class CatalogSearchState(private val saved: SavedStateHandle, private val key: String) {
    private val mutable =
        MutableStateFlow(SearchState(saved["$key.query"] ?: "", saved["$key.open"] ?: false))
    val state = mutable.asStateFlow()

    fun query(value: String) {
        mutable.value = mutable.value.copy(query = value)
        saved["$key.query"] = value
    }

    fun toggle() {
        val next = !mutable.value.open
        mutable.value = SearchState(if (next) mutable.value.query else "", next)
        saved["$key.open"] = next
        saved["$key.query"] = mutable.value.query
    }
}

internal fun SavedStateHandle.kanjiFilters(
    prefix: String,
    defaults: KanjiFilters = KanjiFilters(),
): Flow<KanjiFilters> =
    combine(
        getStateFlow<Int?>("$prefix.chapter", defaults.chapter),
        getStateFlow<Int?>("$prefix.radical", defaults.radicalId),
        getStateFlow<Int?>("$prefix.strokes", defaults.strokes),
        getStateFlow("$prefix.jlpt", defaults.jlpt.savedValue()),
    ) { chapter, radical, strokes, jlpt ->
        KanjiFilters(chapter, radical, strokes, decodeJlpt(jlpt))
    }

internal fun SavedStateHandle.wordFilters(defaultChapter: Int?): Flow<WordFilters> =
    combine(
        getStateFlow<Int?>("words.chapter", defaultChapter),
        getStateFlow<Int?>("words.kanji", null),
    ) { chapter, kanji ->
        WordFilters(chapter, kanji)
    }

/** Integer encoding is confined to SavedStateHandle/navigation boundaries. */
internal fun decodeJlpt(value: Int): JlptFilter =
    when (value) {
        -1 -> JlptFilter.Missing
        0 -> JlptFilter.AnyLevel
        else ->
            JlptLevel.entries.firstOrNull { it.number == value }?.let { JlptFilter.Exact(it) }
                ?: JlptFilter.AnyLevel
    }

internal fun JlptFilter.savedValue(): Int =
    when (this) {
        JlptFilter.AnyLevel -> 0
        JlptFilter.Missing -> -1
        is JlptFilter.Exact -> level.number
    }

internal fun CatalogSnapshot.numbers(sourceId: Int?): Map<Int, String> =
    if (sourceId == null) emptyMap()
    else
        placements
            .filter { it.sourceId == sourceId }
            .associate { it.kanjiId to "${it.chapter}–${it.number}" }

internal fun List<CatalogChapter>.chapterRange(): IntRange? =
    if (isEmpty()) null else first().number..last().number
