package ru.mobui.kanji.checker.shape.matching

import ru.mobui.kanji.checker.shape.ShapeCheckConfig

import ru.mobui.kanji.domain.practice.CheckCompensation
import ru.mobui.kanji.domain.handwriting.ComparisonDrawing
import kotlin.math.min

/** Percent is geometric coverage, not a probability of correct recognition. */
data class ShapeMatch(
    val inputCoveragePercent: Float,
    val referenceCoveragePercent: Float,
    val strokeCountMatches: Boolean,
    val compensation: CheckCompensation = CheckCompensation(),
    val baselineMatchPercent: Float = 0f,
    val evaluatedCandidates: Int = 0,
    val strokeChecks: ru.mobui.kanji.domain.practice.StrokeCheckResult = ru.mobui.kanji.domain.practice.StrokeCheckResult(),
    val globalInputCoveragePercent: Float = inputCoveragePercent,
    val globalReferenceCoveragePercent: Float = referenceCoveragePercent,
    val strokeInputCoveragePercent: Float = inputCoveragePercent,
    val strokeReferenceCoveragePercent: Float = referenceCoveragePercent,
    val compensatedDrawing: ComparisonDrawing? = null,
) {
    val globalMatchPercent: Float get() = min(globalInputCoveragePercent, globalReferenceCoveragePercent)
    val strokeMatchPercent: Float get() = min(strokeInputCoveragePercent, strokeReferenceCoveragePercent)
    val matchPercent: Float get() = min(inputCoveragePercent, referenceCoveragePercent)
    fun isAccepted(config: ShapeCheckConfig): Boolean =
        strokeChecks.failures.isEmpty() && compensation.failures.isEmpty() && strokeCountMatches && matchPercent >= config.requiredMatchPercent
}

