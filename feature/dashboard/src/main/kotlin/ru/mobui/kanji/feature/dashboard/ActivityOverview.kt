package ru.mobui.kanji.feature.dashboard

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class ActivityTone {
    Start,
    Rebuild,
    KeepGoing,
    KeepItUp,
}

data class ActivityOverview(
    val dailyCounts: List<Int>,
    val streakDays: Int,
    val tone: ActivityTone,
)

/** Brightness factor for a day cell; saturates at [MAX_ACTIVITY_INTENSITY] completed tasks. */
fun activityIntensity(count: Int): Float =
    (count / MAX_ACTIVITY_INTENSITY).coerceIn(0f, 1f)

/**
 * Builds the HOME.W03 snapshot from terminal task `ended_at` timestamps.
 *
 * Grid is six Monday–Sunday weeks ending with the week that contains [today].
 */
fun activityOverviewFromEndedAt(
    endedAtMillis: List<Long>,
    zone: ZoneId,
    today: LocalDate = LocalDate.now(zone),
): ActivityOverview {
    val byDay =
        endedAtMillis
            .groupingBy { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            .eachCount()
    val gridStart =
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(
            (VISIBLE_WEEKS - 1).toLong(),
        )
    val dailyCounts =
        List(VISIBLE_DAYS) { index ->
            val day = gridStart.plusDays(index.toLong())
            if (day.isAfter(today)) 0 else byDay[day] ?: 0
        }
    val streakDays = streakDays(dailyCounts, gridStart, today)
    val todayCount = byDay[today] ?: 0
    val tone =
        when {
            streakDays > 0 && todayCount > 0 -> ActivityTone.KeepItUp
            streakDays > 0 -> ActivityTone.KeepGoing
            endedAtMillis.isEmpty() -> ActivityTone.Start
            else -> ActivityTone.Rebuild
        }
    return ActivityOverview(dailyCounts, streakDays, tone)
}

private fun streakDays(dailyCounts: List<Int>, gridStart: LocalDate, today: LocalDate): Int {
    val todayIndex = ChronoUnit.DAYS.between(gridStart, today).toInt()
    if (todayIndex !in dailyCounts.indices) return 0
    val startIndex =
        when {
            dailyCounts[todayIndex] > 0 -> todayIndex
            todayIndex > 0 && dailyCounts[todayIndex - 1] > 0 -> todayIndex - 1
            else -> return 0
        }
    var streak = 0
    for (index in startIndex downTo 0) {
        if (dailyCounts[index] <= 0) break
        streak++
    }
    return streak
}

internal const val MAX_ACTIVITY_INTENSITY = 10f
internal const val VISIBLE_WEEKS = 6
internal const val VISIBLE_DAYS = 42
