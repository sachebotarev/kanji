package ru.mobui.kanji.checker.shape

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
import ru.mobui.kanji.domain.handwriting.*

class StrokeEndpointCompensatorTest {
    private fun points(vararg coordinates: Pair<Float, Float>) = coordinates.map { ReferencePoint(it.first, it.second) }
    private val noisy = listOf(
        points(.1f to .484f, .1f to .5f, .9f to .5f, .9f to .516f),
        points(.5f to .1f, .5f to .9f),
    )
    private val clean = listOf(points(.1f to .5f, .9f to .5f), noisy[1])
    private fun sample(lines: List<List<ReferencePoint>>) = WritingSample(lines.map { line ->
        InkStroke(line.mapIndexed { index, p -> InkPoint(p.x, p.y, index.toLong()) }, InkTool.Finger)
    })
    private val strict = ShapeCheckConfig(enabledCompensations = ru.mobui.kanji.domain.practice.CompensationStage.entries.toSet(), requiredMatchPercent = 99.9f, distanceTolerance = .001f,
        maxHorizontalStretchPercent = 0f, maxVerticalStretchPercent = 0f, maxRotationDegrees = 0f)

    @Test fun trimsOnlyShortSharpEndsAndPreservesStrokeCount() {
        val result = StrokeEndpointCompensator().compensate(noisy, .025f)
        assertEquals(clean, result.strokes)
        assertEquals(2, result.correctedEnds)
        assertTrue(result.removedLengthPercent > 0f && result.removedLengthPercent < 12f)
        assertEquals(noisy, StrokeEndpointCompensator().compensate(noisy, 0f).strokes)
        val tooLong = listOf(points(.1f to .5f, .9f to .5f, .9f to .6f))
        assertEquals(tooLong, StrokeEndpointCompensator().compensate(tooLong, .025f).strokes)
        val shortStroke = listOf(points(.1f to .5f, .12f to .5f, .12f to .51f))
        assertEquals(shortStroke, StrokeEndpointCompensator().compensate(shortStroke, .025f).strokes)
    }

    @Test fun selectsCleanupOnlyWhenReferenceScoreImprovesAndPreservesOriginalInk() {
        val original = sample(noisy)
        val disabled = strict.copy(endpointArtifactTolerance = 0f)
        assertFalse(ShapeMatcher(disabled).compare(original, ReferenceDrawing(clean)).isAccepted(disabled))
        val result = ShapeMatcher(strict).compare(original, ReferenceDrawing(clean))
        assertTrue(result.isAccepted(strict))
        assertEquals(2, result.compensation.correctedStrokeEnds)
        assertEquals(noisy[0].size, original.strokes[0].points.size)
        // A legitimate small hook present in the reference must not be erased.
        val legitimate = ShapeMatcher(strict).compare(original, ReferenceDrawing(noisy))
        assertTrue(legitimate.isAccepted(strict))
        assertFalse(legitimate.compensation.isApplied)
        assertFalse(ShapeMatcher(strict).compare(sample(noisy + noisy.take(1)), ReferenceDrawing(clean)).isAccepted(strict))
    }

    @Test fun cleanupIsScaleAndDirectionIndependent() {
        val transformed = noisy.map { line -> line.asReversed().map { p -> ReferencePoint(p.x * .3f + .2f, p.y * .3f + .3f) } }
        assertEquals(2, StrokeEndpointCompensator().compensate(transformed, .025f).correctedEnds)
        // Allow one raster pixel of rounding after translating/scaling float coordinates.
        val config = strict.copy(distanceTolerance = .01f)
        val result = ShapeMatcher(config).compare(sample(transformed), ReferenceDrawing(clean))
        assertTrue(result.isAccepted(config))
        assertEquals(2, result.compensation.correctedStrokeEnds)
    }

    @Test fun combinesEndpointSizeAndStretchCompensationsBeforeScoring() {
        val angle = 0f
        val transformed = noisy.map { line -> line.map { p ->
            val x = (p.x - .5f) * .88f * .8f
            val y = (p.y - .5f) * .8f
            ReferencePoint(.5f + x * cos(angle) - y * sin(angle), .5f + x * sin(angle) + y * cos(angle))
        } }
        val input = sample(transformed)
        val snapshot = input.strokes.map { it.points.toList() }
        val config = strict.copy(pipelineType = ru.mobui.kanji.domain.practice.CompensationPipelineType.Cascade, distanceTolerance = .01f, maxHorizontalStretchPercent = 15f, maxSizeCompensationPercent = 30f)
        val target = ReferenceDrawing(clean)
        val result = ShapeMatcher(config).compare(input, target)
        assertTrue(result.isAccepted(config))
        assertEquals(2, result.compensation.correctedStrokeEnds)
        assertEquals(0f, result.compensation.rotationDegrees, .001f)
        assertTrue(result.compensation.scaleX > 1f)
        assertFalse(ShapeMatcher(config.copy(endpointArtifactTolerance = 0f)).compare(input, target).isAccepted(config))
        assertFalse(ShapeMatcher(config.copy(maxHorizontalStretchPercent = 0f)).compare(input, target).isAccepted(config))
        assertFalse(ShapeMatcher(config.copy(maxSizeCompensationPercent = 0f)).compare(input, target).isAccepted(config))
        assertEquals(snapshot, input.strokes.map { it.points })
    }
}
