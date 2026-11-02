package ru.mobui.kanji.feature.catalog

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.mobui.kanji.domain.catalog.JlptFilter
import ru.mobui.kanji.domain.catalog.JlptLevel

class FilterValueTest {
    @Test fun unknownPersistedLevelFallsBackWithoutInventingARealLevel() {
        assertEquals(JlptFilter.AnyLevel, decodeJlpt(77))
        assertEquals(JlptFilter.AnyLevel, decodeJlpt(Int.MIN_VALUE))
        assertEquals(JlptFilter.Missing, decodeJlpt(-1))
        assertEquals(JlptFilter.Exact(JlptLevel.N5), decodeJlpt(5))
    }
}
