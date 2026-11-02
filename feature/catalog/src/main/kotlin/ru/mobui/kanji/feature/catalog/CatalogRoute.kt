package ru.mobui.kanji.feature.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import ru.mobui.kanji.domain.catalog.CatalogRepository

@Serializable private data object CatalogHome
@Serializable private data class SourceRoute(val sourceId: Int, val chapter: Int? = null, val tab: SourceTab = SourceTab.Chapters)
@Serializable private data class WordRoute(val wordId: Int, val sourceId: Int?)
@Serializable private data class KanjiRoute(val kanjiId: Int, val sourceId: Int?)
@Serializable private data class RadicalRoute(val radicalId: Int, val sourceId: Int?)
@Serializable private data object AllKanjiRoute
@Serializable private data object AllRadicalsRoute
@Serializable private data class WordsRoute(val kanjiId: Int, val sourceId: Int?)
@Serializable private data class KanjiChapterRoute(val sourceId: Int, val chapter: Int?, val radical: Int?, val strokes: Int?, val jlpt: Int)
@Serializable private data class WordChapterRoute(val sourceId: Int, val chapter: Int?, val kanji: Int?)
@Serializable private data class KanjiChoiceRoute(val sourceId: Int, val chapter: Int?, val selectedKanji: Int?)
@Serializable private data class RadicalChoiceRoute(val sourceId: Int?, val chapter: Int?, val radical: Int?, val strokes: Int?, val jlpt: Int)
@Serializable private data class KanjiStrokesRoute(val sourceId: Int?, val chapter: Int?, val radical: Int?, val strokes: Int?, val jlpt: Int)
@Serializable private data class RadicalStrokesRoute(val strokes: Int?)
@Serializable private data class JlptChoiceRoute(val sourceId: Int?, val chapter: Int?, val radical: Int?, val strokes: Int?, val jlpt: Int)

