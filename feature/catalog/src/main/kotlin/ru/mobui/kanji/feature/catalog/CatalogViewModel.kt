package ru.mobui.kanji.feature.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.mobui.kanji.domain.catalog.*

internal enum class CatalogPage { Home, Source, Word, Kanji, Radical, ChapterFilter, RadicalFilter, KanjiFilter, AllKanji, AllRadicals, Words }
internal data class CatalogRequest(val page: CatalogPage, val sourceId: Int = 0, val id: Int = 0,
    val chapter: Int = 0, val radical: Int = 0, val tab: Int = 0, val language: String = "ru")
internal data class CatalogUiState(val request: CatalogRequest, val snapshot: CatalogSnapshot? = null,
    val loading: Boolean = true, val error: Boolean = false, val query: String = "", val tab: Int = 0,
    val chapter: Int = 0, val radical: Int = 0, val selectedKanji: Int = 0, val chapters: List<CatalogChapter> = emptyList(),
    val kanji: List<CatalogKanji> = emptyList(), val words: List<CatalogWord> = emptyList(),
    val radicals: List<CatalogRadical> = emptyList())

private data class BrowseInputs(val query: String, val tab: Int, val chapter: Int, val radical: Int, val kanji: Int)

internal class CatalogViewModel(private val saved: SavedStateHandle, private val repository: CatalogRepository,
    private val request: CatalogRequest, private val compute: CoroutineDispatcher = Dispatchers.Default) : ViewModel() {
    private val mutable = MutableStateFlow(CatalogUiState(request))
    val state = mutable.asStateFlow()
    private var job: Job? = null
    init { retry() }
    fun query(value: String) {
        // TextField's edit must be acknowledged synchronously; filtering stays asynchronous.
        mutable.value = mutable.value.copy(query = value)
        saved["query"] = value
    }
    fun tab(value: Int) {
        val previous = saved.get<Int>("tab") ?: request.tab
        saved["query-$previous"] = saved.get<String>("query").orEmpty()
        saved["query"] = saved.get<String>("query-$value").orEmpty()
        saved["tab"] = value
    }
    fun chapter(value: Int) { saved["chapter"] = value }
    fun kanji(value: Int) { saved["kanji"] = value }
    fun radical(value: Int) { saved["radical"] = value }
    fun clearFilters() { chapter(request.chapter); radical(0) }
    fun retry() {
        job?.cancel()
        job = viewModelScope.launch {
            mutable.value = mutable.value.copy(loading = true, error = false)
            try {
                val snapshot = repository.load()
                combine(saved.getStateFlow("query", ""), saved.getStateFlow("tab", request.tab),
                    saved.getStateFlow("chapter", request.chapter), saved.getStateFlow("radical", request.radical),
                    saved.getStateFlow("kanji", if (request.page in setOf(CatalogPage.KanjiFilter, CatalogPage.ChapterFilter, CatalogPage.RadicalFilter)) request.id else 0)) {
                        query, tab, chapter, radical, kanji -> BrowseInputs(query, tab, chapter, radical, kanji)
                    }.collectLatest { values ->
                    val query = values.query
                    val tab = values.tab
                    val chapter = values.chapter
                    val radical = values.radical
                    val next = withContext(compute) {
                        val selection = CatalogSelection(request.sourceId.takeIf { it != 0 }, chapter.takeIf { it != 0 },
                            (if (request.page == CatalogPage.Radical) request.id else if (request.page == CatalogPage.Source && tab == 2) 0 else radical).takeIf { it != 0 },
                            if (request.page == CatalogPage.Kanji || request.page == CatalogPage.Words) request.id else values.kanji.takeIf { it != 0 })
                        val kanjiSelection = if (request.page == CatalogPage.Source) selection.copy(radicalId = radical.takeIf { it != 0 }, kanjiId = null) else selection
                        val wordSelection = if (request.page == CatalogPage.Source) selection.copy(radicalId = null, kanjiId = values.kanji.takeIf { it != 0 }) else selection
                        CatalogUiState(request, snapshot, loading = false, query = query, tab = tab, chapter = chapter,
                            radical = radical, selectedKanji = values.kanji,
                            chapters = (if (request.page == CatalogPage.ChapterFilter) snapshot.availableChapters(selection) else snapshot.chapters(request.sourceId)).filter { query.isBlank() || it.number.toString().contains(query.trim()) },
                            kanji = if (request.page in setOf(CatalogPage.Source, CatalogPage.Radical, CatalogPage.AllKanji, CatalogPage.KanjiFilter))
                                snapshot.kanji(kanjiSelection, if (request.page == CatalogPage.Source && tab != 1) "" else query, request.language) else emptyList(),
                            words = if (request.page in setOf(CatalogPage.Source, CatalogPage.Kanji, CatalogPage.Words))
                                snapshot.words(wordSelection, if (request.page == CatalogPage.Source && tab != 2) "" else query, request.language) else emptyList(),
                            radicals = if (request.page in setOf(CatalogPage.AllRadicals, CatalogPage.RadicalFilter))
                                (if (request.page == CatalogPage.RadicalFilter) snapshot.availableRadicals(selection, query, request.language)
                                 else snapshot.radicals(query, request.language)) else emptyList())
                    }
                    mutable.value = next
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.value = mutable.value.copy(loading = false, error = true) }
        }
    }
}
