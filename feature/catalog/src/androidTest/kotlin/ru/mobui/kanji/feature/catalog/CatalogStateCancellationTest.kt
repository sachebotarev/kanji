package ru.mobui.kanji.feature.catalog

import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.domain.catalog.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class CatalogStateCancellationTest {
    @Test fun cancelledComputationCannotPublishStaleContentOrTurnIntoAnError() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val dispatcher = QueuedDispatcher()
        val input = MutableStateFlow("old")
        val data = CatalogSnapshot(1, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val repository = object : CatalogRepository { override suspend fun load() = data }
        val store = ViewModelStore()
        lateinit var model: TestOwner
        instrumentation.runOnMainSync { model = TestOwner(repository, dispatcher, input); store.put("owner", model) }
        try {
            val old = dispatcher.next()
            instrumentation.runOnMainSync { input.value = "new" }
            old.run() // The queued old computation has already been cancelled.
            instrumentation.waitForIdleSync()
            assertEquals(CatalogLoad.Loading, model.state.value)
            dispatcher.next().run()
            val result = runBlocking { withTimeout(5000) { model.state.filterIsInstance<CatalogLoad.Ready<String>>().first() } }
            assertEquals("new", result.content)
        } finally { instrumentation.runOnMainSync { store.clear() } }
    }

    private class TestOwner(repository: CatalogRepository, dispatcher: CoroutineDispatcher, private val input: MutableStateFlow<String>)
        : CatalogStateViewModel<String, String>(repository, dispatcher) {
        init { retry() }
        override fun inputs() = input
        override suspend fun content(snapshot: CatalogSnapshot, input: String) = input
    }
    private class QueuedDispatcher : CoroutineDispatcher() {
        private val queue = LinkedBlockingQueue<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) { queue.add(block) }
        fun next(): Runnable = requireNotNull(queue.poll(5, TimeUnit.SECONDS)) { "No computation was dispatched" }
    }
}
