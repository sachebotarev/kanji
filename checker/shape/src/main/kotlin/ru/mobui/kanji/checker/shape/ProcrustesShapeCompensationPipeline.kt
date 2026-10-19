package ru.mobui.kanji.checker.shape

import ru.mobui.kanji.domain.practice.*

/** Joint similarity candidates, optional shear/stretch, then protected stroke cleanup. */
internal class ProcrustesShapeCompensationPipeline(
    private val procrustes: ProcrustesCompensator = ProcrustesCompensator(),
    private val jitter: JitterCompensator = JitterCompensator(0f),
    private val budgetLimitPercent: Float? = null,
    private val maxTranslationPercent: Float = 20f,
    private val maxSizeCompensationPercent: Float? = 30f,
    private val maxRotationDegrees: Float = 15f,
    private val enabled: Set<CompensationStage> = CompensationStage.entries.toSet(),
    private val shear: ShearCompensator = ShearCompensator(null),
    private val stretch: StretchCompensator = StretchCompensator(15f, 15f, preserveCenter = true),
) : CompensationPipeline {
    override fun prepare(
        strokes: List<List<ShapePoint>>,
        reference: List<List<ShapePoint>>,
        protectedStrokes: Set<Int>,
        assignment: List<Int>?,
    ): PreparedShapeCandidates {
        val cost = CompensationCost(reference)
        val original = mutableListOf<CompensatedShape>()
        val identity = CheckCompensation(budget = CompensationBudget(limitPercent = budgetLimitPercent))

        fun add(candidate: CompensatedShape) {
            original.add(candidate)
            val smoothed = if (CompensationStage.Jitter in enabled)
                jitter.compensate(candidate.strokes, protectedStrokes)
            else SmoothedShape(candidate.strokes, JitterCompensation())
            val result = candidate.compensation.copy(jitter = smoothed.result,
                budget = candidate.compensation.budget.copy(
                    jitterPercent = cost.removal(candidate.strokes, smoothed.strokes)))
            if (smoothed.result.isApplied) original.add(CompensatedShape(smoothed.strokes, result))

        }

        // Every subset is fitted independently: omitting a component is an actual constraint.
        // The matcher chooses the least expensive passing candidate, including the raw drawing.
        val stages = listOf(CompensationStage.Translation, CompensationStage.Size, CompensationStage.Rotation)
        for (mask in 0..7) {
            if (stages.indices.any { mask and (1 shl it) != 0 && stages[it] !in enabled }) continue
            val fitted = procrustes.compensate(strokes, reference, assignment,
                translate = mask and 1 != 0, resize = mask and 2 != 0, rotate = mask and 4 != 0)
            if (!fitted.rootMeanSquaredError.isFinite()) continue
            val translated = if (fitted.translationX == 0f && fitted.translationY == 0f) strokes else strokes.map { s -> s.map { ShapePoint(it.x + fitted.translationX, it.y + fitted.translationY) } }
            val pivotX = fitted.centroid.x + fitted.translationX
            val pivotY = fitted.centroid.y + fitted.translationY
            val resized = if (fitted.scaleFactor == 1f) translated else translated.map { s -> s.map {
                ShapePoint(pivotX + (it.x - pivotX) * fitted.scaleFactor,
                    pivotY + (it.y - pivotY) * fitted.scaleFactor)
            } }
            val compensation = identity.copy(
                translation = TranslationCompensation(fitted.translationX * 100, fitted.translationY * 100, maxTranslationPercent),
                size = SizeCompensation(fitted.scaleFactor, maxSizeCompensationPercent),
                rotation = RotationCompensation(fitted.rotationDegrees, RotationPivot.Centroid, maxRotationDegrees),
                budget = identity.budget.copy(
                    translationPercent = cost.movement(strokes, translated),
                    sizePercent = cost.movement(translated, resized),
                    rotationPercent = if (fitted.rotationDegrees == 0f) 0f else cost.movement(resized, fitted.strokes)),
            )
            val joint = CompensatedShape(fitted.strokes, compensation)
            add(joint)
            val sheared = if (CompensationStage.Shear in enabled) shear.compensate(joint.strokes, reference)
                else ShearedShape(joint.strokes, ShearCompensation())
            val throughShear = CompensatedShape(sheared.strokes, compensation.copy(shear = sheared.result,
                budget = compensation.budget.copy(shearPercent = cost.movement(joint.strokes, sheared.strokes))))
            if (sheared.result.isApplied) add(throughShear)
            if (CompensationStage.Stretch in enabled) {
                // Stretch is optional independently of shear, including when shear exceeds its limit.
                for (candidate in if (sheared.result.isApplied) listOf(joint, throughShear) else listOf(joint)) {
                    val stretched = stretch.compensate(candidate.strokes, reference)
                    if (stretched.result.isApplied) add(CompensatedShape(stretched.strokes,
                        candidate.compensation.copy(stretch = stretched.result,
                            budget = candidate.compensation.budget.copy(
                                stretchPercent = cost.movement(candidate.strokes, stretched.strokes)))))
                }
            }
        }
        return PreparedShapeCandidates(original, identity)
    }
}
