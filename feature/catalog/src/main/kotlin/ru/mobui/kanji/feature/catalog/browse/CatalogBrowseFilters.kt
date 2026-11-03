package ru.mobui.kanji.feature.catalog.browse

import ru.mobui.kanji.feature.catalog.R
import ru.mobui.kanji.feature.catalog.components.CatalogFilterChip
import ru.mobui.kanji.feature.catalog.components.CatalogMaterialCount
import ru.mobui.kanji.feature.catalog.components.NumberingHelpButton
import ru.mobui.kanji.feature.catalog.filter.FilterRequest
import ru.mobui.kanji.feature.catalog.filter.FilterResult
import ru.mobui.kanji.feature.catalog.filter.KanjiFilterContext
import ru.mobui.kanji.feature.catalog.filter.jlptLabel
import ru.mobui.kanji.feature.catalog.presentation.KanjiFilters
import ru.mobui.kanji.feature.catalog.presentation.KanjiRows
import ru.mobui.kanji.feature.catalog.presentation.MaterialKind
import ru.mobui.kanji.feature.catalog.presentation.WordFilters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
        is BrowseContent.Source ->
            when (val rows = content.rows) {
                is SourceRows.Chapters -> Unit
                is SourceRows.Kanji -> {
                    KanjiFilterBar(
                        rows.filters,
                        rows.radicalSymbol,
                        KanjiFilterContext(
                            content.request.sourceId,
                            content.request.chapter != null,
                            false,
                            rows.canChangeChapter,
                        ),
                        actions,
                    )
                    KanjiCount(rows.rows, legend = true)
                }
                is SourceRows.Words -> {
                    WordFilterBar(
                        rows,
                        content.request.sourceId,
                        content.request.chapter != null,
                        actions,
                    )
                    CatalogMaterialCount(rows.rows.entries.size, rows.rows.total, MaterialKind.Word)
                }
            }
        is BrowseContent.AllKanji -> {
            KanjiFilterBar(content.filters, content.radicalSymbol, KanjiFilterContext(null, false, true), actions)
            KanjiCount(content.rows, legend = false)
        }
        is BrowseContent.AllRadicals -> {
            FlowRow(
                Modifier.fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CatalogFilterChip(
                    content.strokes?.let { pluralStringResource(R.plurals.catalog_strokes, it, it) }
                        ?: stringResource(R.string.catalog_all_strokes),
                    {
                        actions.navigate(
                            BrowseNavigation.Filter(FilterRequest.RadicalStrokes(content.strokes)),
                        )
                    },
                    if (content.strokes == null) null
                    else ({ actions.filter(FilterResult.Strokes(null)) }),
                )
            }
            CatalogMaterialCount(content.entries.size, content.total, MaterialKind.Radical)
        }
        is BrowseContent.RelatedWords ->
            CatalogMaterialCount(content.rows.entries.size, content.rows.total, MaterialKind.Word)
    }
}

@Composable
private fun KanjiCount(rows: KanjiRows, legend: Boolean) {
    CatalogMaterialCount(
        rows.entries.size,
        rows.total,
        MaterialKind.Kanji,
        action =
            if (legend && rows.entries.isNotEmpty()) {
                { NumberingHelpButton(stringResource(R.string.catalog_placement_legend)) }
            } else null,
    )
}

@Composable
private fun KanjiFilterBar(filters: KanjiFilters, radical: String?, context: KanjiFilterContext, actions: BrowseActions) {
    with(context) {
    FlowRow(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (sourceId != null && !fixed)
            CatalogFilterChip(
                filters.chapter?.let { stringResource(R.string.catalog_chapter_filter, it) }
                    ?: stringResource(R.string.catalog_all_chapters),
                {
                    actions.navigate(
                        BrowseNavigation.Filter(FilterRequest.ChaptersForKanji(sourceId, filters)),
                    )
                },
                if (filters.chapter == null) null
                else ({ actions.filter(FilterResult.Chapter(null)) }),
                Modifier.testTag("catalog-filter-chapter"),
                openEnabled = canChangeChapter,
            )
        CatalogFilterChip(
            filters.strokes?.let { pluralStringResource(R.plurals.catalog_strokes, it, it) }
                ?: stringResource(R.string.catalog_all_strokes),
            {
                actions.navigate(
                    BrowseNavigation.Filter(FilterRequest.KanjiStrokes(sourceId, filters)),
                )
            },
            if (filters.strokes == null) null else ({ actions.filter(FilterResult.Strokes(null)) }),
        )
        CatalogFilterChip(
            radical?.let { stringResource(R.string.catalog_radical_filter, it) }
                ?: stringResource(R.string.catalog_all_keys),
            { actions.navigate(BrowseNavigation.Filter(FilterRequest.Radical(sourceId, filters))) },
            if (filters.radicalId == null) null
            else ({ actions.filter(FilterResult.Radical(null)) }),
            Modifier.testTag("catalog-filter-radical"),
        )
        if (jlpt)
            CatalogFilterChip(
                if (filters.jlpt == JlptFilter.AnyLevel) stringResource(R.string.catalog_jlpt_chip)
                else jlptLabel(filters.jlpt),
                {
                    actions.navigate(BrowseNavigation.Filter(FilterRequest.Jlpt(sourceId, filters)))
                },
                if (filters.jlpt == JlptFilter.AnyLevel) null
                else ({ actions.filter(FilterResult.Jlpt(JlptFilter.AnyLevel)) }),
            )
    }

    }
}

@Composable
private fun WordFilterBar(
    rows: SourceRows.Words,
    sourceId: Int,
    fixed: Boolean,
    actions: BrowseActions,
) {
    val filters = rows.filters
    val symbol = rows.kanjiSymbol
    val canChangeChapter = rows.canChangeChapter
    FlowRow(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!fixed)
            CatalogFilterChip(
                filters.chapter?.let { stringResource(R.string.catalog_chapter_filter, it) }
                    ?: stringResource(R.string.catalog_all_chapters),
                {
                    actions.navigate(
                        BrowseNavigation.Filter(FilterRequest.ChaptersForWords(sourceId, filters)),
                    )
                },
                if (filters.chapter == null) null
                else ({ actions.filter(FilterResult.Chapter(null)) }),
                Modifier.testTag("catalog-filter-chapter"),
                openEnabled = canChangeChapter,
            )
        CatalogFilterChip(
            symbol?.let { stringResource(R.string.catalog_kanji_filter, it) }
                ?: stringResource(R.string.catalog_all_kanji_filter),
            { actions.navigate(BrowseNavigation.Filter(FilterRequest.Kanji(sourceId, filters))) },
            if (filters.kanjiId == null) null else ({ actions.filter(FilterResult.Kanji(null)) }),
            Modifier.testTag("catalog-filter-kanji"),
        )
    }
}
