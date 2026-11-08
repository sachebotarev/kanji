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
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.feature.dashboard.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import ru.mobui.kanji.core.wordui.FacetPickerScreen
import ru.mobui.kanji.core.wordui.SelectionApplyButton
import ru.mobui.kanji.core.wordui.SymbolFacet
import ru.mobui.kanji.core.wordui.SymbolGridItem
import ru.mobui.kanji.core.wordui.SymbolPickerHeader
import ru.mobui.kanji.core.wordui.SymbolPickerScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ru.mobui.kanji.core.wordui.R as WordUiR

/** The owner supplies the complete catalogue + personal word pool and literal kanji links. */
@Composable
internal fun KanjiMaterialEditorScreen(
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
    kanjiPool: List<CatalogKanji> = emptyList(),
) {
    require(initial.type == MaterialType.Kanji && initial.id.isNotBlank())
    var ids by rememberSaveable(initial.id) { mutableStateOf(initial.kanjiIds.toIntArray()) }
    var jlpt by rememberSaveable(initial.id) { mutableStateOf(initial.jlptLevels.sorted().toIntArray()) }
    var page by rememberSaveable(initial.id) { mutableStateOf("form") }
    var invalid by rememberSaveable(initial.id) { mutableStateOf(false) }
    val jlptById = remember(kanjiPool) {
        kanjiPool.associate { it.id to (it.jlptLevel?.number ?: -1) }
    }
    val jlptSelected = jlpt.toSet()
    fun matchesJlpt(kanjiId: Int): Boolean =
        jlptSelected.isEmpty() || (jlptById[kanjiId] ?: -1) in jlptSelected
    val scopedEntries = kanjiEntries.filter { matchesJlpt(it.id) }
    val eligible = scopedEntries.map { it.id }.toSet()
    val selected = ids.toSet().intersect(eligible)
    val pool = words.distinctBy { it.id }.filter { word ->
        val linked = kanjiByWord[word.id].orEmpty()
        val jlptOk = jlptSelected.isEmpty() || linked.any { matchesJlpt(it) }
        val kanjiOk = selected.isEmpty() || linked.any { it in selected }
        jlptOk && kanjiOk
    }
    val jlptFacet = kanjiFacets.firstOrNull { it.key == "jlpt" }
    val up = { page = "form" }
    if (page == "jlpt" && jlptFacet != null) {
        val counts = remember(scopedEntries, jlptById, kanjiEntries) {
            kanjiEntries.map { jlptById[it.id] ?: -1 }.groupingBy { it }.eachCount()
        }
        BackHandler(onBack = up)
        FacetPickerScreen(
            jlptFacet,
            counts,
            jlptSelected,
            onApply = { chosen ->
                jlpt = chosen.sorted().toIntArray()
                ids = ids.filter { matchesJlptFor(it, chosen, jlptById) }.toIntArray()
                invalid = false
                page = "form"
            },
            onBack = up,
            modifier = modifier,
            backTag = "material-back",
            searchTag = "material-jlpt-search-toggle",
        )
        return
    }
    if (page == "kanji") {
        val title = stringResource(R.string.material_kanji_title)
        SymbolPickerScreen(scopedEntries, selected, {
            ids = it.toIntArray(); invalid = false; page = "form"
        }, { search -> SymbolPickerHeader(title, up, search) }, modifier,
            facets = kanjiFacets, onBack = up, backHandler = { back -> BackHandler(onBack = back) },
            facetHeader = { name, back, search -> SymbolPickerHeader(name, back, search) })
        return
    }
    if (page == "preview") {
        MaterialWordsPreview(pool, language, onWord, up, modifier)
        return
    }
    val dirty = ids.toSet() != initial.kanjiIds || jlptSelected != initial.jlptLevels
    val leave = rememberMaterialLeave(dirty, onBack)
    val errorMessage = stringResource(R.string.material_empty_pool)
    val symbols = scopedEntries.filter { it.id in selected }.sortedBy { it.id }
    val kanjiSummary = if (selected.isEmpty()) stringResource(R.string.material_all_kanji)
        else symbols.take(3).joinToString(" · ") { it.symbol } +
            if (symbols.size > 3) " · " + stringResource(R.string.material_more, symbols.size - 3) else ""
    val noJlptLabel = stringResource(WordUiR.string.selection_no_jlpt)
    val jlptSummary = if (jlptSelected.isEmpty()) stringResource(R.string.material_all_jlpt)
        else jlptSelected.sortedDescending().joinToString(" · ") { level ->
            if (level < 0) noJlptLabel else "N$level"
        }
    Scaffold(modifier, floatingActionButton = {
        SelectionApplyButton(stringResource(if (editing) R.string.material_apply else R.string.material_add), {
            if (pool.isEmpty()) invalid = true
            else onApply(
                RecipeMaterial(
                    initial.id,
                    MaterialType.Kanji,
                    kanjiIds = selected,
                    jlptLevels = jlptSelected,
                ),
            )
        }, Modifier.testTag("material-apply"))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MaterialHeader(stringResource(if (editing) R.string.material_edit_title else R.string.material_add_title), leave)
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                contentPadding = MaterialFormContentPadding,
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item { if (typeSelector != null) typeSelector() else Text(stringResource(R.string.material_kanji_type),
                    style = MaterialTheme.typography.titleMedium) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MaterialFiltersSection(
                            listOfNotNull(
                                if (jlptFacet != null) MaterialFilterRow(
                                    stringResource(WordUiR.string.selection_jlpt), jlptSummary, { page = "jlpt" },
                                    Modifier.testTag("material-jlpt"),
                                ) else null,
                                MaterialFilterRow(
                                    stringResource(R.string.material_kanji_title), kanjiSummary, { page = "kanji" },
                                    Modifier.testTag("material-kanji").semantics { if (invalid) error(errorMessage) },
                                    invalid,
                                ),
                            ),
                        )
                        if (invalid) MaterialFieldError(errorMessage, Modifier.testTag("material-error"))
                        if (pool.isEmpty()) MaterialEmptyWords()
                        if (pool.isNotEmpty()) {
                            MaterialWordsPreviewLink(pool.count(), { page = "preview" }, Modifier.testTag("material-preview"))
                        }
                    }
                }
            }
        }
    }
}

