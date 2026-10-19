package ru.mobui.kanji

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.domain.practice.CompensationStage

class RecognitionSettingsRestorationTest {
    @Test fun restoresEveryParameterAndIndependentFlags() {
        val handle = SavedStateHandle()
        val first = RecognitionSettingsViewModel(handle)
        val changed = first.settings.value.copy(
            pipelineType = ru.mobui.kanji.domain.practice.CompensationPipelineType.Cascade,
            requiredMatchPercent = 96f, distanceTolerance = .028f, maxTranslationPercent = 8f,
            maxSizeCompensationPercent = null, maxHorizontalStretchPercent = 12f, maxVerticalStretchPercent = 14f,
            maxRotationDegrees = 6f, maxShearDegrees = 4f, jitterTolerancePercent = .3f,
            maxCompensationBudgetPercent = 13f,
            connectionTolerancePercent = 4f, protrusionTolerancePercent = 2f, lengthDifferenceTolerancePercent = 7f,
            checkStrokeStructure = false, checkStrokeOrder = true, checkStrokeDirection = false,
            enabledCompensations = setOf(CompensationStage.Shear, CompensationStage.Size),
        )
        first.update(changed)
        val restored = RecognitionSettingsViewModel(handle)
        assertEquals(changed, restored.settings.value)
        restored.reset()
        assertEquals(72f, restored.settings.value.requiredMatchPercent, .0001f)
        assertEquals(28f, restored.settings.value.maxCompensationBudgetPercent!!, .0001f)
        assertFalse(restored.settings.value.checkStrokeOrder)
        assertFalse(restored.settings.value.checkStrokeDirection)
    }
    @Test fun restoresLegacyParametersAndDropsRemovedStageWithoutResettingOtherSettings() {
        val handle = SavedStateHandle(mapOf(
            "recognition.parameters" to floatArrayOf(
                83f, .04f, 9f, 25f, 12f, 14f, 6f, 4f, .3f, .015f, 23f,
                4f, 2f, 0f, 1f, 0f, 7f,
            ),
            "recognition.stages" to intArrayOf(0, 5, 6),
        ))
        val restored = RecognitionSettingsViewModel(handle)
        val settings = restored.settings.value
        assertEquals(83f, settings.requiredMatchPercent, 0f)
        assertEquals(23f, settings.maxCompensationBudgetPercent!!, 0f)
        assertEquals(4f, settings.connectionTolerancePercent, 0f)
        assertEquals(2f, settings.protrusionTolerancePercent, 0f)
        assertEquals(7f, settings.lengthDifferenceTolerancePercent, 0f)
        assertFalse(settings.checkStrokeStructure)
        assertTrue(settings.checkStrokeOrder)
        assertFalse(settings.checkStrokeDirection)
        assertEquals(setOf(CompensationStage.Translation, CompensationStage.Jitter), settings.enabledCompensations)
        restored.update(settings)
        assertEquals(settings, RecognitionSettingsViewModel(handle).settings.value)
    }

}
