package ru.mobui.kanji.feature.dictionary

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.core.wordui.WordEditorActions
import ru.mobui.kanji.domain.catalog.WordDraft
import ru.mobui.kanji.domain.catalog.WordTranslationDraft
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class DictionaryTranslationsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun newTranslationIsOnlyAppliedOnConfirmationAndExhaustedFabDisappears() {
        val translations = mutableStateOf(listOf(WordTranslationDraft("ru", "кошка")))
        compose.setContent { KanjiTheme {
            DictionaryTranslationsScreen(translations.value, "ru", { language, text ->
                translations.value += WordTranslationDraft(language, text)
            }, {}, {})
        } }
        compose.onNodeWithTag("translation-add").performClick()
        compose.onNodeWithTag("translation-text").performTextInput("cat")
        compose.onNodeWithTag("translation-cancel").performClick()
        compose.runOnIdle { assertEquals(1, translations.value.size) }
        // Invoke the action independent of the IME dismissal animation after the dialog.
        compose.onNodeWithTag("translation-add").performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithTag("translation-text").performTextInput("cat")
        compose.onNodeWithTag("translation-confirm").performClick()
        compose.onNodeWithTag("translation-en").assertIsDisplayed()
        compose.onNodeWithTag("translation-add").assertDoesNotExist()
        compose.runOnIdle { assertEquals("cat", translations.value.last().text) }
    }

    @Test fun legacyLanguageCanBeEditedOrRemovedAndModalCancelPreservesIt() {
        val translations = mutableStateOf(listOf(WordTranslationDraft("ru", "кошка"), WordTranslationDraft("de", "Katze")))
        compose.setContent { KanjiTheme {
            DictionaryTranslationsScreen(translations.value, "ru", { language, text ->
                translations.value = translations.value.map { if (it.languageTag == language) it.copy(text = text) else it }
            }, { index -> translations.value = translations.value.filterIndexed { i, _ -> i != index } }, {})
        } }
        compose.onNodeWithTag("translation-de").performClick()
        compose.onNodeWithTag("translation-text").performTextReplacement("Kater")
        compose.onNodeWithTag("translation-cancel").performClick()
        compose.runOnIdle { assertEquals("Katze", translations.value.last().text) }
        compose.onNodeWithTag("translation-de").performClick()
        compose.onNodeWithTag("translation-text").performTextReplacement("Kater")
        compose.onNodeWithTag("translation-confirm").performClick()
        compose.runOnIdle { assertEquals("Kater", translations.value.last().text) }
        compose.onNodeWithTag("translation-de").performClick()
        compose.onNodeWithTag("translation-delete").performClick()
        compose.onNodeWithTag("translation-de").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("ru"), translations.value.map { it.languageTag }) }
    }

    @Test fun primaryFieldNeverCopiesFallbackAndEditorSizeDoesNotDependOnOtherLanguages() {
        val draft = WordDraft("猫", "ねこ", listOf("ru", "de", "fr", "it", "es", "pt", "ko", "zh", "pl").map {
            WordTranslationDraft(it, "translation-$it")
        })
        var opened = false
        compose.setContent { KanjiTheme {
            WordEditorScreen(EditorState(draft, original = draft, loading = false), false,
                WordEditorActions({}, {}, { _, _ -> }, { _, _ -> }, {}, {}, {}), {}, {},
                language = "en", onTranslations = { opened = true })
        } }
        compose.onNodeWithTag("editor-current-translation").performScrollTo()
        assertEquals("", compose.onNodeWithTag("editor-current-translation").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        compose.onNodeWithText("translation-ru").assertDoesNotExist()
        compose.onNodeWithText("translation-de").assertDoesNotExist()
        compose.onNodeWithTag("editor-other-translations").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(opened) }
    }
}
