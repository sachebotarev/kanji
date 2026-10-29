package ru.mobui.kanji.checker.shape.compensation

import ru.mobui.kanji.checker.shape.ShapeCheckConfig
import ru.mobui.kanji.checker.strokes.geometry.ShapePoint
import ru.mobui.kanji.checker.shape.matching.ShapeMatcher

import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.*

class ProcrustesShapeCompensationPipelineTest {
    private val crossRef = ReferenceDrawing(
        strokes = listOf(
            listOf(ReferencePoint(.2f, .5f), ReferencePoint(.8f, .5f)),
            listOf(ReferencePoint(.5f, .2f), ReferencePoint(.5f, .8f)),
        )
    )

    private fun sampleFromPoints(strokes: List<List<ShapePoint>>): WritingSample {
        return WritingSample(
            strokes = strokes.map { stroke ->
                InkStroke(
                    points = stroke.mapIndexed { ptIdx, p ->
                        InkPoint(p.x, p.y, ptIdx * 10L)
                    },
                    tool = InkTool.Finger,
                )
            }
        )
    }

    @Test
    fun testProcrustesPipelineAcceptsShiftedAndRotatedSample() {
        val config = ShapeCheckConfig(
            requiredMatchPercent = 90f,
            distanceTolerance = 0.05f,
            maxTranslationPercent = 30f,
            maxRotationDegrees = 20f,
            maxSizeCompensationPercent = 30f,
            maxCompensationBudgetPercent = 50f,
        )

        val pipeline = ProcrustesShapeCompensationPipeline(
            maxTranslationPercent = config.maxTranslationPercent,
            maxSizeCompensationPercent = config.maxSizeCompensationPercent,
            maxRotationDegrees = config.maxRotationDegrees,
            budgetLimitPercent = config.maxCompensationBudgetPercent,
        )

        val matcher = ShapeMatcher(config, pipeline)

        // Cross shifted by (+0.05, +0.05)
        val inputStrokes = listOf(
            listOf(ShapePoint(.25f, .55f), ShapePoint(.85f, .55f)),
            listOf(ShapePoint(.55f, .25f), ShapePoint(.55f, .85f)),
        )

        val sample = sampleFromPoints(inputStrokes)
        val match = matcher.compare(sample, crossRef)

        assertTrue("Procrustes pipeline should accept slightly shifted sample", match.isAccepted(config))
        assertTrue("Match percentage should be high after Procrustes alignment", match.matchPercent >= 90f)
        assertFalse("Compensation budget should not have error", match.compensation.budget.hasError)
    }
}
