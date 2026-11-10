package ru.mobui.kanji.feature.dashboard

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityOverviewTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 8) // Sunday

    @Test
    fun gridIsMondayAlignedSixWeeksEndingWithTodayWeek() {
        val monday = LocalDate.of(2026, 9, 28)
        val overview =
            activityOverviewFromEndedAt(
                listOf(monday.atStartOfDay(zone).toInstant().toEpochMilli()),
                zone,
                today,
            )
        assertEquals(VISIBLE_DAYS, overview.dailyCounts.size)
        assertEquals(1, overview.dailyCounts.first())
        assertEquals(0, overview.dailyCounts.last()) // Sunday today empty in this fixture
        // Index 0 is Monday 28 Sep; today Sunday is index 41
        assertEquals(41, ChronoUnit.DAYS.between(monday, today))
    }

    @Test
    fun futureDaysInCurrentWeekStayZero() {
        val wednesday = LocalDate.of(2026, 11, 4)
        val overview =
            activityOverviewFromEndedAt(
                emptyList(),
                zone,
                wednesday,
            )
        // Wednesday index within its week: Mon=0 … Wed=2 → last column indices 35..41
        // Days after Wednesday in the last week: Thu–Sun = indices 38..41
        assertTrue(overview.dailyCounts.subList(38, 42).all { it == 0 })
    }

    @Test
    fun intensitySaturatesAtTen() {
        assertEquals(0f, activityIntensity(0))
        assertEquals(0.5f, activityIntensity(5))
        assertEquals(1f, activityIntensity(10))
        assertEquals(1f, activityIntensity(25))
    }

    @Test
    fun streakCountsFromTodayWhenActive() {
        val yesterday = today.minusDays(1)
        val dayBefore = today.minusDays(2)
        val overview =
            activityOverviewFromEndedAt(
                listOf(dayBefore, yesterday, today).flatMap { day ->
                    List(2) { day.atStartOfDay(zone).toInstant().toEpochMilli() }
                },
                zone,
                today,
            )
        assertEquals(3, overview.streakDays)
        assertEquals(ActivityTone.KeepItUp, overview.tone)
    }

    @Test
    fun streakSurvivesWhenTodayEmptyButYesterdayActive() {
        val yesterday = today.minusDays(1)
        val overview =
            activityOverviewFromEndedAt(
                listOf(
                    yesterday.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
                    yesterday.atStartOfDay(zone).toInstant().toEpochMilli(),
                ),
                zone,
                today,
            )
        assertEquals(2, overview.streakDays)
        assertEquals(ActivityTone.KeepGoing, overview.tone)
    }

    @Test
    fun brokenStreakUsesRebuildTone() {
        val overview =
            activityOverviewFromEndedAt(
                listOf(today.minusDays(3).atStartOfDay(zone).toInstant().toEpochMilli()),
                zone,
                today,
            )
        assertEquals(0, overview.streakDays)
        assertEquals(ActivityTone.Rebuild, overview.tone)
    }

    @Test
    fun emptyHistoryUsesStartTone() {
        val overview = activityOverviewFromEndedAt(emptyList(), zone, today)
        assertEquals(0, overview.streakDays)
        assertEquals(ActivityTone.Start, overview.tone)
        assertTrue(overview.dailyCounts.all { it == 0 })
    }

    @Test
    fun dayCountIncludesAllTerminalCompletions() {
        val overview =
            activityOverviewFromEndedAt(
                List(7) { today.atStartOfDay(zone).toInstant().toEpochMilli() },
                zone,
                today,
            )
        assertEquals(7, overview.dailyCounts.last())
        assertEquals(1, overview.streakDays)
        assertEquals(ActivityTone.KeepItUp, overview.tone)
    }
}
