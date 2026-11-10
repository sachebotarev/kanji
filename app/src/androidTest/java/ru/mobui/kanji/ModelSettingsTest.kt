package ru.mobui.kanji

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
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
    fun clearTrainingHistoryRequiresConfirmation() {
        var cleared = 0
        compose.setContent {
            KanjiTheme {
                SettingsScreen({}, {}, onClearTrainingHistory = { cleared++ })
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
