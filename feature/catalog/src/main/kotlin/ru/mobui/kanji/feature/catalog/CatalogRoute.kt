package ru.mobui.kanji.feature.catalog

import ru.mobui.kanji.feature.catalog.browse.BrowseNavigation
import ru.mobui.kanji.feature.catalog.browse.SourceTab
import ru.mobui.kanji.feature.catalog.components.savedValue
import ru.mobui.kanji.feature.catalog.detail.DetailNavigation
import ru.mobui.kanji.feature.catalog.filter.FilterRequest
import ru.mobui.kanji.feature.catalog.filter.FilterResult
import ru.mobui.kanji.feature.catalog.navigation.CatalogGraphContext
import ru.mobui.kanji.feature.catalog.navigation.catalogBrowseLibrary
import ru.mobui.kanji.feature.catalog.navigation.catalogBrowseSources
import ru.mobui.kanji.feature.catalog.navigation.catalogChapterFilters
import ru.mobui.kanji.feature.catalog.navigation.catalogJlptFilters
import ru.mobui.kanji.feature.catalog.navigation.catalogRelatedWordsAndWord
import ru.mobui.kanji.feature.catalog.navigation.catalogStrokeFilters
import ru.mobui.kanji.feature.catalog.navigation.catalogSymbolDetails
import ru.mobui.kanji.feature.catalog.navigation.catalogSymbolFilters
import ru.mobui.kanji.feature.catalog.presentation.encoded
import ru.mobui.kanji.feature.catalog.theme.CatalogTheme

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import ru.mobui.kanji.domain.catalog.CatalogRepository

@Serializable internal data object CatalogHome

@Serializable
internal data class SourceRoute(
    val sourceId: Int,
    val chapter: Int? = null,
    val tab: SourceTab = SourceTab.Chapters,
)

@Serializable internal data class WordRoute(val wordId: Int, val sourceId: Int?)

@Serializable internal data class KanjiRoute(val kanjiId: Int, val sourceId: Int?)

@Serializable internal data class RadicalRoute(val radicalId: Int, val sourceId: Int?)

@Serializable internal data object AllKanjiRoute

@Serializable internal data object AllRadicalsRoute

@Serializable internal data class WordsRoute(val kanjiId: Int, val sourceId: Int?)

@Serializable
internal data class KanjiChapterRoute(
    val sourceId: Int,
    val chapter: Int?,
    val radical: Int?,
    val strokes: Int?,
    val jlpt: Int,
)

@Serializable
internal data class WordChapterRoute(val sourceId: Int, val chapter: Int?, val kanji: Int?)

@Serializable
internal data class KanjiChoiceRoute(val sourceId: Int, val chapter: Int?, val selectedKanji: Int?)

@Serializable
internal data class RadicalChoiceRoute(
    val sourceId: Int?,
    val chapter: Int?,
    val radical: Int?,
    val strokes: Int?,
    val jlpt: Int,
)

@Serializable
internal data class KanjiStrokesRoute(
    val sourceId: Int?,
    val chapter: Int?,
    val radical: Int?,
    val strokes: Int?,
    val jlpt: Int,
)

@Serializable internal data class RadicalStrokesRoute(val strokes: Int?)

@Serializable
internal data class JlptChoiceRoute(
    val sourceId: Int?,
    val chapter: Int?,
    val radical: Int?,
    val strokes: Int?,
    val jlpt: Int,
)