@Composable
fun CatalogRoute(repository: CatalogRepository, onBack: () -> Unit, initialKanjiId: Int = 0, initialSourceId: Int = 0,
    loadDrawing: suspend (Int) -> ru.mobui.kanji.domain.handwriting.ReferenceDrawing? = { null }) {
    val nav = rememberNavController()
    val back: () -> Unit = { if (!nav.popBackStack()) onBack() }
    BackHandler(onBack = back)
    val language = LocalConfiguration.current.locales[0].toLanguageTag()
    val browse: (BrowseNavigation) -> Unit = { target ->
        when (target) {
            is BrowseNavigation.Source -> nav.navigate(SourceRoute(target.sourceId, target.chapter, if (target.chapter == null) SourceTab.Chapters else SourceTab.Kanji))
            is BrowseNavigation.Word -> nav.navigate(WordRoute(target.wordId, target.sourceId))
            is BrowseNavigation.Kanji -> nav.navigate(KanjiRoute(target.kanjiId, target.sourceId))
            is BrowseNavigation.Radical -> nav.navigate(RadicalRoute(target.radicalId, null))
            BrowseNavigation.AllKanji -> nav.navigate(AllKanjiRoute)
            BrowseNavigation.AllRadicals -> nav.navigate(AllRadicalsRoute)
            is BrowseNavigation.Filter -> nav.openFilter(target.request)
        }
    }
    val detail: (DetailNavigation) -> Unit = { target ->
        when (target) {
            is DetailNavigation.Kanji -> nav.navigate(KanjiRoute(target.kanjiId, target.sourceId))
            is DetailNavigation.Radical -> nav.navigate(RadicalRoute(target.radicalId, target.sourceId))
            is DetailNavigation.Words -> nav.navigate(WordsRoute(target.kanjiId, target.sourceId))
        }
    }
    val select: (FilterResult) -> Unit = { result ->
        val (key, value) = result.encoded()
        nav.previousBackStackEntry?.savedStateHandle?.set(key, value)
        nav.popBackStack()
    }
    val start: Any = remember(initialKanjiId, initialSourceId) {
        if (initialKanjiId != 0) KanjiRoute(initialKanjiId, initialSourceId.takeIf { it != 0 })
        else if (initialSourceId != 0) SourceRoute(initialSourceId) else CatalogHome
    }
    CatalogTheme {
        NavHost(nav, startDestination = start) {
            composable<CatalogHome> { CatalogBrowseEntry(it, BrowseRequest.Home, language, repository, back, browse) }
            composable<SourceRoute> { entry -> val route = entry.toRoute<SourceRoute>()
                CatalogBrowseEntry(entry, BrowseRequest.Source(route.sourceId, route.chapter, route.tab), language, repository, back, browse) }
            composable<AllKanjiRoute> { CatalogBrowseEntry(it, BrowseRequest.AllKanji, language, repository, back, browse) }
            composable<AllRadicalsRoute> { CatalogBrowseEntry(it, BrowseRequest.AllRadicals, language, repository, back, browse) }
            composable<WordsRoute> { entry -> val route = entry.toRoute<WordsRoute>()
                CatalogBrowseEntry(entry, BrowseRequest.RelatedWords(route.kanjiId, route.sourceId), language, repository, back, browse) }
            composable<WordRoute> { entry -> val route = entry.toRoute<WordRoute>()
                CatalogDetailEntry(entry, DetailRequest.Word(route.wordId, route.sourceId), language, repository, loadDrawing, back, detail) }
            composable<KanjiRoute> { entry -> val route = entry.toRoute<KanjiRoute>()
                CatalogDetailEntry(entry, DetailRequest.Kanji(route.kanjiId, route.sourceId), language, repository, loadDrawing, back, detail) }
            composable<RadicalRoute> { entry -> val route = entry.toRoute<RadicalRoute>()
                CatalogDetailEntry(entry, DetailRequest.Radical(route.radicalId, route.sourceId), language, repository, loadDrawing, back, detail) }
            composable<KanjiChapterRoute> { entry -> val route = entry.toRoute<KanjiChapterRoute>()
                CatalogFilterEntry(entry, FilterRequest.ChaptersForKanji(route.sourceId, KanjiFilters(route.chapter, route.radical, route.strokes, decodeJlpt(route.jlpt))), language, repository, back, select) }
            composable<WordChapterRoute> { entry -> val route = entry.toRoute<WordChapterRoute>()
                CatalogFilterEntry(entry, FilterRequest.ChaptersForWords(route.sourceId, WordFilters(route.chapter, route.kanji)), language, repository, back, select) }
            composable<KanjiChoiceRoute> { entry -> val route = entry.toRoute<KanjiChoiceRoute>()
                CatalogFilterEntry(entry, FilterRequest.Kanji(route.sourceId, WordFilters(route.chapter, route.selectedKanji)), language, repository, back, select) }
            composable<RadicalChoiceRoute> { entry -> val route = entry.toRoute<RadicalChoiceRoute>()
                CatalogFilterEntry(entry, FilterRequest.Radical(route.sourceId, KanjiFilters(route.chapter, route.radical, route.strokes, decodeJlpt(route.jlpt))), language, repository, back, select) }
            composable<KanjiStrokesRoute> { entry -> val route = entry.toRoute<KanjiStrokesRoute>()
                CatalogFilterEntry(entry, FilterRequest.KanjiStrokes(route.sourceId, KanjiFilters(route.chapter, route.radical, route.strokes, decodeJlpt(route.jlpt))), language, repository, back, select) }
            composable<RadicalStrokesRoute> { entry -> val route = entry.toRoute<RadicalStrokesRoute>()
                CatalogFilterEntry(entry, FilterRequest.RadicalStrokes(route.strokes), language, repository, back, select) }
            composable<JlptChoiceRoute> { entry -> val route = entry.toRoute<JlptChoiceRoute>()
                CatalogFilterEntry(entry, FilterRequest.Jlpt(route.sourceId, KanjiFilters(route.chapter, route.radical, route.strokes, decodeJlpt(route.jlpt))), language, repository, back, select) }
        }
    }
}

private fun NavHostController.openFilter(request: FilterRequest) {
    when (request) {
        is FilterRequest.ChaptersForKanji -> with(request.filters) { navigate(KanjiChapterRoute(request.sourceId, chapter, radicalId, strokes, jlpt.savedValue())) }
        is FilterRequest.ChaptersForWords -> navigate(WordChapterRoute(request.sourceId, request.filters.chapter, request.filters.kanjiId))
        is FilterRequest.Kanji -> navigate(KanjiChoiceRoute(request.sourceId, request.filters.chapter, request.filters.kanjiId))
        is FilterRequest.Radical -> with(request.filters) { navigate(RadicalChoiceRoute(request.sourceId, chapter, radicalId, strokes, jlpt.savedValue())) }
        is FilterRequest.KanjiStrokes -> with(request.filters) { navigate(KanjiStrokesRoute(request.sourceId, chapter, radicalId, strokes, jlpt.savedValue())) }
        is FilterRequest.RadicalStrokes -> navigate(RadicalStrokesRoute(request.selected))
        is FilterRequest.Jlpt -> with(request.filters) { navigate(JlptChoiceRoute(request.sourceId, chapter, radicalId, strokes, jlpt.savedValue())) }
    }
}
