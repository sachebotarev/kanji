package ru.mobui.kanji.checker.shape

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.*
import ru.mobui.kanji.domain.practice.*
import kotlin.math.*

class ProcrustesRegressionTest {
    private val reference = listOf(
        listOf(ShapePoint(.2f, .2f), ShapePoint(.8f, .2f), ShapePoint(.8f, .8f)),
        listOf(ShapePoint(.3f, .4f), ShapePoint(.4f, .7f)),
    )
    private val settings = RecognitionSettings.touchInput().copy(requiredMatchPercent = 99f,
        distanceTolerance = .002f, maxCompensationBudgetPercent = null,
        enabledCompensations = setOf(CompensationStage.Translation, CompensationStage.Size, CompensationStage.Rotation))
    private fun drawing(strokes: List<List<ShapePoint>>) = ReferenceDrawing(strokes.map { s -> s.map { ReferencePoint(it.x, it.y) } })
    private fun sample(strokes: List<List<ShapePoint>>) = WritingSample(strokes.map { s ->
        InkStroke(s.mapIndexed { i, p -> InkPoint(p.x, p.y, i.toLong()) }, InkTool.Finger)
    })
    private fun transformed(strokes: List<List<ShapePoint>>, scale: Float = .9f, angle: Float = 5f,
        dx: Float = .03f, dy: Float = -.02f): List<List<ShapePoint>> {
        val rad = angle * PI.toFloat() / 180f
        return strokes.map { s -> s.map {
            val x = (it.x - .5f) * scale; val y = (it.y - .5f) * scale
            ShapePoint(.5f + dx + x * cos(rad) - y * sin(rad), .5f + dy + x * sin(rad) + y * cos(rad))
        } }
    }

    @Test fun eachDisabledJointComponentIsIdentityForEveryCombination() {
        val input = transformed(reference)
        for (mask in 0..7) {
            val result = ProcrustesCompensator().compensate(input, reference, listOf(0, 1),
                translate = mask and 1 != 0, resize = mask and 2 != 0, rotate = mask and 4 != 0)
            if (mask and 1 == 0) {
                assertEquals(0f, result.translationX, 0f)
                assertEquals(0f, result.translationY, 0f)
            }
            if (mask and 2 == 0) assertEquals(1f, result.scaleFactor, 0f)
            if (mask and 4 == 0) assertEquals(0f, result.rotationDegrees, 0f)
        }
        val config = settings.copy(enabledCompensations = setOf(CompensationStage.Translation))
        val match = ShapeMatcher(config).compare(sample(input), drawing(reference))
        assertFalse(match.isAccepted(config))
        assertFalse(match.compensation.size.isApplied)
        assertFalse(match.compensation.rotation.isApplied)
    }

    @Test fun mixedStrokeDirectionsAndPermutationDoNotChangeJointFit() {
        val input = transformed(reference)
        val normal = ShapeMatcher(settings).compare(sample(input), drawing(reference))
        val reversed = ShapeMatcher(settings).compare(sample(listOf(input[1].reversed(), input[0])), drawing(reference))
        assertTrue(normal.isAccepted(settings))
        assertTrue(reversed.isAccepted(settings))
        assertEquals(normal.matchPercent, reversed.matchPercent, .001f)
        assertEquals(normal.compensation.size.scaleFactor, reversed.compensation.size.scaleFactor, .0001f)
        assertEquals(normal.compensation.rotationDegrees, reversed.compensation.rotationDegrees, .0001f)
    }

    @Test fun reversedShiftedLineNeedsOnlyTranslationAndDirectionRemainsIndependent() {
        val target = listOf(listOf(ShapePoint(.2f, .5f), ShapePoint(.8f, .5f)))
        val ink = sample(target.map { s -> s.reversed().map { ShapePoint(it.x, it.y + .08f) } })
        val match = ShapeMatcher(settings).compare(ink, drawing(target))
        assertTrue(match.isAccepted(settings))
        assertEquals(0f, match.compensation.rotationDegrees, 0f)
        val enforced = settings.copy(checkStrokeDirection = true)
        val wrongDirection = ShapeMatcher(enforced).compare(ink, drawing(target))
        assertFalse(wrongDirection.isAccepted(enforced))
        assertTrue(wrongDirection.strokeChecks.reversedStrokes.isNotEmpty())
    }

    @Test fun pureScaleHasZeroRotationBudgetAndKeepsInputUntouched() {
        val input = transformed(reference, scale = .8f, angle = 0f, dx = 0f, dy = 0f)
        val ink = sample(input)
        val snapshot = ink.strokes.map { it.points.toList() }
        val match = ShapeMatcher(settings).compare(ink, drawing(reference))
        assertTrue(match.isAccepted(settings))
        assertEquals(0f, match.compensation.rotationDegrees, 0f)
        assertEquals(0f, match.compensation.budget.rotationPercent, 0f)
        assertEquals(1.25f, match.compensation.size.scaleFactor, .0001f)
        assertEquals(snapshot, ink.strokes.map { it.points })
    }

