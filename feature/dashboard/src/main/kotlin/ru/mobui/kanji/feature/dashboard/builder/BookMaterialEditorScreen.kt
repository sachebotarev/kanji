package ru.mobui.kanji.feature.dashboard.builder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import ru.mobui.kanji.core.wordui.ChapterPickerItem
import ru.mobui.kanji.core.wordui.ChapterPickerScreen
import ru.mobui.kanji.core.wordui.SelectionApplyButton
import ru.mobui.kanji.core.wordui.SymbolGridItem
import ru.mobui.kanji.core.wordui.SymbolPickerHeader
import ru.mobui.kanji.core.wordui.SymbolPickerScreen
import ru.mobui.kanji.core.wordui.kanjiSymbolFacets
import ru.mobui.kanji.domain.catalog.CatalogSelection
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import ru.mobui.kanji.domain.catalog.title
import ru.mobui.kanji.domain.catalog.meaning

/** A local Book draft. Only actual study links in the selected textbook produce words. */
@Composable
internal fun BookMaterialEditorScreen(
    initial: RecipeMaterial,
    snapshot: CatalogSnapshot,
    language: String,
    onApply: (RecipeMaterial) -> Unit,
    onBack: () -> Unit,
    onWord: (Int) -> Unit,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    typeSelector: (@Composable () -> Unit)? = null,
) {
    require(initial.type == MaterialType.Book && initial.id.isNotBlank())
    val books = snapshot.sources.filter { it.sourceType in setOf("book", "textbook") }
    var sourceId by rememberSaveable(initial.id) {
        mutableStateOf(initial.sourceId ?: if (!editing) books.firstOrNull()?.id else null)
    }
    var chapters by rememberSaveable(initial.id) { mutableStateOf(initial.chapters.toIntArray()) }
    var kanji by rememberSaveable(initial.id) { mutableStateOf(initial.kanjiIds.toIntArray()) }
    var page by rememberSaveable(initial.id) { mutableStateOf("form") }
    var invalid by rememberSaveable(initial.id) { mutableStateOf(false) }
    val source = books.firstOrNull { it.id == sourceId }
    val placements = snapshot.placements.filter { it.sourceId == source?.id }
    val chapterIds = chapters.toSet().intersect(placements.map { it.chapter }.toSet())
    val range = placements.filter { chapterIds.isEmpty() || it.chapter in chapterIds }.map { it.kanjiId }.toSet()
    val kanjiIds = kanji.toSet().intersect(range)
    val selection = CatalogSelection(sourceId = source?.id, chapterIds = chapterIds, kanjiIds = kanjiIds)
    val pool = if (source == null) emptyList() else snapshot.words(selection, "", language)
    val up = { page = "form" }
    if (page == "chapters" && source != null) {
        BackHandler(onBack = up)
        val available = snapshot.chapters(source.id).filter { chapter ->
            kanjiIds.isEmpty() || placements.any { it.chapter == chapter.number && it.kanjiId in kanjiIds }
        }
        val entries = available.map { chapter -> ChapterPickerItem(chapter.number,
            stringResource(R.string.material_chapter, chapter.number),
            pluralStringResource(R.plurals.material_kanji_count, chapter.kanjiCount, chapter.kanjiCount) + " · " +
                pluralStringResource(R.plurals.material_word_count, chapter.wordCount, chapter.wordCount), chapter.preview) }
        val chaptersTitle = stringResource(
            R.string.material_select_context,
            stringResource(R.string.material_chapters),
            source.title(language),
        )
        ChapterPickerScreen(entries, chapterIds, { chosen ->
            chapters = chosen.toIntArray()
            val allowed = placements.filter { chosen.isEmpty() || it.chapter in chosen }.map { it.kanjiId }.toSet()
            kanji = kanjiIds.intersect(allowed).toIntArray()
            invalid = false; page = "form"
        }, up, modifier, title = chaptersTitle)
        return
    }
    if (page == "kanji" && source != null) {
        val symbols = snapshot.kanji(selection.copy(kanjiIds = emptySet()), "", language)
        val entries = symbols.map { SymbolGridItem(it.id, it.symbol, it.meaning(language).orEmpty(),
            listOf(it.symbol, it.meaning(language).orEmpty(), it.on.joinToString(" "), it.kun.joinToString(" ")).joinToString(" ")) }
        val title = stringResource(
            R.string.material_select_context,
            stringResource(R.string.material_kanji_title),
            source.title(language),
        )
        SymbolPickerScreen(entries, kanjiIds, { chosen ->
            kanji = chosen.toIntArray()
            val allowed = placements.filter { chosen.isEmpty() || it.kanjiId in chosen }.map { it.chapter }.toSet()
            chapters = chapterIds.intersect(allowed).toIntArray()
            invalid = false; page = "form"
        }, { search -> SymbolPickerHeader(title, up, search) }, modifier,
            facets = kanjiSymbolFacets(symbols, snapshot.radicals, language), onBack = up,
            backHandler = { back -> BackHandler(onBack = back) },
            facetHeader = { titleText, back, search -> SymbolPickerHeader(titleText, back, search) })
        return
    }
    if (page == "preview") {
        MaterialWordsPreview(pool, language, onWord, up, modifier)
        return
    }
    val baselineSource = remember(initial.id) {
        initial.sourceId ?: if (!editing) books.firstOrNull()?.id else null
    }
    val dirty = sourceId != baselineSource ||
        chapters.toSet() != initial.chapters ||
        kanji.toSet() != initial.kanjiIds
    val leave = rememberMaterialLeave(dirty, onBack)
    val errorMessage = stringResource(if (source == null) R.string.material_choose_book else R.string.material_empty_book_pool)
    val chapterSummary = if (chapterIds.isEmpty()) stringResource(R.string.material_all_chapters)
        else chapterIds.sorted().take(3).joinToString(" · ") +
            if (chapterIds.count() > 3) " · " + stringResource(R.string.material_more, chapterIds.count() - 3) else ""
    val kanjiSummary = if (kanjiIds.isEmpty()) stringResource(R.string.material_all_kanji)
        else kanjiIds.sorted().take(3).joinToString(" · ") { snapshot.kanjiById[it]?.symbol.orEmpty() } +
            if (kanjiIds.count() > 3) " · " + stringResource(R.string.material_more, kanjiIds.count() - 3) else ""
    Scaffold(modifier, floatingActionButton = {
        SelectionApplyButton(stringResource(if (editing) R.string.material_apply else R.string.material_add), {
            if (source == null || pool.isEmpty()) invalid = true
            else onApply(RecipeMaterial(initial.id, MaterialType.Book, sourceId = source.id,
                chapters = chapterIds, kanjiIds = kanjiIds))
        }, Modifier.testTag("material-apply"))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MaterialHeader(stringResource(if (editing) R.string.material_edit_title else R.string.material_add_title), leave)
            LazyColumn(Modifier.weight(1f).fillMaxWidth().clipToBounds(),
                contentPadding = MaterialFormContentPadding,
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item { if (typeSelector != null) typeSelector() else Text(stringResource(R.string.material_book_type),
                    style = MaterialTheme.typography.titleMedium) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MaterialSectionTitle(stringResource(R.string.material_book_title))
                        Column(Modifier.selectableGroup().testTag("material-book").semantics {
                            if (invalid && source == null) error(errorMessage)
                        }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            books.forEach { book ->
                                MaterialBookChoice(book, language, book.id == sourceId, {
                                    if (sourceId != book.id) {
                                        sourceId = book.id
                                        chapters = intArrayOf()
                                        val allowed = snapshot.placements.filter { it.sourceId == book.id }.map { it.kanjiId }.toSet()
                                        kanji = kanji.toSet().intersect(allowed).toIntArray()
                                    }
                                    invalid = false
                                }, Modifier.testTag("material-book-${book.id}"))
                            }
                        }
                        if (invalid && source == null) {
                            MaterialFieldError(errorMessage, Modifier.padding(top = 4.dp).testTag("material-error"))
                        }
                    }
                }
                if (source != null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MaterialFiltersSection(
                                listOf(
                                    MaterialFilterRow(
                                        stringResource(R.string.material_chapters), chapterSummary, { page = "chapters" },
                                        Modifier.testTag("material-chapters"),
                                    ),
                                    MaterialFilterRow(
                                        stringResource(R.string.material_kanji_title), kanjiSummary, { page = "kanji" },
                                        Modifier.testTag("material-kanji").semantics { if (invalid) error(errorMessage) },
                                        invalid,
                                    ),
                                ),
                            )
                            if (invalid && source != null) MaterialFieldError(errorMessage, Modifier.testTag("material-error"))
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
}
