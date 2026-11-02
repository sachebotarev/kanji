package ru.mobui.kanji.feature.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.mobui.kanji.domain.catalog.*

internal data class DetailActions(val back: () -> Unit, val retry: () -> Unit,
    val query: (String) -> Unit, val toggleSearch: () -> Unit, val navigate: (DetailNavigation) -> Unit)

@Composable
internal fun CatalogDetailScreen(request: DetailRequest, state: CatalogLoad<DetailContent>, search: SearchState,
    language: String, actions: DetailActions) {
    val data = (state as? CatalogLoad.Ready)?.content
    val kind = stringResource(when (request) {
        is DetailRequest.Word -> R.string.catalog_word
        is DetailRequest.Kanji -> R.string.catalog_kanji
        is DetailRequest.Radical -> R.string.catalog_radical
    })
    val entity = when (data) {
        is DetailContent.Word -> data.word?.written?.takeIf { it.isNotBlank() }
        is DetailContent.Kanji -> data.kanji?.symbol
        is DetailContent.Radical -> data.radical?.name(language)
        null -> null
    }
    val subtitle = listOfNotNull(kind.takeIf { entity != null }, data?.source?.title(language)).joinToString(" · ")
    CatalogScaffold(entity ?: kind, actions.back, subtitle = subtitle,
        search = if (request is DetailRequest.Radical) SearchUi(search, R.string.catalog_search, query = actions.query, toggle = actions.toggleSearch) else null) {
        CatalogLoaded(state, actions.retry) { content ->
            when (content) {
                is DetailContent.Word -> WordDetail(content, language) { actions.navigate(DetailNavigation.Kanji(it, request.sourceId)) }
                is DetailContent.Kanji -> KanjiDetail(content, language,
                    { actions.navigate(DetailNavigation.Radical(it, request.sourceId)) },
                    { content.kanji?.let { actions.navigate(DetailNavigation.Words(it.id, request.sourceId)) } })
                is DetailContent.Radical -> RadicalDetail(content, language, search.query) { actions.navigate(DetailNavigation.Kanji(it, request.sourceId)) }
            }
        }
    }
}
