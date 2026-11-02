package ru.mobui.kanji.feature.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
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
        assertEquals("", model.search.value.query)
        assertFalse(model.search.value.open)
        awaitSource(model) { (it.rows as? SourceRows.Words)?.rows?.entries?.size == 2 }
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
