package ru.mobui.kanji.feature.dashboard

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** HOME.W02: completed tasks for one calendar day. */
data class WordStatistics(val correct: Int, val assisted: Int, val missed: Int) {
    val total: Int
        get() = correct + assisted + missed
}

/**
 * Counts terminal tasks for [day].
 *
 * @param tasks pairs of epoch-ms `ended_at` and status key
 * (`correct` / `assisted` / `skipped` / `timed_out`).
 */
fun wordStatisticsForDay(
    tasks: List<Pair<Long, String>>,
    zone: ZoneId,
    day: LocalDate = LocalDate.now(zone),
): WordStatistics {
    var correct = 0
    var assisted = 0
    var missed = 0
    tasks.forEach { (endedAt, statusKey) ->
        if (Instant.ofEpochMilli(endedAt).atZone(zone).toLocalDate() != day) return@forEach
        when (statusKey) {
            "correct" -> correct++
            "assisted" -> assisted++
            "skipped",
            "timed_out" -> missed++
        }
    }
    return WordStatistics(correct, assisted, missed)
}
