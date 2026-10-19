package ru.mobui.kanji.checker.shape

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.*
import ru.mobui.kanji.domain.practice.*

class CompensationBudgetTest {
    private val reference = ReferenceDrawing(listOf(listOf(ReferencePoint(.2f, .2f), ReferencePoint(.6f, .4f))))
    private fun input(factor: Float) = WritingSample(listOf(InkStroke(reference.strokes.single().mapIndexed { i, p ->
        InkPoint(.21f + (p.x - .2f) * factor, .21f + (p.y - .2f) * factor, i.toLong())
    }, InkTool.Finger)))
    private val config = ShapeCheckConfig(requiredMatchPercent = 99.9f, distanceTolerance = .002f, maxTranslationPercent = 10f, maxSizeCompensationPercent = 20f, maxCompensationBudgetPercent = 10f, pipelineType = CompensationPipelineType.Cascade)

    @Test fun aggregateRejectsEvenWhenEachStageAndShapePass() = runBlocking {
        val checker = ShapeKanjiChecker(KanjiReferenceSource { reference }, Dispatchers.Unconfined, config, diagnosticsEnabled = true)
        val report = checker.checkWithDetails(CheckRequest("t", "c", KanjiId("any"), 0, input(.94f)))
        assertEquals(CheckVerdict.Incorrect, report.verdict)
        assertFalse(report.compensation.translation.hasError)
        assertFalse(report.compensation.size.hasError)
        assertFalse(report.compensation.stretch.hasError)
        assertFalse(report.compensation.rotation.hasError)
        assertTrue(report.compensation.budget.hasError)
        assertTrue(report.compensation.budget.totalPercent > 10f)
        assertEquals(listOf(CheckFailure.Budget(report.compensation.budget)), report.failures)
        assertNotNull(report.compensatedDrawing)
        assertEquals("100.0", report.metrics["coverage.final.percent"])
        assertEquals(CheckVerdict.Correct, checker.check(CheckRequest("t", "c", KanjiId("any"), 0, input(.95f))))
    }

    @Test fun thresholdIsInclusiveAndCorrectionsDoNotCancel() {
        val exact = CompensationBudget(translationPercent = 4f, sizePercent = 6f, limitPercent = 10f)
        assertFalse(exact.hasError)
        assertTrue(exact.copy(stretchPercent = .001f).hasError)
        assertFalse(exact.copy(stretchPercent = 20f, limitPercent = null).hasError)
        val cost = CompensationCost(listOf(listOf(ShapePoint(0f, 0f), ShapePoint(1f, 0f))))
        val before = listOf(listOf(ShapePoint(0f, 0f), ShapePoint(1f, 0f)))
        val moved = listOf(listOf(ShapePoint(.1f, 0f), ShapePoint(1.1f, 0f)))
        assertEquals(20f, cost.movement(before, moved) + cost.movement(moved, before), .0001f)
    }

    @Test fun budgetCannotWeakenAnIndividualLimitAndAllFailuresAreReturned() {
        val tight = config.copy(maxTranslationPercent = .5f, maxSizeCompensationPercent = 1f, maxCompensationBudgetPercent = 1f)
        val result = ShapeMatcher(tight).compare(input(.94f), reference)
        assertFalse(result.isAccepted(tight))
        assertTrue(result.compensation.failures.any { it is CheckFailure.Translation })
        assertTrue(result.compensation.failures.any { it is CheckFailure.Size })
        assertTrue(result.compensation.failures.any { it is CheckFailure.Budget })
        val generous = config.copy(maxTranslationPercent = .5f, maxCompensationBudgetPercent = 100f)
        val generousResult = ShapeMatcher(generous).compare(input(.94f), reference)
        assertFalse(generousResult.compensation.budget.hasError)
        assertFalse(generousResult.isAccepted(generous))
    }
}
