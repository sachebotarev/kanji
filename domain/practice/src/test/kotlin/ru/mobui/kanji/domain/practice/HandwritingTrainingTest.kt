package ru.mobui.kanji.domain.practice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.InkPoint
import ru.mobui.kanji.domain.handwriting.InkStroke
import ru.mobui.kanji.domain.handwriting.InkTool
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.ReferencePoint

class HandwritingTrainingTest {
    private val stroke =
        InkStroke(listOf(InkPoint(.2f, .3f, 0), InkPoint(.8f, .3f, 100)), InkTool.Finger)
    private val reference =
        ReferenceDrawing(listOf(listOf(ReferencePoint(.2f, .3f), ReferencePoint(.8f, .3f))))

    private fun task(limit: Long? = null) =
        PracticeTask(
            "word-1",
            "Библиотека",
            listOf(
                KanjiTask("position-0", KanjiId("same-kanji"), EntryMode.User),
                KanjiTask("position-1", KanjiId("same-kanji"), EntryMode.User),
                KanjiTask("position-2", KanjiId("prefilled"), EntryMode.Prefilled),
            ),
            limit,
        )

    @Test
    fun settingsInvalidateAcceptedVerdictAndRejectOldResponseWithoutChangingInkOrUndo() {
        val training = HandwritingTraining(task())
        training.addStroke("position-0", 0, stroke)
        val previous = training.checkRequests().single()
        assertTrue(training.applyCheck(previous, CheckVerdict.Correct))
        val sample = training.state.selected!!.sample
        training.invalidateChecks()
        val next = training.checkRequests().single()
        assertTrue(next.revision > previous.revision)
        assertSame(sample, next.sample)
        assertEquals(EntryStatus.Editing, training.state.selected!!.status)
        assertFalse(training.applyCheck(previous, CheckVerdict.Correct))
        assertTrue(training.applyCheck(next, CheckVerdict.Incorrect))
        assertTrue(training.canUndo())
        training.undo()
        assertEquals(0, training.state.selected!!.sample.strokeCount)
    }

    @Test
    fun settingsAlsoInvalidateVerdictsStoredInUndoHistory() {
        val training = HandwritingTraining(task())
        training.addStroke("position-0", 0, stroke)
        assertTrue(training.applyCheck(training.checkRequests().single(), CheckVerdict.Correct))
        training.clear()
        training.invalidateChecks()
        training.undo()
        assertEquals(EntryStatus.Editing, training.state.selected!!.status)
        assertEquals(1, training.checkRequests().size)
    }

    @Test
    fun timeoutMarksUnsetCellsIncorrectAndWaitsForSubmit() {
        val training = HandwritingTraining(task(100))
        training.setReference(KanjiId("same-kanji"), reference)
        training.setReference(KanjiId("prefilled"), reference)
        training.advanceTime(40)
        training.select(1)
        training.advanceTime(500)
        assertNull(training.state.result)
        assertTrue(training.state.timedOut)
        assertFalse(training.state.readingHintUsed)
        assertFalse(training.state.translationHintUsed)
        assertTrue(training.state.isResolved)
        assertEquals(100L, training.state.elapsedMs)
        assertEquals(
            listOf(EntryStatus.Incorrect, EntryStatus.Incorrect, EntryStatus.Prefilled),
            training.state.cells.map { it.status },
        )
        assertEquals(listOf(40L, 60L, 0L), training.state.cells.map { it.elapsedMs })
        training.addStroke("position-0", training.state.cells[0].revision, stroke)
        assertEquals(0, training.state.cells[0].sample.strokeCount)
        training.submit()
        val result = requireNotNull(training.state.result)
        assertEquals(AnswerStatus.TimedOut, result.status)
        assertFalse(result.readingHintUsed)
        assertFalse(result.translationHintUsed)
        assertEquals(100L, result.elapsedMs)
        assertEquals(
            listOf(EntryStatus.Incorrect, EntryStatus.Incorrect, EntryStatus.Prefilled),
            result.kanji.map { it.status },
        )
        assertEquals(
            listOf("position-0", "position-1", "position-2"),
            result.kanji.map { it.occurrenceId },
        )
    }

    @Test
    fun timeoutKeepsAlreadyCorrectCells() {
        val training = HandwritingTraining(task(100))
        training.setReference(KanjiId("same-kanji"), reference)
        training.setReference(KanjiId("prefilled"), reference)
        training.addStroke("position-0", 0, stroke)
        training.applyCheck(training.checkRequests().single(), CheckVerdict.Correct)
        training.advanceTime(100)
        assertTrue(training.state.timedOut)
        assertEquals(EntryStatus.Correct, training.state.cells[0].status)
        assertEquals(EntryStatus.Incorrect, training.state.cells[1].status)
        training.submit()
        assertEquals(AnswerStatus.TimedOut, training.state.result!!.status)
    }

