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
            endpointArtifactTolerance = .015f, maxCompensationBudgetPercent = 13f,
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
}
