package ru.mobui.kanji.feature.dictionary

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.core.wordui.WordEditorActions
import ru.mobui.kanji.domain.catalog.WordDraft
import ru.mobui.kanji.domain.catalog.WordTranslationDraft
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.waitForIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class DictionaryEditorAppearanceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fieldsFollowApprovedOrderInBothLanguagesThemesAndScales() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mode = mutableStateOf(Triple("ru", false, 1f))
        val draft = WordDraft("", "", emptyList())
        compose.setContent {
            val (language, dark, scale) = mode.value
            val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
            CompositionLocalProvider(LocalContext provides context.createConfigurationContext(config), LocalConfiguration provides config,
                LocalDensity provides Density(context.resources.displayMetrics.density, scale)) {
                KanjiTheme(darkTheme = dark) {
                    WordEditorScreen(EditorState(draft, original = draft, loading = false), true,
                        WordEditorActions({}, {}, { _, _ -> }, { _, _ -> }, {}, {}, {}), {}, {}, language = language)
                }
            }
        }
        for (language in listOf("ru", "en")) for (dark in listOf(false, true)) for (scale in listOf(1f, 1.5f)) {
            compose.runOnIdle { mode.value = Triple(language, dark, scale) }
            compose.waitForIdle()
            val written = compose.onNodeWithTag("word-input-written").getUnclippedBoundsInRoot()
            val reading = compose.onNodeWithTag("word-input-reading").getUnclippedBoundsInRoot()
            val translation = compose.onNodeWithTag("editor-current-translation").getUnclippedBoundsInRoot()
            val other = compose.onNodeWithTag("editor-other-translations").getUnclippedBoundsInRoot()
            val tags = compose.onNodeWithTag("editor-tags").getUnclippedBoundsInRoot()
            assertTrue(written.bottom < reading.top && reading.bottom < translation.top)
            assertTrue(translation.bottom < other.top && other.bottom < tags.top)
            compose.onNodeWithTag("editor-current-translation").assertHeightIsAtLeast(94.dp)
            compose.onNodeWithTag("editor-other-translations").assertHeightIsAtLeast(64.dp)
            compose.onNodeWithTag("editor-tags").assertHeightIsAtLeast(64.dp)
            compose.onNodeWithTag("save-word").assertHeightIsAtLeast(56.dp).assertIsDisplayed()
            compose.onNodeWithTag("dictionary-back").assertHeightIsAtLeast(48.dp)
            val folder = File(context.getExternalFilesDir(null), "editor-parity").apply { mkdirs() }
            val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(folder, "create-$language-${if (dark) "dark" else "light"}-$scale.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
    }

    @Test fun multilineSentencesGrowAndTranslationNeverCopiesFallback() {
        val state = mutableStateOf(WordDraft("猫", "ねこ", listOf(WordTranslationDraft("ru", "кошка"))))
        compose.setContent { KanjiTheme {
            WordEditorScreen(EditorState(state.value, original = state.value, loading = false), false,
                WordEditorActions({ state.value = state.value.copy(written = it) }, {}, { _, _ -> }, { _, _ -> }, {}, {}, {}), {}, {}, language = "en",
                onCurrentTranslation = { text -> state.value = state.value.copy(translations = state.value.translations + WordTranslationDraft("en", text)) })
        } }
        val original = compose.onNodeWithTag("word-input-written").getUnclippedBoundsInRoot()
        val sentence = List(6) { "日本語を勉強しています。" }.joinToString("\n")
        compose.onNodeWithTag("word-input-written").performTextReplacement(sentence)
        val grown = compose.onNodeWithTag("word-input-written").getUnclippedBoundsInRoot()
        assertTrue(grown.bottom - grown.top > original.bottom - original.top)
        compose.onNodeWithTag("editor-current-translation").performScrollTo()
        assertEquals("", compose.onNodeWithTag("editor-current-translation").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        compose.onNodeWithTag("editor-current-translation").performTextInput("cat")
        compose.runOnIdle {
            assertEquals(sentence, state.value.written)
            assertEquals("кошка", state.value.translations.first().text)
            assertEquals("cat", state.value.translations.last().text)
        }
    }
}
