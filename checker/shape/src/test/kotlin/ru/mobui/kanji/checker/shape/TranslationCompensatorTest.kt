package ru.mobui.kanji.checker.shape

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.*
import ru.mobui.kanji.domain.practice.*

class TranslationCompensatorTest {
    private val lines = listOf(
        listOf(ShapePoint(.2f, .4f), ShapePoint(.6f, .4f)),
        listOf(ShapePoint(.4f, .2f), ShapePoint(.4f, .6f)),
    )
    private val reference = ReferenceDrawing(lines.map { s -> s.map { ReferencePoint(it.x, it.y) } })
    private fun shifted(dx: Float, dy: Float) = lines.map { s -> s.map { ShapePoint(it.x + dx, it.y + dy) } }
    private fun sample(lines: List<List<ShapePoint>>) = WritingSample(lines.map { s ->
        InkStroke(s.mapIndexed { i, p -> InkPoint(p.x, p.y, i.toLong()) }, InkTool.Finger)
    })

    @Test fun alignsUpperLeftCornerInBothDirectionsAndPreservesGeometry() {
        for (dx in listOf(-.1f, .1f)) for (dy in listOf(-.1f, .1f)) {
            val input = shifted(dx, dy)
            val snapshot = input.map { it.toList() }
            val result = TranslationCompensator(10f).compensate(input, lines)
            assertFalse(result.result.hasError)
            assertEquals(-dx * 100f, result.result.shiftXPercent, .00001f)
            assertEquals(-dy * 100f, result.result.shiftYPercent, .00001f)
            assertEquals(lines.size, result.strokes.size)
            lines.zip(result.strokes).forEach { (expected, actual) ->
                expected.zip(actual).forEach { (a, b) ->
                    assertEquals(a.x, b.x, .000001f)
                    assertEquals(a.y, b.y, .000001f)
                }
            }
            assertEquals(snapshot, input)
        }
    }

    @Test fun enforcesInclusiveLimitPerAxisAndHandlesZeroAndEmptyInput() {
        val compensator = TranslationCompensator(10f)
        // A diagonal shift is measured per axis, not by Euclidean length.
        assertFalse(compensator.compensate(shifted(.1f, .1f), lines).result.hasError)
        assertTrue(compensator.compensate(shifted(.1001f, 0f), lines).result.hasError)
        assertTrue(compensator.compensate(shifted(0f, -.1001f), lines).result.hasError)
        assertFalse(TranslationCompensator(0f).compensate(lines, lines).result.hasError)
        assertTrue(TranslationCompensator(0f).compensate(shifted(.001f, 0f), lines).result.hasError)
        assertTrue(compensator.compensate(emptyList(), lines).strokes.isEmpty())
        assertFalse(compensator.compensate(emptyList(), lines).result.hasError)
    }

    @Test fun rejectsExcessiveTranslationEvenWithPerfectShapeAndReturnsDetails() = runBlocking {
        val config = ShapeCheckConfig(maxTranslationPercent = 10f)
        val input = sample(shifted(.15f, -.1f))
        val match = ShapeMatcher(config).compare(input, reference)
        assertEquals(100f, match.matchPercent, .001f)
        assertTrue(match.compensation.translation.hasError)
        assertFalse(match.isAccepted(config))
        val checker = ShapeKanjiChecker(KanjiReferenceSource { reference }, Dispatchers.Unconfined, config)
        val request = CheckRequest("task", "cell", KanjiId("any"), 0, input)
        val report = checker.checkWithDetails(request)
        assertEquals(CheckVerdict.Incorrect, report.verdict)
        assertEquals(-15f, report.compensation.translation.shiftXPercent, .00001f)
        assertEquals(10f, report.compensation.translation.shiftYPercent, .00001f)
        assertEquals(listOf(CheckFailure.Translation(report.compensation.translation)), report.failures)
        assertEquals(CheckVerdict.Correct, checker.check(request.copy(sample = sample(shifted(.1f, -.1f)))))
    }

    @Test fun measuresOriginalShiftBeforeEndpointCleanup() {
        val input = listOf(listOf(ShapePoint(.3f, .2f), ShapePoint(.3f, .5f), ShapePoint(.8f, .5f), ShapePoint(.8f, .51f)))
        val target = ReferenceDrawing(listOf(listOf(ReferencePoint(.2f, .4f), ReferencePoint(.7f, .4f))))
        val config = ShapeCheckConfig(pipelineType = CompensationPipelineType.Cascade, maxTranslationPercent = 15f)
        val result = ShapeMatcher(config).compare(sample(input), target)
        assertEquals(-10f, result.compensation.translation.shiftXPercent, .00001f)
        assertEquals(20f, result.compensation.translation.shiftYPercent, .00001f)
        assertTrue(result.compensation.translation.hasError)
    }

    @Test fun alignedWorkingCoordinatesMayExceedUnitSquareWithoutClipping() {
        val input = listOf(listOf(ShapePoint(0f, 0f), ShapePoint(1f, 1f)))
        val target = listOf(listOf(ShapePoint(.5f, .5f), ShapePoint(.6f, .6f)))
        val result = TranslationCompensator(100f).compensate(input, target)
        assertEquals(ShapePoint(1.5f, 1.5f), result.strokes.single().last())
        assertEquals(100f, ShapeMatcher().compare(sample(input), ReferenceDrawing(target.map { s -> s.map { ReferencePoint(it.x, it.y) } })).matchPercent, .001f)
    }

    @Test fun rejectsInvalidConfiguration() {
        for (limit in listOf(-1f, 101f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { ShapeCheckConfig(maxTranslationPercent = limit) }
        }
    }
}
