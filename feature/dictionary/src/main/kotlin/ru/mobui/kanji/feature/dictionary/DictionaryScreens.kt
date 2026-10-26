package ru.mobui.kanji.feature.dictionary

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.core.wordui.*
import ru.mobui.kanji.domain.catalog.*
import ru.mobui.kanji.core.designsystem.R as DesignR

@Composable
internal fun DictionaryHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack, enabled = enabled, modifier = Modifier.testTag("dictionary-back")) {
            Icon(painterResource(R.drawable.word_back), stringResource(R.string.dictionary_back))
        }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
        actions()
    }
}

@Composable
internal fun DictionaryStatus(loading: Boolean, error: Boolean, retry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (loading) CircularProgressIndicator()
        else if (error) Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.dictionary_error))
            TextButton(retry, Modifier.testTag("dictionary-retry")) { Text(stringResource(R.string.dictionary_retry)) }
        }
    }
}

@Composable
internal fun DictionaryEmptyState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(bottom = 88.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(DesignR.drawable.mascot_sticker_magnify),
            contentDescription = null,
            modifier = Modifier.size(168.dp).testTag("dictionary-empty-mascot"),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun DictionaryListScreen(state: DictionaryState, visible: List<CatalogWord>, language: String,
    onBack: () -> Unit, onQuery: (String) -> Unit, onWord: (Int) -> Unit, onAdd: () -> Unit, retry: () -> Unit) {
    Scaffold(Modifier.testTag("dictionary-screen").safeDrawingPadding(),
        floatingActionButton = {
            if (!state.loading && !state.error) ExtendedFloatingActionButton(onAdd, Modifier.testTag("add-word"),
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                Text("+  " + stringResource(R.string.dictionary_add))
            }
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DictionaryHeader(stringResource(R.string.dictionary_title), onBack)
            if (state.loading || state.error) DictionaryStatus(state.loading, state.error, retry)
            else {
                Text(pluralStringResource(R.plurals.dictionary_count, state.words.size, state.words.size),
                    Modifier.padding(horizontal = 20.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                WordSearch(state.query, stringResource(ru.mobui.kanji.core.wordui.R.string.word_search), onQuery,
                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp).testTag("dictionary-search"))
                if (visible.isNotEmpty()) {
                    WordList(visible, language, onWord, Modifier.weight(1f), showOrigin = true, bottomPadding = 100)
                } else {
                    DictionaryEmptyState(
                        message = stringResource(
                            if (state.query.isBlank()) R.string.dictionary_empty else R.string.dictionary_no_results,
                        ),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun DictionaryWordScreen(word: CatalogWord?, state: DictionaryState, language: String, onBack: () -> Unit,
    onEdit: () -> Unit, onDelete: () -> Unit, onKanji: (Int) -> Unit, onStudy: (WordStudyItem) -> Unit, retry: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val resources = androidx.compose.ui.platform.LocalResources.current
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        DictionaryHeader(stringResource(R.string.dictionary_word), onBack, enabled = !state.busy) {
            if (word?.isUserCreated == true) {
                IconButton(onEdit, enabled = !state.busy, modifier = Modifier.testTag("edit-word")) {
                    Icon(painterResource(R.drawable.word_edit), stringResource(R.string.dictionary_edit_action))
                }
                Box {
                    IconButton({ menu = true }, enabled = !state.busy, modifier = Modifier.testTag("word-actions")) {
                        Icon(painterResource(R.drawable.word_more), stringResource(R.string.dictionary_more))
                    }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.dictionary_delete_action)) },
                            onClick = { menu = false; confirmDelete = true }, modifier = Modifier.testTag("delete-word"))
                    }
                }
            }
        }
        if (state.loading || state.error) DictionaryStatus(state.loading, state.error, retry)
        else if (word == null) Text(stringResource(R.string.dictionary_missing), Modifier.padding(20.dp))
        else {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            operationError(state.operationError, state.storageError)?.let { Text(it, Modifier.padding(20.dp), color = MaterialTheme.colorScheme.error) }
            val snapshot = state.snapshot
            WordDetailsContent(word, language, snapshot?.literalKanji(word).orEmpty(),
                snapshot?.let { wordStudyItems(it, word.id, language) { chapter -> resources.getString(R.string.dictionary_chapter, chapter) } }.orEmpty(),
                onKanji, onStudy, showOrigin = true)
        }
    }
    if (confirmDelete && word?.isUserCreated == true) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.dictionary_delete_title)) },
        text = { Text(stringResource(R.string.dictionary_delete_text, word.written)) },
        confirmButton = { TextButton({ confirmDelete = false; onDelete() }, Modifier.testTag("confirm-delete-word"), enabled = !state.busy) {
            Text(stringResource(R.string.dictionary_delete), color = MaterialTheme.colorScheme.error)
        } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text(stringResource(R.string.dictionary_cancel)) } })
}

@Composable
internal fun WordEditorScreen(state: EditorState, creating: Boolean, actions: WordEditorActions,
    onBack: () -> Unit, retry: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().testTag("word-editor")) {
        DictionaryHeader(stringResource(if (creating) R.string.dictionary_new else R.string.dictionary_edit), onBack, enabled = !state.busy)
        if (state.loading || state.loadError) DictionaryStatus(state.loading, state.loadError, retry)
        else if (state.original == null) Text(operationError(state.failure, state.storageError) ?: stringResource(R.string.dictionary_missing), Modifier.padding(20.dp))
        else WordEditorForm(state.draft, state.invalid, state.busy, creating, actions,
            error = operationError(state.failure, state.storageError))
    }
}

@Composable
internal fun operationError(failure: DictionaryFailure?, storageError: Boolean): String? = when {
    storageError -> stringResource(R.string.dictionary_write_error)
    failure == DictionaryFailure.Duplicate -> stringResource(R.string.dictionary_duplicate)
    failure == DictionaryFailure.ReadOnly -> stringResource(R.string.dictionary_read_only)
    failure == DictionaryFailure.Missing -> stringResource(R.string.dictionary_missing)
    failure == DictionaryFailure.Invalid -> stringResource(R.string.dictionary_invalid)
    else -> null
}
