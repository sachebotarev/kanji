package ru.mobui.kanji.domain.practice

// >=80% of count-compatible Tomoe kanji entries that pass hooks; see doc/calibration/tomoe-full-2026-10-19.md.
private const val TOUCH_MATCH_PERCENT = 73.03f
private const val TOUCH_BUDGET_PERCENT = 27.36f

enum class CompensationPipelineType { Cascade, Procrustes }

data class RecognitionSettings(
    // Generic callers have unrestricted translation/size by default; the calibrated app uses touchInput().
    val requiredMatchPercent: Float = 72f,
    /** Allowed distance as a fraction of the normalized glyph's longest side. */
    val distanceTolerance: Float = 0.048f,
    val pipelineType: CompensationPipelineType = CompensationPipelineType.Procrustes,
    /** Maximum absolute independent X/Y correction, in percent, after uniform scaling. */
    val maxHorizontalStretchPercent: Float = 15f,
    val maxVerticalStretchPercent: Float = 15f,
    val maxRotationDegrees: Float = 8f,
    /** Max shift on either axis, in percent of the field. 100 preserves unrestricted translation. */
    val maxTranslationPercent: Float = 100f,
    /** Absolute uniform size correction, in percent of the drawn size. Null means unrestricted. */
    val maxSizeCompensationPercent: Float? = null,
    /** Max deviation when simplifying hand jitter, in percent of glyph size; zero disables. */
    val jitterTolerancePercent: Float = 0f,
    /** Total geometric correction budget. Null disables only the aggregate gate. */
    val maxCompensationBudgetPercent: Float? = null,
    /** Horizontal slant correction; null disables estimation, zero permits only zero shear. */
    val maxShearDegrees: Float? = null,
    val checkStrokeStructure: Boolean = true,
    val connectionTolerancePercent: Float = 5f,
    val protrusionTolerancePercent: Float = 3f,
    val lengthDifferenceTolerancePercent: Float = 5f,
    val checkStrokeOrder: Boolean = false,
    val checkStrokeDirection: Boolean = false,
    val enabledCompensations: Set<CompensationStage> = CompensationStage.entries.toSet(),
) {
    companion object {
        /** Shared starting profile for finger input; values remain adjustable in settings. */
        fun touchInput() = RecognitionSettings(
            requiredMatchPercent = TOUCH_MATCH_PERCENT, distanceTolerance = .048f,
            maxTranslationPercent = 12f, maxSizeCompensationPercent = 30f,
            maxCompensationBudgetPercent = TOUCH_BUDGET_PERCENT, jitterTolerancePercent = .5f,
            maxShearDegrees = 8f, maxRotationDegrees = 8f,
        )
    }

    init {
        require(lengthDifferenceTolerancePercent.isFinite() && lengthDifferenceTolerancePercent in 0f..30f)
        require(connectionTolerancePercent.isFinite() && connectionTolerancePercent in 0f..10f)
        require(protrusionTolerancePercent.isFinite() && protrusionTolerancePercent in 0f..10f)
        require(maxShearDegrees == null || (maxShearDegrees.isFinite() && maxShearDegrees in 0f..20f))
        require(maxCompensationBudgetPercent == null || (maxCompensationBudgetPercent.isFinite() && maxCompensationBudgetPercent >= 0f))
        require(jitterTolerancePercent.isFinite() && jitterTolerancePercent in 0f..5f)
        require(maxSizeCompensationPercent == null || (maxSizeCompensationPercent.isFinite() && maxSizeCompensationPercent >= 0f))
        require(maxTranslationPercent.isFinite() && maxTranslationPercent in 0f..100f)
        require(requiredMatchPercent.isFinite() && requiredMatchPercent > 0f && requiredMatchPercent <= 100f)
        require(distanceTolerance.isFinite() && distanceTolerance in 0.001f..0.15f)
        require(maxHorizontalStretchPercent.isFinite() && maxHorizontalStretchPercent >= 0f)
        require(maxVerticalStretchPercent.isFinite() && maxVerticalStretchPercent >= 0f)
        require(maxRotationDegrees.isFinite() && maxRotationDegrees in 0f..20f)
    }
}


enum class CompensationStage { Translation, Shear, Size, Stretch, Rotation, Jitter }
