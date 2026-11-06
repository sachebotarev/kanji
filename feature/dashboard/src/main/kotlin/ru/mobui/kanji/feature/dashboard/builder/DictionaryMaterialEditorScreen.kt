package ru.mobui.kanji.feature.dashboard.builder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import ru.mobui.kanji.core.wordui.SelectionApplyButton
import ru.mobui.kanji.core.wordui.SymbolGridItem
import ru.mobui.kanji.core.wordui.SymbolPickerHeader
import ru.mobui.kanji.core.wordui.SymbolPickerScreen
import ru.mobui.kanji.core.wordui.TagPickerHeader
import ru.mobui.kanji.core.wordui.TagPickerScreen
import ru.mobui.kanji.core.wordui.kanjiSymbolFacets
import ru.mobui.kanji.domain.catalog.CatalogSelection
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.WordTagAssignment
import androidx.compose.runtime.setValue
import ru.mobui.kanji.domain.catalog.meaning
import ru.mobui.kanji.domain.catalog.localized

/** Local material draft. Global tag management and word navigation belong to the owner. */
@Composable
internal fun DictionaryMaterialEditorScreen(initial: RecipeMaterial, snapshot: CatalogSnapshot,
    tags: List<DictionaryTag>, assignments: List<WordTagAssignment>, language: String,
    onApply: (RecipeMaterial) -> Unit, onBack: () -> Unit, onWord: (Int) -> Unit,
    onManageTags: () -> Unit, modifier: Modifier = Modifier, editing: Boolean = false,
    typeSelector: (@Composable () -> Unit)? = null) {
    require(initial.type == MaterialType.Dictionary && initial.id.isNotBlank())
    var tagIds by rememberSaveable(initial.id) { mutableStateOf(initial.tagIds.toLongArray()) }
    var kanjiIds by rememberSaveable(initial.id) { mutableStateOf(initial.kanjiIds.toIntArray()) }
    var page by rememberSaveable(initial.id) { mutableStateOf("form") }
    var invalid by rememberSaveable(initial.id) { mutableStateOf(false) }
    val source = snapshot.sources.singleOrNull { it.sourceType == "personal" }
    val personal = source?.let { snapshot.words(CatalogSelection(sourceId = it.id), "", language) }.orEmpty()
    val chosenTags = tagIds.toSet().intersect(tags.map { it.id }.toSet())
    val byWord = assignments.associate { it.wordId to it.tagIds }
    val tagged = personal.filter { chosenTags.isEmpty() || byWord[it.id].orEmpty().any { id -> id in chosenTags } }
    val symbols = tagged.flatMap { snapshot.literalKanji(it) }.distinctBy { it.id }
    val chosenKanji = kanjiIds.toSet().intersect(symbols.map { it.id }.toSet())
    val pool = tagged.filter { chosenKanji.isEmpty() || snapshot.literalKanji(it).any { symbol -> symbol.id in chosenKanji } }
    val up = { page = "form" }
    if (page == "tags") {
        var draft by rememberSaveable { mutableStateOf(chosenTags.toLongArray()) }
        var query by rememberSaveable { mutableStateOf("") }
        val selected = draft.toSet().intersect(tags.map { it.id }.toSet())
        BackHandler(onBack = up)
        TagPickerScreen(tags, selected, language, query, { query = it }, { id ->
            draft = (if (id in selected) selected - id else selected + id).toLongArray()
        }, { draft = longArrayOf() }, onApply = {
            tagIds = selected.toLongArray()
            val words = personal.filter { selected.isEmpty() || byWord[it.id].orEmpty().any { id -> id in selected } }
            kanjiIds = chosenKanji.intersect(words.flatMap { snapshot.literalKanji(it) }.map { it.id }.toSet()).toIntArray()
            invalid = false; up()
        }, header = { search -> TagPickerHeader(up, search, onManage = onManageTags) }, modifier = modifier,
            usageCounts = byWord.values.flatMap { it }.groupingBy { it }.eachCount())
        return
    }
    if (page == "kanji") {
        val missing = stringResource(ru.mobui.kanji.core.wordui.R.string.word_missing)
        val items = symbols.map { symbol ->
            val meaning = symbol.meaning(language) ?: missing
            SymbolGridItem(symbol.id, symbol.symbol, symbol.meanings.localized(language)?.firstOrNull() ?: meaning,
                symbol.symbol + ", " + meaning)
        }
        val byId = symbols.associateBy { it.id }
        SymbolPickerScreen(items, chosenKanji, { kanjiIds = it.toIntArray(); invalid = false; up() },
            { search -> SymbolPickerHeader(stringResource(R.string.material_kanji_title), up, search) }, modifier,
            facets = kanjiSymbolFacets(symbols, snapshot.radicals, language), onBack = up,
            backHandler = { back -> BackHandler(onBack = back) },
            facetHeader = { title, back, search -> SymbolPickerHeader(title, back, search) },
            searchText = { item -> val symbol = byId.getValue(item.id)
                item.description + " " + symbol.on.joinToString(" ") + " " + symbol.kun.joinToString(" ") })
        return
    }
    if (page == "preview") {
        MaterialWordsPreview(pool, language, onWord, up, modifier)
        return
    }
    val dirty = tagIds.toSet() != initial.tagIds || kanjiIds.toSet() != initial.kanjiIds
    val leave = rememberMaterialLeave(dirty, onBack)
    val errorMessage = stringResource(if (source == null) R.string.material_dictionary_missing else R.string.material_empty_pool)
    val selectedTags = tags.filter { it.id in chosenTags }
    val tagSummary = if (selectedTags.isEmpty()) stringResource(R.string.material_all_tags)
        else selectedTags.take(3).joinToString(" · ") { it.name(language) } +
            if (selectedTags.size > 3) " · " + stringResource(R.string.material_more, selectedTags.size - 3) else ""
    val selectedSymbols = symbols.filter { it.id in chosenKanji }.sortedBy { it.id }
    val kanjiSummary = if (selectedSymbols.isEmpty()) stringResource(R.string.material_all_kanji)
        else selectedSymbols.take(3).joinToString(" · ") { it.symbol } +
            if (selectedSymbols.size > 3) " · " + stringResource(R.string.material_more, selectedSymbols.size - 3) else ""
    Scaffold(modifier, floatingActionButton = {
        SelectionApplyButton(stringResource(if (editing) R.string.material_apply else R.string.material_add), {
            if (source == null || pool.isEmpty()) invalid = true
            else onApply(RecipeMaterial(initial.id, MaterialType.Dictionary, sourceId = source.id,
                kanjiIds = chosenKanji, tagIds = chosenTags))
        }, Modifier.testTag("material-apply"))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MaterialHeader(stringResource(if (editing) R.string.material_edit_title else R.string.material_add_title), leave)
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = MaterialFormContentPadding,
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item { if (typeSelector != null) typeSelector() else Text(stringResource(R.string.material_dictionary_type),
                    style = MaterialTheme.typography.titleMedium) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MaterialFiltersSection(
                            listOf(
                                MaterialFilterRow(
                                    stringResource(R.string.material_kanji_title), kanjiSummary, { page = "kanji" },
                                    Modifier.testTag("material-kanji"), invalid,
                                ),
                                MaterialFilterRow(
                                    stringResource(R.string.material_tags_title), tagSummary, { page = "tags" },
                                    Modifier.testTag("material-tags").semantics { if (invalid) error(errorMessage) },
                                    invalid,
                                ),
                            ),
                        )
                        if (invalid) MaterialFieldError(errorMessage, Modifier.testTag("material-error"))
                        if (pool.isEmpty()) {
                            MaterialEmptyWordsNote(
                                stringResource(
                                    when {
                                        personal.isEmpty() -> R.string.material_dictionary_empty
                                        else -> R.string.material_dictionary_no_match
                                    },
                                ),
                            )
                        } else {
                            MaterialWordsPreviewLink(pool.count(), { page = "preview" }, Modifier.testTag("material-preview"))
                        }
                    }
                }
            }
        }
    }
}
