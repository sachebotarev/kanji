package ru.mobui.kanji.checker.shape

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

/** One shared graph for the sole Procrustes implementation. */
internal fun compensationPipeline(cfg: ShapeCheckConfig): CompensationPipeline =
    ProcrustesShapeCompensationPipeline(
        jitter = JitterCompensator(cfg.jitterTolerancePercent),
        shear = ShearCompensator(cfg.maxShearDegrees),
        stretch = StretchCompensator(cfg.maxHorizontalStretchPercent, cfg.maxVerticalStretchPercent),
        maxTranslationPercent = cfg.maxTranslationPercent,
        maxSizeCompensationPercent = cfg.maxSizeCompensationPercent,
        maxRotationDegrees = cfg.maxRotationDegrees,
        budgetLimitPercent = cfg.maxCompensationBudgetPercent,
        enabled = cfg.effectiveCompensations,
    )
