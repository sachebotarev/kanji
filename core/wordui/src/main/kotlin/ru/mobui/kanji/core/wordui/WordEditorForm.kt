package ru.mobui.kanji.core.wordui

import ru.mobui.kanji.core.designsystem.inputShape
import ru.mobui.kanji.core.designsystem.kanjiOutlinedTextFieldColors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.WordDraft
import ru.mobui.kanji.domain.catalog.WordField
import ru.mobui.kanji.domain.catalog.WordTranslationDraft
import ru.mobui.kanji.domain.catalog.invalidFields
import ru.mobui.kanji.domain.catalog.normalized

@Composable
fun WordEditorForm(
    draft: WordDraft,
    invalid: Set<WordField>,
    busy: Boolean,
    creating: Boolean,
    actions: WordEditorActions,
    modifier: Modifier = Modifier,
    error: String? = null,
    showSave: Boolean = true,
    extraContent: @Composable () -> Unit = {},
    translationContent: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(19.dp),
    ) {
        WordInputField(
            WordInputState(
                stringResource(R.string.word_editor_written),
                draft.written,
                "word-input-written",
                stringResource(R.string.word_written_hint),
                WordField.Written in invalid,
                !busy,
            ),
            actions.written,
        )
        WordInputField(
            WordInputState(
                stringResource(R.string.word_editor_reading),
                draft.reading,
                "word-input-reading",
                stringResource(R.string.word_reading_hint),
                WordField.Reading in invalid,
                !busy,
            ),
            actions.reading,
        )
        if (translationContent != null) translationContent() else {
        Text(
            stringResource(R.string.word_translations),
            style = MaterialTheme.typography.titleMedium,
        )
        draft.translations.forEachIndexed { index, item ->
            WordInfoPanel {
                Row {
                    LanguageSelector(
                        item.languageTag,
                        { actions.language(index, it) },
                        !busy,
                        index,
                        Modifier.weight(1f),
                    )
                    TextButton(
                        { actions.removeTranslation(index) },
                        enabled = !busy,
                        modifier = Modifier.testTag("remove-translation-$index"),
                    ) {
                        Text(
                            "×",
                            modifier =
                                Modifier.semanticsLabel(
                                    stringResource(
                                        R.string.word_remove_translation,
                                        item.languageTag,
                                    ),
                                ),
                        )
                    }
                }
                WordInputField(
                    WordInputState(
                        stringResource(R.string.word_translation),
                        item.text,
                        "word-input-translation-$index",
                        stringResource(R.string.word_translation_hint),
                        WordField.Translations in invalid && item.text.isBlank(),
                        !busy,
                    ),
                    { actions.translation(index, it) },
                )
            }
        }
        if (WordField.Translations in invalid)
            Text(
                stringResource(R.string.word_translations_error),
                color = MaterialTheme.colorScheme.error,
            )
        FilledTonalButton(
            actions.addTranslation,
            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add-translation"),
            enabled = !busy,
        ) {
            Text("+  " + stringResource(R.string.word_add_translation))
        }
        }
        extraContent()
        if (error != null)
            Text(
                error,
                Modifier.testTag("word-save-error"),
                color = MaterialTheme.colorScheme.error,
            )
        if (!showSave) androidx.compose.foundation.layout.Spacer(Modifier.size(112.dp))
        if (showSave) Button(
            actions.save,
            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("save-word"),
            enabled = !busy,
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(stringResource(if (creating) R.string.word_create else R.string.word_save))
        }
    }
}

@Composable
private fun WordInputField(field: WordInputState, onChange: (String) -> Unit) {
    with(field) {
        WordEditorTextField(value, onChange, label, inputModifier = Modifier.testTag(tag),
            placeholder = hint, enabled = enabled, invalid = invalid,
            errorText = stringResource(R.string.word_required))
    }
}

@Composable
private fun LanguageSelector(
    tag: String,
    onSelect: (String) -> Unit,
    enabled: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    var custom by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val languageName =
        Locale.forLanguageTag(tag).getDisplayName(locale).ifBlank {
            stringResource(R.string.word_language)
        }
    Box(modifier) {
        TextButton(
            { menu = true },
            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("translation-language-$index"),
            enabled = enabled,
        ) {
            Text(languageName + "  ▾")
        }
        DropdownMenu(menu, { menu = false }) {
            (listOf("ru", "en") + tag)
                .filter { it.isNotBlank() }
                .distinct()
                .forEach { value ->
                    DropdownMenuItem(
                        text = { Text(Locale.forLanguageTag(value).getDisplayName(locale)) },
                        onClick = {
                            onSelect(value)
                            menu = false
                        },
                        modifier = Modifier.testTag("language-$value"),
                    )
                }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.word_other_language)) },
                onClick = {
                    menu = false
                    input = ""
                    invalid = false
                    custom = true
                },
            )
        }
    }
    if (custom)
        AlertDialog(
            onDismissRequest = { custom = false },
            title = { Text(stringResource(R.string.word_language)) },
            text = {
                OutlinedTextField(
                    input,
                    {
                        input = it
                        invalid = false
                    },
                    label = { Text(stringResource(R.string.word_language_tag)) },
                    shape = MaterialTheme.inputShape,
                    isError = invalid,
                    supportingText =
                        if (invalid) {
                            { Text(stringResource(R.string.word_language_error)) }
                        } else null,
                    colors = kanjiOutlinedTextFieldColors(),
                )
            },
            confirmButton = {
                TextButton(
                    {
                        val candidate =
                            WordDraft("x", "x", listOf(WordTranslationDraft(input, "x")))
                        invalid = candidate.invalidFields().isNotEmpty()
                        if (!invalid) {
                            onSelect(candidate.normalized().translations.single().languageTag)
                            custom = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.word_choose))
                }
            },
            dismissButton = {
                TextButton({ custom = false }) { Text(stringResource(R.string.word_cancel)) }
            },
        )
}

private fun Modifier.semanticsLabel(label: String): Modifier =
    this.then(Modifier.semantics { contentDescription = label })

@Preview(locale = "ru")
@Composable
private fun EditorPreview() =
    KanjiTheme(true) {
        WordEditorForm(
            WordDraft(
                "木漏れ日",
                "こもれび",
                listOf(WordTranslationDraft("ru", "солнечный свет сквозь листву")),
            ),
            emptySet(),
            false,
            false,
            WordEditorActions({}, {}, { _, _ -> }, { _, _ -> }, {}, {}, {}),
        )
    }

private data class WordInputState(
    val label: String,
    val value: String,
    val tag: String,
    val hint: String,
    val invalid: Boolean,
    val enabled: Boolean,
)
