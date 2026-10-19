package ru.mobui.kanji.domain.practice

import org.junit.Assert.*
import org.junit.Test

class RecognitionSettingsRoundingTest {
    @Test fun roundsDisplayedPercentagesAndDegreesButKeepsJitterTenths() {
        val result = RecognitionSettings(
            requiredMatchPercent = 73.03f, distanceTolerance = .048f,
            maxTranslationPercent = 12.7f, maxSizeCompensationPercent = 30.2f,
            maxHorizontalStretchPercent = 15.2f, maxVerticalStretchPercent = 15.9f,
            maxRotationDegrees = 8.2f, maxShearDegrees = 8.9f,
            jitterTolerancePercent = .54f, maxCompensationBudgetPercent = 27.36f,
            connectionTolerancePercent = 5.2f, protrusionTolerancePercent = 3.7f,
            lengthDifferenceTolerancePercent = 5.2f,
        ).roundedParameters()
        assertEquals(73f, result.requiredMatchPercent, 0f)
        assertEquals(.05f, result.distanceTolerance, .000001f)
        assertEquals(13f, result.maxTranslationPercent, 0f)
        assertEquals(30f, result.maxSizeCompensationPercent!!, 0f)
        assertEquals(15f, result.maxHorizontalStretchPercent, 0f)
        assertEquals(16f, result.maxVerticalStretchPercent, 0f)
        assertEquals(8f, result.maxRotationDegrees, 0f)
        assertEquals(9f, result.maxShearDegrees!!, 0f)
        assertEquals(.5f, result.jitterTolerancePercent, 0f)
        assertEquals(27f, result.maxCompensationBudgetPercent!!, 0f)
        assertEquals(5f, result.connectionTolerancePercent, 0f)
        assertEquals(4f, result.protrusionTolerancePercent, 0f)
        assertEquals(5f, result.lengthDifferenceTolerancePercent, 0f)
        assertEquals(result, result.roundedParameters())
    }

    @Test fun zerosDisableCorrectionsAndMinimumDistanceStaysValid() {
        val result = RecognitionSettings(requiredMatchPercent = .1f, distanceTolerance = .001f,
            maxTranslationPercent = 0f, maxSizeCompensationPercent = 0f, maxCompensationBudgetPercent = null,
            maxHorizontalStretchPercent = 0f, maxVerticalStretchPercent = 0f, maxRotationDegrees = 0f,
            maxShearDegrees = null, jitterTolerancePercent = .66f,
            checkStrokeOrder = true).roundedParameters()
        assertEquals(1f, result.requiredMatchPercent, 0f)
        assertEquals(.01f, result.distanceTolerance, .000001f)
        assertEquals(.7f, result.jitterTolerancePercent, .000001f)
        assertEquals(0f, result.maxSizeCompensationPercent!!, 0f)
        assertNull(result.maxCompensationBudgetPercent)
        assertEquals(0f, result.maxShearDegrees!!, 0f)
        assertEquals(setOf(CompensationStage.Jitter), result.enabledCompensations)
        assertTrue(result.checkStrokeOrder)
        assertEquals(RecognitionSettings.touchInput(), RecognitionSettings.touchInput().roundedParameters())
    }
    @Test fun effectiveStagesFollowNonzeroLimitsAndZeroBudgetRemovesAggregateLimitForUserSettings() {
        val disabled = RecognitionSettings(maxTranslationPercent = 0f, maxSizeCompensationPercent = 0f,
            maxHorizontalStretchPercent = 0f, maxVerticalStretchPercent = 0f,
            maxRotationDegrees = 0f, maxShearDegrees = 0f, jitterTolerancePercent = 0f,
            maxCompensationBudgetPercent = 0f)
        assertTrue(disabled.effectiveCompensations.isEmpty())
        assertNull(disabled.roundedParameters().maxCompensationBudgetPercent)
        assertEquals(30f, RecognitionSettings(maxSizeCompensationPercent = null)
            .roundedParameters().maxSizeCompensationPercent!!, 0f)
        val enabled = disabled.copy(maxRotationDegrees = 4f, maxVerticalStretchPercent = 7f).roundedParameters()
        assertEquals(setOf(CompensationStage.Rotation, CompensationStage.Stretch), enabled.enabledCompensations)
        assertEquals(0f, enabled.maxHorizontalStretchPercent, 0f)
        assertEquals(enabled, enabled.roundedParameters())
    }

}
