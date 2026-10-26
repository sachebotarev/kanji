package ru.mobui.kanji.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import ru.mobui.kanji.domain.practice.RecognitionSettings
import ru.mobui.kanji.domain.practice.RecognitionSettingsRepository

// One DataStore per file across Activity recreation. Access remains inside the data adapter.
private val Context.recognitionDataStore by preferencesDataStore(name = "recognition_settings")

class DataStoreRecognitionSettingsRepository internal constructor(private val store: DataStore<Preferences>) : RecognitionSettingsRepository {
    constructor(context: Context) : this(context.applicationContext.recognitionDataStore)

    override suspend fun load(): RecognitionSettings? {
        val values = store.data.first()
        if (values[Version] == null) return null
        val defaults = RecognitionSettings.touchInput()
        return defaults.copy(
            requiredMatchPercent = values[RequiredMatchPercent] ?: defaults.requiredMatchPercent,
            distanceTolerance = values[DistanceTolerance] ?: defaults.distanceTolerance,
            maxTranslationPercent = values[MaxTranslationPercent] ?: defaults.maxTranslationPercent,
            maxSizeCompensationPercent = values[MaxSizeCompensationPercent]?.takeUnless { it.isNaN() },
            maxHorizontalStretchPercent = values[MaxHorizontalStretchPercent] ?: defaults.maxHorizontalStretchPercent,
            maxVerticalStretchPercent = values[MaxVerticalStretchPercent] ?: defaults.maxVerticalStretchPercent,
            maxRotationDegrees = values[MaxRotationDegrees] ?: defaults.maxRotationDegrees,
            maxShearDegrees = values[MaxShearDegrees]?.takeUnless { it.isNaN() },
            jitterTolerancePercent = values[JitterTolerancePercent] ?: defaults.jitterTolerancePercent,
            maxCompensationBudgetPercent = values[MaxCompensationBudgetPercent]?.takeUnless { it.isNaN() },
            connectionTolerancePercent = values[ConnectionTolerancePercent] ?: defaults.connectionTolerancePercent,
            protrusionTolerancePercent = values[ProtrusionTolerancePercent] ?: defaults.protrusionTolerancePercent,
            lengthDifferenceTolerancePercent = values[LengthDifferenceTolerancePercent] ?: defaults.lengthDifferenceTolerancePercent,
            checkStrokeStructure = values[CheckStrokeStructure] ?: defaults.checkStrokeStructure,
            checkStrokeOrder = values[CheckStrokeOrder] ?: defaults.checkStrokeOrder,
            checkStrokeDirection = values[CheckStrokeDirection] ?: defaults.checkStrokeDirection,
            readabilityEnabled = values[ReadabilityEnabled] ?: defaults.readabilityEnabled,
            shapeEnabled = values[ShapeEnabled] ?: defaults.shapeEnabled,
            strokeWritingEnabled = values[StrokeWritingEnabled] ?: defaults.strokeWritingEnabled,
        ).roundedParameters()
    }

    override suspend fun save(settings: RecognitionSettings) {
        val value = settings.roundedParameters()
        store.edit { values ->
            values[Version] = 1
            values[RequiredMatchPercent] = value.requiredMatchPercent
            values[DistanceTolerance] = value.distanceTolerance
            values[MaxTranslationPercent] = value.maxTranslationPercent
            values[MaxSizeCompensationPercent] = value.maxSizeCompensationPercent ?: Float.NaN
            values[MaxHorizontalStretchPercent] = value.maxHorizontalStretchPercent
            values[MaxVerticalStretchPercent] = value.maxVerticalStretchPercent
            values[MaxRotationDegrees] = value.maxRotationDegrees
            values[MaxShearDegrees] = value.maxShearDegrees ?: Float.NaN
            values[JitterTolerancePercent] = value.jitterTolerancePercent
            values[MaxCompensationBudgetPercent] = value.maxCompensationBudgetPercent ?: Float.NaN
            values[ConnectionTolerancePercent] = value.connectionTolerancePercent
            values[ProtrusionTolerancePercent] = value.protrusionTolerancePercent
            values[LengthDifferenceTolerancePercent] = value.lengthDifferenceTolerancePercent
            values[CheckStrokeStructure] = value.checkStrokeStructure
            values[CheckStrokeOrder] = value.checkStrokeOrder
            values[CheckStrokeDirection] = value.checkStrokeDirection
            values[ReadabilityEnabled] = value.readabilityEnabled
            values[ShapeEnabled] = value.shapeEnabled
            values[StrokeWritingEnabled] = value.strokeWritingEnabled
        }
    }

    private companion object {
        val Version = intPreferencesKey("schema_version")
        val RequiredMatchPercent = floatPreferencesKey("requiredMatchPercent")
        val DistanceTolerance = floatPreferencesKey("distanceTolerance")
        val MaxTranslationPercent = floatPreferencesKey("maxTranslationPercent")
        val MaxSizeCompensationPercent = floatPreferencesKey("maxSizeCompensationPercent")
        val MaxHorizontalStretchPercent = floatPreferencesKey("maxHorizontalStretchPercent")
        val MaxVerticalStretchPercent = floatPreferencesKey("maxVerticalStretchPercent")
        val MaxRotationDegrees = floatPreferencesKey("maxRotationDegrees")
        val MaxShearDegrees = floatPreferencesKey("maxShearDegrees")
        val JitterTolerancePercent = floatPreferencesKey("jitterTolerancePercent")
        val MaxCompensationBudgetPercent = floatPreferencesKey("maxCompensationBudgetPercent")
        val ConnectionTolerancePercent = floatPreferencesKey("connectionTolerancePercent")
        val ProtrusionTolerancePercent = floatPreferencesKey("protrusionTolerancePercent")
        val LengthDifferenceTolerancePercent = floatPreferencesKey("lengthDifferenceTolerancePercent")
        val CheckStrokeStructure = booleanPreferencesKey("checkStrokeStructure")
        val CheckStrokeOrder = booleanPreferencesKey("checkStrokeOrder")
        val CheckStrokeDirection = booleanPreferencesKey("checkStrokeDirection")
        val ReadabilityEnabled = booleanPreferencesKey("readabilityEnabled")
        val ShapeEnabled = booleanPreferencesKey("shapeEnabled")
        val StrokeWritingEnabled = booleanPreferencesKey("strokeWritingEnabled")
    }
}
