package ru.mobui.kanji.domain.practice

import kotlin.math.roundToInt

/** Three-tier outcome shown on the training completion screen (PRACTICE.01). */
enum class TrainingGradeLevel {
    Excellent,
    Steady,
    KeepGoing,
}

data class TrainingSummary(
    val recipeName: String,
    val taskCount: Int,
    val correctCount: Int,
    val assistedCount: Int,
    val skippedCount: Int,
    val timedOutCount: Int,
    val elapsedMs: Long,
    val percent: Int,
    val grade: TrainingGradeLevel,
)

/**
 * Scores a completed training from terminal task statuses.
 *
 * Weights: correct 1.0, assisted 0.6, timed_out 0.25, skipped 0.0.
 * Tiers: ≥85 excellent, ≥55 steady, else keep going. See training-grade.md.
 */
fun Training.summarize(languageTag: String = "ru"): TrainingSummary {
    require(tasks.isNotEmpty())
    var correct = 0
    var assisted = 0
    var skipped = 0
    var timedOut = 0
    var points = 0.0
    var elapsed = 0L
    tasks.forEach { task ->
        elapsed += task.elapsedMs
        when (task.status) {
            TrainingTaskStatus.Correct -> {
                correct++
                points += CORRECT_POINTS
            }
            TrainingTaskStatus.Assisted -> {
                assisted++
                points += ASSISTED_POINTS
            }
            TrainingTaskStatus.TimedOut -> {
                timedOut++
                points += TIMED_OUT_POINTS
            }
            TrainingTaskStatus.Skipped -> skipped++
            TrainingTaskStatus.Pending,
            TrainingTaskStatus.InProgress ->
                error("Incomplete task ${task.id} status=${task.status.key}")
        }
    }
    val percent = (100.0 * points / tasks.size).roundToInt().coerceIn(0, 100)
    return TrainingSummary(
        recipeName = recipeName(languageTag),
        taskCount = tasks.size,
        correctCount = correct,
        assistedCount = assisted,
        skippedCount = skipped,
        timedOutCount = timedOut,
        elapsedMs = elapsed,
        percent = percent,
        grade =
            when {
                percent >= EXCELLENT_MIN -> TrainingGradeLevel.Excellent
                percent >= STEADY_MIN -> TrainingGradeLevel.Steady
                else -> TrainingGradeLevel.KeepGoing
            },
    )
}

private fun Training.recipeName(languageTag: String): String {
    val wanted = languageTag.lowercase(java.util.Locale.ROOT)
    return recipeNames.firstOrNull { it.languageTag.equals(wanted, ignoreCase = true) }?.name
        ?: recipeNames.firstOrNull { it.languageTag.equals("ru", ignoreCase = true) }?.name
        ?: recipeNames.firstOrNull()?.name
        ?: ""
}

private const val CORRECT_POINTS = 1.0
private const val ASSISTED_POINTS = 0.6
private const val TIMED_OUT_POINTS = 0.25
private const val EXCELLENT_MIN = 85
private const val STEADY_MIN = 55
