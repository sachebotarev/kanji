package ru.mobui.kanji

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.domain.practice.CompensationStage

class RecognitionSettingsRestorationTest {
    private fun model(handle: SavedStateHandle): RecognitionSettingsViewModel {
        val repository = object : ru.mobui.kanji.domain.practice.RecognitionSettingsRepository {
            override suspend fun load(): ru.mobui.kanji.domain.practice.RecognitionSettings? = null
            override suspend fun save(settings: ru.mobui.kanji.domain.practice.RecognitionSettings) = Unit
        }
        return RecognitionSettingsViewModel(handle, repository,
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Unconfined))
    }

    @Test fun restoresEveryParameterAndIndependentFlags() {
        val handle = SavedStateHandle()
        val first = model(handle)
        val changed = first.settings.value.copy(
            requiredMatchPercent = 96f, distanceTolerance = .03f, maxTranslationPercent = 8f,
            maxSizeCompensationPercent = 0f, maxHorizontalStretchPercent = 12f, maxVerticalStretchPercent = 14f,
            maxRotationDegrees = 6f, maxShearDegrees = 4f, jitterTolerancePercent = .3f,
            maxCompensationBudgetPercent = 13f,
            connectionTolerancePercent = 4f, protrusionTolerancePercent = 2f, lengthDifferenceTolerancePercent = 7f,
            checkStrokeStructure = false, checkStrokeOrder = true, checkStrokeDirection = false,
            readabilityEnabled = false, shapeEnabled = true, strokeWritingEnabled = false,
        )
        first.update(changed)
        val restored = model(handle)
        assertEquals(first.settings.value, restored.settings.value)
        restored.reset()
        assertEquals(73f, restored.settings.value.requiredMatchPercent, .0001f)
        assertEquals(27f, restored.settings.value.maxCompensationBudgetPercent!!, .0001f)
        assertFalse(restored.settings.value.checkStrokeOrder)
        assertFalse(restored.settings.value.checkStrokeDirection)
    }
    @Test fun restoresLegacyParametersAndDropsRemovedStageWithoutResettingOtherSettings() {
        val handle = SavedStateHandle(mapOf(
            "recognition.pipeline" to "Cascade",
            "recognition.parameters" to floatArrayOf(
                83f, .04f, 9f, 25f, 12f, 14f, 6f, 4f, .3f, .015f, 23f,
                4f, 2f, 0f, 1f, 0f, 7f,
            ),
            "recognition.stages" to intArrayOf(0, 5, 6),
        ))
        val restored = model(handle)
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
        assertEquals(settings, model(handle).settings.value)
        restored.update(settings.copy(maxRotationDegrees = 4f))
        val reenabled = model(handle).settings.value
        assertEquals(4f, reenabled.maxRotationDegrees, 0f)
        assertTrue(CompensationStage.Rotation in reenabled.enabledCompensations)
        assertFalse(handle.contains("recognition.stages"))
        assertFalse(handle.contains("recognition.pipeline"))
    }

    @Test fun roundsSavedFractionalValuesAndKeepsJitterTenths() {
        val handle = SavedStateHandle(mapOf("recognition.parameters" to floatArrayOf(
            73.03f, .048f, 12.4f, 30.2f, 15.2f, 15.1f, 8.2f, 8.1f,
            .54f, 27.36f, 5.2f, 3.2f, 1f, 0f, 0f, 5.2f,
        )))
        val vm = model(handle)
        assertEquals(73f, vm.settings.value.requiredMatchPercent, 0f)
        assertEquals(.05f, vm.settings.value.distanceTolerance, .000001f)
        assertEquals(.5f, vm.settings.value.jitterTolerancePercent, 0f)
        assertEquals(27f, vm.settings.value.maxCompensationBudgetPercent!!, 0f)
        vm.update(vm.settings.value.copy(requiredMatchPercent = 81.7f, jitterTolerancePercent = .66f))
        assertEquals(82f, vm.settings.value.requiredMatchPercent, 0f)
        assertEquals(.7f, vm.settings.value.jitterTolerancePercent, .000001f)
        assertEquals(vm.settings.value, model(handle).settings.value)
    }

}