private fun matchesJlptFor(kanjiId: Int, selected: Set<Int>, jlptById: Map<Int, Int>): Boolean =
    selected.isEmpty() || (jlptById[kanjiId] ?: -1) in selected

@androidx.compose.ui.tooling.preview.Preview(name = "Русский · крупный текст", locale = "ru", fontScale = 1.5f, showBackground = true)
@androidx.compose.ui.tooling.preview.Preview(name = "English · dark", locale = "en", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true)
@Composable
private fun KanjiMaterialPreview() {
    val language = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language
    val sun = '日'.code
    val pool = listOf(
        CatalogKanji(sun, "日", 4, null, true, true, false,
            mapOf("ru" to listOf("солнце"), "en" to listOf("sun")), listOf("ニチ"), listOf("ひ"),
            jlptLevel = ru.mobui.kanji.domain.catalog.JlptLevel.N5),
    )
    ru.mobui.kanji.core.designsystem.KanjiTheme {
        KanjiMaterialEditorScreen(RecipeMaterial("preview", MaterialType.Kanji, kanjiIds = setOf(sun)),
            listOf(CatalogWord(1, "日", "ひ", mapOf("ru" to "солнце", "en" to "sun"))),
            listOf(SymbolGridItem(sun, "日", "日")), mapOf(1 to setOf(sun)), language, {}, {}, {},
            kanjiFacets = ru.mobui.kanji.core.wordui.kanjiSymbolFacets(pool, emptyList(), language),
            kanjiPool = pool)
    }
}
