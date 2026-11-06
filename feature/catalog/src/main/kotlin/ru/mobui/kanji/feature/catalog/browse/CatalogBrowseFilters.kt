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
                    CatalogMaterialCount(rows.rows.entries.count(), rows.rows.total, MaterialKind.Word)
                }
            }
        is BrowseContent.AllKanji -> {
            KanjiFilterBar(content.filters, content.radicalSymbol, KanjiFilterContext(null, false, true), actions)
            KanjiCount(content.rows, legend = false)
        }
        is BrowseContent.AllRadicals -> {
            FlowRow(
                Modifier.fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CatalogFilterChip(
                    strokeFilterLabel(content.strokeIds),
                    {
                        actions.navigate(
                            BrowseNavigation.Filter(FilterRequest.RadicalStrokes(content.strokes, content.strokeIds)),
                        )
                    },
                    if (content.strokeIds.isEmpty()) null
                    else ({ actions.filter(FilterResult.Strokes(null)) }),
                    Modifier.testTag("catalog-filter-strokes"),
                )
            }
            CatalogMaterialCount(content.entries.count(), content.total, MaterialKind.Radical)
        }
        is BrowseContent.RelatedWords ->
            CatalogMaterialCount(content.rows.entries.count(), content.rows.total, MaterialKind.Word)
    }
}

@Composable
private fun KanjiCount(rows: KanjiRows, legend: Boolean) {
    CatalogMaterialCount(
        rows.entries.count(),
        rows.total,
        MaterialKind.Kanji,
        action =
            if (legend && rows.entries.isNotEmpty()) {
                { NumberingHelpButton(stringResource(R.string.catalog_placement_legend)) }
            } else null,
    )
}

@Composable
private fun strokeFilterLabel(ids: Set<Int>): String {
    if (ids.isEmpty()) return stringResource(R.string.catalog_all_strokes)
    return ids.sorted().take(2).map { pluralStringResource(R.plurals.catalog_strokes, it, it) }.joinToString("、") +
        if (ids.count() > 2) " +${ids.count() - 2}" else ""
}

@Composable
private fun chapterFilterLabel(ids: Set<Int>): String {
    if (ids.isEmpty()) return stringResource(R.string.catalog_all_chapters)
    if (ids.count() == 1) return stringResource(R.string.catalog_chapter_filter, ids.single())
    val numbers = ids.sorted().take(2).joinToString("、") + if (ids.count() > 2) " +${ids.count() - 2}" else ""
    return stringResource(R.string.catalog_chapters_selected, numbers)
}

@Composable
private fun KanjiFilterBar(filters: KanjiFilters, radical: String?, context: KanjiFilterContext, actions: BrowseActions) {
    with(context) {
    FlowRow(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (sourceId != null && !fixed)
            CatalogFilterChip(
                chapterFilterLabel(filters.chapterIds),
                {
                    actions.navigate(
                        BrowseNavigation.Filter(FilterRequest.ChaptersForKanji(sourceId, filters)),
                    )
                },
                if (filters.chapterIds.isEmpty()) null
                else ({ actions.filter(FilterResult.Chapter(null)) }),
                Modifier.testTag("catalog-filter-chapter"),
                openEnabled = canChangeChapter,
            )
        CatalogFilterChip(
            strokeFilterLabel(filters.strokeIds),
            {
                actions.navigate(
                    BrowseNavigation.Filter(FilterRequest.KanjiStrokes(sourceId, filters)),
                )
            },
            if (filters.strokeIds.isEmpty()) null else ({ actions.filter(FilterResult.Strokes(null)) }),
            Modifier.testTag("catalog-filter-strokes"),
        )
        CatalogFilterChip(
            radical?.let { stringResource(R.string.catalog_radical_filter, it) }
                ?: stringResource(R.string.catalog_all_keys),
            { actions.navigate(BrowseNavigation.Filter(FilterRequest.Radical(sourceId, filters))) },
            if (filters.radicalIds.isEmpty()) null
            else ({ actions.filter(FilterResult.Radical(null)) }),
            Modifier.testTag("catalog-filter-radical"),
        )
        if (jlpt)
            CatalogFilterChip(
                if (filters.jlptFilters.isEmpty()) stringResource(R.string.catalog_jlpt_chip)
                else filters.jlptFilters.sortedByDescending { when (it) {
                    JlptFilter.AnyLevel -> 0
                    JlptFilter.Missing -> -1
                    is JlptFilter.Exact -> it.level.number
                } }.take(2).map { jlptLabel(it) }.joinToString("、") +
                    if (filters.jlptFilters.size > 2) " +${filters.jlptFilters.size - 2}" else "",
                {
                    actions.navigate(BrowseNavigation.Filter(FilterRequest.Jlpt(sourceId, filters)))
                },
                if (filters.jlptFilters.isEmpty()) null
                else ({ actions.filter(FilterResult.Jlpt(JlptFilter.AnyLevel)) }),
                Modifier.testTag("catalog-filter-jlpt"),
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
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!fixed)
            CatalogFilterChip(
                chapterFilterLabel(filters.chapterIds),
                {
                    actions.navigate(
                        BrowseNavigation.Filter(FilterRequest.ChaptersForWords(sourceId, filters)),
                    )
                },
                if (filters.chapterIds.isEmpty()) null
                else ({ actions.filter(FilterResult.Chapter(null)) }),
                Modifier.testTag("catalog-filter-chapter"),
                openEnabled = canChangeChapter,
            )
        CatalogFilterChip(
            symbol?.let { stringResource(R.string.catalog_kanji_filter, it) }
                ?: stringResource(R.string.catalog_all_kanji_filter),
            { actions.navigate(BrowseNavigation.Filter(FilterRequest.Kanji(sourceId, filters))) },
            if (filters.kanjiIds.isEmpty()) null else ({ actions.filter(FilterResult.Kanji(null)) }),
            Modifier.testTag("catalog-filter-kanji"),
        )
    }
}
