package ru.mobui.kanji.core.wordui

import ru.mobui.kanji.core.designsystem.inputShape
import ru.mobui.kanji.core.designsystem.kanjiOutlinedTextFieldColors

import android.os.Bundle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.text.Normalizer
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Transient selection only. The caller owns the transaction and Back navigation. */
@Composable
fun SymbolPickerScreen(entries: List<SymbolGridItem>, initialIds: Set<Int>, onApply: (Set<Int>) -> Unit,
    header: @Composable (() -> Unit) -> Unit, modifier: Modifier = Modifier,
    filters: @Composable () -> Unit = {}, searchText: (SymbolGridItem) -> String = { it.description },
    facets: List<SymbolFacet> = emptyList(), onBack: () -> Unit = {},
    backHandler: @Composable (() -> Unit) -> Unit = {},
    facetHeader: @Composable (String, () -> Unit, () -> Unit) -> Unit = { title, back, _ ->
        Row { TextButton(back) { Text(stringResource(R.string.selection_back)) }; Text(title) }
    }) {
    var ids by rememberSaveable { mutableStateOf(initialIds.toIntArray()) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var selectedOnly by rememberSaveable { mutableStateOf(false) }
    var facetValues by rememberSaveable { mutableStateOf(Bundle()) }
    var facetKey by rememberSaveable { mutableStateOf<String?>(null) }
    var facetDraft by rememberSaveable { mutableStateOf(intArrayOf()) }
    val selections: Map<String, Set<Int>> = facets.associate { it.key to (facetValues.getIntArray(it.key) ?: intArrayOf()).toSet() }
    val activeFacet = facets.firstOrNull { it.key == facetKey }
    val back = { if (activeFacet != null) facetKey = null else onBack() }
    backHandler(back)
    if (activeFacet != null) {
        key(activeFacet.key) {
            SymbolFacetScreen(activeFacet, entries.filter { matchesSymbolFacets(it, facets, selections, activeFacet.key) },
                facetDraft.toSet(), { facetDraft = it.toIntArray() }, {
                    facetValues = Bundle(facetValues).apply { putIntArray(activeFacet.key, facetDraft) }
                    facetKey = null
                }, back, facetHeader, modifier)
        }
        return
    }
    val eligible = entries.map { it.id }.toSet()
    val selected = ids.toSet().intersect(eligible)
    val normalizedQuery = normalizeSymbolSearch(query.trim())
    val shown = entries.filter { (!selectedOnly || it.id in selected) &&
        matchesSymbolFacets(it, facets, selections) && normalizeSymbolSearch(searchText(it)).contains(normalizedQuery) }
    Scaffold(modifier, floatingActionButton = {
        SelectionApplyButton(if (selected.isEmpty()) stringResource(R.string.selection_apply)
            else stringResource(R.string.selection_choose, selected.count()), { onApply(selected) },
            Modifier.testTag("symbol-picker-apply"), showCheck = true)
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            header { searchOpen = !searchOpen }
            if (searchOpen) OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 18.dp).testTag("symbol-picker-search"),
                label = { Text(stringResource(R.string.selection_search)) }, shape = MaterialTheme.inputShape, singleLine = true,
                colors = kanjiOutlinedTextFieldColors())
            filters()
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                facets.forEach { facet ->
                    val values = selections.getValue(facet.key)
                    val chosen = facet.options.filter { it.id in values }
                    val label = if (chosen.isEmpty()) facet.emptyLabel else facet.title + " · " + chosen.take(2).joinToString { it.chipLabel } +
                        if (chosen.size > 2) " +${chosen.size - 2}" else ""
                    SelectionFilterChip(label, {
                        facetDraft = values.toIntArray(); facetKey = facet.key
                    }, if (values.isEmpty()) null else ({ facetValues = Bundle(facetValues).apply { remove(facet.key) } }),
                        stringResource(R.string.selection_clear), Modifier.testTag("facet-${facet.key}"))
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton({ selectedOnly = !selectedOnly }, Modifier.weight(1f).heightIn(min = 48.dp).testTag("symbol-picker-view")) {
                    Text(if (selectedOnly) stringResource(R.string.selection_all) else stringResource(R.string.selection_count, selected.count()))
                }
                TextButton({ ids = intArrayOf() }, Modifier.weight(1f).heightIn(min = 48.dp).testTag("symbol-picker-clear"), enabled = selected.isNotEmpty()) {
                    Text(stringResource(R.string.selection_clear))
                }
            }
            if (shown.isEmpty()) Text(stringResource(R.string.selection_empty), Modifier.padding(20.dp))
            else SymbolGrid(shown, selected, { id -> ids = (if (id in selected) selected - id else selected + id).toIntArray() },
                Modifier.weight(1f), bottomPadding = 112.dp)
        }
    }
}

private fun normalizeSymbolSearch(value: String) = Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase(Locale.ROOT)

@androidx.compose.ui.tooling.preview.Preview(locale = "ru", widthDp = 360, fontScale = 1.6f)
@androidx.compose.ui.tooling.preview.Preview(locale = "en", widthDp = 412, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SymbolPickerPreview() {
    ru.mobui.kanji.core.designsystem.KanjiTheme {
        SymbolPickerScreen(listOf(SymbolGridItem(1, "日", "日"), SymbolGridItem(2, "月", "月")), setOf(1), {},
            header = { Text(stringResource(R.string.word_kanji), Modifier.padding(20.dp)) },
            facets = listOf(SymbolFacet("strokes", stringResource(R.string.selection_strokes), listOf(SymbolFacetOption(4, "4"))) { 4 }))
    }
}
