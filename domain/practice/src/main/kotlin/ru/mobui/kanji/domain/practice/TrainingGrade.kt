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
 * Scores a completed training from terminal tasks.
 *
 * Continuous word points come from [TrainingTask.wordPoints] (hint-scoring.md). Tiers: ≥85
 * excellent, ≥55 steady, else keep going.
 */
fun Training.summarize(
    languageTag: String = "ru",
    maxHintsPerKanji: Int = maxNextStrokeHints,
): TrainingSummary {
    require(tasks.isNotEmpty())
    require(maxHintsPerKanji > 0)
    var correct = 0
    var assisted = 0
    var skipped = 0
    var timedOut = 0
    var points = 0.0
    var elapsed = 0L
    tasks.forEach { task ->
        elapsed += task.elapsedMs
        points += task.wordPoints(maxHintsPerKanji)
        when (task.status) {
            TrainingTaskStatus.Correct -> correct++
            TrainingTaskStatus.Assisted -> assisted++
            TrainingTaskStatus.TimedOut -> timedOut++
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

private const val EXCELLENT_MIN = 85
private const val STEADY_MIN = 55
