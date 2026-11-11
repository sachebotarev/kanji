package ru.mobui.kanji.feature.practice

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.mobui.kanji.domain.practice.EntryMode
import ru.mobui.kanji.domain.practice.EntryStatus
import ru.mobui.kanji.domain.practice.HandwritingTraining
import ru.mobui.kanji.domain.practice.KanjiId
import ru.mobui.kanji.domain.practice.KanjiTask
import ru.mobui.kanji.domain.practice.PracticeTask

class PracticeFeedbackToneTest {
    @Test
    fun unusedHelpBecomesSuccessOnlyAfterAcceptance() {
        assertEquals(PracticeFeedbackTone.Neutral, hintUsageTone(0, 3, false))
        assertEquals(PracticeFeedbackTone.Success, hintUsageTone(0, 3, true))
    }

    @Test
    fun usedHelpDistinguishesPartialAndFullBudget() {
        assertEquals(PracticeFeedbackTone.Assisted, hintUsageTone(1, 3, true))
        assertEquals(PracticeFeedbackTone.Assisted, hintUsageTone(2, 3, false))
        assertEquals(PracticeFeedbackTone.Exhausted, hintUsageTone(3, 3, true))
        assertEquals(PracticeFeedbackTone.Exhausted, hintUsageTone(1, 1, false))
    }

    @Test
    fun disabledHelpDoesNotCountAsSpentBudget() {
        assertEquals(PracticeFeedbackTone.Neutral, hintUsageTone(0, 0, false))
        assertEquals(PracticeFeedbackTone.Success, hintUsageTone(0, 0, true))
        assertEquals(PracticeFeedbackTone.Success, completed(0, 0).completionTone(0))
    }

    @Test
    fun nextReflectsAllCharactersRatherThanOnlySelectedOne() {
        assertEquals(PracticeFeedbackTone.Success, completed(0, 0).completionTone(3))
        assertEquals(PracticeFeedbackTone.Assisted, completed(1, 0).completionTone(3))
        assertEquals(PracticeFeedbackTone.Exhausted, completed(3, 0).completionTone(3))
    }

    @Test
    fun textHintsAndTimeoutAlsoAffectNext() {
        assertEquals(PracticeFeedbackTone.Assisted, completed(0, 0).copy(readingHintUsed = true).completionTone(3))
        assertEquals(PracticeFeedbackTone.Assisted, completed(0, 0).copy(translationHintUsed = true).completionTone(3))
        assertEquals(PracticeFeedbackTone.Exhausted, completed(0, 0).copy(timedOut = true).completionTone(3))
    }

    @Test
    fun softShapeMakesNextAssistedWithoutSpendingHelp() {
        val base = completed(0, 0)
        val state = base.copy(cells = base.cells.mapIndexed { index, cell -> cell.copy(shapeAcceptedWithPenalty = index == 0) })
        assertEquals(PracticeFeedbackTone.Assisted, state.completionTone(3))
        assertEquals(PracticeFeedbackTone.Success, hintUsageTone(0, 3, true))
    }

    private fun completed(firstHints: Int, secondHints: Int) =
        HandwritingTraining(
            PracticeTask(
                "usage", "Япония",
                listOf(
                    KanjiTask("first", KanjiId("日"), EntryMode.User),
                    KanjiTask("second", KanjiId("本"), EntryMode.User),
                ),
            ),
        ).state.let { state ->
            state.copy(cells = state.cells.mapIndexed { index, cell ->
                cell.copy(status = EntryStatus.Correct, strokeHintCount = if (index == 0) firstHints else secondHints)
            })
        }
}
