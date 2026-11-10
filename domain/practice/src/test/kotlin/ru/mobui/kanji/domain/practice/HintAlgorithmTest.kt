package ru.mobui.kanji.domain.practice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.InkPoint
import ru.mobui.kanji.domain.handwriting.InkStroke
import ru.mobui.kanji.domain.handwriting.InkTool
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.ReferencePoint
import ru.mobui.kanji.domain.handwriting.WritingSample

class HintAlgorithmTest {
    private val strokeA = listOf(ReferencePoint(0.1f, 0.1f), ReferencePoint(0.9f, 0.1f))
    private val strokeB = listOf(ReferencePoint(0.1f, 0.5f), ReferencePoint(0.9f, 0.5f))
    private val strokeC = listOf(ReferencePoint(0.1f, 0.9f), ReferencePoint(0.9f, 0.9f))
    private val reference = ReferenceDrawing(listOf(strokeA, strokeB, strokeC))

    @Test
    fun fullCharacterReturnsWholeReferenceAsStaticOverlay() {
        val hint =
            hintAlgorithmFor(HintMode.FullCharacter)
                .create(HintRequest(WritingSample(), reference))
        assertNotNull(hint)
        assertEquals(reference, hint!!.overlay)
        assertEquals(HintPlayback.Static, hint.playback)
        assertTrue(!hint.blocksInput)
    }

    @Test
    fun nextStrokeReturnsReferenceStrokesUpToNextMissingStroke() {
        val sample =
            WritingSample(
                listOf(
                    InkStroke(
                        listOf(InkPoint(0.1f, 0.1f, 0L), InkPoint(0.9f, 0.1f, 10L)),
                        InkTool.Finger,
                    ),
                ),
            )
        val hint =
            hintAlgorithmFor(HintMode.NextStroke).create(HintRequest(sample, reference))
        assertNotNull(hint)
        assertEquals(listOf(strokeA, strokeB), hint!!.overlay.strokes)
        assertEquals(HintPlayback.AnimateOnce, hint.playback)
        assertTrue(!hint.blocksInput)
    }

    @Test
    fun nextStrokeWhenAllStrokesDrawnReturnsFullReferenceWithAnimateAll() {
        val sample =
            WritingSample(
                List(3) {
                    InkStroke(listOf(InkPoint(0f, 0f, 0L), InkPoint(1f, 1f, 1L)), InkTool.Finger)
                },
            )
        val hint = hintAlgorithmFor(HintMode.NextStroke).create(HintRequest(sample, reference))
        assertNotNull(hint)
        assertEquals(reference, hint!!.overlay)
        assertEquals(HintPlayback.AnimateAll, hint.playback)
        assertTrue(!hint.blocksInput)
    }

    @Test
    fun fromKeyIsResilientWithFallbackAndAliases() {
        assertEquals(HintMode.NextStroke, HintMode.fromKey(null))
        assertEquals(HintMode.NextStroke, HintMode.fromKey("unknown"))
        assertEquals(HintMode.NextStroke, HintMode.fromKey(" next_stroke "))
        assertEquals(HintMode.FullCharacter, HintMode.fromKey("FULL_CHARACTER"))
        assertEquals(HintMode.FullCharacter, HintMode.fromKey("full"))
        assertEquals(HintMode.FullCharacter, HintMode.fromKey("full_kanji"))

        assertEquals(TaskClueMode.Reading, TaskClueMode.fromKey(null))
        assertEquals(TaskClueMode.Reading, TaskClueMode.fromKey("unknown"))
        assertEquals(TaskClueMode.Reading, TaskClueMode.fromKey(" reading "))
        assertEquals(TaskClueMode.Translation, TaskClueMode.fromKey("TRANSLATION"))
        assertEquals(TaskClueMode.None, TaskClueMode.fromKey("none"))
    }
}
