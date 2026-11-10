package ru.mobui.kanji.feature.dictionary

import androidx.activity.compose.BackHandler
import ru.mobui.kanji.core.designsystem.KanjiDeleteConfirmDialog
import ru.mobui.kanji.core.designsystem.KanjiSearchToggle
import ru.mobui.kanji.core.wordui.WordDetailsActions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.core.designsystem.R as DesignR
import ru.mobui.kanji.core.wordui.WordDetailsContent
import ru.mobui.kanji.core.wordui.WordEditorActions
import ru.mobui.kanji.core.wordui.WordEditorForm
import ru.mobui.kanji.core.wordui.WordList
import ru.mobui.kanji.core.wordui.WordSearch
import ru.mobui.kanji.core.wordui.WordStudyItem
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.DictionaryFailure

@Composable
internal fun DictionaryHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = if (compact) 60.dp else 64.dp)
            .padding(start = if (compact) 12.dp else 8.dp, end = 12.dp,
            top = if (compact) 4.dp else 8.dp, bottom = if (compact) 4.dp else 8.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onBack, enabled = enabled, modifier = Modifier.size(48.dp).testTag("dictionary-back")) {
            Icon(
                painterResource(ru.mobui.kanji.core.designsystem.R.drawable.ic_arrow_back),
                stringResource(R.string.dictionary_back),
                Modifier.size(24.dp),
            )
        }
        Text(
            title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = if (compact) FontWeight.Normal else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        actions()
    }
}

@Composable
internal fun DictionaryStatus(
    loading: Boolean,
    error: Boolean,
    retry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (loading) CircularProgressIndicator()
        else if (error)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.dictionary_error))
                TextButton(retry, Modifier.testTag("dictionary-retry")) {
                    Text(stringResource(R.string.dictionary_retry))
                }
            }
    }
}

