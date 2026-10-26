package ru.mobui.kanji.feature.dictionary

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.*
import java.util.Locale

class DictionaryLocalizationTest {
    @get:Rule val compose = createComposeRule()
    private fun open(language: String, translations: Map<String, String>, manual: Boolean = false) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        val localized = context.createConfigurationContext(configuration)
        val word = CatalogWord(1, "猫", "ねこ", translations, isUserCreated = manual)
        compose.setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration) {
                KanjiTheme {
                    DictionaryWordScreen(word, DictionaryState(loading = false, words = listOf(word)), language,
                        {}, {}, {}, {}, {}, {})
                }
            }
        }
    }
    @Test fun englishUsesRussianContentAndCatalogWordHasNoMutationActions() {
        open("en", mapOf("ru" to "кошка"))
        compose.onNodeWithText("Full reading").assertIsDisplayed()
        compose.onNodeWithText("кошка").assertIsDisplayed()
        compose.onNodeWithTag("edit-word").assertDoesNotExist()
        compose.onNodeWithTag("word-actions").assertDoesNotExist()
    }
    @Test fun missingTranslationIsExplicitInRussian() {
        open("ru", emptyMap())
        compose.onNodeWithText("Перевод отсутствует").assertIsDisplayed()
    }
    @Test fun unsupportedLanguageFallsBackToRussianAndManualActionsAreAvailable() {
        open("de", mapOf("ru" to "кошка"), manual = true)
        compose.onNodeWithText("Создано вручную").assertIsDisplayed()
        compose.onNodeWithTag("edit-word").assertIsDisplayed()
        compose.onNodeWithTag("word-actions").performClick()
        compose.onNodeWithTag("delete-word").assertIsDisplayed()
    }
}
