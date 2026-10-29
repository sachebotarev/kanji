package ru.mobui.kanji.checker.shape.compensation

import ru.mobui.kanji.checker.shape.coverage.StrokeCoverageCalculator
import ru.mobui.kanji.checker.strokes.geometry.ShapePoint
import ru.mobui.kanji.checker.shape.matching.ShapeMatcher

import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.*
import ru.mobui.kanji.domain.practice.*

class MinimumCompensationTest {
    private val lines = listOf(
        listOf(.2f to .2f, .2f to .8f),
        listOf(.8f to .2f, .8f to .8f),
        listOf(.2f to .5f, .8f to .5f),
    )
    private val reference = ReferenceDrawing(lines.map { s -> s.map { ReferencePoint(it.first, it.second) } })
    private fun sample(ink: List<List<Pair<Float, Float>>>) = WritingSample(ink.map { s ->
        InkStroke(s.mapIndexed { i, p -> InkPoint(p.first, p.second, i.toLong()) }, InkTool.Finger)
    })
    private val settings = RecognitionSettings(requiredMatchPercent = 95f, distanceTolerance = .035f,
        maxShearDegrees = 5f, maxCompensationBudgetPercent = 15f, jitterTolerancePercent = .5f)

    @Test fun closeTracingKeepsOriginalCoordinatesAndZeroBudget() {
        val ink = lines.map { s -> s.map { (x, y) -> x + .006f * (y - .5f) to y + .004f } }
        val result = ShapeMatcher(settings).compare(sample(ink), reference)
        assertTrue(result.isAccepted(settings))
        assertEquals(0f, result.compensation.budget.totalPercent, 0f)
        assertEquals(ink, result.compensatedDrawing!!.strokes.map { s -> s.map { it.x to it.y } })
        assertEquals(result.matchPercent, result.baselineMatchPercent, 0f)
    }

    @Test fun nearTraceWithShortEndpointOutlierDoesNotRescaleWholeGlyph() {
        val ink = lines.toMutableList()
        ink[0] = listOf(.19f to .2f, .2f to .2f, .2f to .8f)
        val result = ShapeMatcher(settings).compare(sample(ink), reference)
        assertTrue(result.isAccepted(settings))
        assertEquals(0f, result.compensation.budget.totalPercent, 0f)
        assertEquals(.19f, result.compensatedDrawing!!.strokes[0][0].x, 0f)
    }

    @Test fun incompleteInkIsNotExpandedToWholeReference() {
        val input = sample(lines.take(1))
        val result = ShapeMatcher(settings).compare(input, reference)
        assertFalse(result.strokeCountMatches)
        assertEquals(0f, result.compensation.budget.totalPercent, 0f)
        assertEquals(lines[0], result.compensatedDrawing!!.strokes[0].map { it.x to it.y })
    }

    @Test fun translationCanPassWithoutUnnecessaryShearSizeOrRotation() {
        val shifted = lines.map { s -> s.map { (x, y) -> x + .04f to y + .04f } }
        val result = ShapeMatcher(settings).compare(sample(shifted), reference)
        assertTrue(result.isAccepted(settings))
        assertTrue(result.compensation.translation.isApplied)
        assertFalse(result.compensation.shear.isApplied)
        assertFalse(result.compensation.size.isApplied)
        assertFalse(result.compensation.rotation.isApplied)
        assertTrue(result.compensation.budget.totalPercent < 10f)
        assertTrue(result.matchPercent >= result.baselineMatchPercent)
    }

    @Test fun identicalUnionCannotHideDifferentStrokeShapes() {
        val expected = listOf(
            listOf(.2f to .2f, .2f to .8f, .8f to .8f),
            listOf(.8f to .8f, .8f to .2f, .2f to .2f),
        )
        val wrong = listOf(
            listOf(.2f to .8f, .2f to .2f, .8f to .2f),
            listOf(.8f to .2f, .8f to .8f, .2f to .8f),
        )
        val target = ReferenceDrawing(expected.map { s -> s.map { ReferencePoint(it.first, it.second) } })
        val config = settings.copy(enabledCompensations = emptySet())
        val result = ShapeMatcher(config).compare(sample(wrong), target)
        assertTrue(result.globalMatchPercent > 98f)
        assertTrue(result.strokeMatchPercent < 65f)
        assertFalse(result.isAccepted(config))
        assertTrue(ShapeMatcher(config).compare(sample(expected.reversed().map { it.reversed() }), target).isAccepted(config))
    }

    @Test fun strokeCoverageDoesNotDependOnSamplingDensity() {
        val sparse = listOf(listOf(ShapePoint(.2f, .2f), ShapePoint(.8f, .8f)))
        val dense = listOf((0..100).map { ShapePoint(.2f + .6f * it / 100, .2f + .6f * it / 100) })
        val checker = StrokeCoverageCalculator()
        val forward = checker.compare(sparse, dense, listOf(0), .001f)
        val reverse = checker.compare(dense.map { it.reversed() }, sparse, listOf(0), .001f)
        assertEquals(100f, forward.inputPercent, 0f)
        assertEquals(100f, forward.referencePercent, 0f)
        assertEquals(forward, reverse)
    }

    @Test fun rejectedComparisonNeverLowersRawCoverage() {
        val changed = lines.mapIndexed { i, s -> s.map { (x, y) -> x + (if (i == 1) -.09f else .01f) to y + .015f } }
        val result = ShapeMatcher(settings).compare(sample(changed), reference)
        val raw = ShapeMatcher(settings.copy(enabledCompensations = emptySet())).compare(sample(changed), reference)
        assertTrue(result.matchPercent >= raw.matchPercent)
    }
}
