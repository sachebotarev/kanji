package ru.mobui.kanji

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.mobui.kanji.domain.practice.CompensationStage
import ru.mobui.kanji.domain.practice.RecognitionSettings
import ru.mobui.kanji.domain.practice.RecognitionSettingsRepository

/**
 * DataStore is authoritative; SavedStateHandle migrates values from the previous in-memory screen.
 */
class RecognitionSettingsViewModel(
    private val savedState: SavedStateHandle,
    private val repository: RecognitionSettingsRepository,
    private val persistenceScope: CoroutineScope,
) : ViewModel() {
    private val defaults = RecognitionSettings.touchInput()
    private val mutableSettings = MutableStateFlow(restore())
    val settings = mutableSettings.asStateFlow()

    private val mutableLoaded = MutableStateFlow(false)
    val loaded = mutableLoaded.asStateFlow()
    private val mutableStorageError = MutableStateFlow(false)
    val storageError = mutableStorageError.asStateFlow()
    private val writes = Mutex()
    private var pendingWrites = 0
    private val mutableSaving = MutableStateFlow(false)
    val saving = mutableSaving.asStateFlow()

    init {
        load()
    }

    fun retryStorage() {
        if (!mutableLoaded.value) load() else persist(mutableSettings.value)
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val durable = repository.load()
                if (durable != null) mutableSettings.value = durable
                else if (savedState.contains("recognition.parameters"))
                    repository.save(mutableSettings.value)
                mutableStorageError.value = false
                mutableLoaded.value = true
            } catch (_: IOException) {
                mutableStorageError.value = true
            }
        }
    }

    private fun persist(value: RecognitionSettings) {
        pendingWrites++
        mutableSaving.value = true
        // An Activity closing must not cancel an already requested disk write.
        persistenceScope.launch {
            try {
                writes.withLock { repository.save(value) }
                mutableStorageError.value = false
            } catch (_: IOException) {
                mutableStorageError.value = true
            } finally {
                pendingWrites--
                mutableSaving.value = pendingWrites > 0
            }
        }
    }

    fun update(settings: RecognitionSettings) {
        val value = settings.roundedParameters()
        mutableSettings.value = value
        persist(value)
        savedState["recognition.readability"] = value.readabilityEnabled
        savedState["recognition.shapeSoftMarginPoints"] = value.shapeSoftMarginPoints
        savedState["recognition.shape"] = value.shapeEnabled
        savedState["recognition.writing"] = value.strokeWritingEnabled
        savedState["recognition.autoAdvanceKanji"] = value.autoAdvanceKanjiOnSuccess
        savedState["recognition.autoAdvance"] = value.autoAdvanceOnSuccess
        savedState["recognition.gesturesOnly"] = value.gesturesOnlyMode
        savedState["recognition.showPadGestures"] = value.showPadGestureHints
        savedState["recognition.parameters"] =
            floatArrayOf(
                value.requiredMatchPercent,
                value.distanceTolerance,
                value.maxTranslationPercent,
                value.maxSizeCompensationPercent ?: Float.NaN,
                value.maxHorizontalStretchPercent,
                value.maxVerticalStretchPercent,
                value.maxRotationDegrees,
                value.maxShearDegrees ?: Float.NaN,
                value.jitterTolerancePercent,
                value.maxCompensationBudgetPercent ?: Float.NaN,
                value.connectionTolerancePercent,
                value.protrusionTolerancePercent,
                if (value.checkStrokeStructure) 1f else 0f,
                if (value.checkStrokeOrder) 1f else 0f,
                if (value.checkStrokeDirection) 1f else 0f,
                value.lengthDifferenceTolerancePercent,
            )
        savedState.remove<Array<String>>("recognition.stageNames")
        savedState.remove<IntArray>("recognition.stages")
        savedState.remove<String>("recognition.pipeline")
    }

    fun reset() = update(defaults)

    private fun restore(): RecognitionSettings {
        val stored = savedState.get<FloatArray>("recognition.parameters")
        val values =
            when (stored?.size) {
                CURRENT_TOLERANCE_COUNT -> stored
                LEGACY_TOLERANCE_COUNT ->
                    stored
                        .filterIndexed { index, _ -> index != LEGACY_REMOVED_TOLERANCE_INDEX }
                        .toFloatArray()
                else -> null
            }
        return if (values == null) defaults
        else {
            val restored = migrateLegacyStages(decodeParameters(values))
            restored
                .copy(
                    readabilityEnabled = savedState["recognition.readability"] ?: true,
                    shapeSoftMarginPoints = savedState["recognition.shapeSoftMarginPoints"] ?: defaults.shapeSoftMarginPoints,
                    shapeEnabled = savedState["recognition.shape"] ?: true,
                    strokeWritingEnabled = savedState["recognition.writing"] ?: true,
                    autoAdvanceKanjiOnSuccess = savedState["recognition.autoAdvanceKanji"] ?: true,
                    autoAdvanceOnSuccess = savedState["recognition.autoAdvance"] ?: true,
                    gesturesOnlyMode = savedState["recognition.gesturesOnly"] ?: false,
                    showPadGestureHints = savedState["recognition.showPadGestures"] ?: true,
                )
                .roundedParameters()
        }
    }

    private fun decodeParameters(values: FloatArray): RecognitionSettings {
        return runCatching {
                defaults.copy(
                    requiredMatchPercent = values[0],
                    distanceTolerance = values[1],
                    maxTranslationPercent = values[2],
                    maxSizeCompensationPercent = values[3].takeUnless { it.isNaN() },
                    maxHorizontalStretchPercent = values[4],
                    maxVerticalStretchPercent = values[5],
                    maxRotationDegrees = values[6],
                    maxShearDegrees = values[7].takeUnless { it.isNaN() },
                    jitterTolerancePercent = values[8],
                    maxCompensationBudgetPercent = values[9].takeUnless { it.isNaN() },
                    connectionTolerancePercent = values[10],
                    protrusionTolerancePercent = values[11],
                    checkStrokeStructure = values[12] == 1f,
                    checkStrokeOrder = values[13] == 1f,
                    checkStrokeDirection = values[14] == 1f,
                    lengthDifferenceTolerancePercent = values[15],
                )
            }
            .getOrDefault(defaults)
    }

    private fun migrateLegacyStages(restored: RecognitionSettings): RecognitionSettings {
        val oldEnabled =
            savedState
                .get<Array<String>>("recognition.stageNames")
                ?.mapNotNull { name ->
                    CompensationStage.entries.firstOrNull { it.name == name }
                }
                ?.toSet()
                ?: savedState
                    .get<IntArray>("recognition.stages")
                    ?.asIterable()
                    ?.mapNotNull {
                        CompensationStage.entries.getOrNull(it)
                    }
                    ?.toSet()
        // Migrate disabled legacy toggles to zero, then stop persisting separate toggles.
        return if (oldEnabled == null) restored
        else
            restored.copy(
                maxTranslationPercent =
                    restored.maxTranslationPercent.takeIf {
                        CompensationStage.Translation in oldEnabled
                    } ?: 0f,
                maxSizeCompensationPercent =
                    if (CompensationStage.Size in oldEnabled) restored.maxSizeCompensationPercent
                    else 0f,
                maxRotationDegrees =
                    restored.maxRotationDegrees.takeIf {
                        CompensationStage.Rotation in oldEnabled
                    } ?: 0f,
                maxShearDegrees =
                    if (CompensationStage.Shear in oldEnabled) restored.maxShearDegrees else 0f,
                maxHorizontalStretchPercent =
                    restored.maxHorizontalStretchPercent.takeIf {
                        CompensationStage.Stretch in oldEnabled
                    } ?: 0f,
                maxVerticalStretchPercent =
                    restored.maxVerticalStretchPercent.takeIf {
                        CompensationStage.Stretch in oldEnabled
                    } ?: 0f,
                jitterTolerancePercent =
                    restored.jitterTolerancePercent.takeIf {
                        CompensationStage.Jitter in oldEnabled
                    } ?: 0f,
            )
    }
}

private const val CURRENT_TOLERANCE_COUNT = 16
private const val LEGACY_TOLERANCE_COUNT = 17
private const val LEGACY_REMOVED_TOLERANCE_INDEX = 9
