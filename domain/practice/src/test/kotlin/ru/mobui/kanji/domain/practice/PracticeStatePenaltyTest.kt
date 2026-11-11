package ru.mobui.kanji.domain.practice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeStatePenaltyTest {
    private fun state() = HandwritingTraining(PracticeTask("penalty", "Япония", listOf(
        KanjiTask("0", KanjiId("日"), EntryMode.User),
        KanjiTask("1", KanjiId("本"), EntryMode.User),
    ))).state

    @Test
    fun noAssistanceOrSoftShapeMeansNoPenalty() {
        assertFalse(state().hasScorePenalty)
        val correct = state().let { it.copy(cells = it.cells.map { cell -> cell.copy(status = EntryStatus.Correct) }) }
        assertFalse(correct.hasScorePenalty)
    }

    @Test
    fun penaltyOnAnyCharacterOrTextClueBlocksAutomaticProgression() {
        val initial = state()
        assertTrue(initial.copy(cells = initial.cells.mapIndexed { index, cell -> cell.copy(strokeHintCount = if (index == 0) 1 else 0) }).hasScorePenalty)
        assertTrue(initial.copy(cells = initial.cells.mapIndexed { index, cell -> cell.copy(shapeAcceptedWithPenalty = index == 0) }).hasScorePenalty)
        assertTrue(initial.copy(readingHintUsed = true).hasScorePenalty)
        assertTrue(initial.copy(translationHintUsed = true).hasScorePenalty)
    }

    @Test
    fun hintsDoNotCreateFixablePenaltyOnlySoftShapeDoes() {
        val initial = state()
        assertFalse(initial.hasFixablePenalty)
        assertFalse(initial.copy(readingHintUsed = true).hasFixablePenalty)
        assertFalse(initial.copy(translationHintUsed = true).hasFixablePenalty)
        assertFalse(initial.copy(cells = initial.cells.mapIndexed { index, cell -> cell.copy(strokeHintCount = if (index == 0) 1 else 0) }).hasFixablePenalty)

        val withSoftShape = initial.copy(cells = initial.cells.mapIndexed { index, cell -> cell.copy(shapeAcceptedWithPenalty = index == 0) })
        assertTrue(withSoftShape.hasFixablePenalty)
        assertTrue(withSoftShape.cells[0].hasFixablePenalty)
        assertFalse(withSoftShape.cells[1].hasFixablePenalty)
    }
}
