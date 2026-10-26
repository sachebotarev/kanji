package ru.mobui.kanji

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.practice.RecognitionSettings
import ru.mobui.kanji.feature.practice.CheckerSettingsScreen
import ru.mobui.kanji.feature.settings.SettingsScreen

class ModelSettingsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun downloadButtonOnlyAcceptsMissingModel() {
        val loading = mutableStateOf(false)
        val ready = mutableStateOf(false)
        var downloads = 0
        compose.setContent { KanjiTheme {
            SettingsScreen({}, {}, modelLoaded = ready.value, modelLoading = loading.value,
                onDownloadModel = { downloads++; loading.value = true })
        } }
        compose.onNodeWithTag("download-japanese-model").assertIsEnabled().performClick()
        compose.onNodeWithTag("download-japanese-model").assertIsNotEnabled()
        compose.runOnIdle { loading.value = false; ready.value = true }
        compose.onNodeWithTag("download-japanese-model").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, downloads) }
    }

    @Test fun readabilityCannotBeEnabledUntilModelIsReady() {
        val ready = mutableStateOf(false)
        val settings = mutableStateOf(RecognitionSettings.touchInput().copy(readabilityEnabled = false))
        compose.setContent { KanjiTheme {
            CheckerSettingsScreen(settings.value, { settings.value = it }, {}, {}, readabilityAvailable = ready.value)
        } }
        compose.onNodeWithTag("enable-readability").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("enable-readability-off").assertIsSelected()
        compose.onNodeWithTag("readability-model-required").assertExists()
        compose.runOnIdle { ready.value = true }
        compose.onNodeWithTag("readability-model-required").assertDoesNotExist()
        compose.onNodeWithTag("enable-readability").assertIsEnabled().performClick().assertIsSelected()
    }
}
