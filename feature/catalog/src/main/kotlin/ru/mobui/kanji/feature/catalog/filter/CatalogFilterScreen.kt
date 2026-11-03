package ru.mobui.kanji.feature.catalog.filter

import ru.mobui.kanji.feature.catalog.R
import ru.mobui.kanji.feature.catalog.components.CatalogClearFilterButton
import ru.mobui.kanji.feature.catalog.components.CatalogEmptyState
import ru.mobui.kanji.feature.catalog.components.CatalogHint
import ru.mobui.kanji.feature.catalog.components.CatalogMaterialCount
import ru.mobui.kanji.feature.catalog.components.ChapterList
import ru.mobui.kanji.feature.catalog.components.KanjiGrid
import ru.mobui.kanji.feature.catalog.components.NumberingHelpButton
import ru.mobui.kanji.feature.catalog.components.RadicalGrid
import ru.mobui.kanji.feature.catalog.components.SearchUi
import ru.mobui.kanji.feature.catalog.components.numbers
import ru.mobui.kanji.feature.catalog.navigation.CatalogLoaded
import ru.mobui.kanji.feature.catalog.navigation.CatalogScaffold
import ru.mobui.kanji.feature.catalog.navigation.ChapterEmpty
import ru.mobui.kanji.feature.catalog.presentation.CatalogLoad
import ru.mobui.kanji.feature.catalog.presentation.MaterialKind
import ru.mobui.kanji.feature.catalog.presentation.SearchState

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ru.mobui.kanji.domain.catalog.JlptFilter
import ru.mobui.kanji.domain.catalog.JlptLevel

@Composable
internal fun CatalogFilterScreen(
    request: FilterRequest,
    state: CatalogLoad<FilterContent>,
    search: SearchState,
    language: String,
    actions: FilterActions,
) {
    val title = filterTitle(request)
    val searchable =
        request is FilterRequest.ChaptersForKanji ||
            request is FilterRequest.ChaptersForWords ||
            request is FilterRequest.Kanji ||
            request is FilterRequest.Radical
    val chapters =
        request is FilterRequest.ChaptersForKanji || request is FilterRequest.ChaptersForWords
    val data = (state as? CatalogLoad.Ready)?.content
    val tools: (@Composable () -> Unit)? =
        if (data == null || !searchable) null else ({ FilterTools(data, actions.select) })
    CatalogScaffold(
        title,
        actions.back,
        search =
            if (searchable)
                SearchUi(
                    search,
                    if (chapters) R.string.catalog_search_chapter
                    else if (request is FilterRequest.Radical) R.string.catalog_search_radical
                    else R.string.catalog_search,
                    chapters,
                    actions.query,
                    actions.toggleSearch,
                )
            else null,
        tools = tools,
    ) {
        CatalogLoaded(state, actions.retry) { content ->
            FilterBody(content, search, language, actions.select)
        }
    }
}

@Composable
private fun FilterTools(content: FilterContent, select: (FilterResult) -> Unit) {
    when (content) {
        is FilterContent.Chapters -> {
            CatalogClearFilterButton(
                stringResource(R.string.catalog_all_chapters),
                { select(FilterResult.Chapter(null)) },
            )
            CatalogMaterialCount(content.entries.size, content.entries.size, MaterialKind.Chapter)
        }
        is FilterContent.Kanji -> {
            CatalogClearFilterButton(
                stringResource(R.string.catalog_all_kanji),
                { select(FilterResult.Kanji(null)) },
            )
            CatalogMaterialCount(
                content.rows.entries.size,
                content.rows.entries.size,
                MaterialKind.Kanji,
            )
        }
        is FilterContent.Radical -> {
            CatalogClearFilterButton(
                stringResource(R.string.catalog_all_keys),
                { select(FilterResult.Radical(null)) },
            )
            CatalogMaterialCount(content.entries.size, content.entries.size, MaterialKind.Radical)
        }
        is FilterContent.Strokes,
        is FilterContent.Jlpt -> Unit
    }
}


