@file:Suppress("MatchingDeclarationName")

package ru.mobui.kanji.feature.catalog

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

import androidx.navigation.NavGraphBuilder
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing

internal data class CatalogGraphContext(
    val repository: CatalogRepository, val language: String, val back: () -> Unit,
    val loadDrawing: suspend (Int) -> ReferenceDrawing?,
    val browse: (BrowseNavigation) -> Unit, val detail: (DetailNavigation) -> Unit,
    val select: (FilterResult) -> Unit,
)

internal fun NavGraphBuilder.catalogBrowseSources(context: CatalogGraphContext) {
    with(context) {
        composable<CatalogHome> {
            CatalogBrowseEntry(
                it,
                BrowseRequest.Home,
                CatalogEntryContext(repository, language, back),
                browse,
            )
        }
        composable<SourceRoute> { entry ->
            val route = entry.toRoute<SourceRoute>()
            CatalogBrowseEntry(
                entry,
                BrowseRequest.Source(route.sourceId, route.chapter, route.tab),
                CatalogEntryContext(repository, language, back),
                browse,
            )
        }
    }
}

internal fun NavGraphBuilder.catalogBrowseLibrary(context: CatalogGraphContext) {
    with(context) {
        composable<AllKanjiRoute> {
            CatalogBrowseEntry(
                it,
                BrowseRequest.AllKanji,
                CatalogEntryContext(repository, language, back),
                browse,
            )
        }
        composable<AllRadicalsRoute> {
            CatalogBrowseEntry(
                it,
                BrowseRequest.AllRadicals,
                CatalogEntryContext(repository, language, back),
                browse,
            )
        }
    }
}

internal fun NavGraphBuilder.catalogRelatedWordsAndWord(context: CatalogGraphContext) {
    with(context) {
        composable<WordsRoute> { entry ->
            val route = entry.toRoute<WordsRoute>()
            CatalogBrowseEntry(
                entry,
                BrowseRequest.RelatedWords(route.kanjiId, route.sourceId),
                CatalogEntryContext(repository, language, back),
                browse,
            )
        }
        composable<WordRoute> { entry ->
            val route = entry.toRoute<WordRoute>()
            CatalogDetailEntry(
                entry,
                DetailRequest.Word(route.wordId, route.sourceId),
                CatalogEntryContext(repository, language, back),
                loadDrawing,
                detail,
            )
        }
    }
}

internal fun NavGraphBuilder.catalogSymbolDetails(context: CatalogGraphContext) {
    with(context) {
        composable<KanjiRoute> { entry ->
            val route = entry.toRoute<KanjiRoute>()
            CatalogDetailEntry(
                entry,
                DetailRequest.Kanji(route.kanjiId, route.sourceId),
                CatalogEntryContext(repository, language, back),
                loadDrawing,
                detail,
            )
        }
        composable<RadicalRoute> { entry ->
            val route = entry.toRoute<RadicalRoute>()
            CatalogDetailEntry(
                entry,
                DetailRequest.Radical(route.radicalId, route.sourceId),
                CatalogEntryContext(repository, language, back),
                loadDrawing,
                detail,
            )
        }
    }
}

internal fun NavGraphBuilder.catalogChapterFilters(context: CatalogGraphContext) {
    with(context) {
        composable<KanjiChapterRoute> { entry ->
            val route = entry.toRoute<KanjiChapterRoute>()
            CatalogFilterEntry(
                entry,
                FilterRequest.ChaptersForKanji(
                    route.sourceId,
                    KanjiFilters(
                        route.chapter,
                        route.radical,
                        route.strokes,
                        decodeJlpt(route.jlpt),
                    ),
                ),
                CatalogEntryContext(repository, language, back),
                select,
            )
        }
        composable<WordChapterRoute> { entry ->
            val route = entry.toRoute<WordChapterRoute>()
            CatalogFilterEntry(
                entry,
                FilterRequest.ChaptersForWords(
                    route.sourceId,
                    WordFilters(route.chapter, route.kanji),
                ),
                CatalogEntryContext(repository, language, back),
                select,
            )
        }
    }
}

internal fun NavGraphBuilder.catalogSymbolFilters(context: CatalogGraphContext) {
    with(context) {
        composable<KanjiChoiceRoute> { entry ->
            val route = entry.toRoute<KanjiChoiceRoute>()
            CatalogFilterEntry(
                entry,
                FilterRequest.Kanji(
                    route.sourceId,
                    WordFilters(route.chapter, route.selectedKanji),
                ),
                CatalogEntryContext(repository, language, back),
                select,
            )
        }
        composable<RadicalChoiceRoute> { entry ->
            val route = entry.toRoute<RadicalChoiceRoute>()
            CatalogFilterEntry(
                entry,
                FilterRequest.Radical(
                    route.sourceId,
                    KanjiFilters(
                        route.chapter,
                        route.radical,
                        route.strokes,
                        decodeJlpt(route.jlpt),
                    ),
                ),
                CatalogEntryContext(repository, language, back),
                select,
            )
        }
    }
}

internal fun NavGraphBuilder.catalogStrokeFilters(context: CatalogGraphContext) {
    with(context) {
        composable<KanjiStrokesRoute> { entry ->
            val route = entry.toRoute<KanjiStrokesRoute>()
            CatalogFilterEntry(
                entry,
                FilterRequest.KanjiStrokes(
                    route.sourceId,
                    KanjiFilters(
                        route.chapter,
                        route.radical,
                        route.strokes,
                        decodeJlpt(route.jlpt),
                    ),
                ),
                CatalogEntryContext(repository, language, back),
                select,
            )
        }
        composable<RadicalStrokesRoute> { entry ->
            val route = entry.toRoute<RadicalStrokesRoute>()
            CatalogFilterEntry(
                entry,
                FilterRequest.RadicalStrokes(route.strokes),
                CatalogEntryContext(repository, language, back),
                select,
            )
        }
    }
}

internal fun NavGraphBuilder.catalogJlptFilters(context: CatalogGraphContext) {
    with(context) {
        composable<JlptChoiceRoute> { entry ->
            val route = entry.toRoute<JlptChoiceRoute>()
            CatalogFilterEntry(
                entry,
                FilterRequest.Jlpt(
                    route.sourceId,
                    KanjiFilters(
                        route.chapter,
                        route.radical,
                        route.strokes,
                        decodeJlpt(route.jlpt),
                    ),
                ),
                CatalogEntryContext(repository, language, back),
                select,
            )
        }
    }
}

