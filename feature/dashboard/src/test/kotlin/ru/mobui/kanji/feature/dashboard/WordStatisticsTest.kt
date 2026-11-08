package ru.mobui.kanji.feature.dashboard

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class WordStatisticsTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 8)

    @Test
    fun countsOnlyTodayByStatus() {
        val yesterday = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val stats =
            wordStatisticsForDay(
                listOf(
                    now to "correct",
                    now to "correct",
                    now to "assisted",
                    now to "skipped",
                    now to "timed_out",
                    yesterday to "correct",
                    yesterday to "assisted",
                ),
                zone,
                today,
            )
        assertEquals(WordStatistics(correct = 2, assisted = 1, missed = 2), stats)
        assertEquals(5, stats.total)
    }

    @Test
    fun emptyDayIsZero() {
        assertEquals(
            WordStatistics(0, 0, 0),
            wordStatisticsForDay(emptyList(), zone, today),
        )
    }
}