@Composable
private fun filterTitle(request: FilterRequest): String =
stringResource(
            when (request) {
                is FilterRequest.ChaptersForKanji,
                is FilterRequest.ChaptersForWords -> R.string.catalog_choose_chapter
                is FilterRequest.Kanji -> R.string.catalog_choose_kanji
                is FilterRequest.Radical -> R.string.catalog_choose_key
                is FilterRequest.KanjiStrokes,
                is FilterRequest.RadicalStrokes -> R.string.catalog_choose_strokes
                is FilterRequest.Jlpt -> R.string.catalog_choose_jlpt
            },
        )

@Composable
private fun FilterBody(content: FilterContent, search: SearchState, language: String, select: (FilterResult) -> Unit) {
    when (content) {
        is FilterContent.Chapters -> ChapterFilterBody(content, search, select)
        is FilterContent.Kanji -> KanjiFilterBody(content, search, language, select)
        is FilterContent.Radical -> RadicalFilterBody(content, search, language, select)
        is FilterContent.Strokes -> StrokeFilterBody(content, select)
        is FilterContent.Jlpt -> JlptFilterBody(content, select)
    }
}

@Composable
private fun ChapterFilterBody(content: FilterContent.Chapters, search: SearchState, select: (FilterResult) -> Unit) {
    if (content.entries.isEmpty()) ChapterEmpty(search.query, content.range)
                        else
                            ChapterList(
                                content.entries,
                                { select(FilterResult.Chapter(it)) },
                                Modifier.fillMaxSize(),
                                content.selected,
                                content.showWordCount,
                                compactSelection = true,
                            )
}

@Composable
private fun KanjiFilterBody(
    content: FilterContent.Kanji,
    search: SearchState,
    language: String,
    select: (FilterResult) -> Unit,
) {
    if (content.rows.entries.isEmpty()) {
        CatalogEmptyState(search.query.isNotBlank(), Modifier.fillMaxSize())
    } else {
        KanjiGrid(
            content.rows.entries,
            language,
            { select(FilterResult.Kanji(it)) },
            Modifier.fillMaxSize(),
            content.selected,
            selectionMode = true,
        )
    }
}

@Composable
private fun RadicalFilterBody(
    content: FilterContent.Radical,
    search: SearchState,
    language: String,
    select: (FilterResult) -> Unit,
) {
    if (content.entries.isEmpty()) {
        CatalogEmptyState(search.query.isNotBlank(), Modifier.fillMaxSize())
    } else {
        RadicalGrid(
            content.entries,
            language,
            { select(FilterResult.Radical(it)) },
            Modifier.fillMaxSize(),
            content.selected,
            fullNames = true,
        )
    }
}

@Composable
private fun StrokeFilterBody(content: FilterContent.Strokes, select: (FilterResult) -> Unit) {
    NumericFilterScreen(
        options =
            content.counts.keys.sorted().map {
                NumericOption(
                    it,
                    pluralStringResource(R.plurals.catalog_strokes, it, it),
                    content.counts.getValue(it),
                )
            },
        selected = content.selected,
        kind = content.kind,
        clearLabel = stringResource(R.string.catalog_all_strokes),
        actions =
            NumericFilterActions(
                onClear = { select(FilterResult.Strokes(null)) },
                onSelect = { select(FilterResult.Strokes(it)) },
            ),
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun JlptFilterBody(content: FilterContent.Jlpt, select: (FilterResult) -> Unit) {
    val levels = JlptLevel.entries.map { JlptFilter.Exact(it) } + JlptFilter.Missing
    NumericFilterScreen(
        options =
            levels
                .filter { it in content.counts }
                .map { NumericOption(it, jlptLabel(it), content.counts.getValue(it)) },
        selected = content.selected,
        kind = MaterialKind.Kanji,
        clearLabel = stringResource(R.string.catalog_all_jlpt),
        actions =
            NumericFilterActions(
                onClear = { select(FilterResult.Jlpt(JlptFilter.AnyLevel)) },
                onSelect = { select(FilterResult.Jlpt(it)) },
            ),
        modifier = Modifier.fillMaxSize(),
        explanation = stringResource(R.string.catalog_jlpt_explanation),
    )
}
