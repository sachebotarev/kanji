package ru.mobui.kanji.checker.shape

import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.domain.practice.RotationPivot

class CompensationStagesTest {
    private val reference = listOf(listOf(ShapePoint(.2f, .2f), ShapePoint(.6f, .4f)))

    @Test fun uniformSizePreservesAspectRatioAndUpperLeftAndReportsBothDirections() {
        for (factor in listOf(.8f, 1.2f)) {
            val input = reference.map { s -> s.map { ShapePoint(.2f + (it.x - .2f) / factor, .2f + (it.y - .2f) / factor) } }
            val result = SizeCompensator(20f).compensate(input, reference)
            assertFalse(result.result.hasError)
            assertEquals(factor, result.result.scaleFactor, .000001f)
            assertEquals((factor - 1) * 100, result.result.changePercent, .0001f)
            assertEquals(input[0][0], result.strokes[0][0])
            assertEquals(.6f, result.strokes[0][1].x, .000001f)
            assertEquals(.4f, result.strokes[0][1].y, .000001f)
            assertTrue(SizeCompensator(19f).compensate(input, reference).result.hasError)
        }
        assertTrue(SizeCompensator(20f).compensate(listOf(listOf(ShapePoint(.3f, .3f))), reference).result.hasError)
    }

    @Test fun stretchFitsBothAxesIndependentlyAndKeepsAlignedCorner() {
        val input = listOf(listOf(ShapePoint(.2f, .2f), ShapePoint(.4f, .6f)))
        val result = StretchCompensator(100f, 50f).compensate(input, reference)
        assertFalse(result.result.hasError)
        assertEquals(2f, result.result.scaleX, .000001f)
        assertEquals(.5f, result.result.scaleY, .000001f)
        assertEquals(input[0][0], result.strokes[0][0])
        assertEquals(.6f, result.strokes[0][1].x, .000001f)
        assertEquals(.4f, result.strokes[0][1].y, .000001f)
        assertTrue(StretchCompensator(99f, 50f).compensate(input, reference).result.hasError)
        assertTrue(StretchCompensator(100f, 49f).compensate(input, reference).result.hasError)
    }

    @Test fun rotationKeepsEveryPivotFixedAndReportsExcessiveAngles() {
        val strokes = listOf(listOf(ShapePoint(.2f, .2f), ShapePoint(.8f, .2f), ShapePoint(.8f, .8f), ShapePoint(.2f, .8f)))
        val compensator = RotationCompensator(7f)
        val candidates = compensator.candidates(strokes)
        for ((kind, pivot) in listOf(
            RotationPivot.Center to ShapePoint(.5f, .5f),
            RotationPivot.TopLeft to ShapePoint(.2f, .2f),
            RotationPivot.TopRight to ShapePoint(.8f, .2f),
            RotationPivot.BottomLeft to ShapePoint(.2f, .8f),
            RotationPivot.BottomRight to ShapePoint(.8f, .8f),
        )) {
            assertTrue(candidates.any { it.result.degrees == 7f && it.result.pivot == kind && !it.result.hasError })
            val result = compensator.compensate(listOf(listOf(pivot)), 7f, pivot)
            assertEquals(pivot, result.single().single())
        }
        assertTrue(candidates.any { it.result.degrees == 20f && it.result.hasError })
        assertEquals(strokes, candidates.first().strokes)
    }

    @Test fun sharedFrameDoesNotSilentlyUndoRotationPivotTranslation() {
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
