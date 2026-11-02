package ru.mobui.kanji.checker.writing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.InkPoint
import ru.mobui.kanji.domain.handwriting.InkStroke
import ru.mobui.kanji.domain.handwriting.InkTool
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.ReferencePoint
import ru.mobui.kanji.domain.handwriting.WritingSample
import ru.mobui.kanji.domain.practice.CheckFailure
import ru.mobui.kanji.domain.practice.CheckRequest
import ru.mobui.kanji.domain.practice.CheckVerdict
import ru.mobui.kanji.domain.practice.KanjiId
import ru.mobui.kanji.domain.practice.KanjiReferenceSource

class StrokeWritingCheckerTest {
    @Test
    fun swappedAndReversedStrokesHaveSeparateFailures() = runBlocking {
        val horizontal = listOf(ReferencePoint(.1f, .2f), ReferencePoint(.9f, .2f))
        val vertical = listOf(ReferencePoint(.3f, .1f), ReferencePoint(.3f, .9f))
        val reference = ReferenceDrawing(listOf(horizontal, vertical))
        fun request(strokes: List<List<ReferencePoint>>) =
            CheckRequest(
                "task",
                "cell",
                KanjiId("u5341"),
                1,
                WritingSample(
                    strokes.map { points ->
                        InkStroke(
                            points.mapIndexed { i, p -> InkPoint(p.x, p.y, i.toLong()) },
                            InkTool.Finger,
                        )
                    },
                ),
            )
        val checker =
            StrokeWritingChecker(KanjiReferenceSource { reference }, Dispatchers.Unconfined)
        assertEquals(CheckVerdict.Correct, checker.check(request(listOf(horizontal, vertical))))
        val swapped = checker.checkWithDetails(request(listOf(vertical, horizontal)))
        assertTrue(swapped.failures.any { it is CheckFailure.StrokeOrder })
        assertFalse(swapped.failures.any { it is CheckFailure.StrokeDirection })
        val reversed = checker.checkWithDetails(request(listOf(horizontal.reversed(), vertical)))
        assertTrue(reversed.failures.any { it is CheckFailure.StrokeDirection })
        assertFalse(reversed.failures.any { it is CheckFailure.StrokeOrder })
    }
}
