package ru.mobui.kanji.domain.practice

import kotlin.math.pow

/** Reading spoiler multiplies the kanji average; see doc/implementation/hint-scoring.md. */
const val READING_HINT_FACTOR = 0.85
/** Translation spoiler multiplies the kanji average; see doc/implementation/hint-scoring.md. */
const val TRANSLATION_HINT_FACTOR = 0.85
/** Each counted stroke hint retains 80% of the remaining character credit. */
const val STROKE_HINT_FACTOR = 0.8

/**
 * Score of one practice symbol after the word is finished.
 *
 * [maxHints] validates the configured budget; the cost of a hint is independent of that budget.
 */
fun practiceSymbolPoints(status: TrainingSymbolStatus, hintRequestCount: Int, maxHints: Int): Double {
    require(maxHints >= 0)
    require(hintRequestCount >= 0)
    return when (status) {
        TrainingSymbolStatus.Incorrect -> 0.0
        TrainingSymbolStatus.Correct -> STROKE_HINT_FACTOR.pow(hintRequestCount)
        TrainingSymbolStatus.Prefilled,
        TrainingSymbolStatus.Empty,
        TrainingSymbolStatus.Editing -> 0.0
    }
}

/**
 * Continuous points for one terminal task. Calculates the practice symbol average
 * (awarding partial credit for correctly answered symbols in timed out or skipped words)
 * multiplied by reading and translation penalty factors if those spoilers were opened.
 */
fun TrainingTask.wordPoints(maxHintsPerKanji: Int = DEFAULT_MAX_NEXT_STROKE_HINTS): Double {
    require(maxHintsPerKanji >= 0)
    return when (status) {
        TrainingTaskStatus.Correct,
        TrainingTaskStatus.Assisted,
        TrainingTaskStatus.TimedOut,
        TrainingTaskStatus.Skipped -> {
            val practice = symbols.filter { it.role == TrainingSymbolRole.Practice }
            val kanjiAvg =
                if (practice.isEmpty()) 1.0
                else
                    practice
                        .map {
                            practiceSymbolPoints(it.status, it.hintRequestCount, maxHintsPerKanji) *
                                if (it.shapeVerdict == StoredCheckVerdict.AcceptedWithPenalty) SHAPE_SOFT_ACCEPTANCE_FACTOR else 1.0
                        }
                        .average()
            val readingFactor = if (readingHintUsed) READING_HINT_FACTOR else 1.0
            val translationFactor = if (translationHintUsed) TRANSLATION_HINT_FACTOR else 1.0
            kanjiAvg * readingFactor * translationFactor
        }
        TrainingTaskStatus.Pending,
        TrainingTaskStatus.InProgress ->
            error("Incomplete task $id status=${status.key}")
    }
}
