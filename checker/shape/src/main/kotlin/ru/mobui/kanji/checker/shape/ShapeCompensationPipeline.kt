package ru.mobui.kanji.checker.shape

import ru.mobui.kanji.domain.practice.CheckCompensation
import ru.mobui.kanji.domain.practice.RotationCompensation
import ru.mobui.kanji.domain.practice.CompensationBudget
import ru.mobui.kanji.domain.practice.*

internal data class CompensatedShape(
    val strokes: List<List<ShapePoint>>,
    val compensation: CheckCompensation,
)

internal data class PreparedShapeCandidates(
    val candidates: List<CompensatedShape>,
    val compensation: CheckCompensation,
)

internal interface CompensationPipeline {
    fun prepare(
        strokes: List<List<ShapePoint>>,
        reference: List<List<ShapePoint>>,
        protectedStrokes: Set<Int> = emptySet(),
        assignment: List<Int>? = null,
    ): PreparedShapeCandidates
}

/** Prepares all geometry before scoring. Compensators never call the checker. */
internal class ShapeCompensationPipeline(
    private val translation: TranslationCompensator,
    private val size: SizeCompensator,
    private val rotation: RotationCompensator,
    private val stretch: StretchCompensator,
    private val jitter: JitterCompensator,
    private val budgetLimitPercent: Float?,
    private val shear: ShearCompensator,
    private val enabled: Set<CompensationStage>,
) : CompensationPipeline {
    override fun prepare(
        strokes: List<List<ShapePoint>>,
        reference: List<List<ShapePoint>>,
        protectedStrokes: Set<Int>,
        assignment: List<Int>?,
    ): PreparedShapeCandidates {
        val cost = CompensationCost(reference)
        val translated = if (CompensationStage.Translation in enabled) translation.compensate(strokes, reference) else TranslatedShape(strokes, TranslationCompensation())
        val sheared = if (CompensationStage.Shear in enabled) shear.compensate(translated.strokes, reference) else ShearedShape(translated.strokes, ShearCompensation())
        val resized = if (CompensationStage.Size in enabled) size.compensate(sheared.strokes, reference) else ResizedShape(sheared.strokes, SizeCompensation())
        val stretched = if (CompensationStage.Stretch in enabled) stretch.compensate(resized.strokes, reference) else StretchedShape(resized.strokes, StretchCompensation())
        val compensation = CheckCompensation(
            translation = translated.result, shear = sheared.result, size = resized.result, stretch = stretched.result,
            rotation = RotationCompensation(limitDegrees = rotation.maxDegrees),
            budget = CompensationBudget(
                translationPercent = cost.movement(strokes, translated.strokes),
                shearPercent = cost.movement(translated.strokes, sheared.strokes),
                sizePercent = cost.movement(sheared.strokes, resized.strokes),
                stretchPercent = cost.movement(resized.strokes, stretched.strokes),
                limitPercent = budgetLimitPercent,
            ),
        )
        // Each prefix is a valid chain with identity transforms for its remaining stages.
        // Do not force later adjustments to undo an already adequate translation/size correction.
        val translationOnly = CheckCompensation(translation = translated.result,
            budget = CompensationBudget(translationPercent = compensation.budget.translationPercent, limitPercent = budgetLimitPercent))
        val throughShear = translationOnly.copy(shear = sheared.result,
            budget = translationOnly.budget.copy(shearPercent = compensation.budget.shearPercent))
        val throughSize = throughShear.copy(size = resized.result,
            budget = throughShear.budget.copy(sizePercent = compensation.budget.sizePercent))
        val original = mutableListOf(
            CompensatedShape(translated.strokes, translationOnly),
            CompensatedShape(sheared.strokes, throughShear),
            CompensatedShape(resized.strokes, throughSize),
        )
        val rotations = if (CompensationStage.Rotation in enabled) rotation.candidates(stretched.strokes)
            else listOf(RotatedShape(stretched.strokes, RotationCompensation()))
        for (rotated in rotations) {
            val smoothed = if (CompensationStage.Jitter in enabled) jitter.compensate(rotated.strokes, protectedStrokes) else SmoothedShape(rotated.strokes, JitterCompensation())
            val result = compensation.copy(
                rotation = rotated.result, jitter = smoothed.result,
                budget = compensation.budget.copy(
                    rotationPercent = cost.movement(stretched.strokes, rotated.strokes),
                    jitterPercent = if (smoothed.result.isApplied) cost.removal(rotated.strokes, smoothed.strokes) else 0f,
                ),
            )
            original.add(CompensatedShape(smoothed.strokes, result))

        }
        return PreparedShapeCandidates(original, compensation)
    }
}
