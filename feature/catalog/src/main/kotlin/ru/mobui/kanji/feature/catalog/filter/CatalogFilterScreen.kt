package ru.mobui.kanji.feature.catalog.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.mobui.kanji.feature.catalog.R
import ru.mobui.kanji.feature.catalog.navigation.CatalogLoaded
import ru.mobui.kanji.feature.catalog.navigation.CatalogScaffold
import ru.mobui.kanji.feature.catalog.presentation.CatalogLoad
import ru.mobui.kanji.feature.catalog.presentation.SearchState

@Composable
internal fun CatalogFilterScreen(
    request: FilterRequest,
    state: CatalogLoad<FilterContent>,
    search: SearchState,
    language: String,
    actions: FilterActions,
) {
    if (state is CatalogLoad.Ready) {
        when (val content = state.content) {
            is FilterContent.Chapters -> CatalogChapterPicker(content, actions)
            is FilterContent.Kanji -> CatalogKanjiPicker(content, language, actions.select, actions.back)
            is FilterContent.Radical -> CatalogRadicalPicker(content, language, actions)
            is FilterContent.Strokes -> CatalogStrokePicker(content, actions)
            is FilterContent.Jlpt -> CatalogJlptPicker(content, actions)
        }
    } else {
        CatalogScaffold(filterTitle(request), actions.back) {
            CatalogLoaded(state, actions.retry) { }
        }
    }
}

@Composable
private fun filterTitle(request: FilterRequest): String = stringResource(when (request) {
    is FilterRequest.ChaptersForKanji, is FilterRequest.ChaptersForWords -> R.string.catalog_choose_chapter
    is FilterRequest.Kanji -> R.string.catalog_choose_kanji
    is FilterRequest.Radical -> R.string.catalog_choose_key
    is FilterRequest.KanjiStrokes, is FilterRequest.RadicalStrokes -> R.string.catalog_choose_strokes
    is FilterRequest.Jlpt -> R.string.catalog_choose_jlpt
})
