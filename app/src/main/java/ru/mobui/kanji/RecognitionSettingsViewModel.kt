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

    fun update(settings: RecognitionSettings) {
        val value = settings.roundedParameters()
        mutableSettings.value = value
        savedState["recognition.parameters"] = floatArrayOf(
            value.requiredMatchPercent, value.distanceTolerance, value.maxTranslationPercent,
            value.maxSizeCompensationPercent ?: Float.NaN, value.maxHorizontalStretchPercent,
            value.maxVerticalStretchPercent, value.maxRotationDegrees, value.maxShearDegrees ?: Float.NaN,
            value.jitterTolerancePercent, value.maxCompensationBudgetPercent ?: Float.NaN,
            value.connectionTolerancePercent, value.protrusionTolerancePercent,
            if (value.checkStrokeStructure) 1f else 0f, if (value.checkStrokeOrder) 1f else 0f, if (value.checkStrokeDirection) 1f else 0f, value.lengthDifferenceTolerancePercent,
        )
        savedState.remove<Array<String>>("recognition.stageNames")
        savedState.remove<IntArray>("recognition.stages")
        savedState.remove<String>("recognition.pipeline")
    }

    fun reset() = update(defaults)

    private fun restore(): RecognitionSettings {
        val stored = savedState.get<FloatArray>("recognition.parameters") ?: return defaults
        // The previous 17-value layout had a removed parameter in slot 9.
        val values = when (stored.size) {
            16 -> stored
            17 -> stored.filterIndexed { index, _ -> index != 9 }.toFloatArray()
            else -> return defaults
        }
        return runCatching {
            defaults.copy(
                requiredMatchPercent = values[0], distanceTolerance = values[1], maxTranslationPercent = values[2],
                maxSizeCompensationPercent = values[3].takeUnless { it.isNaN() },
                maxHorizontalStretchPercent = values[4], maxVerticalStretchPercent = values[5],
                maxRotationDegrees = values[6], maxShearDegrees = values[7].takeUnless { it.isNaN() },
                jitterTolerancePercent = values[8],
                maxCompensationBudgetPercent = values[9].takeUnless { it.isNaN() },
                connectionTolerancePercent = values[10], protrusionTolerancePercent = values[11],
                checkStrokeStructure = values[12] == 1f, checkStrokeOrder = values[13] == 1f, checkStrokeDirection = values[14] == 1f, lengthDifferenceTolerancePercent = values[15],
            )
        }.getOrDefault(defaults).let { restored ->
            val oldEnabled = savedState.get<Array<String>>("recognition.stageNames")?.mapNotNull { name ->
                CompensationStage.entries.firstOrNull { it.name == name }
            }?.toSet() ?: savedState.get<IntArray>("recognition.stages")?.asIterable()?.mapNotNull {
                CompensationStage.entries.getOrNull(it)
            }?.toSet()
            // Migrate disabled legacy toggles to zero, then stop persisting separate toggles.
            if (oldEnabled == null) restored else restored.copy(
                maxTranslationPercent = restored.maxTranslationPercent.takeIf { CompensationStage.Translation in oldEnabled } ?: 0f,
                maxSizeCompensationPercent = if (CompensationStage.Size in oldEnabled) restored.maxSizeCompensationPercent else 0f,
                maxRotationDegrees = restored.maxRotationDegrees.takeIf { CompensationStage.Rotation in oldEnabled } ?: 0f,
                maxShearDegrees = if (CompensationStage.Shear in oldEnabled) restored.maxShearDegrees else 0f,
                maxHorizontalStretchPercent = restored.maxHorizontalStretchPercent.takeIf { CompensationStage.Stretch in oldEnabled } ?: 0f,
                maxVerticalStretchPercent = restored.maxVerticalStretchPercent.takeIf { CompensationStage.Stretch in oldEnabled } ?: 0f,
                jitterTolerancePercent = restored.jitterTolerancePercent.takeIf { CompensationStage.Jitter in oldEnabled } ?: 0f,
            )
        }.roundedParameters()
    }
}
