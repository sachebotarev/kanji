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
    private fun open(language: String, translations: Map<String, String>, written: String = "一月") {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        val localized = context.createConfigurationContext(configuration)
        val word = CatalogWord(1, written, "いちがつ", translations)
        compose.setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration) {
                KanjiTheme { CatalogDetailScreen(DetailRequest.Word(1, null), CatalogLoad.Ready(DetailContent.Word(word, emptyList(), null)), SearchState(), language, DetailActions({}, {}, {}, {}, {})) }
            }
        }
    }
    @Test fun englishUiUsesRussianContentFallback() {
        open("en", mapOf("ru" to "январь"))
        compose.onNodeWithText("いちがつ").assertIsDisplayed()
        compose.onNodeWithText("январь").assertIsDisplayed()
    }
    @Test fun russianUiShowsExplicitMissingTranslation() {
        open("ru", emptyMap())
        compose.onNodeWithText("いちがつ").assertIsDisplayed()
        compose.onNodeWithText("Перевод отсутствует").assertIsDisplayed()
    }
    @Test fun unsupportedUiLanguageFallsBackToRussian() {
        open("de", mapOf("ru" to "январь"))
        compose.onNodeWithText("いちがつ").assertIsDisplayed()
        compose.onNodeWithText("январь").assertIsDisplayed()
    }

    @Test fun kanaOnlyWordDoesNotShowAnEmptyKanjiSection() {
        open("ru", mapOf("ru" to "январь"), written = "いちがつ")
        compose.onNodeWithText("Кандзи в написании").assertDoesNotExist()
        compose.onNodeWithText("январь").assertIsDisplayed()
    }
}
