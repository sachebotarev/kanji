package ru.mobui.kanji.feature.dashboard.builder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.feature.dashboard.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import ru.mobui.kanji.core.wordui.SelectionApplyButton
import ru.mobui.kanji.core.wordui.SymbolFacet
import ru.mobui.kanji.core.wordui.SymbolGridItem
import ru.mobui.kanji.core.wordui.WordPickerScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** Edits only a Words material. The owner commits it to the recipe draft, not the repository. */
@Composable
internal fun WordMaterialEditorScreen(
    initial: RecipeMaterial,
    words: List<CatalogWord>,
    kanjiEntries: List<SymbolGridItem>,
    kanjiByWord: Map<Int, Set<Int>>,
    language: String,
    onApply: (RecipeMaterial) -> Unit,
    onBack: () -> Unit,
    onWord: (Int) -> Unit,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    kanjiFacets: List<SymbolFacet> = emptyList(),
    typeSelector: (@Composable () -> Unit)? = null,
) {
    require(initial.type == MaterialType.Words && initial.id.isNotBlank())
    var ids by rememberSaveable(initial.id) { mutableStateOf(initial.wordIds.toIntArray()) }
    var page by rememberSaveable(initial.id) { mutableStateOf("form") }
    var invalid by rememberSaveable(initial.id) { mutableStateOf(false) }
    val chosen = ids.toSet()
    val selected = words.filter { it.id in chosen }
    val selectedIds = selected.map { it.id }.toSet()
    val up = { page = "form" }
    when (page) {
        "words" -> {
            WordPickerScreen(words, kanjiEntries, kanjiByWord, language, selectedIds,
                { ids = it.toIntArray(); invalid = false; page = "form" }, up, modifier,
                kanjiFacets, backHandler = { back -> BackHandler(onBack = back) })
            return
        }
        "preview" -> {
            MaterialWordsPreview(selected, language, onWord, up, modifier)
            return
        }
    }
    val dirty = ids.toSet() != initial.wordIds
    val leave = rememberMaterialLeave(dirty, onBack)
    val errorMessage = stringResource(R.string.material_words_required)
    Scaffold(modifier, floatingActionButton = {
        SelectionApplyButton(stringResource(if (editing) R.string.material_apply else R.string.material_add), {
            if (selectedIds.isEmpty()) invalid = true
            else onApply(RecipeMaterial(initial.id, MaterialType.Words, wordIds = selectedIds))
        }, Modifier.testTag("material-apply"))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MaterialHeader(stringResource(if (editing) R.string.material_edit_title else R.string.material_add_title), leave)
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                contentPadding = MaterialFormContentPadding,
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item { if (typeSelector != null) typeSelector() else Text(stringResource(R.string.material_words_type),
                    style = MaterialTheme.typography.titleMedium) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MaterialFiltersSection(
                            listOf(
                                MaterialFilterRow(
                                    stringResource(R.string.material_choose_words),
                                    pluralStringResource(R.plurals.material_words_selected, selected.count(), selected.count()),
                                    { page = "words" },
                                    Modifier.testTag("material-words").semantics { if (invalid) error(errorMessage) },
                                    invalid,
                                ),
                            ),
                        )
                        if (invalid) MaterialFieldError(errorMessage, Modifier.testTag("material-error"))
                        if (selected.isEmpty()) {
                            MaterialEmptyWordsNote(stringResource(R.string.material_words_none_selected))
                        } else {
                            MaterialWordsPreviewLink(selected.count(), { page = "preview" }, Modifier.testTag("material-preview"))
                        }
                    }
                }
            }
        }
    }
}

