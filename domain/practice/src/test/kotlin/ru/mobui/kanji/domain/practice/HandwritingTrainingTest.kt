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
    fun timeoutClampsTimeAndAttributesItByOccurrence() {
        val training = HandwritingTraining(task(100))
        training.advanceTime(40)
        training.select(1)
        training.advanceTime(500)
        val result = requireNotNull(training.state.result)
        assertEquals(AnswerStatus.TimedOut, result.status)
        assertEquals(100L, result.elapsedMs)
        assertEquals(listOf(40L, 60L, 0L), result.kanji.map { it.elapsedMs })
        assertEquals(
            listOf("position-0", "position-1", "position-2"),
            result.kanji.map { it.occurrenceId },
        )
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
    fun hintPreservesAnswerAndCannotCompleteItButUsageSurvivesUndo() {
        val training = HandwritingTraining(task())
        training.setReference(KanjiId("same-kanji"), reference)
        training.setReference(KanjiId("prefilled"), reference)
        training.addStroke("position-0", 0, stroke)
        val sample = training.state.selected!!.sample
        val revision = training.state.selected!!.revision
        training.recordHintUsed()
        assertSame(sample, training.state.selected!!.sample)
        assertEquals(revision, training.state.selected!!.revision)
        training.submit()
        assertNull(training.state.result)
        training.clear()
        training.undo()
        assertTrue(training.state.selected!!.hintUsed)
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
}
