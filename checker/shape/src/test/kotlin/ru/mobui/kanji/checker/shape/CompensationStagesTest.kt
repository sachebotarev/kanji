package ru.mobui.kanji.checker.shape

import org.junit.Assert.*
import org.junit.Test

class CompensationStagesTest {
    private val reference = listOf(listOf(ShapePoint(.2f, .2f), ShapePoint(.6f, .4f)))

    @Test fun stretchFitsBothAxesIndependentlyAndKeepsCenter() {
        val input = listOf(listOf(ShapePoint(.2f, .2f), ShapePoint(.4f, .6f)))
        val result = StretchCompensator(100f, 50f).compensate(input, reference)
        assertFalse(result.result.hasError)
        assertEquals(2f, result.result.scaleX, .000001f)
        assertEquals(.5f, result.result.scaleY, .000001f)
        assertEquals(.1f, result.strokes[0][0].x, .000001f)
        assertEquals(.3f, result.strokes[0][0].y, .000001f)
        assertEquals(.5f, result.strokes[0][1].x, .000001f)
        assertEquals(.5f, result.strokes[0][1].y, .000001f)
        assertTrue(StretchCompensator(99f, 50f).compensate(input, reference).result.hasError)
        assertTrue(StretchCompensator(100f, 49f).compensate(input, reference).result.hasError)
    }

    @Test fun zeroStretchDisablesOnlyTheSelectedAxis() {
        val input = listOf(listOf(ShapePoint(.2f, .2f), ShapePoint(.4f, .6f)))
        val vertical = StretchCompensator(0f, 50f).compensate(input, reference)
        assertFalse(vertical.result.hasError)
        assertEquals(1f, vertical.result.scaleX, 0f)
        assertEquals(.5f, vertical.result.scaleY, .000001f)
        assertEquals(.2f, vertical.strokes[0][0].x, 0f)
        assertEquals(.4f, vertical.strokes[0][1].x, 0f)
        val horizontal = StretchCompensator(100f, 0f).compensate(input, reference)
        assertFalse(horizontal.result.hasError)
        assertEquals(2f, horizontal.result.scaleX, .000001f)
        assertEquals(1f, horizontal.result.scaleY, 0f)
        assertEquals(.2f, horizontal.strokes[0][0].y, 0f)
        assertEquals(.6f, horizontal.strokes[0][1].y, 0f)
    }

    @Test fun sharedFrameDoesNotSilentlyUndoTranslation() {
        val target = listOf(listOf(ShapePoint(.2f, .2f), ShapePoint(.8f, .6f)))
        val shifted = target.map { s -> s.map { ShapePoint(it.x + .15f, it.y) } }
        val checker = RasterShapeChecker(.01f)
        val prepared = checker.prepare(target, listOf(target, shifted))
        assertEquals(100f, checker.compare(target, prepared)!!.inputPercent, .0001f)
        assertTrue(checker.compare(shifted, prepared)!!.inputPercent < 50f)
    }

    @Test fun jitterRemovesSmallOscillationsWithoutChangingEndpointsOrLargeCorner() {
        val noisy = listOf((0..20).map { i -> ShapePoint(.1f + i * .04f, if (i == 0 || i == 20) .5f else .5f + if (i % 2 == 0) .002f else -.002f) })
        val snapshot = noisy.map { it.toList() }
        val result = JitterCompensator(.5f).compensate(noisy)
        assertEquals(19, result.result.removedPoints)
        assertEquals(listOf(noisy[0].first(), noisy[0].last()), result.strokes[0])
        assertTrue(result.result.maxDeviationPercent <= .5f)
        assertEquals(snapshot, noisy)
        assertEquals(noisy, JitterCompensator(0f).compensate(noisy).strokes)
        val corner = listOf(listOf(ShapePoint(.1f, .1f), ShapePoint(.1f, .9f), ShapePoint(.9f, .9f)))
        assertEquals(corner, JitterCompensator(.5f).compensate(corner).strokes)
    }
}
