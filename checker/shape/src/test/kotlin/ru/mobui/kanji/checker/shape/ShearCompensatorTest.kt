package ru.mobui.kanji.checker.shape

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
import ru.mobui.kanji.domain.handwriting.*
import ru.mobui.kanji.domain.practice.CheckFailure

class ShearCompensatorTest {
    private val reference = listOf(
        listOf(ShapePoint(.25f, .2f), ShapePoint(.25f, .8f)),
        listOf(ShapePoint(.75f, .2f), ShapePoint(.75f, .8f)),
        listOf(ShapePoint(.25f, .5f), ShapePoint(.75f, .5f)),
    )
    private fun slanted(degrees: Float) = reference.map { s -> s.map { ShapePoint(it.x + tan(degrees * PI.toFloat() / 180) * (it.y - .2f), it.y) } }

    @Test fun correctsBothDirectionsPreservesStrokeCountAndHorizontalLines() {
        for (angle in listOf(-5f, 5f)) {
            val input = slanted(angle)
            val snapshot = input.map { it.toList() }
            val result = ShearCompensator(5f).compensate(input, reference)
            assertFalse(result.result.hasError)
            assertEquals(-angle, result.result.degrees, .0001f)
            assertEquals(3, result.strokes.size)
            assertEquals(.5f, result.strokes[2][0].y, .000001f)
            assertEquals(.5f, result.strokes[2][1].y, .000001f)
            assertEquals(snapshot, input)
            assertEquals(result.strokes[0][0].x, result.strokes[0][1].x, .000001f)
        }
    }

    @Test fun rejectsExcessiveShearAndChargesBudgetBeforeFinalComparison() {
        val config = ShapeCheckConfig(pipelineType = ru.mobui.kanji.domain.practice.CompensationPipelineType.Cascade, requiredMatchPercent = 95f, distanceTolerance = .035f, maxShearDegrees = 5f, maxCompensationBudgetPercent = 100f)
        val input = slanted(8f)
        val sample = WritingSample(input.map { s -> InkStroke(s.mapIndexed { i, p -> InkPoint(p.x, p.y, i.toLong()) }, InkTool.Finger) })
        val target = ReferenceDrawing(reference.map { s -> s.map { ReferencePoint(it.x, it.y) } })
        val result = ShapeMatcher(config).compare(sample, target)
        assertTrue(result.compensation.shear.hasError)
        assertTrue(result.compensation.budget.shearPercent > 0f)
        assertTrue(result.compensation.failures.any { it is CheckFailure.Shear })
        assertFalse(result.isAccepted(config))
        val valid = ShapeMatcher(config.copy(maxShearDegrees = 10f)).compare(sample, target)
        assertTrue(valid.isAccepted(config.copy(maxShearDegrees = 10f)))
    }

    @Test fun uniformScalingDoesNotInventShearInCurvedOrDiagonalStrokes() {
        val curved = listOf(listOf(ShapePoint(.2f, .1f), ShapePoint(.21f, .4f), ShapePoint(.5f, .8f)))
        val scaled = curved.map { s -> s.map { ShapePoint(.5f + (it.x - .5f) * .95f, .5f + (it.y - .5f) * .95f) } }
        assertFalse(ShearCompensator(5f).compensate(scaled, curved).result.isEstimated)
        val scaledVertical = reference.map { s -> s.map { ShapePoint(it.x * .9f, it.y * .9f) } }
        assertEquals(0f, ShearCompensator(5f).compensate(scaledVertical, reference).result.degrees, .0001f)
    }

    @Test fun doesNotInventSlantWithoutVerticalEvidenceAndCanBeDisabled() {
        val horizontal = reference.takeLast(1)
        assertFalse(ShearCompensator(5f).compensate(horizontal, horizontal).result.isEstimated)
        assertFalse(ShearCompensator(null).compensate(slanted(5f), reference).result.isEstimated)
        assertFalse(ShearCompensator(5f).compensate(reference, reference).result.isApplied)
        assertTrue(ShearCompensator(0f).compensate(slanted(1f), reference).result.hasError)
        for (bad in listOf(-1f, 21f, Float.NaN)) assertThrows(IllegalArgumentException::class.java) { ShapeCheckConfig(maxShearDegrees = bad) }
    }
}
