package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.domain.catalog.*

internal data class BrowseActions(val back: () -> Unit, val retry: () -> Unit, val query: (String) -> Unit,
    val toggleSearch: () -> Unit, val tab: (SourceTab) -> Unit, val filter: (FilterResult) -> Unit,
    val navigate: (BrowseNavigation) -> Unit)

@Composable
internal fun CatalogBrowseScreen(request: BrowseRequest, state: CatalogLoad<BrowseContent>, search: SearchState,
    language: String, actions: BrowseActions) {
    val data = (state as? CatalogLoad.Ready)?.content
    val source = data as? BrowseContent.Source
    val tab = source?.tab ?: (request as? BrowseRequest.Source)?.initialTab
    val title = when (request) {
        BrowseRequest.Home -> stringResource(R.string.catalog_title)
        is BrowseRequest.Source -> request.chapter?.let { stringResource(R.string.catalog_chapter, it) }
            ?: source?.source?.title(language).orEmpty()
        BrowseRequest.AllKanji -> stringResource(R.string.catalog_kanji)
        BrowseRequest.AllRadicals -> stringResource(R.string.catalog_all_radicals)
        is BrowseRequest.RelatedWords -> stringResource(R.string.catalog_words_with, (data as? BrowseContent.RelatedWords)?.symbol.orEmpty())
    }
    val subtitle = when (request) {
        BrowseRequest.AllKanji, BrowseRequest.AllRadicals -> stringResource(R.string.catalog_global_reference)
        is BrowseRequest.Source -> if (request.chapter != null) source?.source?.title(language).orEmpty() else ""
        is BrowseRequest.RelatedWords -> (data as? BrowseContent.RelatedWords)?.source?.title(language).orEmpty()
        BrowseRequest.Home -> ""
    }
    val label = when {
        request == BrowseRequest.AllRadicals -> R.string.catalog_search_radical
        request is BrowseRequest.RelatedWords -> R.string.catalog_search_words
        tab == SourceTab.Chapters -> R.string.catalog_search_chapter
        tab == SourceTab.Words -> R.string.catalog_search_words_short
        else -> R.string.catalog_search
    }
    val tools: (@Composable () -> Unit)? = if (data == null || data is BrowseContent.Home ||
        data is BrowseContent.Source && data.rows is SourceRows.Chapters) null else ({ BrowseTools(data, actions) })
    val tabs: (@Composable () -> Unit)? = source?.let { ({ SourceTabs(it.tab, it.request.chapter != null, actions.tab) }) }
    CatalogScaffold(title, actions.back, subtitle = subtitle, home = request == BrowseRequest.Home,
        search = if (request == BrowseRequest.Home) null else SearchUi(search, label, tab == SourceTab.Chapters, actions.query, actions.toggleSearch),
        resetKey = tab, tabs = tabs, tools = tools) {
        CatalogLoaded(state, actions.retry) { content ->
            when (content) {
                is BrowseContent.Home -> CatalogHomeScreen(content.sources, language, actions.navigate, Modifier.fillMaxSize())
                is BrowseContent.Source -> SourceBody(content, search.query, language, actions.navigate)
                is BrowseContent.AllKanji -> if (content.rows.entries.isEmpty()) CatalogEmptyState(search.query.isNotBlank(), Modifier.fillMaxSize())
                    else KanjiGrid(content.rows.entries, language, { actions.navigate(BrowseNavigation.Kanji(it, null)) }, Modifier.fillMaxSize())
                is BrowseContent.AllRadicals -> if (content.entries.isEmpty()) CatalogEmptyState(search.query.isNotBlank(), Modifier.fillMaxSize())
                    else RadicalGrid(content.entries, language, { actions.navigate(BrowseNavigation.Radical(it)) }, Modifier.fillMaxSize())
                is BrowseContent.RelatedWords -> if (content.rows.entries.isEmpty()) CatalogEmptyState(search.query.isNotBlank(), Modifier.fillMaxSize())
                    else if (content.sourceId != null)
                        CatalogSourceWords(content.rows.groups, language, true, false, false,
                            { actions.navigate(BrowseNavigation.Word(it, content.sourceId)) }, Modifier.fillMaxSize())
                    else WordList(content.rows.entries, language, { actions.navigate(BrowseNavigation.Word(it, null)) }, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun SourceTabs(tab: SourceTab, fixedChapter: Boolean, onTab: (SourceTab) -> Unit) {
    val tabs = if (fixedChapter) listOf(SourceTab.Kanji, SourceTab.Words) else SourceTab.entries
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        tabs.forEach { item ->
            val active = tab == item
            Column(Modifier.weight(1f).testTag("catalog-tab-${item.ordinal}").clickable { onTab(item) }
                .semantics { selected = active; role = Role.Tab }.heightIn(min = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Text(stringResource(when (item) { SourceTab.Chapters -> R.string.catalog_chapters; SourceTab.Kanji -> R.string.catalog_kanji; SourceTab.Words -> R.string.catalog_words }),
                    Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.fillMaxWidth(.65f).height(3.dp).background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
private fun SourceBody(content: BrowseContent.Source, query: String, language: String, navigate: (BrowseNavigation) -> Unit) {
    val holder = rememberSaveableStateHolder()
    holder.SaveableStateProvider(content.tab.name) {
        val id = content.request.sourceId
        when (val rows = content.rows) {
            is SourceRows.Chapters -> if (rows.entries.isEmpty()) ChapterEmpty(query, rows.range)
                else ChapterList(rows.entries, { navigate(BrowseNavigation.Source(id, it)) }, Modifier.fillMaxSize())
            is SourceRows.Kanji -> if (rows.rows.entries.isEmpty()) CatalogEmptyState(query.isNotBlank(), Modifier.fillMaxSize())
                else KanjiGrid(rows.rows.entries, language, { navigate(BrowseNavigation.Kanji(it, id)) }, Modifier.fillMaxSize(), numbers = rows.rows.numbers)
            is SourceRows.Words -> if (rows.rows.entries.isEmpty()) CatalogEmptyState(query.isNotBlank(), Modifier.fillMaxSize())
                else CatalogSourceWords(rows.rows.groups, language, false, rows.showEnd, content.request.chapter != null,
                    { navigate(BrowseNavigation.Word(it, id)) }, Modifier.fillMaxSize())
        }
    }
}

