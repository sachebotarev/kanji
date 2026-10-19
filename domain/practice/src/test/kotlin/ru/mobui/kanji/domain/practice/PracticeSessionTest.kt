package ru.mobui.kanji.domain.practice

import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.*

class PracticeSessionTest {
    private val stroke = InkStroke(listOf(InkPoint(.2f, .3f, 0), InkPoint(.8f, .3f, 100)), InkTool.Finger)
    private val reference = ReferenceDrawing(listOf(listOf(ReferencePoint(.2f, .3f), ReferencePoint(.8f, .3f))))
    private fun task(limit: Long? = null) = PracticeTask("word-1", "Библиотека", listOf(
        KanjiTask("position-0", KanjiId("same-kanji"), EntryMode.User),
        KanjiTask("position-1", KanjiId("same-kanji"), EntryMode.User),
        KanjiTask("position-2", KanjiId("prefilled"), EntryMode.Prefilled),
    ), limit)

    @Test fun settingsInvalidateAcceptedVerdictAndRejectOldResponseWithoutChangingInkOrUndo() {
        val session = PracticeSession(task())
        session.addStroke("position-0", 0, stroke)
        val previous = session.checkRequests().single()
        assertTrue(session.applyCheck(previous, CheckVerdict.Correct))
        val sample = session.state.selected!!.sample
        session.invalidateChecks()
        val next = session.checkRequests().single()
        assertTrue(next.revision > previous.revision)
        assertSame(sample, next.sample)
        assertEquals(EntryStatus.Editing, session.state.selected!!.status)
        assertFalse(session.applyCheck(previous, CheckVerdict.Correct))
        assertTrue(session.applyCheck(next, CheckVerdict.Incorrect))
        assertTrue(session.canUndo())
        session.undo()
        assertEquals(0, session.state.selected!!.sample.strokeCount)
    }

    @Test fun settingsAlsoInvalidateVerdictsStoredInUndoHistory() {
        val session = PracticeSession(task())
        session.addStroke("position-0", 0, stroke)
        assertTrue(session.applyCheck(session.checkRequests().single(), CheckVerdict.Correct))
        session.clear()
        session.invalidateChecks()
        session.undo()
        assertEquals(EntryStatus.Editing, session.state.selected!!.status)
        assertEquals(1, session.checkRequests().size)
    }

    @Test fun timeoutClampsTimeAndAttributesItByOccurrence() {
        val session = PracticeSession(task(100))
        session.advanceTime(40)
        session.select(1)
        session.advanceTime(500)
        val result = requireNotNull(session.state.result)
        assertEquals(AnswerStatus.TimedOut, result.status)
        assertEquals(100L, result.elapsedMs)
        assertEquals(listOf(40L, 60L, 0L), result.kanji.map { it.elapsedMs })
        assertEquals(listOf("position-0", "position-1", "position-2"), result.kanji.map { it.occurrenceId })
    }

    @Test fun undoRestoresStatusWithNewRevisionAndRejectsOldCheck() {
        val session = PracticeSession(task())
        session.addStroke("position-0", 0, stroke)
        val request = session.checkRequests().single()
        assertTrue(session.applyCheck(request, CheckVerdict.Correct))
        session.clear()
        session.undo()
        assertEquals(EntryStatus.Correct, session.state.selected!!.status)
        assertTrue(session.state.selected!!.revision > request.revision)
        assertFalse(session.applyCheck(request, CheckVerdict.Incorrect))
        session.select(1)
        assertFalse(session.canUndo())
        session.select(0)
        assertTrue(session.canUndo())
    }

    @Test fun hintPreservesAnswerAndCannotCompleteItButUsageSurvivesUndo() {
        val session = PracticeSession(task())
        session.setReference(KanjiId("same-kanji"), reference)
        session.setReference(KanjiId("prefilled"), reference)
        session.addStroke("position-0", 0, stroke)
        val sample = session.state.selected!!.sample
        val revision = session.state.selected!!.revision
        session.recordHintUsed()
        assertSame(sample, session.state.selected!!.sample)
        assertEquals(revision, session.state.selected!!.revision)
        session.submit()
        assertNull(session.state.result)
        session.clear()
        session.undo()
        assertTrue(session.state.selected!!.hintUsed)
        session.applyCheck(session.checkRequests().single(), CheckVerdict.Correct)
        session.select(1)
        session.addStroke("position-1", 0, stroke)
        session.applyCheck(session.checkRequests().single(), CheckVerdict.Correct)
        session.submit()
        val result = requireNotNull(session.state.result)
        assertEquals(AnswerStatus.Assisted, result.status)
        assertTrue(result.kanji.first().hintUsed)
        assertFalse(result.kanji[1].hintUsed)
        assertEquals(listOf(EntryStatus.Correct, EntryStatus.Correct, EntryStatus.Prefilled), result.kanji.map { it.status })
    }

    @Test fun skippedResultPreservesUnverifiedStrokesAndCannotBeChanged() {
        val session = PracticeSession(task())
        session.advanceTime(25)
        session.addStroke("position-0", 0, stroke)
        session.skip()
        val result = requireNotNull(session.state.result)
        session.clear()
        session.advanceTime(100)
        assertSame(result, session.state.result)
        assertEquals("word-1", result.taskId)
        assertEquals("Библиотека", result.russianWord)
        assertEquals(AnswerStatus.Skipped, result.status)
        assertEquals(EntryStatus.Editing, result.kanji.first().status)
        assertEquals(1, result.kanji.first().sample.strokeCount)
        assertEquals(25L, result.kanji.first().elapsedMs)
    }
}
