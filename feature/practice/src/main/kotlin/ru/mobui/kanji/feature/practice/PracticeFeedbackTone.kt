package ru.mobui.kanji.feature.practice

import ru.mobui.kanji.domain.practice.EntryMode
import ru.mobui.kanji.domain.practice.PracticeState

/** Presentation of assistance usage; it does not change acceptance or score. */
internal enum class PracticeFeedbackTone { Neutral, Success, Assisted, Exhausted }

internal fun hintUsageTone(used: Int, limit: Int, accepted: Boolean): PracticeFeedbackTone =
    when {
        used > 0 && limit > 0 && used >= limit -> PracticeFeedbackTone.Exhausted
        used > 0 -> PracticeFeedbackTone.Assisted
        accepted -> PracticeFeedbackTone.Success
        else -> PracticeFeedbackTone.Neutral
    }

internal fun PracticeState.completionTone(limit: Int): PracticeFeedbackTone {
    if (timedOut || !isComplete) return PracticeFeedbackTone.Exhausted
    val writable = cells.filter { it.task.mode == EntryMode.User }
    if (limit > 0 && (totalHintCount >= limit || writable.any { it.strokeHintsExhausted })) return PracticeFeedbackTone.Exhausted
    if (readingHintUsed || translationHintUsed || writable.any { it.strokeHintCount > 0 || it.shapeAcceptedWithPenalty }) {
        return PracticeFeedbackTone.Assisted
    }
    return PracticeFeedbackTone.Success
}
