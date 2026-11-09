package ru.mobui.kanji.feature.dictionary

import ru.mobui.kanji.core.designsystem.KanjiDeleteConfirmDialog
import ru.mobui.kanji.core.designsystem.inputShape
import ru.mobui.kanji.core.designsystem.kanjiOutlinedTextFieldColors

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ru.mobui.kanji.core.wordui.TagPickerScreen
import ru.mobui.kanji.core.wordui.TagPickerHeader
import ru.mobui.kanji.core.wordui.TagSwatch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.RecipesRepository
import ru.mobui.kanji.domain.catalog.TagColor
import ru.mobui.kanji.domain.catalog.TagDraft
import ru.mobui.kanji.domain.catalog.TagTranslation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
internal fun DictionaryTagsRoute(repository: RecipesRepository, sessionKey: String,
    selected: Set<Long>, pending: List<TagDraft>, privateDraft: Boolean, language: String,
    onApply: (Set<Long>, List<TagDraft>) -> Unit, onBack: () -> Unit, managementOnly: Boolean = false) {
    val model: TagSelectionViewModel = viewModel(key = "tags-$sessionKey", factory = viewModelFactory {
        initializer { TagSelectionViewModel(createSavedStateHandle(), repository) }
    })
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(sessionKey) { model.start(selected, pending); if (managementOnly) model.manage(true) }
    fun back() { if (!state.busy) { if (state.manage && !managementOnly) model.manage(false) else { model.finish(); onBack() } } }
    BackHandler { back() }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) } // 0 = create
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val staged = state.pending.mapIndexed { index, tag -> DictionaryTag(-index.toLong()-1, tag.color, tag.translations) }
    val all = state.tags + if (state.manage) emptyList() else staged
    TagPickerScreen(all, state.selected, language, state.query, model::query, model::toggle, model::clear,
        onApply = { if (state.manage) editId = 0L else {
            val ids = model.selectedIds(); val drafts = model.selectedPending(); model.finish(); onApply(ids, drafts)
        } }, header = { search ->
            TagPickerHeader(::back, search, manage = state.manage, busy = state.busy, onCreate = if (state.manage) null else ({ editId = 0L }),
                onManage = if (state.manage) null else ({ model.manage(true) }))
        }, manage = state.manage, busy = state.busy, loading = state.loading, error = state.error,
        onRetry = model::retry, onEdit = { editId = it }, usageCounts = state.usageCounts,
        onDelete = { deleteId = it })
    editId?.let { id ->
        val tag = state.tags.firstOrNull { it.id == id }
        val base = if (language.startsWith("en")) "en" else "ru"
        var name by rememberSaveable(id) {
            mutableStateOf(tag?.translations?.firstOrNull { it.languageTag.equals(base, true) }?.name.orEmpty())
        }
        var color by rememberSaveable(id) { mutableStateOf((tag?.color ?: TagColor.Neutral).key) }
        val colors = stringArrayResource(R.array.dictionary_tag_colors)
        AlertDialog(onDismissRequest = { if (!state.busy) editId = null },
            title = { Text(stringResource(if (id == 0L) R.string.dictionary_tags_create else R.string.dictionary_tags_rename)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.dictionary_tags_name)) }, enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().testTag("tag-name"), shape = MaterialTheme.inputShape, singleLine = true,
                    colors = kanjiOutlinedTextFieldColors())
                Text(stringResource(R.string.dictionary_tags_color), Modifier.padding(top = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.labelLarge)
                FlowRow {
                    TagColor.entries.forEachIndexed { index, option ->
                        TagSwatch(
                            color = option,
                            selected = color == option.key,
                            onClick = { color = option.key },
                            label = colors[index],
                            modifier = Modifier.testTag("tag-color-${option.key}"),
                            enabled = !state.busy,
                        )
                    }
                }
                if (state.error) Text(stringResource(R.string.dictionary_tags_error), color = MaterialTheme.colorScheme.error)
            } },
            confirmButton = { TextButton({
                val translations = tag?.translations.orEmpty().filterNot { it.languageTag.equals(base, true) } + TagTranslation(base, name)
                val draft = TagDraft(TagColor.entries.first { it.key == color }, translations)
                if (id == 0L) model.create(draft, privateDraft && !state.manage) { editId = null }
                else model.rename(id, draft) { editId = null }
            }, enabled = !state.busy && name.isNotBlank(), modifier = Modifier.testTag("tag-save")) {
                Text(stringResource(if (id == 0L) R.string.dictionary_tags_create else R.string.dictionary_save))
            } },
            dismissButton = { TextButton({ editId = null }, enabled = !state.busy) { Text(stringResource(R.string.dictionary_cancel)) } })
    }
    deleteId?.let { id ->
        KanjiDeleteConfirmDialog(
            title = stringResource(R.string.dictionary_tags_delete_title),
            message = stringResource(R.string.dictionary_tags_delete_text),
            onConfirm = { model.delete(id) { deleteId = null } },
            onDismiss = { if (!state.busy) deleteId = null },
            confirmTag = "tag-delete-confirm",
            confirmText = stringResource(R.string.dictionary_delete),
            dismissText = stringResource(R.string.dictionary_cancel),
            enabled = !state.busy,
        )
    }
}
