package ru.mobui.kanji.core.wordui

import androidx.compose.runtime.CompositionLocalProvider
import ru.mobui.kanji.core.designsystem.KanjiEmptyState
import ru.mobui.kanji.core.designsystem.KanjiSearchToggle
import ru.mobui.kanji.core.designsystem.LocalSearchOpen

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.domain.catalog.DictionaryTag
import java.text.Normalizer
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** Controlled tag draft; only search visibility and selected-only presentation are local. */
@Composable
fun TagPickerScreen(tags: List<DictionaryTag>, selectedIds: Set<Long>, language: String,
    query: String, onQuery: (String) -> Unit, onToggle: (Long) -> Unit, onClear: () -> Unit,
    onApply: () -> Unit, header: @Composable (() -> Unit) -> Unit,
    modifier: Modifier = Modifier, manage: Boolean = false, busy: Boolean = false,
    loading: Boolean = false, error: Boolean = false, onRetry: () -> Unit = {}, onEdit: (Long) -> Unit = {},
    usageCounts: Map<Long, Int> = emptyMap(), onDelete: (Long) -> Unit = {},
    onCreate: (() -> Unit)? = null) {
    val handleCreate = onCreate ?: if (manage) onApply else null
    var search by rememberSaveable { mutableStateOf(false) }
    var selectedOnly by rememberSaveable(manage) { mutableStateOf(false) }
    fun normalize(value: String) = Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
    val needle = normalize(query.trim())
    val shown = tags.filter { (!selectedOnly || manage || it.id in selectedIds) && normalize(it.name(language)).contains(needle) }
    Scaffold(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .safeDrawingPadding().testTag("tag-picker"), floatingActionButton = {
        if (!loading && !busy && tags.isNotEmpty()) SelectionApplyButton(stringResource(
            if (manage) R.string.tag_picker_create else if (selectedIds.isEmpty()) R.string.tag_picker_apply
            else R.string.tag_picker_choose, selectedIds.count()), onApply, Modifier.testTag("tags-apply"))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            CompositionLocalProvider(LocalSearchOpen provides search) {
                header {
                    search = !search
                    if (!search) onQuery("")
                }
            }
            if (search) WordSearch(query, stringResource(R.string.tag_picker_search), onQuery,
                Modifier.padding(horizontal = 18.dp).testTag("tags-search"))
            if (!manage && tags.isNotEmpty()) FlowRow(Modifier.padding(horizontal = 8.dp)) {
                TextButton({ selectedOnly = !selectedOnly }, enabled = !busy, modifier = Modifier.testTag("tags-selected-only")) {
                    Text(stringResource(if (selectedOnly) R.string.tag_picker_all else R.string.tag_picker_selected, selectedIds.count()))
                }
                TextButton(onClear, enabled = selectedIds.isNotEmpty() && !busy, modifier = Modifier.testTag("tags-clear")) {
                    Text(stringResource(R.string.tag_picker_clear))
                }
            }
            if (loading) CircularProgressIndicator(Modifier.padding(20.dp))
            if (error) {
                Text(stringResource(R.string.tag_picker_error), Modifier.padding(20.dp).testTag("tag-error"), color = MaterialTheme.colorScheme.error)
                TextButton(onRetry) { Text(stringResource(R.string.tag_picker_retry)) }
            }
            if (!loading && shown.isEmpty()) {
                val isCompletelyEmpty = tags.isEmpty()
                val hasQuery = needle.isNotEmpty() || (selectedOnly && !manage)
                KanjiEmptyState(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    hasQuery = hasQuery,
                    title = stringResource(if (isCompletelyEmpty) R.string.tag_picker_none else R.string.tag_picker_no_results),
                    hint = stringResource(if (isCompletelyEmpty) R.string.tag_picker_empty_hint else R.string.tag_picker_no_results_hint),
                    ctaText = if (isCompletelyEmpty && handleCreate != null) stringResource(R.string.tag_picker_create_first) else null,
                    onCtaClick = if (isCompletelyEmpty) handleCreate else null,
                    testTag = "tags-empty-state",
                )
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = ru.mobui.kanji.core.designsystem.KanjiFormContentPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(shown, key = { it.id }) { tag ->
                        TagChoiceRow(tag, language, !manage && tag.id in selectedIds,
                            { if (manage) onEdit(tag.id) else onToggle(tag.id) }, enabled = !busy,
                            subtitle = pluralStringResource(R.plurals.tag_picker_word_count, usageCounts[tag.id] ?: 0, usageCounts[tag.id] ?: 0),
                            managing = manage, onDelete = if (manage) ({ onDelete(tag.id) }) else null)
                    }
                }
            }
        }
    }
}

@Composable
fun TagPickerHeader(
    onBack: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    manage: Boolean = false,
    busy: Boolean = false,
    searchOpen: Boolean = LocalSearchOpen.current,
    onCreate: (() -> Unit)? = null,
    onManage: (() -> Unit)? = null,
) {
    var menu by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        IconButton(onBack, Modifier.testTag("tags-back"), enabled = !busy) {
            Icon(
                painterResource(ru.mobui.kanji.core.designsystem.R.drawable.ic_arrow_back),
                stringResource(R.string.selection_back),
                Modifier.size(24.dp),
            )
        }
        Text(
            stringResource(if (manage) R.string.tag_picker_manage else R.string.tag_picker_title),
            Modifier.weight(1f).padding(horizontal = 8.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        KanjiSearchToggle(
            open = searchOpen,
            onToggle = onSearch,
            testTag = "tags-search-toggle",
            enabled = !busy,
        )
        if (onCreate != null || onManage != null) Box {
            IconButton({ menu = true }, Modifier.testTag("tags-menu"), enabled = !busy) {
                Icon(painterResource(R.drawable.selection_more), stringResource(R.string.tag_picker_more))
            }
            DropdownMenu(menu, { menu = false }) {
                if (onCreate != null) DropdownMenuItem({ Text(stringResource(R.string.tag_picker_create)) }, { menu = false; onCreate() })
                if (onManage != null) DropdownMenuItem({ Text(stringResource(R.string.tag_picker_manage)) }, { menu = false; onManage() },
                    modifier = Modifier.testTag("tags-manage"))
            }
        }
    }
}