@Composable
internal fun DictionaryEmptyState(message: String, hint: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(bottom = 88.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(DesignR.drawable.mascot_sticker_peek_empty_box),
            contentDescription = null,
            modifier = Modifier.size(112.dp).testTag("dictionary-empty-mascot"),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(hint, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun DictionaryListScreen(
    state: DictionaryState,
    visible: List<CatalogWord>,
    language: String,
    actions: DictionaryListActions,
    onTags: () -> Unit = {},
    onClearTags: () -> Unit = {},
    onKanji: () -> Unit = {},
    onClearKanji: () -> Unit = {},
) {
    with(actions) {
    var searchOpen by rememberSaveable { mutableStateOf(state.query.isNotEmpty()) }
    BackHandler(enabled = searchOpen) {
        searchOpen = false
        onQuery("")
    }
    Scaffold(
        Modifier.testTag("dictionary-screen").background(MaterialTheme.colorScheme.background).safeDrawingPadding(),
        floatingActionButton = {
            if (!state.loading && !state.error)
                ExtendedFloatingActionButton(
                    onAdd,
                    Modifier.testTag("add-word"),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(painterResource(R.drawable.word_add), null, Modifier.size(24.dp))
                    Spacer(Modifier.size(10.dp))
                    Text(
                        stringResource(R.string.dictionary_add),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DictionaryHeader(stringResource(R.string.dictionary_title), onBack, compact = true) {
                KanjiSearchToggle(
                    open = searchOpen,
                    onToggle = {
                        searchOpen = !searchOpen
                        if (!searchOpen) onQuery("")
                    },
                    testTag = "dictionary-search-toggle",
                    contentDescription = stringResource(
                        if (searchOpen) R.string.dictionary_search_close else R.string.dictionary_search_toggle,
                    ),
                )
            }
            if (state.loading || state.error) DictionaryStatus(state.loading, state.error, retry)
            else {
                if (searchOpen) WordSearch(
                    state.query,
                    stringResource(ru.mobui.kanji.core.wordui.R.string.word_search),
                    onQuery,
                    Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        .testTag("dictionary-search"),
                )
                FlowRow(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp)
                    .testTag("dictionary-filters"), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val symbols = state.words.flatMap { state.snapshot?.literalKanji(it).orEmpty() }
                        .distinctBy { it.id }.filter { it.id in state.kanjiIds }.map { it.symbol }
                    val kanjiLabel = stringResource(ru.mobui.kanji.core.wordui.R.string.word_picker_kanji) +
                        if (symbols.isEmpty()) "" else " · " + symbols.take(2).joinToString("、") +
                            if (symbols.size > 2) " · " + stringResource(ru.mobui.kanji.core.wordui.R.string.tags_more, symbols.size - 2) else ""
                    ru.mobui.kanji.core.wordui.SelectionFilterChip(kanjiLabel, onKanji, if (state.kanjiIds.isEmpty()) null else onClearKanji,
                        stringResource(ru.mobui.kanji.core.wordui.R.string.selection_clear), Modifier.testTag("dictionary-kanji"))
                    val chosen = state.tags.filter { it.id in state.tagIds }
                    val label = stringResource(R.string.dictionary_tags) + if (chosen.isEmpty()) "" else " · " + chosen.take(2).joinToString { it.name(language)
                        } +
                        if (chosen.size > 2) " · " + stringResource(ru.mobui.kanji.core.wordui.R.string.tags_more, chosen.size - 2) else ""
                    ru.mobui.kanji.core.wordui.SelectionFilterChip(label, onTags, if (chosen.isEmpty()) null else onClearTags,
                        stringResource(R.string.dictionary_tags_clear), Modifier.testTag("dictionary-tags"))
                }
                if (visible.isNotEmpty()) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 18.dp)
                        .testTag("dictionary-count").semantics { liveRegion = LiveRegionMode.Polite },
                        contentAlignment = Alignment.CenterStart) {
                        Text(pluralStringResource(R.plurals.dictionary_count, visible.size, visible.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    WordList(
                        visible,
                        language,
                        onWord,
                        Modifier.weight(1f),
                        showOrigin = false,
                        bottomPadding = 112,
                        stacked = true,
                        footer = { word ->
                            val tags = state.tags.filter { it.id in state.wordTags[word.id].orEmpty() }
                            if (tags.isNotEmpty()) ru.mobui.kanji.core.wordui.TagChips(tags, language,
                                Modifier.padding(top = 8.dp), limit = 2, compact = true)
                        },
                    )
                } else {
                    DictionaryEmptyState(
                        message =
                            stringResource(
                                if (state.query.isBlank() && state.tagIds.isEmpty() && state.kanjiIds.isEmpty()) R.string.dictionary_empty_title
                                else R.string.dictionary_no_results,
                            ),
                        hint = stringResource(if (state.query.isBlank() && state.tagIds.isEmpty() && state.kanjiIds.isEmpty())
                            R.string.dictionary_empty_hint else R.string.dictionary_retry_hint),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        }
    }

    }
}

@Composable
internal fun DictionaryWordScreen(
    word: CatalogWord?,
    state: DictionaryState,
    language: String,
    actions: DictionaryWordActions,
) {
    with(actions) {
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val resources = androidx.compose.ui.platform.LocalResources.current
    Scaffold(Modifier.fillMaxSize().safeDrawingPadding(), floatingActionButton = {
        if (word?.isUserCreated == true && !state.busy && !state.loading)
            ExtendedFloatingActionButton(onEdit, Modifier.testTag("edit-word")) {
                Text(stringResource(R.string.dictionary_edit_action))
            }
    }) { padding ->
    Column(Modifier.fillMaxSize().padding(padding)) {
        DictionaryHeader(stringResource(R.string.dictionary_word), onBack, enabled = !state.busy) {
            if (word?.isUserCreated == true) {
                Box {
                    IconButton(
                        { menu = true },
                        enabled = !state.busy,
                        modifier = Modifier.testTag("word-actions"),
                    ) {
                        Icon(
                            painterResource(R.drawable.word_more),
                            stringResource(R.string.dictionary_more),
                        )
                    }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dictionary_delete_action)) },
                            onClick = {
                                menu = false
                                confirmDelete = true
                            },
                            modifier = Modifier.testTag("delete-word"),
                        )
                    }
                }
            }
        }
        if (state.loading || state.error) DictionaryStatus(state.loading, state.error, retry)
        else if (word == null)
            Text(stringResource(R.string.dictionary_missing), Modifier.padding(20.dp))
        else {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            operationError(state.operationError, state.storageError)?.let {
                Text(it, Modifier.padding(20.dp), color = MaterialTheme.colorScheme.error)
            }
            val snapshot = state.snapshot
            WordDetailsContent(word, language, snapshot?.literalKanji(word).orEmpty(), snapshot
                    ?.let {
                        wordStudyItems(it, word.id, language) { chapter ->
                            resources.getString(R.string.dictionary_chapter, chapter)
                        }
                    }
                    .orEmpty(), WordDetailsActions(onKanji, onStudy), showOrigin = false,
                    footer = { ru.mobui.kanji.core.wordui.TagChips(state.tags.filter { it.id in state.wordTags[word.id].orEmpty() }, language) },
                    bottomPadding = if (word.isUserCreated) 112.dp else 0.dp)
        }
    }
    }
    if (confirmDelete && word?.isUserCreated == true) {
        KanjiDeleteConfirmDialog(
            title = stringResource(R.string.dictionary_delete_title),
            message = stringResource(R.string.dictionary_delete_text, word.written),
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
            confirmTag = "confirm-delete-word",
            confirmText = stringResource(R.string.dictionary_delete),
            dismissText = stringResource(R.string.dictionary_cancel),
            enabled = !state.busy,
        )
    }

    }
}

@Composable
internal fun WordEditorScreen(
    state: EditorState,
    creating: Boolean,
    actions: WordEditorActions,
    onBack: () -> Unit,
    retry: () -> Unit,
    tags: List<ru.mobui.kanji.domain.catalog.DictionaryTag> = emptyList(),
    language: String = "ru",
    onTags: () -> Unit = {},
    onTranslations: () -> Unit = {},
    onCurrentTranslation: (String) -> Unit = {},
) {
    Scaffold(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .safeDrawingPadding().testTag("word-editor"), floatingActionButton = {
        if (!state.loading && !state.loadError && state.original != null && !state.busy)
            ExtendedFloatingActionButton(actions.save, Modifier.testTag("save-word"),
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                Icon(painterResource(if (creating) R.drawable.word_add else ru.mobui.kanji.core.wordui.R.drawable.selection_check),
                    null, Modifier.size(24.dp))
                Spacer(Modifier.size(10.dp))
                Text(
                    stringResource(if (creating) R.string.editor_add_to_dictionary else R.string.dictionary_save),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
    }) { padding ->
    Column(Modifier.fillMaxSize().padding(padding)) {
        DictionaryHeader(
            stringResource(if (creating) R.string.dictionary_new else R.string.dictionary_edit),
            onBack,
            enabled = !state.busy,
            compact = true,
        )
        if (state.loading || state.loadError)
            DictionaryStatus(state.loading, state.loadError, retry)
        else if (state.original == null)
            Text(
                operationError(state.failure, state.storageError)
                    ?: stringResource(R.string.dictionary_missing),
                Modifier.padding(20.dp),
            )
        else
            WordEditorForm(
                state.draft,
                state.invalid,
                state.busy,
                creating,
                actions,
                modifier = Modifier.clipToBounds(),
                error = operationError(state.failure, state.storageError),
                showSave = false,
                translationContent = {
                    val current = editorLanguage(language)
                    val translation = state.draft.translations.firstOrNull { it.languageTag.equals(current, true) }
                    ru.mobui.kanji.core.wordui.WordEditorTextField(
                        translation?.text.orEmpty(), onCurrentTranslation,
                        label = stringResource(ru.mobui.kanji.core.wordui.R.string.word_translation),
                        inputModifier = Modifier.testTag("editor-current-translation"),
                        enabled = !state.busy, minHeight = 94.dp,
                        invalid = ru.mobui.kanji.domain.catalog.WordField.Translations in state.invalid,
                        errorText = stringResource(ru.mobui.kanji.core.wordui.R.string.word_translations_error),
                    )
                    EditorNavigationRow(
                        stringResource(R.string.dictionary_other_translations, state.draft.translations.count { !it.languageTag.equals(current, true) }),
                        null, { if (!state.busy) onTranslations() }, Modifier.testTag("editor-other-translations"))
                },
                extraContent = {
                    EditorTagsRow(
                        tags = tags,
                        language = language,
                        enabled = !state.busy,
                        onClick = onTags,
                    )
                },
            )
    }
    }
}

@Composable
private fun EditorNavigationRow(title: String, subtitle: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ru.mobui.kanji.core.wordui.WordNavigationRow(title, subtitle, onClick,
        modifier.heightIn(min = 64.dp), compact = true,
        containerColor = MaterialTheme.colorScheme.background,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
        trailing = {
            Icon(
                painterResource(ru.mobui.kanji.core.designsystem.R.drawable.ic_chevron_right),
                null,
                Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        })
}

@Composable
private fun EditorTagsRow(
    tags: List<ru.mobui.kanji.domain.catalog.DictionaryTag>,
    language: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ru.mobui.kanji.core.wordui.WordNavigationRow(
        title = stringResource(R.string.dictionary_tags),
        subtitle = null,
        onClick = { if (enabled) onClick() },
        modifier = Modifier.heightIn(min = 64.dp).testTag("editor-tags"),
        compact = true,
        containerColor = MaterialTheme.colorScheme.background,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
        trailing = {
            Icon(
                painterResource(ru.mobui.kanji.core.designsystem.R.drawable.ic_chevron_right),
                null,
                Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        subtitleContent = {
            ru.mobui.kanji.core.wordui.TagInlineNames(
                tags = tags,
                language = language,
                empty = stringResource(R.string.editor_no_tags),
            )
        },
    )
}

@Composable
internal fun operationError(failure: DictionaryFailure?, storageError: Boolean): String? =
    when {
        storageError -> stringResource(R.string.dictionary_write_error)
        failure == DictionaryFailure.Duplicate -> stringResource(R.string.dictionary_duplicate)
        failure == DictionaryFailure.ReadOnly -> stringResource(R.string.dictionary_read_only)
        failure == DictionaryFailure.Missing -> stringResource(R.string.dictionary_missing)
        failure == DictionaryFailure.Invalid -> stringResource(R.string.dictionary_invalid)
        else -> null
    }
