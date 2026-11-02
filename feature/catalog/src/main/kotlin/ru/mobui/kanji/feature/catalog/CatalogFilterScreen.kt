package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ru.mobui.kanji.domain.catalog.*

internal data class FilterActions(val back: () -> Unit, val retry: () -> Unit, val query: (String) -> Unit,
    val toggleSearch: () -> Unit, val select: (FilterResult) -> Unit)

@Composable
internal fun CatalogFilterScreen(request: FilterRequest, state: CatalogLoad<FilterContent>, search: SearchState,
    language: String, actions: FilterActions) {
    val title = stringResource(when (request) {
        is FilterRequest.ChaptersForKanji, is FilterRequest.ChaptersForWords -> R.string.catalog_choose_chapter
        is FilterRequest.Kanji -> R.string.catalog_choose_kanji
        is FilterRequest.Radical -> R.string.catalog_choose_key
        is FilterRequest.KanjiStrokes, is FilterRequest.RadicalStrokes -> R.string.catalog_choose_strokes
        is FilterRequest.Jlpt -> R.string.catalog_choose_jlpt
    })
    val searchable = request is FilterRequest.ChaptersForKanji || request is FilterRequest.ChaptersForWords ||
        request is FilterRequest.Kanji || request is FilterRequest.Radical
    val chapters = request is FilterRequest.ChaptersForKanji || request is FilterRequest.ChaptersForWords
    val data = (state as? CatalogLoad.Ready)?.content
    val tools: (@Composable () -> Unit)? = if (data == null || !searchable) null else ({ FilterTools(data, actions.select) })
    CatalogScaffold(title, actions.back, search = if (searchable) SearchUi(search,
        if (chapters) R.string.catalog_search_chapter else if (request is FilterRequest.Radical) R.string.catalog_search_radical else R.string.catalog_search,
        chapters, actions.query, actions.toggleSearch) else null, tools = tools) {
        CatalogLoaded(state, actions.retry) { content ->
            when (content) {
                is FilterContent.Chapters -> if (content.entries.isEmpty()) ChapterEmpty(search.query, content.range)
                    else ChapterList(content.entries, { actions.select(FilterResult.Chapter(it)) }, Modifier.fillMaxSize(), content.selected, content.showWordCount)
                is FilterContent.Kanji -> if (content.rows.entries.isEmpty()) CatalogEmptyState(search.query.isNotBlank(), Modifier.fillMaxSize())
                    else KanjiGrid(content.rows.entries, language, { actions.select(FilterResult.Kanji(it)) }, Modifier.fillMaxSize(),
                        content.selected, content.rows.numbers)
                is FilterContent.Radical -> if (content.entries.isEmpty()) CatalogEmptyState(search.query.isNotBlank(), Modifier.fillMaxSize())
                    else RadicalGrid(content.entries, language, { actions.select(FilterResult.Radical(it)) }, Modifier.fillMaxSize(), content.selected, fullNames = true)
                is FilterContent.Strokes -> NumericFilterScreen(content.counts.keys.sorted().map {
                    NumericOption(it, pluralStringResource(R.plurals.catalog_strokes, it, it), content.counts.getValue(it)) },
                    content.selected, content.kind, stringResource(R.string.catalog_all_strokes),
                    { actions.select(FilterResult.Strokes(null)) }, { actions.select(FilterResult.Strokes(it)) }, Modifier.fillMaxSize())
                is FilterContent.Jlpt -> {
                    val levels = JlptLevel.entries.map { JlptFilter.Exact(it) } + JlptFilter.Missing
                    NumericFilterScreen(levels.filter { it in content.counts }.map { NumericOption(it, jlptLabel(it), content.counts.getValue(it)) },
                        content.selected, MaterialKind.Kanji, stringResource(R.string.catalog_all_jlpt),
                        { actions.select(FilterResult.Jlpt(JlptFilter.AnyLevel)) }, { actions.select(FilterResult.Jlpt(it)) },
                        Modifier.fillMaxSize(), stringResource(R.string.catalog_jlpt_explanation))
                }
            }
        }
    }
}

@Composable
private fun FilterTools(content: FilterContent, select: (FilterResult) -> Unit) {
    when (content) {
        is FilterContent.Chapters -> {
            CatalogClearFilterButton(stringResource(R.string.catalog_all_chapters), { select(FilterResult.Chapter(null)) })
            CatalogMaterialCount(content.entries.size, content.entries.size, MaterialKind.Chapter)
        }
        is FilterContent.Kanji -> {
            CatalogClearFilterButton(stringResource(R.string.catalog_all_kanji), { select(FilterResult.Kanji(null)) })
            CatalogHint(stringResource(R.string.catalog_choose_kanji_hint))
            CatalogMaterialCount(content.rows.entries.size, content.rows.entries.size, MaterialKind.Kanji)
            if (content.rows.entries.isNotEmpty()) CatalogHint(stringResource(R.string.catalog_placement_legend))
        }
        is FilterContent.Radical -> {
            CatalogClearFilterButton(stringResource(R.string.catalog_all_keys), { select(FilterResult.Radical(null)) })
            CatalogMaterialCount(content.entries.size, content.entries.size, MaterialKind.Radical)
        }
        is FilterContent.Strokes, is FilterContent.Jlpt -> Unit
    }
}
