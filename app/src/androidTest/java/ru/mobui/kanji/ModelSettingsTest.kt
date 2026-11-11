package ru.mobui.kanji

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.practice.RecognitionSettings
import ru.mobui.kanji.feature.settings.SettingsScreen

class ModelSettingsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dismissingFailedHistoryClearStartsANewConfirmation() {
        compose.setContent {
            KanjiTheme { SettingsScreen({}, {}, onClearTrainingHistory = { false }) }
        }
        val error = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            .getString(ru.mobui.kanji.feature.settings.R.string.settings_clear_history_failed)
        compose.onNodeWithTag("clear-training-history").performScrollTo().performClick()
        compose.onNodeWithTag("clear-training-history-confirm").performClick()
        compose.onNodeWithText(error).assertExists()
        compose.onNodeWithTag("clear-training-history-cancel").performClick()
        compose.onNodeWithTag("clear-training-history").performScrollTo().performClick()
        compose.onNodeWithText(error).assertDoesNotExist()
        compose.onNodeWithTag("clear-training-history-dialog").assertExists()
    }

    @Test
    fun unexpectedInitialReadFailureOffersRetry() {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)
        var attempts = 0
        lateinit var model: RecognitionSettingsViewModel
        try {
            compose.runOnIdle {
                model = RecognitionSettingsViewModel(androidx.lifecycle.SavedStateHandle(),
                    object : ru.mobui.kanji.domain.practice.RecognitionSettingsRepository {
                        override suspend fun load(): RecognitionSettings? {
                            if (++attempts == 1) error("unavailable")
                            return RecognitionSettings.touchInput()
                        }
                        override suspend fun save(settings: RecognitionSettings) = Unit
                    }, scope)
            }
            compose.waitUntil(5_000) { model.storageError.value }
            compose.runOnIdle { model.retryStorage() }
            compose.waitUntil(5_000) { model.loaded.value }
            compose.runOnIdle {
                org.junit.Assert.assertFalse(model.storageError.value)
                assertEquals(2, attempts)
            }
        } finally { scope.coroutineContext[kotlinx.coroutines.Job]?.cancel() }
    }

    @Test
    fun clearTrainingHistoryRequiresConfirmation() {
        var cleared = 0
        compose.setContent {
            KanjiTheme {
                SettingsScreen({}, {}, onClearTrainingHistory = { cleared++; true })
            }
        }
        compose.onNodeWithTag("clear-training-history").performScrollTo().performClick()
        compose.onNodeWithTag("clear-training-history-dialog").assertExists()
        compose.onNodeWithTag("clear-training-history-cancel").performClick()
        compose.onNodeWithTag("clear-training-history-dialog").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, cleared) }
        compose.onNodeWithTag("clear-training-history").performScrollTo().performClick()
        compose.onNodeWithTag("clear-training-history-confirm").performClick()
        compose.onNodeWithTag("clear-training-history-dialog").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, cleared) }
    }

    @Test
    fun downloadButtonOnlyAcceptsMissingModel() {
        val loading = mutableStateOf(false)
        val ready = mutableStateOf(false)
        var downloads = 0
        compose.setContent {
            KanjiTheme {
                SettingsScreen(
                    {},
                    {},
                    modelLoaded = ready.value,
                    modelLoading = loading.value,
                    onDownloadModel = {
                        downloads++
                        loading.value = true
                    },
                )
            }
        }
        compose.onNodeWithTag("download-japanese-model").assertIsEnabled().performClick()
        compose.onNodeWithTag("download-japanese-model").assertIsNotEnabled()
        compose.runOnIdle {
            loading.value = false
            ready.value = true
        }
        compose.onNodeWithTag("download-japanese-model").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, downloads) }
    }

    @Test
    fun readabilityCannotBeEnabledUntilModelIsReady() {
        val ready = mutableStateOf(false)
        val settings =
            mutableStateOf(RecognitionSettings.touchInput().copy(readabilityEnabled = false))
        compose.setContent {
            KanjiTheme {
                SettingsScreen(
                    onBack = {},
                    onOpenChecker = {},
                    settings = settings.value,
                    onChangeSettings = { settings.value = it },
                    readabilityAvailable = ready.value,
                )
            }
        }
        compose.onNodeWithTag("enable-readability").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("enable-readability-off").assertIsSelected()
        compose.onNodeWithTag("readability-model-required").assertExists()
        compose.runOnIdle { ready.value = true }
        compose.onNodeWithTag("readability-model-required").assertDoesNotExist()
        compose
            .onNodeWithTag("enable-readability")
            .assertIsEnabled()
            .performClick()
            .assertIsSelected()
    }
}
