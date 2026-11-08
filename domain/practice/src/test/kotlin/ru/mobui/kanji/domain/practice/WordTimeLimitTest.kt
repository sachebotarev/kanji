package ru.mobui.kanji.domain.practice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordTimeLimitTest {
    @Test
    fun nullBudgetMeansNoTimer() {
        assertNull(wordTimeLimitMs(null, 3))
    }

    @Test
    fun multipliesSecondsByPracticeSymbols() {
        assertEquals(10_000L, wordTimeLimitMs(10, 1))
        assertEquals(30_000L, wordTimeLimitMs(10, 3))
        assertEquals(20_000L, wordTimeLimitMs(5, 4))
    }

    @Test
    fun emptyPracticePoolStillGetsOneSlot() {
        assertEquals(10_000L, wordTimeLimitMs(10, 0))
    }
}
