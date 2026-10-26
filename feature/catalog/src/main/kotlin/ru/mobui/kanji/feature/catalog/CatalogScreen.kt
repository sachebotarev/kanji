package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.*

internal data class CatalogActions(
    val back: () -> Unit = {}, val retry: () -> Unit = {}, val query: (String) -> Unit = {},
    val tab: (Int) -> Unit = {}, val clear: () -> Unit = {}, val clearChapter: () -> Unit = {},
    val clearRadical: () -> Unit = {}, val source: (Int) -> Unit = {}, val chapter: (Int) -> Unit = {},
    val word: (Int) -> Unit = {}, val kanji: (Int) -> Unit = {}, val radical: (Int) -> Unit = {},
    val filter: (Boolean) -> Unit = {}, val selectFilter: (Int) -> Unit = {},
    val allKanji: () -> Unit = {}, val allRadicals: () -> Unit = {}, val filterKanji: () -> Unit = {}, val clearKanji: () -> Unit = {}, val selectKanji: (Int) -> Unit = {},
    val allWords: () -> Unit = {}, val studyKanji: (Int, Int) -> Unit = { _, _ -> },
)

@Composable
internal fun CatalogScreen(state: CatalogUiState, actions: CatalogActions, modifier: Modifier = Modifier) {
    val snapshot = state.snapshot
    val request = state.request
    val title = when (request.page) {
        CatalogPage.Home -> stringResource(R.string.catalog_title)
        CatalogPage.Source -> if (request.chapter != 0) stringResource(R.string.catalog_chapter, request.chapter)
            else snapshot?.sourceById?.get(request.sourceId)?.title(request.language).orEmpty()
        CatalogPage.Word -> stringResource(R.string.catalog_word)
        CatalogPage.Kanji, CatalogPage.AllKanji -> stringResource(R.string.catalog_kanji)
        CatalogPage.Radical -> stringResource(R.string.catalog_radical)
        CatalogPage.KanjiFilter -> stringResource(R.string.catalog_choose_kanji)
        CatalogPage.ChapterFilter -> stringResource(R.string.catalog_choose_chapter)
        CatalogPage.RadicalFilter -> stringResource(R.string.catalog_choose_key)
        CatalogPage.AllRadicals -> stringResource(R.string.catalog_all_radicals)
        CatalogPage.Words -> stringResource(R.string.catalog_related_words)
    }
    val header = rememberCatalogHeaderState()
    Scaffold(modifier.testTag("catalog-screen")) { padding ->
        Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(padding).nestedScroll(header.connection)) {
            CatalogHeader(header, 0) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(actions.back, Modifier.testTag("catalog-back")) {
                    Icon(painterResource(R.drawable.catalog_back), stringResource(R.string.catalog_back), Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.Medium))
                }

            }
            if (request.page in setOf(CatalogPage.Kanji, CatalogPage.Radical, CatalogPage.Words) && request.sourceId != 0) {
                snapshot?.sourceById?.get(request.sourceId)?.let {
                    Text(stringResource(R.string.catalog_source_context, it.title(request.language)), Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(); Text(stringResource(R.string.catalog_loading))
                    }
                }
                state.error -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.catalog_error)); Button(actions.retry) { Text(stringResource(R.string.catalog_retry)) }
                }
                snapshot != null -> when (request.page) {
                    CatalogPage.Home -> CatalogHomeScreen(snapshot, request.language, actions)
                    CatalogPage.Source -> SourceScreen(state, actions, header)
                    CatalogPage.Word -> WordDetail(state, actions)
                    CatalogPage.Kanji -> KanjiDetail(state, actions)
                    CatalogPage.Radical -> RadicalDetail(state, actions, header)
                    CatalogPage.KanjiFilter -> {
                        CatalogHeader(header, 1) {
                        CatalogSearch(state.query, stringResource(R.string.catalog_search), actions.query)
                        TextButton({ actions.selectKanji(0) }, Modifier.padding(horizontal = 20.dp)) { Text(stringResource(R.string.catalog_all_kanji)) }
                        ResultCount(state.kanji.size)
                        }
                        KanjiGrid(state.kanji, request.language, actions.selectKanji, Modifier.weight(1f), state.selectedKanji)
                    }
                    CatalogPage.AllKanji -> {
                        CatalogHeader(header, 1) {
                        CatalogSearch(state.query, stringResource(R.string.catalog_search), actions.query)
                        FilterBar(state, actions)
                        ResultCount(state.kanji.size)
                        }
                        KanjiGrid(state.kanji, request.language, actions.kanji, Modifier.weight(1f))
                    }
                    CatalogPage.AllRadicals, CatalogPage.RadicalFilter -> {
                        CatalogHeader(header, 1) {
                        CatalogSearch(state.query, stringResource(R.string.catalog_search_radical), actions.query)
                        if (request.page == CatalogPage.RadicalFilter) TextButton({ actions.selectFilter(0) }, Modifier.padding(horizontal = 20.dp)) {
                            Text(stringResource(R.string.catalog_all_keys))
                        }
                        ResultCount(state.radicals.size)
                        }
                        RadicalGrid(state.radicals, request.language,
                            if (request.page == CatalogPage.RadicalFilter) actions.selectFilter else actions.radical,
                            Modifier.weight(1f), state.radical)
                    }
                    CatalogPage.ChapterFilter -> {
                        CatalogHeader(header, 1) {
                        CatalogSearch(state.query, stringResource(R.string.catalog_search_chapter), actions.query)
                        TextButton({ actions.selectFilter(0) }, Modifier.padding(horizontal = 20.dp)) { Text(stringResource(R.string.catalog_all_chapters)) }
                        }
                        ChapterList(state.chapters, actions.selectFilter, Modifier.weight(1f), state.chapter)
                    }
                    CatalogPage.Words -> {
                        CatalogHeader(header, 1) {
                        CatalogSearch(state.query, stringResource(R.string.catalog_search_words), actions.query)
                        ResultCount(state.words.size)
                        }
                        WordList(state.words, request.language, actions.word, Modifier.weight(1f))
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun CatalogHomeScreen(snapshot: CatalogSnapshot, language: String, actions: CatalogActions) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(stringResource(R.string.catalog_sources), Modifier.padding(bottom = 2.dp),
            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(snapshot.sources, key = { it.id }) { source ->
            val chapters = snapshot.chapters(source.id)
            SourceCard(source, language, stringResource(R.string.catalog_source_counts, chapters.size,
                snapshot.placements.count { it.sourceId == source.id },
                snapshot.links.filter { it.sourceId == source.id }.map { it.wordId }.distinct().size), { actions.source(source.id) })
        }
        item { Text(stringResource(R.string.catalog_reference), Modifier.padding(top = 18.dp, bottom = 2.dp),
            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Surface(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    CatalogRow(stringResource(R.string.catalog_all_kanji), null, actions.allKanji, Modifier.testTag("catalog-all-kanji"))
                }
                Surface(shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    CatalogRow(stringResource(R.string.catalog_all_radicals), null, actions.allRadicals, Modifier.testTag("catalog-all-radicals"))
                }
            }
        }
    }
}

@Composable
private fun SourceScreen(state: CatalogUiState, actions: CatalogActions, header: CatalogHeaderState) {
    val fixedChapter = state.request.chapter != 0
    val tabs = if (fixedChapter) listOf(R.string.catalog_kanji, R.string.catalog_words)
        else listOf(R.string.catalog_chapters, R.string.catalog_kanji, R.string.catalog_words)
    val selected = if (fixedChapter) (state.tab - 1).coerceIn(0, 1) else state.tab.coerceIn(0, 2)
    CatalogHeader(header, 1) {

    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        tabs.forEachIndexed { index, label ->
            val active = selected == index
            Column(Modifier.weight(1f).testTag("catalog-tab-${if (fixedChapter) index + 1 else index}")
                .clickable { actions.tab(if (fixedChapter) index + 1 else index) }
                .semantics { this.selected = active; role = Role.Tab }.heightIn(min = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                val tabId = if (fixedChapter) index + 1 else index
                val count = if (tabId == 1) state.kanji.size else if (tabId == 2) state.words.size else null
                Text(if (count == null) stringResource(label) else stringResource(R.string.catalog_tab_count, stringResource(label), count),
                    Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.titleSmall,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.fillMaxWidth(.65f).height(3.dp).background(
                    if (active) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(2.dp)))
            }
        }
    }
    }
    val tab = if (fixedChapter) selected + 1 else selected
    val tabState = rememberSaveableStateHolder()
    tabState.SaveableStateProvider(tab) {
        Column(Modifier.fillMaxSize()) {
            CatalogHeader(header, 2) {
            if (tab == 2) WordSearchFilters(state, actions)
            else {
                CatalogSearch(state.query, stringResource(if (tab == 0) R.string.catalog_search_chapter else R.string.catalog_search), actions.query)
                if (tab == 1) FilterBar(state, actions)
            }
            }
            when (tab) {
                0 -> ChapterList(state.chapters, actions.chapter, Modifier.weight(1f))
                1 -> { KanjiGrid(state.kanji, state.request.language, actions.kanji, Modifier.weight(1f)) }
                else -> {
                    if (state.words.isEmpty()) ResultCount(0)
                    WordList(state.words, state.request.language, actions.word, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WordSearchFilters(state: CatalogUiState, actions: CatalogActions) {
    Column {
        CatalogSearch(state.query, stringResource(R.string.catalog_search_words_short), actions.query)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.request.chapter == 0) FilledTonalButton({ actions.filter(false) }, Modifier.weight(1f).testTag("catalog-filter-chapter"),
                shape = RoundedCornerShape(16.dp)) {
                CatalogFilterIcon()
                Text(if (state.chapter == 0) stringResource(R.string.catalog_all_chapters) else stringResource(R.string.catalog_chapter_filter, state.chapter))
            }
            FilledTonalButton(actions.filterKanji, Modifier.weight(1f).testTag("catalog-filter-kanji"), shape = RoundedCornerShape(16.dp)) {
                CatalogFilterIcon()
                val symbol = state.snapshot?.kanjiById?.get(state.selectedKanji)?.symbol
                Text(if (symbol == null) stringResource(R.string.catalog_all_kanji_filter) else stringResource(R.string.catalog_kanji_filter, symbol))
            }
        }
        if (state.selectedKanji != 0 || state.chapter != state.request.chapter) TextButton(
            { actions.clearChapter(); actions.clearKanji() }, Modifier.padding(horizontal = 20.dp)) {
            Text(stringResource(R.string.catalog_reset))
        }
    }
}

@Composable
private fun FilterBar(state: CatalogUiState, actions: CatalogActions) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.request.sourceId != 0 && state.request.chapter == 0) FilledTonalButton({ actions.filter(false) }, Modifier.weight(1f).testTag("catalog-filter-chapter"), enabled = state.request.chapter == 0) {
                CatalogFilterIcon()
                Text(if (state.chapter == 0) stringResource(R.string.catalog_all_chapters) else stringResource(R.string.catalog_chapter_filter, state.chapter))
            }
            FilledTonalButton({ actions.filter(true) }, Modifier.weight(1f).testTag("catalog-filter-radical")) {
                CatalogFilterIcon()
                val symbol = state.snapshot?.radicalById?.get(state.radical)?.symbol
                Text(if (symbol == null) stringResource(R.string.catalog_all_keys) else stringResource(R.string.catalog_radical_filter, symbol))
            }
        }
        if (state.radical != 0 || state.chapter != state.request.chapter) Row {
            if (state.chapter != state.request.chapter) TextButton(actions.clearChapter) { Text(stringResource(R.string.catalog_remove_chapter)) }
            if (state.radical != 0) TextButton(actions.clearRadical) { Text(stringResource(R.string.catalog_remove_radical)) }
        }
    }
}

@Composable
private fun ResultCount(count: Int) {
    Text(stringResource(if (count == 0) R.string.catalog_empty else R.string.catalog_results, count),
        Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ChapterList(chapters: List<CatalogChapter>, onClick: (Int) -> Unit, modifier: Modifier = Modifier, selected: Int = 0) {
    LazyColumn(modifier.testTag("chapter-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (chapters.isEmpty()) item { Text(stringResource(R.string.catalog_empty)) }
        items(chapters, key = { it.number }) { chapter ->
            Surface(onClick = { onClick(chapter.number) }, Modifier.fillMaxWidth().testTag("chapter-${chapter.number}")
                .semantics { this.selected = chapter.number == selected }, shape = RoundedCornerShape(24.dp),
                color = if (chapter.number == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.catalog_chapter, chapter.number), Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp))
                    }
                    Text(stringResource(R.string.catalog_chapter_counts, chapter.kanjiCount, chapter.wordCount),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ChapterKanjiPreview(chapter.preview)
                }
            }
        }
    }
}

@Composable
private fun ChapterKanjiPreview(preview: String) {
    val symbols = preview.split(" ").filter { it.isNotEmpty() }
    val density = LocalDensity.current
    val symbolWidth = with(density) { 24.sp.toDp() }
    val rowHeight = with(density) { 32.sp.toDp() }
    BoxWithConstraints(Modifier.fillMaxWidth().clearAndSetSemantics { text = AnnotatedString(preview) }) {
        // Extra rows are allowed for narrow windows or accessibility font scaling.
        val capacity = (maxWidth / (symbolWidth + 8.dp)).toInt().coerceAtLeast(1)
        val rowCount = maxOf(2, (symbols.size + capacity - 1) / capacity)
        val perRow = symbols.size / rowCount
        val extra = symbols.size % rowCount
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            var start = 0
            repeat(rowCount) { row ->
                val count = perRow + if (row < extra) 1 else 0
                Row(Modifier.fillMaxWidth().heightIn(min = rowHeight),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    symbols.subList(start, start + count).forEach { symbol ->
                        Text(symbol, style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Normal))
                    }
                }
                start += count
            }
        }
    }
}

