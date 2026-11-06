package ru.mobui.kanji.feature.dashboard

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.feature.dashboard.widgets.VocabularyWidget

class VocabularyNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun russianEntriesDispatchSeparateActions() = verify("ru", "Учебники", "Справочники")
    @Test fun englishEntriesDispatchSeparateActions() = verify("en", "Textbooks", "References")

    private fun verify(language: String, textbooks: String, references: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        val localized = context.createConfigurationContext(configuration)
        val calls = mutableListOf<String>()
        compose.setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration) {
                KanjiTheme {
                    VocabularyWidget(DashboardDemo.state.vocabulary,
                        onOpenCatalog = { calls += "books" },
                        onOpenReferences = { calls += "references" },
                        onOpenDictionary = { calls += "personal" })
                }
            }
        }
        compose.onNodeWithText(textbooks).assertExists()
        compose.onNodeWithText(references).assertExists()
        compose.onNodeWithTag("open-catalog").performClick()
        compose.onNodeWithTag("open-references").performClick()
        compose.onNodeWithTag("open-dictionary").performClick()
        compose.runOnIdle { assertEquals(listOf("books", "references", "personal"), calls) }
    }
}
