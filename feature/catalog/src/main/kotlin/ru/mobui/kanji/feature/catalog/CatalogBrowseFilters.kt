package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.domain.catalog.JlptFilter

@Composable
internal fun BrowseTools(content: BrowseContent, actions: BrowseActions) {
    when (content) {
        is BrowseContent.Home -> Unit
        is BrowseContent.Source -> when (val rows = content.rows) {
            is SourceRows.Chapters -> Unit
            is SourceRows.Kanji -> {
                KanjiFilterBar(rows.filters, rows.radicalSymbol, content.request.sourceId, content.request.chapter != null, false, actions)
                KanjiCount(rows.rows, legend = true)
            }
            is SourceRows.Words -> {
                WordFilterBar(rows.filters, rows.kanjiSymbol, content.request.sourceId, content.request.chapter != null, actions)
                CatalogMaterialCount(rows.rows.entries.size, rows.rows.total, MaterialKind.Word)
            }
        }
        is BrowseContent.AllKanji -> {
            KanjiFilterBar(content.filters, content.radicalSymbol, null, false, true, actions)
            KanjiCount(content.rows, legend = false)
        }
        is BrowseContent.AllRadicals -> {
            FlowRow(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CatalogFilterChip(content.strokes?.let { pluralStringResource(R.plurals.catalog_strokes, it, it) } ?: stringResource(R.string.catalog_all_strokes),
                    { actions.navigate(BrowseNavigation.Filter(FilterRequest.RadicalStrokes(content.strokes))) },
                    if (content.strokes == null) null else ({ actions.filter(FilterResult.Strokes(null)) }))
            }
            CatalogMaterialCount(content.entries.size, content.total, MaterialKind.Radical)
        }
        is BrowseContent.RelatedWords -> CatalogMaterialCount(content.rows.entries.size, content.rows.total, MaterialKind.Word)
    }
}

@Composable
private fun KanjiCount(rows: KanjiRows, legend: Boolean) {
    CatalogMaterialCount(rows.entries.size, rows.total, MaterialKind.Kanji)
    if (legend && rows.entries.isNotEmpty()) CatalogHint(stringResource(R.string.catalog_placement_legend))
}

@Composable
private fun KanjiFilterBar(filters: KanjiFilters, radical: String?, sourceId: Int?, fixed: Boolean, jlpt: Boolean, actions: BrowseActions) {
    FlowRow(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (sourceId != null && !fixed) CatalogFilterChip(filters.chapter?.let { stringResource(R.string.catalog_chapter_filter, it) } ?: stringResource(R.string.catalog_all_chapters),
            { actions.navigate(BrowseNavigation.Filter(FilterRequest.ChaptersForKanji(sourceId, filters))) },
            if (filters.chapter == null) null else ({ actions.filter(FilterResult.Chapter(null)) }), Modifier.testTag("catalog-filter-chapter"))
        CatalogFilterChip(filters.strokes?.let { pluralStringResource(R.plurals.catalog_strokes, it, it) } ?: stringResource(R.string.catalog_all_strokes),
            { actions.navigate(BrowseNavigation.Filter(FilterRequest.KanjiStrokes(sourceId, filters))) },
            if (filters.strokes == null) null else ({ actions.filter(FilterResult.Strokes(null)) }))
        CatalogFilterChip(radical?.let { stringResource(R.string.catalog_radical_filter, it) } ?: stringResource(R.string.catalog_all_keys),
            { actions.navigate(BrowseNavigation.Filter(FilterRequest.Radical(sourceId, filters))) },
            if (filters.radicalId == null) null else ({ actions.filter(FilterResult.Radical(null)) }), Modifier.testTag("catalog-filter-radical"))
        if (jlpt) CatalogFilterChip(if (filters.jlpt == JlptFilter.AnyLevel) stringResource(R.string.catalog_jlpt_chip) else jlptLabel(filters.jlpt),
            { actions.navigate(BrowseNavigation.Filter(FilterRequest.Jlpt(sourceId, filters))) },
            if (filters.jlpt == JlptFilter.AnyLevel) null else ({ actions.filter(FilterResult.Jlpt(JlptFilter.AnyLevel)) }))
    }
}

@Composable
private fun WordFilterBar(filters: WordFilters, symbol: String?, sourceId: Int, fixed: Boolean, actions: BrowseActions) {
    FlowRow(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!fixed) CatalogFilterChip(filters.chapter?.let { stringResource(R.string.catalog_chapter_filter, it) } ?: stringResource(R.string.catalog_all_chapters),
            { actions.navigate(BrowseNavigation.Filter(FilterRequest.ChaptersForWords(sourceId, filters))) },
            if (filters.chapter == null) null else ({ actions.filter(FilterResult.Chapter(null)) }), Modifier.testTag("catalog-filter-chapter"))
        CatalogFilterChip(symbol?.let { stringResource(R.string.catalog_kanji_filter, it) } ?: stringResource(R.string.catalog_all_kanji_filter),
            { actions.navigate(BrowseNavigation.Filter(FilterRequest.Kanji(sourceId, filters))) },
            if (filters.kanjiId == null) null else ({ actions.filter(FilterResult.Kanji(null)) }), Modifier.testTag("catalog-filter-kanji"))
    }
}
