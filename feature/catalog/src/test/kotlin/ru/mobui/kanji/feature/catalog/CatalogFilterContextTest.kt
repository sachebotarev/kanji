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
    fun multipleKanjiAndExplicitEmptyResultRoundTrip() {
        listOf(setOf(19968, 26376), emptySet<Int>()).forEach { ids ->
            val result = FilterResult.Kanji(ids)
            val (key, value) = result.encoded()
            assertEquals("resultKanji", key)
            assertEquals(result, CatalogResultKind.Kanji.decode(value))
        }
        assertEquals(FilterResult.Kanji(26376), CatalogResultKind.Kanji.decode(26376))
    }

    @Test
    fun radicalAndJlptSetsRoundTripWithoutLosingMissingOrEmptyValues() {
        listOf(emptySet(), setOf(1), setOf(1, 74)).forEach { ids ->
            val result = FilterResult.Radical(ids)
            assertEquals(result, CatalogResultKind.Radical.decode(result.encoded().second))
        }
        listOf(emptySet(), setOf<JlptFilter>(JlptFilter.Missing),
            setOf(JlptFilter.Exact(JlptLevel.N5), JlptFilter.Missing)).forEach { ids ->
            val result = FilterResult.Jlpt(ids)
            assertEquals(result, CatalogResultKind.Jlpt.decode(result.encoded().second))
        }
        assertEquals(FilterResult.Radical(74), CatalogResultKind.Radical.decode(74))
        assertEquals(FilterResult.Jlpt(JlptFilter.Missing), CatalogResultKind.Jlpt.decode(-1))
    }

    @Test
    fun strokeSetsAndLegacyScalarRoundTrip() {
        listOf(emptySet(), setOf(1), setOf(1, 4)).forEach { ids ->
            val result = FilterResult.Strokes(ids)
            assertEquals(result, CatalogResultKind.Strokes.decode(result.encoded().second))
        }
        assertEquals(FilterResult.Strokes(4), CatalogResultKind.Strokes.decode(4))
    }

    @Test
    fun chapterSetsRoundTripAndKeepTheOtherFacet() {
        listOf(emptySet(), setOf(3), setOf(3, 4)).forEach { ids ->
            val result = FilterResult.Chapter(ids)
            val (key, encoded) = result.encoded()
            assertEquals("resultChapter", key)
            assertEquals(result, CatalogResultKind.Chapter.decode(encoded))
        }
        assertEquals(FilterResult.Chapter(3), CatalogResultKind.Chapter.decode(3))
        val filter = WordFilters(chapterIds = setOf(3, 4), kanjiIds = setOf(19968, 26376))
        assertEquals(setOf(3, 4), filter.selection(1).chapterIds)
        assertEquals(setOf(19968, 26376), filter.selection(1).kanjiIds)
    }

    @Test
    fun chapterRequestKeepsAllSelectedKanji() {
        val ids = setOf(19968, 26376)
        val request = FilterRequest.ChaptersForWords(1, WordFilters(4, kanjiIds = ids))
        assertEquals(ids, request.filters.selection(1).kanjiIds)
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