@Composable
fun CatalogRoute(
    repository: CatalogRepository,
    onBack: () -> Unit,
    initialKanjiId: Int = 0,
    initialSourceId: Int = 0,
    loadDrawing: suspend (Int) -> ru.mobui.kanji.domain.handwriting.ReferenceDrawing? = { null },
    revision: Long = 0L,
) {
    val nav = rememberNavController()
    val back: () -> Unit = { if (!nav.popBackStack()) onBack() }
    BackHandler(onBack = back)
    val language = LocalConfiguration.current.locales[0].toLanguageTag()
    val browse: (BrowseNavigation) -> Unit = { nav.openBrowse(it) }
    val detail: (DetailNavigation) -> Unit = { nav.openDetail(it) }
    val select: (FilterResult) -> Unit = { result ->
        val (key, value) = result.encoded()
        nav.previousBackStackEntry?.savedStateHandle?.set(key, value)
        nav.popBackStack()
    }
    val start: Any =
        remember(initialKanjiId, initialSourceId) {
            if (initialKanjiId != 0) KanjiRoute(initialKanjiId, initialSourceId.takeIf { it != 0 })
            else if (initialSourceId != 0) SourceRoute(initialSourceId) else CatalogHome
        }
    val context = CatalogGraphContext(repository, language, back, loadDrawing, browse, detail, select, revision)
    CatalogTheme {
        NavHost(nav, startDestination = start) {
            catalogBrowseSources(context)
            catalogBrowseLibrary(context)
            catalogRelatedWordsAndWord(context)
            catalogSymbolDetails(context)
            catalogChapterFilters(context)
            catalogSymbolFilters(context)
            catalogStrokeFilters(context)
            catalogJlptFilters(context)

        }
    }
}

private fun NavHostController.openFilter(request: FilterRequest) {
    when (request) {
        is FilterRequest.ChaptersForKanji,
        is FilterRequest.Radical,
        is FilterRequest.KanjiStrokes,
        is FilterRequest.Jlpt -> openKanjiFacetFilter(request)
        is FilterRequest.ChaptersForWords ->
            navigate(
                WordChapterRoute(
                    request.sourceId,
                    request.filters.chapter,
                    request.filters.kanjiId,
                ),
            )
        is FilterRequest.Kanji ->
            navigate(
                KanjiChoiceRoute(
                    request.sourceId,
                    request.filters.chapter,
                    request.filters.kanjiId,
                ),
            )
        is FilterRequest.RadicalStrokes -> navigate(RadicalStrokesRoute(request.selected))
    }
}

private fun NavHostController.openKanjiFacetFilter(request: FilterRequest) {
    val destination =
        when (request) {
            is FilterRequest.ChaptersForKanji ->
                KanjiChapterRoute(
                    request.sourceId,
                    request.filters.chapter,
                    request.filters.radicalId,
                    request.filters.strokes,
                    request.filters.jlpt.savedValue(),
                )
            is FilterRequest.Radical ->
                RadicalChoiceRoute(
                    request.sourceId,
                    request.filters.chapter,
                    request.filters.radicalId,
                    request.filters.strokes,
                    request.filters.jlpt.savedValue(),
                )
            is FilterRequest.KanjiStrokes ->
                KanjiStrokesRoute(
                    request.sourceId,
                    request.filters.chapter,
                    request.filters.radicalId,
                    request.filters.strokes,
                    request.filters.jlpt.savedValue(),
                )
            is FilterRequest.Jlpt ->
                JlptChoiceRoute(
                    request.sourceId,
                    request.filters.chapter,
                    request.filters.radicalId,
                    request.filters.strokes,
                    request.filters.jlpt.savedValue(),
                )
            else -> return
        }
    navigate(destination)
}

private fun NavHostController.openBrowse(target: BrowseNavigation) {
when (target) {
    is BrowseNavigation.Source ->
        navigate(
            SourceRoute(
                target.sourceId,
                target.chapter,
                if (target.chapter == null) SourceTab.Chapters else SourceTab.Kanji,
            ),
        )
    is BrowseNavigation.Word -> navigate(WordRoute(target.wordId, target.sourceId))
    is BrowseNavigation.Kanji -> navigate(KanjiRoute(target.kanjiId, target.sourceId))
    is BrowseNavigation.Radical -> navigate(RadicalRoute(target.radicalId, null))
    BrowseNavigation.AllKanji -> navigate(AllKanjiRoute)
    BrowseNavigation.AllRadicals -> navigate(AllRadicalsRoute)
    is BrowseNavigation.Filter -> openFilter(target.request)
}
}

private fun NavHostController.openDetail(target: DetailNavigation) {
when (target) {
    is DetailNavigation.Kanji -> navigate(KanjiRoute(target.kanjiId, target.sourceId))
    is DetailNavigation.Radical ->
        navigate(RadicalRoute(target.radicalId, target.sourceId))
    is DetailNavigation.Words -> navigate(WordsRoute(target.kanjiId, target.sourceId))
}
}
