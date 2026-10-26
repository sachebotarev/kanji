package ru.mobui.kanji.feature.catalog

import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import ru.mobui.kanji.domain.catalog.CatalogRepository

@Serializable private data object CatalogHome
@Serializable private data class SourceRoute(val sourceId: Int, val chapter: Int = 0, val tab: Int = 0)
@Serializable private data class WordRoute(val id: Int, val sourceId: Int = 0)
@Serializable private data class KanjiRoute(val id: Int, val sourceId: Int = 0)
@Serializable private data class RadicalRoute(val id: Int, val sourceId: Int = 0)
@Serializable private data class FilterRoute(val sourceId: Int, val chapter: Int, val radical: Int, val key: Boolean, val kanji: Int = 0)
@Serializable private data class KanjiFilterRoute(val sourceId: Int, val chapter: Int, val radical: Int, val selected: Int)
@Serializable private data object AllKanjiRoute
@Serializable private data object AllRadicalsRoute
@Serializable private data class WordsRoute(val kanjiId: Int, val sourceId: Int)

@Composable
fun CatalogRoute(repository: CatalogRepository, onBack: () -> Unit, initialKanjiId: Int = 0, initialSourceId: Int = 0) {
    val nav = rememberNavController()
    BackHandler { if (!nav.popBackStack()) onBack() }
    val language = LocalConfiguration.current.locales[0].toLanguageTag()
    @Composable
    fun Page(entry: NavBackStackEntry, initial: CatalogRequest) {
        val request = initial.copy(language = language)
        val model: CatalogViewModel = viewModel(entry, key = "catalog-$language", factory = viewModelFactory {
            initializer { CatalogViewModel(createSavedStateHandle(), repository, request) }
        })
        val state by model.state.collectAsStateWithLifecycle()
        val resultChapter by entry.savedStateHandle.getStateFlow("resultChapter", -1).collectAsStateWithLifecycle()
        val resultRadical by entry.savedStateHandle.getStateFlow("resultRadical", -1).collectAsStateWithLifecycle()
        val resultKanji by entry.savedStateHandle.getStateFlow("resultKanji", -1).collectAsStateWithLifecycle()
        LaunchedEffect(resultKanji) { if (resultKanji >= 0) { model.kanji(resultKanji); entry.savedStateHandle["resultKanji"] = -1 } }
        LaunchedEffect(resultChapter) { if (resultChapter >= 0) { model.chapter(resultChapter); entry.savedStateHandle["resultChapter"] = -1 } }
        LaunchedEffect(resultRadical) { if (resultRadical >= 0) { model.radical(resultRadical); entry.savedStateHandle["resultRadical"] = -1 } }
        CatalogScreen(state, CatalogActions(
            back = { if (!nav.popBackStack()) onBack() }, retry = model::retry,
            query = model::query, tab = model::tab, clear = model::clearFilters,
            clearChapter = { model.chapter(request.chapter) }, clearRadical = { model.radical(0) },
            source = { nav.navigate(SourceRoute(it)) },
            chapter = { nav.navigate(SourceRoute(request.sourceId, it, 1)) },
            word = { nav.navigate(WordRoute(it, request.sourceId)) },
            kanji = { nav.navigate(KanjiRoute(it, request.sourceId)) },
            radical = { nav.navigate(RadicalRoute(it, request.sourceId)) },
            filter = { key -> nav.navigate(FilterRoute(request.sourceId, state.chapter, if (state.tab == 2) 0 else state.radical, key, if (state.tab == 2) state.selectedKanji else 0)) },
            filterKanji = { nav.navigate(KanjiFilterRoute(request.sourceId, state.chapter, 0, state.selectedKanji)) },
            clearKanji = { model.kanji(0) },
            selectKanji = { value -> nav.previousBackStackEntry?.savedStateHandle?.set("resultKanji", value); nav.popBackStack() },
            selectFilter = { value ->
                nav.previousBackStackEntry?.savedStateHandle?.set(if (request.page == CatalogPage.RadicalFilter) "resultRadical" else "resultChapter", value)
                nav.popBackStack()
            },
            allKanji = { nav.navigate(AllKanjiRoute) }, allRadicals = { nav.navigate(AllRadicalsRoute) },
            allWords = { nav.navigate(WordsRoute(request.id, request.sourceId)) },
            studyKanji = { id, sourceId -> nav.navigate(KanjiRoute(id, sourceId)) },
        ))
    }
    val start: Any = remember(initialKanjiId, initialSourceId) {
        if (initialKanjiId != 0) KanjiRoute(initialKanjiId, initialSourceId)
        else if (initialSourceId != 0) SourceRoute(initialSourceId) else CatalogHome
    }
    NavHost(nav, startDestination = start) {
        composable<CatalogHome> { Page(it, CatalogRequest(CatalogPage.Home)) }
        composable<SourceRoute> { entry ->
            val route = entry.toRoute<SourceRoute>()
            Page(entry, CatalogRequest(CatalogPage.Source, route.sourceId, chapter = route.chapter, tab = route.tab))
        }
        composable<WordRoute> { entry -> val route = entry.toRoute<WordRoute>(); Page(entry, CatalogRequest(CatalogPage.Word, route.sourceId, route.id)) }
        composable<KanjiRoute> { entry -> val route = entry.toRoute<KanjiRoute>(); Page(entry, CatalogRequest(CatalogPage.Kanji, route.sourceId, route.id)) }
        composable<RadicalRoute> { entry -> val route = entry.toRoute<RadicalRoute>(); Page(entry, CatalogRequest(CatalogPage.Radical, route.sourceId, route.id)) }
        composable<FilterRoute> { entry ->
            val route = entry.toRoute<FilterRoute>()
            Page(entry, CatalogRequest(if (route.key) CatalogPage.RadicalFilter else CatalogPage.ChapterFilter,
                route.sourceId, id = route.kanji, chapter = route.chapter, radical = route.radical))
        }
        composable<KanjiFilterRoute> { entry ->
            val route = entry.toRoute<KanjiFilterRoute>()
            Page(entry, CatalogRequest(CatalogPage.KanjiFilter, route.sourceId, route.selected, route.chapter, route.radical))
        }
        composable<AllKanjiRoute> { Page(it, CatalogRequest(CatalogPage.AllKanji)) }
        composable<AllRadicalsRoute> { Page(it, CatalogRequest(CatalogPage.AllRadicals)) }
        composable<WordsRoute> { entry -> val route = entry.toRoute<WordsRoute>(); Page(entry, CatalogRequest(CatalogPage.Words, route.sourceId, route.kanjiId)) }
    }
}
