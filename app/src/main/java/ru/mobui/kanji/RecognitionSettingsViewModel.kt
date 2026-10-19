package ru.mobui.kanji

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.mobui.kanji.domain.practice.*

/** Saved across recreation/process restoration; a future preferences repository can own persistence. */
class RecognitionSettingsViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    private val defaults = RecognitionSettings.touchInput()
    private val mutableSettings = MutableStateFlow(restore())
    val settings = mutableSettings.asStateFlow()

    fun update(value: RecognitionSettings) {
        mutableSettings.value = value
        savedState["recognition.parameters"] = floatArrayOf(
            value.requiredMatchPercent, value.distanceTolerance, value.maxTranslationPercent,
            value.maxSizeCompensationPercent ?: Float.NaN, value.maxHorizontalStretchPercent,
            value.maxVerticalStretchPercent, value.maxRotationDegrees, value.maxShearDegrees ?: Float.NaN,
            value.jitterTolerancePercent, value.endpointArtifactTolerance, value.maxCompensationBudgetPercent ?: Float.NaN,
            value.connectionTolerancePercent, value.protrusionTolerancePercent,
            if (value.checkStrokeStructure) 1f else 0f, if (value.checkStrokeOrder) 1f else 0f, if (value.checkStrokeDirection) 1f else 0f, value.lengthDifferenceTolerancePercent,
        )
        savedState["recognition.pipeline"] = value.pipelineType.name
        savedState["recognition.stages"] = value.enabledCompensations.map { it.ordinal }.toIntArray()
    }

    fun reset() = update(defaults)

    private fun restore(): RecognitionSettings {
        val values = savedState.get<FloatArray>("recognition.parameters") ?: return defaults
        if (values.size != 17) return defaults
        return runCatching {
            defaults.copy(
                pipelineType = savedState.get<String>("recognition.pipeline")?.let { name ->
                    CompensationPipelineType.entries.firstOrNull { it.name == name }
                } ?: defaults.pipelineType,
                requiredMatchPercent = values[0], distanceTolerance = values[1], maxTranslationPercent = values[2],
                maxSizeCompensationPercent = values[3].takeUnless { it.isNaN() },
                maxHorizontalStretchPercent = values[4], maxVerticalStretchPercent = values[5],
                maxRotationDegrees = values[6], maxShearDegrees = values[7].takeUnless { it.isNaN() },
                jitterTolerancePercent = values[8], endpointArtifactTolerance = values[9],
                maxCompensationBudgetPercent = values[10].takeUnless { it.isNaN() },
                connectionTolerancePercent = values[11], protrusionTolerancePercent = values[12],
                checkStrokeStructure = values[13] == 1f, checkStrokeOrder = values[14] == 1f, checkStrokeDirection = values[15] == 1f, lengthDifferenceTolerancePercent = values[16],
                enabledCompensations = savedState.get<IntArray>("recognition.stages")?.map { CompensationStage.entries[it] }?.toSet()
                    ?: defaults.enabledCompensations,
            )
        }.getOrDefault(defaults)
    }
}
