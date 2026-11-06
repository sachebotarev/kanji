package ru.mobui.kanji.core.wordui

import ru.mobui.kanji.core.designsystem.inputShape
import ru.mobui.kanji.core.designsystem.kanjiOutlinedTextFieldColors
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.text.Normalizer

/** Localized display data, independent of repositories and navigation. */
data class ChapterPickerItem(val number: Int, val title: String, val summary: String, val preview: String)

/** A draft is published only by Apply; the caller owns Back and screen lifetime. */
@Composable
fun ChapterPickerScreen(
    entries: List<ChapterPickerItem>,
    selectedIds: Set<Int>,
    onApply: (Set<Int>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    backTag: String = "symbol-picker-back",
    searchTag: String = "symbol-picker-search-toggle",
) {
    val focus = LocalFocusManager.current
    var selected by rememberSaveable { mutableStateOf(selectedIds.sorted().toIntArray()) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var selectedOnly by rememberSaveable { mutableStateOf(false) }
    val eligible = entries.map { it.number }.toSet()
    val chosen = selected.toSet().intersect(eligible)
    val number = Normalizer.normalize(query.trim(), Normalizer.Form.NFKC).toIntOrNull()
    val visible = entries.filter {
        (!selectedOnly || it.number in chosen) && (query.isBlank() || it.number == number)
    }
    Scaffold(modifier, floatingActionButton = {
        SelectionApplyButton(if (chosen.isEmpty()) stringResource(R.string.selection_apply)
            else stringResource(R.string.selection_choose, chosen.size), { onApply(chosen) },
            Modifier.testTag("chapter-apply"))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SymbolPickerHeader(title ?: stringResource(R.string.selection_chapters), onBack, { searchOpen = !searchOpen },
                backTag = backTag, searchTag = searchTag)
            if (searchOpen) OutlinedTextField(
                query, { query = it },
                Modifier.fillMaxWidth().padding(horizontal = 18.dp).testTag("chapter-search"),
                label = { Text(stringResource(R.string.selection_chapter_search)) }, shape = MaterialTheme.inputShape, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                colors = kanjiOutlinedTextFieldColors(),
            )
            Row {
                TextButton({ selectedOnly = !selectedOnly }, Modifier.weight(1f).heightIn(min = 48.dp).testTag("chapter-view")) {
                    Text(if (selectedOnly) stringResource(R.string.selection_all)
                        else stringResource(R.string.selection_count, chosen.size))
                }
                TextButton({ selected = intArrayOf() }, Modifier.weight(1f).heightIn(min = 48.dp).testTag("chapter-clear"), enabled = chosen.isNotEmpty()) {
                    Text(stringResource(R.string.selection_clear))
                }
            }
            LazyColumn(
                Modifier.weight(1f).testTag("chapter-picker-list"),
                contentPadding = ru.mobui.kanji.core.designsystem.KanjiFormContentPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (visible.isEmpty()) item { Text(stringResource(R.string.selection_empty), Modifier.padding(vertical = 20.dp)) }
                items(visible, key = { it.number }) { chapter ->
                    ChapterChoiceCard(chapter.title, chapter.summary, chapter.preview, chapter.number in chosen,
                        onClick = {
                            selected = (if (chapter.number in chosen) chosen - chapter.number else chosen + chapter.number).sorted().toIntArray()
                        }, modifier = Modifier.testTag("chapter-${chapter.number}"))
                }
            }
        }
    }
}
