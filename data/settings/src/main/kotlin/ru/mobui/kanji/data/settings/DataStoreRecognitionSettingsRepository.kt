package ru.mobui.kanji.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import ru.mobui.kanji.domain.practice.HintMode
import ru.mobui.kanji.domain.practice.RecognitionSettings
import ru.mobui.kanji.domain.practice.RecognitionSettingsRepository

// One DataStore per file across Activity recreation. Access remains inside the data adapter.
private val Context.recognitionDataStore by preferencesDataStore(name = "recognition_settings")

class DataStoreRecognitionSettingsRepository
internal constructor(private val store: DataStore<Preferences>) : RecognitionSettingsRepository {
    constructor(context: Context) : this(context.applicationContext.recognitionDataStore)

    override suspend fun load(): RecognitionSettings? {
        val values = store.data.first()
        if (values[Version] == null) return null
        return values.toRecognitionSettings(RecognitionSettings.touchInput())
    }

    private fun Preferences.toRecognitionSettings(
        defaults: RecognitionSettings,
    ): RecognitionSettings =
        defaults
            .copy(
                requiredMatchPercent =
                    valueOrDefault(RequiredMatchPercent, defaults.requiredMatchPercent),
                shapeSoftMarginPoints = valueOrDefault(ShapeSoftMarginPoints, defaults.shapeSoftMarginPoints),
                distanceTolerance =
                    valueOrDefault(DistanceTolerance, defaults.distanceTolerance),
                maxTranslationPercent =
                    valueOrDefault(MaxTranslationPercent, defaults.maxTranslationPercent),
                maxSizeCompensationPercent =
                    this[MaxSizeCompensationPercent]?.takeUnless { it.isNaN() },
                maxHorizontalStretchPercent =
                    valueOrDefault(
                        MaxHorizontalStretchPercent,
                        defaults.maxHorizontalStretchPercent,
                    ),
                maxVerticalStretchPercent =
                    valueOrDefault(
                        MaxVerticalStretchPercent,
                        defaults.maxVerticalStretchPercent,
                    ),
                maxRotationDegrees =
                    valueOrDefault(MaxRotationDegrees, defaults.maxRotationDegrees),
                maxShearDegrees = this[MaxShearDegrees]?.takeUnless { it.isNaN() },
                jitterTolerancePercent =
                    valueOrDefault(JitterTolerancePercent, defaults.jitterTolerancePercent),
                maxCompensationBudgetPercent =
                    this[MaxCompensationBudgetPercent]?.takeUnless { it.isNaN() },
                connectionTolerancePercent =
                    valueOrDefault(
                        ConnectionTolerancePercent,
                        defaults.connectionTolerancePercent,
                    ),
                protrusionTolerancePercent =
                    valueOrDefault(
                        ProtrusionTolerancePercent,
                        defaults.protrusionTolerancePercent,
                    ),
                lengthDifferenceTolerancePercent =
                    valueOrDefault(
                        LengthDifferenceTolerancePercent,
                        defaults.lengthDifferenceTolerancePercent,
                    ),
                checkStrokeStructure =
                    valueOrDefault(CheckStrokeStructure, defaults.checkStrokeStructure),
                checkStrokeOrder =
                    valueOrDefault(CheckStrokeOrder, defaults.checkStrokeOrder),
                checkStrokeDirection =
                    valueOrDefault(CheckStrokeDirection, defaults.checkStrokeDirection),
                readabilityEnabled =
                    valueOrDefault(ReadabilityEnabled, defaults.readabilityEnabled),
                shapeEnabled = valueOrDefault(ShapeEnabled, defaults.shapeEnabled),
                strokeWritingEnabled =
                    valueOrDefault(StrokeWritingEnabled, defaults.strokeWritingEnabled),
                hintMode =
                    this[HintModeKey]?.let { name ->
                        HintMode.entries.firstOrNull { it.name == name }
                    } ?: defaults.hintMode,
                maxNextStrokeHints =
                    valueOrDefault(MaxNextStrokeHints, defaults.maxNextStrokeHints),
                fullHintDisplayMillis =
                    valueOrDefault(FullHintDisplayMillis, defaults.fullHintDisplayMillis),
                autoAdvanceKanjiOnSuccess =
                    valueOrDefault(
                        AutoAdvanceKanjiOnSuccess,
                        defaults.autoAdvanceKanjiOnSuccess,
                    ),
                autoAdvanceOnSuccess =
                    valueOrDefault(AutoAdvanceOnSuccess, defaults.autoAdvanceOnSuccess),
                gesturesOnlyMode =
                    valueOrDefault(GesturesOnlyMode, defaults.gesturesOnlyMode),
                showPadGestureHints =
                    valueOrDefault(ShowPadGestureHints, defaults.showPadGestureHints),
            )
            .roundedParameters()

    override suspend fun save(settings: RecognitionSettings) {
        val value = settings.roundedParameters()
        store.edit { values ->
            values[Version] = 1
            values[RequiredMatchPercent] = value.requiredMatchPercent
            values[ShapeSoftMarginPoints] = value.shapeSoftMarginPoints
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
            values[HintModeKey] = value.hintMode.name
            values[MaxNextStrokeHints] = value.maxNextStrokeHints
            values[FullHintDisplayMillis] = value.fullHintDisplayMillis
            values[AutoAdvanceKanjiOnSuccess] = value.autoAdvanceKanjiOnSuccess
            values[AutoAdvanceOnSuccess] = value.autoAdvanceOnSuccess
            values[GesturesOnlyMode] = value.gesturesOnlyMode
            values[ShowPadGestureHints] = value.showPadGestureHints
        }
    }

    private companion object {
        val Version = intPreferencesKey("schema_version")
        val RequiredMatchPercent = floatPreferencesKey("requiredMatchPercent")
        val ShapeSoftMarginPoints = floatPreferencesKey("shapeSoftMarginPoints")
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
        val LengthDifferenceTolerancePercent =
            floatPreferencesKey("lengthDifferenceTolerancePercent")
        val CheckStrokeStructure = booleanPreferencesKey("checkStrokeStructure")
        val CheckStrokeOrder = booleanPreferencesKey("checkStrokeOrder")
        val CheckStrokeDirection = booleanPreferencesKey("checkStrokeDirection")
        val ReadabilityEnabled = booleanPreferencesKey("readabilityEnabled")
        val ShapeEnabled = booleanPreferencesKey("shapeEnabled")
        val StrokeWritingEnabled = booleanPreferencesKey("strokeWritingEnabled")
        val HintModeKey = stringPreferencesKey("hintMode")
        val MaxNextStrokeHints = intPreferencesKey("maxNextStrokeHints")
        val FullHintDisplayMillis = intPreferencesKey("fullHintDisplayMillis")
        val AutoAdvanceKanjiOnSuccess = booleanPreferencesKey("autoAdvanceKanjiOnSuccess")
        val AutoAdvanceOnSuccess = booleanPreferencesKey("autoAdvanceOnSuccess")
        val GesturesOnlyMode = booleanPreferencesKey("gesturesOnlyMode")
        val ShowPadGestureHints = booleanPreferencesKey("showPadGestureHints")
    }
}

private fun <T> Preferences.valueOrDefault(key: Preferences.Key<T>, fallback: T): T =
    this[key] ?: fallback