    @Test
    fun advanceTimeStopsWhenAllCellsAreResolvedEvenIfSomeAreIncorrect() {
        val training = HandwritingTraining(task(1000))
        training.setReference(KanjiId("same-kanji"), reference)
        training.setReference(KanjiId("prefilled"), reference)
        training.addStroke("position-0", 0, stroke)
        training.applyCheck(training.checkRequests().single(), CheckVerdict.Correct)
        training.select(1)
        training.exhaustStrokeHints()
        assertTrue(training.state.isResolved)
        assertFalse(training.state.isComplete)
        training.advanceTime(50)
        assertEquals(0L, training.state.elapsedMs)
    }

    @Test
    fun timeoutKeepsASpoilerTheLearnerAlreadyOpened() {
        val training = HandwritingTraining(task(100))
        training.recordTranslationHint()
        training.advanceTime(100)
        assertTrue(training.state.timedOut)
        assertTrue(training.state.translationHintUsed)
        assertFalse(training.state.readingHintUsed)
        training.submit()
        val result = requireNotNull(training.state.result)
        assertTrue(result.translationHintUsed)
        assertFalse(result.readingHintUsed)
    }

    @Test
    fun undoRestoresStatusWithNewRevisionAndRejectsOldCheck() {
        val training = HandwritingTraining(task())
        training.addStroke("position-0", 0, stroke)
        val request = training.checkRequests().single()
        assertTrue(training.applyCheck(request, CheckVerdict.Correct))
        training.clear()
        training.undo()
        assertEquals(EntryStatus.Correct, training.state.selected!!.status)
        assertTrue(training.state.selected!!.revision > request.revision)
        assertFalse(training.applyCheck(request, CheckVerdict.Incorrect))
        training.select(1)
        assertFalse(training.canUndo())
        training.select(0)
        assertTrue(training.canUndo())
    }

    @Test
    fun nextUnsatisfiedUserIndexSkipsPrefillsAndCorrectCells() {
        val training = HandwritingTraining(task())
        assertEquals(1, training.nextUnsatisfiedUserIndex(0))
        assertNull(training.nextUnsatisfiedUserIndex(1))
        training.setReference(KanjiId("same-kanji"), reference)
        training.addStroke("position-0", 0, stroke)
        training.applyCheck(training.checkRequests().single(), CheckVerdict.Correct)
        assertEquals(1, training.nextUnsatisfiedUserIndex(0))
        training.select(1)
        training.addStroke("position-1", 0, stroke)
        training.applyCheck(training.checkRequests().single(), CheckVerdict.Correct)
        assertNull(training.nextUnsatisfiedUserIndex(0))
    }

    @Test
    fun prefilledCellIsSatisfiedWithoutReferenceDrawing() {
        val training = HandwritingTraining(task())
        assertTrue(training.state.cells.last().isSatisfied)
        assertFalse(training.state.isComplete)
        training.setReference(KanjiId("same-kanji"), reference)
        training.addStroke("position-0", 0, stroke)
        training.applyCheck(training.checkRequests().single(), CheckVerdict.Correct)
        training.select(1)
        training.addStroke("position-1", 0, stroke)
        training.applyCheck(training.checkRequests().single(), CheckVerdict.Correct)
        assertTrue(training.state.isComplete)
    }

    @Test
    fun hintPreservesAnswerAndCannotCompleteItButUsageSurvivesUndo() {
        val training = HandwritingTraining(task())
        training.setReference(KanjiId("same-kanji"), reference)
        training.setReference(KanjiId("prefilled"), reference)
        training.addStroke("position-0", 0, stroke)
        val sample = training.state.selected!!.sample
        val revision = training.state.selected!!.revision
        assertEquals(1, training.recordStrokeHint())
        assertSame(sample, training.state.selected!!.sample)
        assertEquals(revision, training.state.selected!!.revision)
        training.clear()
        training.undo()
        assertTrue(training.state.selected!!.hintUsed)
        assertEquals(1, training.state.selected!!.strokeHintCount)
        training.applyCheck(training.checkRequests().single(), CheckVerdict.Correct)
        training.select(1)
        training.addStroke("position-1", 0, stroke)
        training.applyCheck(training.checkRequests().single(), CheckVerdict.Correct)
        training.submit()
        val result = requireNotNull(training.state.result)
        assertEquals(AnswerStatus.Assisted, result.status)
        assertTrue(result.kanji.first().hintUsed)
        assertFalse(result.kanji[1].hintUsed)
        assertEquals(
            listOf(EntryStatus.Correct, EntryStatus.Correct, EntryStatus.Prefilled),
            result.kanji.map { it.status },
        )
        assertEquals(1, result.kanji.first().hintRequestCount)
        assertFalse(result.readingHintUsed)
    }

    @Test
    fun readingHintMarksWordAssistedWithoutCellHint() {
        val training = HandwritingTraining(task())
        training.setReference(KanjiId("same-kanji"), reference)
        training.setReference(KanjiId("prefilled"), reference)
        training.recordReadingHint()
        assertTrue(training.state.readingHintUsed)
        assertFalse(training.state.selected!!.hintUsed)
        training.addStroke("position-0", 0, stroke)
        training.applyCheck(training.checkRequests().single(), CheckVerdict.Correct)
        training.select(1)
        training.addStroke("position-1", 0, stroke)
        training.applyCheck(training.checkRequests().single(), CheckVerdict.Correct)
        training.submit()
        val result = requireNotNull(training.state.result)
        assertEquals(AnswerStatus.Assisted, result.status)
        assertTrue(result.readingHintUsed)
        assertTrue(result.kanji.none { it.hintUsed })
        assertTrue(result.kanji.none { it.hintRequestCount > 0 })
    }

