package ru.mobui.kanji.feature.catalog

import ru.mobui.kanji.feature.catalog.filter.FilterRequest
import ru.mobui.kanji.feature.catalog.filter.FilterResult
import ru.mobui.kanji.feature.catalog.presentation.CatalogResultKind
import ru.mobui.kanji.feature.catalog.presentation.WordFilters
import ru.mobui.kanji.feature.catalog.presentation.encoded

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import ru.mobui.kanji.domain.catalog.CatalogSelection
import ru.mobui.kanji.domain.catalog.JlptFilter
import ru.mobui.kanji.domain.catalog.JlptLevel

class CatalogFilterContextTest {
    @Test
    fun wordChapterRequestCarriesOnlyWordConditions() {
        val request = FilterRequest.ChaptersForWords(1, WordFilters(4, 26376))
        assertEquals(
            CatalogSelection(sourceId = 1, chapter = 4, kanjiId = 26376),
            request.filters.selection(request.sourceId),
        )
    }

    @Test
    fun clearingASelectionRoundTripsWithoutConfusingAnAbsentResult() {
        val result = FilterResult.Chapter(null)
        val (key, value) = result.encoded()
        assertEquals("resultChapter", key)
        assertEquals(result, CatalogResultKind.Chapter.decode(value))
    }

    @Test
    fun jlptAndStrokeResultsRemainDistinctForTheSameNumber() {
        val strokes = FilterResult.Strokes(5)
        val jlpt = FilterResult.Jlpt(JlptFilter.Exact(JlptLevel.N5))
        assertNotEquals(strokes.encoded().first, jlpt.encoded().first)
        assertEquals(strokes, CatalogResultKind.Strokes.decode(strokes.encoded().second))
        assertEquals(jlpt, CatalogResultKind.Jlpt.decode(jlpt.encoded().second))
    }
}