    @Test fun rotationBudgetUsesTheSameArcCentroidAsTheFit() {
        val input = transformed(reference)
        val fitted = ProcrustesCompensator().compensate(input, reference, listOf(0, 1))
        val candidates = compensationPipeline(settings).prepare(input, reference, emptySet(), listOf(0, 1))
        val candidate = candidates.candidates.first { abs(it.compensation.rotationDegrees + 5f) < .01f &&
            abs(it.compensation.size.scaleFactor - 1f / .9f) < .001f }
        val pivot = ShapePoint(fitted.centroid.x + fitted.translationX, fitted.centroid.y + fitted.translationY)
        val resized = input.map { s -> s.map {
            ShapePoint(pivot.x + (it.x - fitted.centroid.x) * fitted.scaleFactor,
                pivot.y + (it.y - fitted.centroid.y) * fitted.scaleFactor)
        } }
        assertEquals(CompensationCost(reference).movement(resized, fitted.strokes),
            candidate.compensation.budget.rotationPercent, .0001f)
    }

    @Test fun smoothingSettingsReachFactoryAndProtectRequiredStrokes() {
        val noisy = listOf(listOf(ShapePoint(.2f, .5f), ShapePoint(.5f, .51f), ShapePoint(.8f, .5f)))
        fun candidates(tolerance: Float, protected: Set<Int> = emptySet()) = compensationPipeline(settings.copy(
            jitterTolerancePercent = tolerance, enabledCompensations = setOf(CompensationStage.Jitter)))
            .prepare(noisy, noisy, protected, listOf(0)).candidates
        assertTrue(candidates(0f).all { it.strokes == noisy })
        assertTrue(candidates(2f).any { it.strokes.single().size == 2 })
        assertTrue(candidates(2f, setOf(0)).all { it.strokes == noisy })

    }

    @Test fun bothPipelinesKeepStrokeEndsWhenSmoothing() {
        val ink = listOf(listOf(ShapePoint(.2f, .49f), ShapePoint(.2f, .5f),
            ShapePoint(.8f, .5f), ShapePoint(.8f, .51f)))
        for (type in CompensationPipelineType.entries) {
            val config = settings.copy(pipelineType = type, jitterTolerancePercent = 5f,
                enabledCompensations = setOf(CompensationStage.Jitter))
            val prepared = compensationPipeline(config).prepare(ink, ink, emptySet(), listOf(0))
            assertTrue(prepared.candidates.isNotEmpty())
            for (candidate in prepared.candidates) {
                assertEquals(ink.single().first(), candidate.strokes.single().first())
                assertEquals(ink.single().last(), candidate.strokes.single().last())
            }
        }
    }

    @Test fun publicMatcherAndCheckerUseTheSamePipelineAndLimits() = runBlocking {
        val ink = sample(transformed(reference))
        val direct = ShapeMatcher(settings).compare(ink, drawing(reference))
        val checker = ShapeKanjiChecker(KanjiReferenceSource { drawing(reference) }, Dispatchers.Unconfined,
            settings, diagnosticsEnabled = true)
        val report = checker.checkWithDetails(CheckRequest("task", "cell", KanjiId("any"), 0, ink))
        assertEquals(CheckVerdict.Correct, report.verdict)
        assertEquals(direct.compensation, report.compensation)
        assertEquals("Procrustes", report.metrics["pipelineType"])
        val strict = settings.copy(maxRotationDegrees = 0f, maxSizeCompensationPercent = 0f)
        assertFalse(ShapeMatcher(strict).compare(ink, drawing(reference)).isAccepted(strict))
        val noBudget = settings.copy(maxCompensationBudgetPercent = 0f)
        assertFalse(ShapeMatcher(noBudget).compare(ink, drawing(reference)).isAccepted(noBudget))
    }

    @Test fun excessiveShiftIsReportedEvenWhenShapeAlignsPerfectly() {
        val config = settings.copy(enabledCompensations = setOf(CompensationStage.Translation))
        val ink = sample(transformed(reference, scale = 1f, angle = 0f, dx = 0f, dy = .16f))
        val match = ShapeMatcher(config).compare(ink, drawing(reference))
        assertEquals(100f, match.matchPercent, .001f)
        assertTrue(match.compensation.translation.hasError)
        assertFalse(match.isAccepted(config))
        assertEquals(listOf(CheckFailure.Translation(match.compensation.translation)), match.compensation.failures)
    }

    @Test fun invalidCorrespondenceAndDegenerateInkReturnFiniteIdentity() {
        val compensator = ProcrustesCompensator()
        val input = listOf(listOf(ShapePoint(.5f, .5f)))
        assertEquals(input, compensator.compensate(input, input, listOf(-1)).strokes)
        assertEquals(input, compensator.compensate(input, input, listOf(0)).strokes)
        assertEquals(reference, compensator.compensate(reference, reference, listOf(0, 0)).strokes)
    }
}
