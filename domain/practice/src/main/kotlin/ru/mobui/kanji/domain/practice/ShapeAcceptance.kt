package ru.mobui.kanji.domain.practice

const val SHAPE_SOFT_ACCEPTANCE_FACTOR = 0.8
const val DEFAULT_SHAPE_SOFT_MARGIN_POINTS = 10f

/** Only a small coverage deficit is negotiable; structural and other checks stay strict. */
internal fun CheckReport.withShapeTolerance(marginPoints: Float = DEFAULT_SHAPE_SOFT_MARGIN_POINTS): CheckReport {
    if (verdict != CheckVerdict.Incorrect || failures.isEmpty()) return this
    if (failures.any { it !is CheckFailure.ShapeCoverage }) return this
    val withinMargin = failures.filterIsInstance<CheckFailure.ShapeCoverage>().all {
        val deficit = it.requiredPercent - it.actualPercent
        it.actualPercent.isFinite() && it.requiredPercent.isFinite() &&
            deficit > 0f && deficit < marginPoints
    }
    return if (withinMargin) copy(verdict = CheckVerdict.Correct, acceptedWithPenalty = true) else this
}
