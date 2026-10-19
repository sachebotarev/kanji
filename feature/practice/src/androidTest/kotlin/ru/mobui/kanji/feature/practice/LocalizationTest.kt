package ru.mobui.kanji.feature.practice

import android.content.res.Configuration
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.domain.practice.CheckVerdict
import java.util.Locale

class LocalizationTest {
    @get:Rule val compose = createComposeRule()

    private fun show(language: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        val localized = context.createConfigurationContext(config)
        compose.setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides config) {
                Text(listOf(stringResource(R.string.checker_settings_title), debugVerdict(CheckVerdict.Incorrect),
                    debugMetricLabel("hooks.missing"), debugMetricValue("budget.hasError", "true"),
                    debugMetricValue("rotation.pivot", "TopLeft"),
                    debugMetricValue("dictionary.rules", "1 ShorterThan 2"),
                    checkFailureText(ru.mobui.kanji.domain.practice.CheckFailure.MissingHooks(listOf(2))))
                    .joinToString("\n"))
            }
        }
    }

    @Test fun russianSettingsDiagnosticsAndRulesAreLocalized() {
        show("ru")
        compose.onNodeWithText("Настройки проверки\nНеверно\nЧерты с пропущенным крючком\nДа\nЛевый верхний угол\nЧерта 1 должна быть короче черты 2.\nОтсутствует обязательный крючок в чертах: 2").assertExists()
    }

    @Test fun unsupportedLanguageFallsBackToRussian() {
        show("de")
        compose.onNodeWithText("Настройки проверки", substring = true).assertExists()
    }

    @Test fun englishSettingsDiagnosticsAndRulesAreLocalized() {
        show("en")
        compose.onNodeWithText("Recognition settings\nIncorrect\nStrokes with missing hooks\nYes\nTop left corner\nStroke 1 must be shorter than stroke 2.\nRequired hook missing from strokes: 2").assertExists()
    }
}
