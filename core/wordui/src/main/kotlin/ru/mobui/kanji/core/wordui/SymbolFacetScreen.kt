package ru.mobui.kanji.core.wordui

import androidx.compose.runtime.CompositionLocalProvider
import ru.mobui.kanji.core.designsystem.LocalSearchOpen
import ru.mobui.kanji.core.designsystem.inputShape
import ru.mobui.kanji.core.designsystem.kanjiOutlinedTextFieldColors

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.text.Normalizer
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** Standalone facet draft, using the same rows/grid and controls as nested symbol facets. */
@Composable
fun FacetPickerScreen(
    facet: SymbolFacet,
    counts: Map<Int, Int>,
    selectedIds: Set<Int>,
    onApply: (Set<Int>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backTag: String = "symbol-picker-back",
    searchTag: String = "symbol-picker-search-toggle",
) {
    var draft by rememberSaveable { mutableStateOf(selectedIds.sorted().toIntArray()) }
    val selected = draft.toSet().intersect(facet.options.map { it.id }.toSet())
    SymbolFacetScreen(facet, emptyList(), selected,
        onSelect = { draft = it.sorted().toIntArray() }, onApply = { onApply(selected) }, onBack = onBack,
        header = { title, back, search -> SymbolPickerHeader(title, back, search, backTag = backTag, searchTag = searchTag) },
        modifier = modifier, counts = counts)
}

@Composable
internal fun SymbolFacetScreen(facet: SymbolFacet, candidates: List<SymbolGridItem>, selected: Set<Int>,
    onSelect: (Set<Int>) -> Unit, onApply: () -> Unit, onBack: () -> Unit,
    header: @Composable (String, () -> Unit, () -> Unit) -> Unit, modifier: Modifier = Modifier,
    counts: Map<Int, Int> = candidates.mapNotNull { facet.value(it) }.groupingBy { it }.eachCount()) {
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var selectedOnly by rememberSaveable { mutableStateOf(false) }
    var helpOpen by rememberSaveable { mutableStateOf(false) }
    fun normalized(text: String) = Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
    val shown = facet.options.filter { (facet.showZeroCounts || (counts[it.id] ?: 0) > 0 || it.id in selected) &&
        (!selectedOnly || it.id in selected) && normalized(it.label + it.symbol.orEmpty() + " " + it.id).contains(normalized(query.trim())) }
    val toggle: (Int) -> Unit = { id -> onSelect(if (id in selected) selected - id else selected + id) }
    if (helpOpen && facet.helpText != null) {
        ru.mobui.kanji.core.designsystem.KanjiInfoDialog(
            title = facet.title,
            message = facet.helpText,
            onDismiss = { helpOpen = false },
            buttonText = stringResource(R.string.selection_close),
            testTag = "facet-help-dialog",
        )
    }
    Scaffold(modifier, floatingActionButton = {
        SelectionApplyButton(if (selected.isEmpty()) stringResource(R.string.selection_apply)
            else stringResource(R.string.selection_choose, selected.count()), onApply,
            Modifier.testTag("facet-apply"), showCheck = true)
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            CompositionLocalProvider(LocalSearchOpen provides searchOpen) {
                header(facet.title, onBack) {
                    searchOpen = !searchOpen
                    if (!searchOpen) query = ""
                }
            }
            if (searchOpen) ru.mobui.kanji.core.designsystem.KanjiSearchBar(
                query = query,
                onQuery = { query = it },
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                placeholder = stringResource(R.string.selection_facet_search),
                autoFocus = true,
                testTag = "facet-search",
            )
            Row {
                TextButton({ selectedOnly = !selectedOnly }, Modifier.weight(1f).heightIn(min = 48.dp).testTag("facet-view")) {
                    Text(if (selectedOnly) stringResource(R.string.selection_all) else stringResource(R.string.selection_count, selected.count()))
                }
                TextButton({ onSelect(emptySet()) }, Modifier.weight(1f).heightIn(min = 48.dp).testTag("facet-clear"), enabled = selected.isNotEmpty()) {
                    Text(stringResource(R.string.selection_clear))
                }
            }
            if (facet.helpLabel != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton({ helpOpen = true }, Modifier.heightIn(min = 48.dp).testTag("facet-help")) { Text(facet.helpLabel) }
            }
            if (shown.isEmpty()) Text(stringResource(R.string.selection_empty), Modifier.padding(20.dp))
            else if (shown.all { it.symbol != null }) SymbolGrid(shown.map { SymbolGridItem(it.id, it.symbol!!, it.label) }, selected, toggle,
                Modifier.weight(1f), bottomPadding = 112.dp)
            else LazyColumn(Modifier.weight(1f), contentPadding = ru.mobui.kanji.core.designsystem.KanjiFormContentPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { option ->
                    WordNavigationRow(option.label, null, { toggle(option.id) }, Modifier.testTag("facet-option-${option.id}"),
                        selected = option.id in selected,
                        selectedLabel = "✓")
                }
            }
        }
    }
}
