package ru.mobui.kanji.feature.catalog

import ru.mobui.kanji.feature.catalog.browse.BrowseContent
import ru.mobui.kanji.feature.catalog.browse.BrowseRequest
import ru.mobui.kanji.feature.catalog.browse.CatalogBrowseViewModel
import ru.mobui.kanji.feature.catalog.browse.SourceRows
import ru.mobui.kanji.feature.catalog.browse.SourceTab
import ru.mobui.kanji.feature.catalog.filter.FilterResult
import ru.mobui.kanji.feature.catalog.presentation.CatalogLoad

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogLink
import ru.mobui.kanji.domain.catalog.CatalogPlacement
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogSource
import ru.mobui.kanji.domain.catalog.CatalogWord

class CatalogBrowseStateTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val data =
        CatalogSnapshot(
            1,
            content =
                CatalogContent(
                    listOf(CatalogSource(1, "Test", emptyMap())),
                    listOf(
                        CatalogWord(1, "一", "いち", mapOf("ru" to "один")),
                        CatalogWord(2, "二", "に", mapOf("ru" to "два")),
                    ),
                    listOf(kanji(1, "一", 1), kanji(2, "二", 2)),
                    emptyList(),
                    listOf(CatalogPlacement(1, 1, 1, 3), CatalogPlacement(1, 2, 2, 4)),
                    listOf(CatalogLink(1, 1, 1), CatalogLink(1, 2, 2)),
                ),
        )
    private val repository =
        object : CatalogRepository {
            override suspend fun load() = data
        }

    @Test
    fun separateTabQueriesAndConditionsSurviveOwnerRecreation() {
        var store = ViewModelStore()
        lateinit var model: CatalogBrowseViewModel
        lateinit var saved: SavedStateHandle
        instrumentation.runOnMainSync {
            saved = SavedStateHandle()
            model = CatalogBrowseViewModel(saved, repository, BrowseRequest.Source(1), "ru")
            store.put("browse", model)
            model.tab(SourceTab.Kanji)
            model.toggleSearch()
            model.query("один")
            model.applyFilter(FilterResult.Strokes(1))
        }
        awaitSource(model) {
            it.tab == SourceTab.Kanji && (it.rows as? SourceRows.Kanji)?.rows?.entries?.size == 1
        }
        instrumentation.runOnMainSync { model.tab(SourceTab.Words) }
        awaitSource(model) { (it.rows as? SourceRows.Words)?.rows?.entries?.size == 2 }
        assertEquals("", model.search.value.query)
        assertFalse(model.search.value.open)
        instrumentation.runOnMainSync {
            model.toggleSearch()
            model.query("два")
        }
        awaitSource(model) {
            (it.rows as? SourceRows.Words)?.rows?.entries?.singleOrNull()?.id == 2
        }
        instrumentation.runOnMainSync {
            model.tab(SourceTab.Kanji)
            val restored = saved.keys().associateWith { saved.get<Any?>(it) }
            store.clear()
            store = ViewModelStore()
            model =
                CatalogBrowseViewModel(
                    SavedStateHandle(restored),
                    repository,
                    BrowseRequest.Source(1),
                    "ru",
                )
            store.put("browse", model)
        }
        try {
            val restored = awaitSource(model) { it.tab == SourceTab.Kanji }
            assertEquals("один", model.search.value.query)
            assertTrue(model.search.value.open)
            assertEquals(1, (restored.rows as SourceRows.Kanji).filters.strokes)
            assertEquals(listOf(1), restored.rows.rows.entries.map { it.id })
            instrumentation.runOnMainSync { model.tab(SourceTab.Words) }
            val words = awaitSource(model) { it.tab == SourceTab.Words }
            assertEquals("два", model.search.value.query)
            assertEquals(listOf(2), (words.rows as SourceRows.Words).rows.entries.map { it.id })
        } finally {
            instrumentation.runOnMainSync { store.clear() }
        }
    }

    @Test
    fun tabChangesNeverPublishResultsForAnotherTabsQuery() = runBlocking {
        val store = ViewModelStore()
        lateinit var model: CatalogBrowseViewModel
        instrumentation.runOnMainSync {
            model = CatalogBrowseViewModel(
                SavedStateHandle(mapOf(
                    "source.tab" to SourceTab.Kanji.name,
                    "source.Kanji.query" to "один",
                    "source.Words.query" to "два",
                )),
                repository,
                BrowseRequest.Source(1),
                "ru",
                Dispatchers.Unconfined,
            )
            store.put("browse", model)
        }
        val observed = CopyOnWriteArrayList<BrowseContent.Source>()
        val collector = launch(Dispatchers.Unconfined) {
            model.state.filterIsInstance<CatalogLoad.Ready<BrowseContent>>().collect {
                (it.content as? BrowseContent.Source)?.let(observed::add)
            }
        }
        try {
            repeat(10) {
                instrumentation.runOnMainSync { model.tab(SourceTab.Words) }
                awaitSource(model) { it.tab == SourceTab.Words }
                instrumentation.runOnMainSync { model.tab(SourceTab.Kanji) }
                awaitSource(model) { it.tab == SourceTab.Kanji }
            }
            assertTrue(observed.isNotEmpty())
            observed.forEach { content ->
                when (val rows = content.rows) {
                    is SourceRows.Kanji -> assertEquals(listOf(1), rows.rows.entries.map { it.id })
                    is SourceRows.Words -> assertEquals(listOf(2), rows.rows.entries.map { it.id })
                    is SourceRows.Chapters -> error("Unexpected chapters tab")
                }
            }
        } finally {
            collector.cancel()
            instrumentation.runOnMainSync { store.clear() }
        }
    }

    @Test
    fun revisionReloadsEditedAndDeletedWordsWithoutClearingSearchOrFilters() {
        val latest = AtomicReference(data)
        val changingRepository = object : CatalogRepository {
            override suspend fun load() = latest.get()
        }
        val store = ViewModelStore()
        lateinit var model: CatalogBrowseViewModel
        instrumentation.runOnMainSync {
            model = CatalogBrowseViewModel(
                SavedStateHandle(), changingRepository, BrowseRequest.Source(1), "ru",
            )
            store.put("browse", model)
            model.tab(SourceTab.Words)
            model.query("一")
            model.applyFilter(FilterResult.Chapter(3))
        }
        try {
            awaitSource(model) { (it.rows as? SourceRows.Words)?.rows?.entries?.size == 1 }
            val edited = data.words.first().copy(translations = mapOf("ru" to "Новый перевод"))
            latest.set(withWords(listOf(edited)))
            instrumentation.runOnMainSync { model.updateContext("ru", 1L) }
            awaitSource(model) {
                (it.rows as? SourceRows.Words)?.rows?.entries?.singleOrNull()
                    ?.translations?.get("ru") == "Новый перевод"
            }
            latest.set(withWords(emptyList()))
            instrumentation.runOnMainSync { model.updateContext("ru", 2L) }
            val deleted = awaitSource(model) {
                (it.rows as? SourceRows.Words)?.rows?.entries?.isEmpty() == true
            }
            assertEquals("一", model.search.value.query)
            assertEquals(3, (deleted.rows as SourceRows.Words).filters.chapter)
        } finally {
            instrumentation.runOnMainSync { store.clear() }
        }
    }

    private fun withWords(words: List<CatalogWord>) = CatalogSnapshot(
        data.version,
        CatalogContent(data.sources, words, data.kanji, data.radicals, data.placements, data.links),
    )

    private fun awaitSource(
        model: CatalogBrowseViewModel,
        matches: (BrowseContent.Source) -> Boolean,
    ): BrowseContent.Source = runBlocking {
        withTimeout(5000) {
            model.state
                .filterIsInstance<CatalogLoad.Ready<BrowseContent>>()
                .first { (it.content as? BrowseContent.Source)?.let(matches) == true }
                .content as BrowseContent.Source
        }
    }

    private fun kanji(id: Int, symbol: String, strokes: Int) =
        CatalogKanji(
            id,
            symbol,
            strokes,
            1,
            false,
            false,
            false,
            mapOf("ru" to listOf(if (id == 1) "один" else "два")),
            emptyList(),
            emptyList(),
        )
}
