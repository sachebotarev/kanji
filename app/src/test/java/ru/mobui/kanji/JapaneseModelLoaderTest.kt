package ru.mobui.kanji

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class JapaneseModelLoaderTest {
    @Test fun existingModelDoesNotDownloadAgain() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val loader = JapaneseModelLoader(scope, { true }, { error("Unexpected download") })
            loader.load()
            assertEquals(JapaneseModelStatus.Ready, loader.status.value)
            assertFalse(loader.error.value)
        } finally { scope.cancel() }
    }

    @Test fun failureCanBeDismissedAndRetriedWithoutDuplicateDownloads() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var calls = 0
            val completion = CompletableDeferred<Boolean>()
            val loader = JapaneseModelLoader(scope, { false }, {
                calls++
                if (calls == 1) throw IllegalStateException("Offline")
                completion.await()
            })
            loader.load()
            assertEquals(JapaneseModelStatus.Missing, loader.status.value)
            assertTrue(loader.error.value)
            loader.dismissError()
            assertFalse(loader.error.value)
            loader.load()
            loader.load()
            assertEquals(2, calls)
            assertEquals(JapaneseModelStatus.Loading, loader.status.value)
            completion.complete(true)
            assertEquals(JapaneseModelStatus.Ready, loader.status.value)
            loader.load()
            assertEquals(2, calls)
        } finally { scope.cancel() }
    }

    @Test fun unsuccessfulDownloadExposesRetryableError() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val loader = JapaneseModelLoader(scope, { false }, { false })
            loader.load()
            assertEquals(JapaneseModelStatus.Missing, loader.status.value)
            assertTrue(loader.error.value)
        } finally { scope.cancel() }
    }
}
