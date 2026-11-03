@file:Suppress("MatchingDeclarationName")

package ru.mobui.kanji.feature.catalog.navigation

import ru.mobui.kanji.feature.catalog.AllKanjiRoute
import ru.mobui.kanji.feature.catalog.AllRadicalsRoute
import ru.mobui.kanji.feature.catalog.CatalogHome
import ru.mobui.kanji.feature.catalog.KanjiRoute
import ru.mobui.kanji.feature.catalog.RadicalRoute
import ru.mobui.kanji.feature.catalog.RadicalStrokesRoute
import ru.mobui.kanji.feature.catalog.WordRoute
import ru.mobui.kanji.feature.catalog.WordsRoute

import ru.mobui.kanji.feature.catalog.JlptChoiceRoute
import ru.mobui.kanji.feature.catalog.KanjiChapterRoute
import ru.mobui.kanji.feature.catalog.KanjiChoiceRoute
import ru.mobui.kanji.feature.catalog.KanjiStrokesRoute
import ru.mobui.kanji.feature.catalog.RadicalChoiceRoute
import ru.mobui.kanji.feature.catalog.SourceRoute
import ru.mobui.kanji.feature.catalog.WordChapterRoute
import ru.mobui.kanji.feature.catalog.browse.BrowseNavigation
import ru.mobui.kanji.feature.catalog.browse.BrowseRequest
import ru.mobui.kanji.feature.catalog.components.decodeJlpt
import ru.mobui.kanji.feature.catalog.detail.DetailNavigation
import ru.mobui.kanji.feature.catalog.detail.DetailRequest
import ru.mobui.kanji.feature.catalog.filter.FilterRequest
import ru.mobui.kanji.feature.catalog.filter.FilterResult
import ru.mobui.kanji.feature.catalog.presentation.CatalogBrowseEntry
import ru.mobui.kanji.feature.catalog.presentation.CatalogDetailEntry
import ru.mobui.kanji.feature.catalog.presentation.CatalogEntryContext
import ru.mobui.kanji.feature.catalog.presentation.CatalogFilterEntry
import ru.mobui.kanji.feature.catalog.presentation.KanjiFilters
import ru.mobui.kanji.feature.catalog.presentation.WordFilters

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
    val revision: Long = 0L,
) {
    val entryContext = CatalogEntryContext(repository, language, back, revision)
}

internal fun NavGraphBuilder.catalogBrowseSources(context: CatalogGraphContext) {
    with(context) {
        composable<CatalogHome> {
            CatalogBrowseEntry(
                it,
                BrowseRequest.Home,
                entryContext,
                browse,
            )
        }
        composable<SourceRoute> { entry ->
            val route = entry.toRoute<SourceRoute>()
            CatalogBrowseEntry(
                entry,
                BrowseRequest.Source(route.sourceId, route.chapter, route.tab),
                entryContext,
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
                entryContext,
                browse,
            )
        }
        composable<AllRadicalsRoute> {
            CatalogBrowseEntry(
                it,
                BrowseRequest.AllRadicals,
                entryContext,
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
                entryContext,
                browse,
            )
        }
        composable<WordRoute> { entry ->
            val route = entry.toRoute<WordRoute>()
            CatalogDetailEntry(
                entry,
                DetailRequest.Word(route.wordId, route.sourceId),
                entryContext,
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
                entryContext,
                loadDrawing,
                detail,
            )
        }
        composable<RadicalRoute> { entry ->
            val route = entry.toRoute<RadicalRoute>()
            CatalogDetailEntry(
                entry,
                DetailRequest.Radical(route.radicalId, route.sourceId),
                entryContext,
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
                entryContext,
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
                entryContext,
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
                entryContext,
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
                entryContext,
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
                entryContext,
                select,
            )
        }
        composable<RadicalStrokesRoute> { entry ->
            val route = entry.toRoute<RadicalStrokesRoute>()
            CatalogFilterEntry(
                entry,
                FilterRequest.RadicalStrokes(route.strokes),
                entryContext,
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
                entryContext,
                select,
            )
        }
    }
}