@Composable
private fun WordList(words: List<CatalogWord>, language: String, onClick: (Int) -> Unit, modifier: Modifier = Modifier) =
    ru.mobui.kanji.core.wordui.WordList(words, language, onClick, modifier)

@Composable
private fun DetailColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

@Composable
private fun WordDetail(state: CatalogUiState, actions: CatalogActions) {
    val snapshot = state.snapshot ?: return
    val word = snapshot.wordById[state.request.id]
    if (word == null) { Text(stringResource(R.string.catalog_not_found)); return }
    val language = state.request.language
    val resources = androidx.compose.ui.platform.LocalResources.current
    val links = ru.mobui.kanji.core.wordui.wordStudyItems(snapshot, word.id, language) {
        resources.getString(R.string.catalog_chapter, it)
    }
    ru.mobui.kanji.core.wordui.WordDetailsContent(word, language, snapshot.literalKanji(word), links,
        actions.kanji, { item ->
            val kanjiId = item.kanjiId
            if (kanjiId == null) actions.source(item.sourceId) else actions.studyKanji(kanjiId, item.sourceId)
        })
}

@Composable
private fun KanjiDetail(state: CatalogUiState, actions: CatalogActions) {
    val snapshot = state.snapshot ?: return
    val kanji = snapshot.kanjiById[state.request.id]
    if (kanji == null) { Text(stringResource(R.string.catalog_not_found)); return }
    val language = state.request.language
    DetailColumn {
        Text(kanji.symbol, style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp), modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).testTag("kanji-symbol"), textAlign = TextAlign.Center)
        Text(kanji.meaning(language) ?: stringResource(R.string.catalog_missing), style = MaterialTheme.typography.titleLarge)
        Text(pluralStringResource(R.plurals.catalog_strokes, kanji.strokes, kanji.strokes))
        Text(listOfNotNull(if (kanji.joyo) stringResource(R.string.catalog_joyo) else null,
            if (kanji.kyoiku) stringResource(R.string.catalog_kyoiku) else null,
            if (kanji.jinmeiyo) stringResource(R.string.catalog_jinmeiyo) else null).joinToString(" · "))
        if (kanji.on.isNotEmpty()) CatalogPanel(title = stringResource(R.string.catalog_on)) { Text(kanji.on.joinToString(" · "), style = MaterialTheme.typography.titleLarge) }
        if (kanji.kun.isNotEmpty()) CatalogPanel(title = stringResource(R.string.catalog_kun)) { Text(kanji.kun.joinToString(" · "), style = MaterialTheme.typography.titleLarge) }
        if (kanji.on.isEmpty() && kanji.kun.isEmpty()) Text(stringResource(R.string.catalog_no_reading))
        kanji.radicalId?.let { id -> snapshot.radicalById[id]?.let { radical ->
            CatalogRow(stringResource(R.string.catalog_radical), radical.symbol + " · " + (radical.name(language) ?: stringResource(R.string.catalog_missing)), { actions.radical(id) }, Modifier.testTag("kanji-key"))
        } }
        CatalogSection(title = stringResource(R.string.catalog_related_words)) {
            if (state.words.isEmpty()) Text(stringResource(R.string.catalog_empty))
            state.words.take(6).forEach { word -> WordRow(word, language, { actions.word(word.id) }) }
            if (state.words.size > 6) TextButton(actions.allWords, Modifier.testTag("kanji-all-words")) { Text(stringResource(R.string.catalog_more_words)) }
        }
    }
}

@Composable
private fun RadicalDetail(state: CatalogUiState, actions: CatalogActions, header: CatalogHeaderState) {
    val radical = state.snapshot?.radicalById?.get(state.request.id)
    if (radical == null) { Text(stringResource(R.string.catalog_not_found)); return }
    Column(Modifier.fillMaxSize()) {
        CatalogHeader(header, 1) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(radical.symbol, style = MaterialTheme.typography.displayMedium)
            Column {
                Text(radical.name(state.request.language) ?: stringResource(R.string.catalog_missing), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.catalog_radical_number, radical.id))
                Text(pluralStringResource(R.plurals.catalog_strokes, radical.strokes, radical.strokes))
            }
        }
        Text(stringResource(R.string.catalog_related_kanji), Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleMedium)
        CatalogSearch(state.query, stringResource(R.string.catalog_search), actions.query)
        ResultCount(state.kanji.size)
        }
        KanjiGrid(state.kanji, state.request.language, actions.kanji, Modifier.weight(1f))
    }
}

@Preview(locale = "ru", showBackground = true)
@Composable
private fun CatalogPreview() { KanjiTheme { CatalogScreen(CatalogUiState(CatalogRequest(CatalogPage.Home), loading = true), CatalogActions()) } }
