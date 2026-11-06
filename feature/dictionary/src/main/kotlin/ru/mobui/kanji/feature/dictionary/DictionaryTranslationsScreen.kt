package ru.mobui.kanji.feature.dictionary

import ru.mobui.kanji.core.designsystem.inputShape
import ru.mobui.kanji.core.designsystem.kanjiOutlinedTextFieldColors

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.util.Locale
import ru.mobui.kanji.core.designsystem.KanjiFormContentPadding
import ru.mobui.kanji.core.wordui.WordNavigationRow
import ru.mobui.kanji.domain.catalog.WordTranslationDraft

internal fun editorLanguage(language: String) = if (language.startsWith("en", true)) "en" else "ru"

@Composable
private fun translationLanguageName(tag: String, language: String): String = when (tag.lowercase(Locale.ROOT)) {
    "ru" -> stringResource(R.string.dictionary_translation_russian)
    "en" -> stringResource(R.string.dictionary_translation_english)
    else -> Locale.forLanguageTag(tag).getDisplayName(Locale.forLanguageTag(editorLanguage(language))).ifBlank { tag }
}

/** Edits the parent word draft only. A modal cancel never invokes a mutation callback. */
@Composable
internal fun DictionaryTranslationsScreen(translations: List<WordTranslationDraft>, language: String,
    onSave: (String, String) -> Unit, onRemove: (Int) -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val current = editorLanguage(language)
    val available = listOf("ru", "en").filter { candidate -> candidate != current && translations.none { it.languageTag.equals(candidate, true) } }
    val others = translations.withIndex().filterNot { it.value.languageTag.equals(current, true) }
    var editing by rememberSaveable { mutableStateOf<Int?>(null) } // -1 = new translation
    Scaffold(Modifier.fillMaxSize().safeDrawingPadding().testTag("dictionary-translations"), floatingActionButton = {
        if (available.isNotEmpty()) ExtendedFloatingActionButton({ editing = -1 }, Modifier.testTag("translation-add")) {
            Text(stringResource(R.string.dictionary_translation_add))
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DictionaryHeader(stringResource(R.string.dictionary_translations_title), onBack)
            if (others.isEmpty()) Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Image(painterResource(ru.mobui.kanji.core.designsystem.R.drawable.mascot_sticker_peek_empty_box), null, Modifier.size(160.dp))
                Text(stringResource(R.string.dictionary_translation_empty))
            } else LazyColumn(Modifier.weight(1f), contentPadding = KanjiFormContentPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(others, key = { _, value -> value.value.languageTag }) { _, value ->
                    WordNavigationRow(translationLanguageName(value.value.languageTag, language), value.value.text,
                        { editing = value.index }, Modifier.testTag("translation-${value.value.languageTag}"))
                }
            }
            if (available.isEmpty()) Text(stringResource(R.string.dictionary_translation_exhausted),
                Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    editing?.let { index ->
        val existing = translations.getOrNull(index)
        var selectedLanguage by rememberSaveable(index) { mutableStateOf(existing?.languageTag ?: available.firstOrNull().orEmpty()) }
        var text by rememberSaveable(index) { mutableStateOf(existing?.text.orEmpty()) }
        AlertDialog(onDismissRequest = { editing = null },
            title = { Text(stringResource(if (index < 0) R.string.dictionary_translation_add else R.string.dictionary_translation_edit)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                if (existing == null) available.forEach { candidate ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selectedLanguage == candidate, role = Role.RadioButton,
                        onClick = { selectedLanguage = candidate }), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selectedLanguage == candidate, null)
                        Text(translationLanguageName(candidate, language))
                    }
                } else Text(translationLanguageName(existing.languageTag, language))
                OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().testTag("translation-text"),
                    label = { Text(stringResource(ru.mobui.kanji.core.wordui.R.string.word_translation)) }, shape = MaterialTheme.inputShape,
                    colors = kanjiOutlinedTextFieldColors())
                if (existing != null) TextButton({ onRemove(index); editing = null }, Modifier.testTag("translation-delete")) {
                    Text(stringResource(R.string.dictionary_delete))
                }
            } },
            confirmButton = { TextButton({ onSave(selectedLanguage, text); editing = null },
                enabled = selectedLanguage.isNotBlank() && text.isNotBlank(), modifier = Modifier.testTag("translation-confirm")) {
                Text(stringResource(R.string.dictionary_save))
            } },
            dismissButton = { TextButton({ editing = null }, Modifier.testTag("translation-cancel")) { Text(stringResource(R.string.dictionary_cancel)) } })
    }
}