    @Test
    fun translationHintMarksWordAssistedWithoutCellHint() {
        val training = HandwritingTraining(task())
        training.setReference(KanjiId("same-kanji"), reference)
        training.setReference(KanjiId("prefilled"), reference)
        training.recordTranslationHint()
        assertTrue(training.state.translationHintUsed)
        assertFalse(training.state.selected!!.hintUsed)
        training.addStroke("position-0", 0, stroke)
        training.applyCheck(training.checkRequests().single(), CheckVerdict.Correct)
        training.select(1)
        training.addStroke("position-1", 0, stroke)
        training.applyCheck(training.checkRequests().single(), CheckVerdict.Correct)
        training.submit()
        val result = requireNotNull(training.state.result)
        assertEquals(AnswerStatus.Assisted, result.status)
        assertTrue(result.translationHintUsed)
        assertFalse(result.readingHintUsed)
        assertTrue(result.kanji.none { it.hintUsed })
        assertTrue(result.kanji.none { it.hintRequestCount > 0 })
    }

    @Test
    fun withoutHintsWordIsCorrectWithoutAssistance() {
        val training = HandwritingTraining(task())
        training.setReference(KanjiId("same-kanji"), reference)
        training.setReference(KanjiId("prefilled"), reference)
        assertFalse(training.state.translationHintUsed)
        assertFalse(training.state.readingHintUsed)
        training.addStroke("position-0", 0, stroke)
        training.applyCheck(training.checkRequests().single(), CheckVerdict.Correct)
        training.select(1)
        training.addStroke("position-1", 0, stroke)
        training.applyCheck(training.checkRequests().single(), CheckVerdict.Correct)
        training.submit()
        val result = requireNotNull(training.state.result)
        assertEquals(AnswerStatus.Correct, result.status)
        assertFalse(result.translationHintUsed)
        assertFalse(result.readingHintUsed)
    }

    @Test
    fun skippedResultPreservesUnverifiedStrokesAndCannotBeChanged() {
        val training = HandwritingTraining(task())
        training.advanceTime(25)
        training.addStroke("position-0", 0, stroke)
        training.skip()
        val result = requireNotNull(training.state.result)
        training.clear()
        training.advanceTime(100)
        assertSame(result, training.state.result)
        assertEquals("word-1", result.taskId)
        assertEquals("Библиотека", result.russianWord)
        assertEquals(AnswerStatus.Skipped, result.status)
        assertEquals(EntryStatus.Editing, result.kanji.first().status)
        assertEquals(1, result.kanji.first().sample.strokeCount)
        assertEquals(25L, result.kanji.first().elapsedMs)
    }

    @Test
    fun earlySubmitDoesNotFinishIncompleteWord() {
        val training = HandwritingTraining(task())
        training.addStroke("position-0", 0, stroke)
        training.submit()
        assertNull(training.state.result)
        assertEquals(EntryStatus.Editing, training.state.selected!!.status)
        assertFalse(training.state.isResolved)
    }

    @Test
    fun submitAfterAllSettledWithIncorrectFinishesAsSkipped() {
        val training = HandwritingTraining(task())
        training.setReference(KanjiId("same-kanji"), reference)
        training.exhaustStrokeHints()
        training.select(1)
        training.exhaustStrokeHints()
        assertTrue(training.state.isResolved)
        assertFalse(training.state.isComplete)
        training.submit()
        assertEquals(AnswerStatus.Skipped, training.state.result!!.status)
    }

    @Test
    fun nextStrokeHintBudgetExhaustionMarksIncorrectAndBlocksEdits() {
        val training = HandwritingTraining(task())
        training.setReference(KanjiId("same-kanji"), reference)
        assertEquals(1, training.recordStrokeHint())
        assertEquals(2, training.recordStrokeHint())
        assertEquals(3, training.recordStrokeHint())
        training.exhaustStrokeHints()
        val cell = training.state.selected!!
        assertTrue(cell.hintUsed)
        assertTrue(cell.strokeHintsExhausted)
        assertEquals(3, cell.strokeHintCount)
        assertEquals(EntryStatus.Incorrect, cell.status)
        assertFalse(training.state.isComplete)
        training.addStroke("position-0", cell.revision, stroke)
        assertEquals(0, training.state.selected!!.sample.strokeCount)
        assertFalse(
            training.applyCheck(
                CheckRequest(
                    "word-1",
                    "position-0",
                    KanjiId("same-kanji"),
                    training.state.selected!!.revision,
                    training.state.selected!!.sample,
                ),
                CheckVerdict.Correct,
            ),
        )
        assertEquals(EntryStatus.Incorrect, training.state.selected!!.status)
    }
}
