package ru.mobui.kanji.feature.dictionary

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.TagColor
import ru.mobui.kanji.domain.catalog.TagTranslation

class DictionaryListAppearanceTest {
    @get:Rule val compose = createComposeRule()
    private val words = listOf(
        CatalogWord(-1, "木漏れ日", "こもれび", mapOf("ru" to "солнечный свет сквозь листву", "en" to "sunlight through leaves"), isUserCreated = true),
        CatalogWord(-2, "日本語を勉強しています。", "にほんごをべんきょうしています。", mapOf("ru" to "Я изучаю японский язык."), isUserCreated = true),
    )

    @Test
    fun rootSearchIsHiddenUntilRequestedAndClosingClearsItWithoutOpeningAWord() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("ru")) }
        val state = mutableStateOf(DictionaryState(loading = false, words = words))
        var opened: Int? = null
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context.createConfigurationContext(configuration), LocalConfiguration provides configuration) {
                KanjiTheme {
                    DictionaryListScreen(state.value, words, "ru", DictionaryListActions({}, { state.value = state.value.copy(query = it) }, { opened = it },
                        {}, {}))
                }
            }
        }
        compose.onNodeWithTag("dictionary-search").assertDoesNotExist()
        val filters = compose.onNodeWithTag("dictionary-filters").getUnclippedBoundsInRoot()
        val count = compose.onNodeWithTag("dictionary-count").getUnclippedBoundsInRoot()
        assertTrue(count.top >= filters.bottom)
        compose.onNodeWithTag("dictionary-search-toggle").assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithTag("dictionary-search").performTextReplacement("木")
        compose.onNodeWithTag("dictionary-search-toggle").performClick()
        compose.onNodeWithTag("dictionary-search").assertDoesNotExist()
        compose.runOnIdle { assertEquals("", state.value.query); assertEquals(null, opened) }
        compose.onNodeWithTag("word--1").performClick()
        compose.runOnIdle { assertEquals(-1, opened) }
    }

    @Test
    fun emptyVisibleListHidesResultCount() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("ru")) }
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context.createConfigurationContext(configuration), LocalConfiguration provides configuration) {
                KanjiTheme {
                    DictionaryListScreen(DictionaryState(loading = false), emptyList(), "ru", DictionaryListActions({}, {}, {}, {}, {}))
                }
            }
        }
        compose.onNodeWithTag("dictionary-count").assertDoesNotExist()
    }

    @Test
    fun populatedRootRemainsReadableInBothLanguagesThemesAndLargeText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mode = mutableStateOf(Triple("ru", false, 1f))
        compose.setContent {
            val (language, dark, scale) = mode.value
            val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
            val localized = context.createConfigurationContext(configuration)
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration,
                LocalDensity provides Density(context.resources.displayMetrics.density, scale)) {
                KanjiTheme(darkTheme = dark) {
                    DictionaryListScreen(DictionaryState(loading = false, words = words,
                        tags = listOf(DictionaryTag(1, TagColor.Green, listOf(TagTranslation("ru", "Повторить"), TagTranslation("en", "Review")))),
                        wordTags = mapOf(-1 to setOf(1L))), words, language, DictionaryListActions({}, {}, {}, {}, {}))
                }
            }
        }
        for (language in listOf("ru", "en")) for (dark in listOf(false, true)) for (scale in listOf(1f, 1.5f)) {
            compose.runOnIdle { mode.value = Triple(language, dark, scale) }
            compose.waitForIdle()
            compose.onNodeWithTag("word--1").assertIsDisplayed()
            compose.onNodeWithText("木漏れ日").assertIsDisplayed()
            compose.onNodeWithText("солнечный свет сквозь листву", substring = true).apply {
                if (language == "ru") assertIsDisplayed()
            }
            compose.onNodeWithTag("dictionary-count").assertHeightIsAtLeast(48.dp)
            compose.onNodeWithTag("add-word").assertHeightIsAtLeast(56.dp).assertIsDisplayed()
            val folder = File(context.getExternalFilesDir(null), "roots-parity").apply { mkdirs() }
            val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(folder, "dictionary-$language-${if (dark) "dark" else "light"}-$scale.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,
                100, it) }
            bitmap.recycle()
        }
    }
}
