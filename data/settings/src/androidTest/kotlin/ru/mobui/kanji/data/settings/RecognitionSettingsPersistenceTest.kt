package ru.mobui.kanji.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.mobui.kanji.domain.practice.HintMode
import ru.mobui.kanji.domain.practice.RecognitionSettings

class RecognitionSettingsPersistenceTest {
    @Test
    fun everySettingSurvivesClosingAndReopeningTheStoreAndResetIsDurable() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "settings-${System.nanoTime()}.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        fun repository() =
            DataStoreRecognitionSettingsRepository(
                PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
            )
        val changed =
            RecognitionSettings.touchInput()
                .copy(
                    requiredMatchPercent = 96f,
                    distanceTolerance = .03f,
                    maxTranslationPercent = 8f,
                    maxSizeCompensationPercent = 0f,
                    maxHorizontalStretchPercent = 12f,
                    maxVerticalStretchPercent = 14f,
                    maxRotationDegrees = 6f,
                    maxShearDegrees = 4f,
                    jitterTolerancePercent = .3f,
                    maxCompensationBudgetPercent = null,
                    connectionTolerancePercent = 4f,
                    protrusionTolerancePercent = 2f,
                    lengthDifferenceTolerancePercent = 7f,
                    checkStrokeStructure = false,
                    checkStrokeOrder = true,
                    checkStrokeDirection = true,
                    readabilityEnabled = false,
                    shapeEnabled = false,
                    strokeWritingEnabled = false,
                    hintMode = HintMode.NextStroke,
                    maxNextStrokeHints = 5,
                    fullHintDisplayMillis = 800,
                    autoAdvanceOnSuccess = true,
                )
                .roundedParameters()
        try {
            val first = repository()
            assertNull(first.load())
            first.save(changed)
            scope.coroutineContext[Job]!!.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val reopened = repository()
            assertEquals(changed, reopened.load())
            reopened.save(RecognitionSettings.touchInput())
            scope.coroutineContext[Job]!!.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            assertEquals(RecognitionSettings.touchInput().roundedParameters(), repository().load())
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
        }
    }
}
