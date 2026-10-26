package ru.mobui.kanji.feature.catalog

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

class CatalogLocalizationTest {
    @get:Rule val compose = createComposeRule()
    private fun open(language: String, translations: Map<String, String>) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        val localized = context.createConfigurationContext(configuration)
        val word = CatalogWord(1, "一月", "いちがつ", translations)
        val data = CatalogSnapshot(1, emptyList(), listOf(word), emptyList(), emptyList(), emptyList(), emptyList())
        compose.setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration) {
                KanjiTheme { CatalogScreen(CatalogUiState(CatalogRequest(CatalogPage.Word, id = 1, language = language), data, loading = false), CatalogActions()) }
            }
        }
    }
    @Test fun englishUiUsesRussianContentFallback() {
        open("en", mapOf("ru" to "январь"))
        compose.onNodeWithText("Full reading").assertIsDisplayed()
        compose.onNodeWithText("январь").assertIsDisplayed()
    }
    @Test fun russianUiShowsExplicitMissingTranslation() {
        open("ru", emptyMap())
        compose.onNodeWithText("Полное чтение").assertIsDisplayed()
        compose.onNodeWithText("Перевод отсутствует").assertIsDisplayed()
    }
    @Test fun unsupportedUiLanguageFallsBackToRussian() {
        open("de", mapOf("ru" to "январь"))
        compose.onNodeWithText("Полное чтение").assertIsDisplayed()
        compose.onNodeWithText("январь").assertIsDisplayed()
    }
}
