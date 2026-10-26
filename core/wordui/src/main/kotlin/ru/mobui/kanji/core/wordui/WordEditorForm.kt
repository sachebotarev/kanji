package ru.mobui.kanji.core.wordui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import java.util.Locale
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.*

data class WordEditorActions(
    val written: (String) -> Unit, val reading: (String) -> Unit,
    val translation: (Int, String) -> Unit, val language: (Int, String) -> Unit,
    val addTranslation: () -> Unit, val removeTranslation: (Int) -> Unit, val save: () -> Unit
)

@Composable
fun WordEditorForm(draft: WordDraft, invalid: Set<WordField>, busy: Boolean, creating: Boolean,
    actions: WordEditorActions, modifier: Modifier = Modifier, error: String? = null) {
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        WordInputField(stringResource(R.string.word_written), draft.written, actions.written,
            "word-input-written", stringResource(R.string.word_written_hint), WordField.Written in invalid, !busy)
        WordInputField(stringResource(R.string.word_reading), draft.reading, actions.reading,
            "word-input-reading", stringResource(R.string.word_reading_hint), WordField.Reading in invalid, !busy)
        Text(stringResource(R.string.word_translations), style = MaterialTheme.typography.titleMedium)
        draft.translations.forEachIndexed { index, item ->
            WordInfoPanel {
                Row {
                    LanguageSelector(item.languageTag, { actions.language(index, it) }, !busy, index, Modifier.weight(1f))
                    TextButton({ actions.removeTranslation(index) }, enabled = !busy,
                        modifier = Modifier.testTag("remove-translation-$index")) {
                        Text("×", modifier = Modifier.semanticsLabel(stringResource(R.string.word_remove_translation, item.languageTag)))
                    }
                }
                WordInputField(stringResource(R.string.word_translation), item.text, { actions.translation(index, it) },
                    "word-input-translation-$index", stringResource(R.string.word_translation_hint),
                    WordField.Translations in invalid && item.text.isBlank(), !busy)
            }
        }
        if (WordField.Translations in invalid) Text(stringResource(R.string.word_translations_error), color = MaterialTheme.colorScheme.error)
        FilledTonalButton(actions.addTranslation, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add-translation"), enabled = !busy) {
            Text("+  " + stringResource(R.string.word_add_translation))
        }
        if (error != null) Text(error, Modifier.testTag("word-save-error"), color = MaterialTheme.colorScheme.error)
        Button(actions.save, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("save-word"), enabled = !busy) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(stringResource(if (creating) R.string.word_create else R.string.word_save))
        }
    }
}

@Composable
private fun WordInputField(label: String, value: String, onChange: (String) -> Unit, tag: String, hint: String,
    invalid: Boolean, enabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(value, onChange, Modifier.fillMaxWidth().testTag(tag).semanticsLabel(label),
            enabled = enabled, isError = invalid, placeholder = { Text(hint) },
            supportingText = if (invalid) { { Text(stringResource(R.string.word_required)) } } else null,
            shape = RoundedCornerShape(20.dp), colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun LanguageSelector(tag: String, onSelect: (String) -> Unit, enabled: Boolean, index: Int, modifier: Modifier = Modifier) {
    var menu by remember { mutableStateOf(false) }
    var custom by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val languageName = Locale.forLanguageTag(tag).getDisplayName(locale).ifBlank { stringResource(R.string.word_language) }
    Box(modifier) {
        TextButton({ menu = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("translation-language-$index"), enabled = enabled) {
            Text(languageName + "  ▾")
        }
        DropdownMenu(menu, { menu = false }) {
            (listOf("ru", "en") + tag).filter { it.isNotBlank() }.distinct().forEach { value ->
                DropdownMenuItem(text = { Text(Locale.forLanguageTag(value).getDisplayName(locale)) },
                    onClick = { onSelect(value); menu = false }, modifier = Modifier.testTag("language-$value"))
            }
            DropdownMenuItem(text = { Text(stringResource(R.string.word_other_language)) }, onClick = {
                menu = false; input = ""; invalid = false; custom = true
            })
        }
    }
    if (custom) AlertDialog(onDismissRequest = { custom = false },
        title = { Text(stringResource(R.string.word_language)) },
        text = {
            OutlinedTextField(input, { input = it; invalid = false }, label = { Text(stringResource(R.string.word_language_tag)) },
                isError = invalid, supportingText = if (invalid) { { Text(stringResource(R.string.word_language_error)) } } else null)
        },
        confirmButton = { TextButton({
            val candidate = WordDraft("x", "x", listOf(WordTranslationDraft(input, "x")))
            invalid = candidate.invalidFields().isNotEmpty()
            if (!invalid) { onSelect(candidate.normalized().translations.single().languageTag); custom = false }
        }) { Text(stringResource(R.string.word_choose)) } },
        dismissButton = { TextButton({ custom = false }) { Text(stringResource(R.string.word_cancel)) } })
}

private fun Modifier.semanticsLabel(label: String): Modifier = this.then(Modifier.semantics { contentDescription = label })

@Preview(locale = "ru")
@Composable
private fun EditorPreview() = KanjiTheme(true) {
    WordEditorForm(WordDraft("木漏れ日", "こもれび", listOf(WordTranslationDraft("ru", "солнечный свет сквозь листву"))),
        emptySet(), false, false, WordEditorActions({}, {}, { _, _ -> }, { _, _ -> }, {}, {}, {}))
}
