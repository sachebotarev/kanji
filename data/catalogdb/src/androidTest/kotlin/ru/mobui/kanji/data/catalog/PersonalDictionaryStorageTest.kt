package ru.mobui.kanji.data.catalog

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import ru.mobui.kanji.domain.catalog.DictionaryException
import ru.mobui.kanji.domain.catalog.DictionaryFailure
import ru.mobui.kanji.domain.catalog.WordDraft
import ru.mobui.kanji.domain.catalog.WordTranslationDraft
import ru.mobui.kanji.domain.catalog.translation

class PersonalDictionaryStorageTest {
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private suspend fun expect(failure: DictionaryFailure, action: suspend () -> Unit) {
        try {
            action()
            fail("Expected $failure")
        } catch (error: DictionaryException) {
            assertEquals(failure, error.failure)
        }
    }

    @Test
    fun savesObservesUpdatesDeletesAndRestoresManualWordWithoutLosingTranslations() = runBlocking {
        val context = instrumentation.targetContext
        val name = "personal-crud-${System.nanoTime()}.db"
        var db = CatalogDatabase.open(context, name)
        try {
            var repository =
                RoomCatalogRepository(db, instrumentation.context.assets, Dispatchers.IO)
            val draft =
                WordDraft(
                    "木漏れ日の例文です。",
                    "こもれびのれいぶんです。",
                    listOf(
                        WordTranslationDraft("ru", "Предложение о солнечном свете."),
                        WordTranslationDraft("en", "A sentence about sunlight."),
                    ),
                )
            val pending = async {
                withTimeout(15_000) { repository.observeWords().first { it.isNotEmpty() } }
            }
            assertEquals(0L, repository.revision.value)
            val id = repository.saveWord(null, draft)
            assertEquals(1L, repository.revision.value)
            assertEquals("木漏れ日の例文です。", repository.load().wordById[id]?.written)
            assertTrue(pending.await().single().isUserCreated)
            assertTrue(id < 0)
            assertEquals(
                "Предложение о солнечном свете.",
                repository.observeWords().first().single().translation("de"),
            )
            val edited =
                draft.copy(
                    translations =
                        listOf(
                            WordTranslationDraft("ru", "Новый перевод"),
                            WordTranslationDraft("pt-br", "Uma frase."),
                        ),
                )
            repository.saveWord(id, edited)
            assertEquals(2L, repository.revision.value)
            assertEquals("Новый перевод", repository.load().wordById[id]?.translation("ru"))
            val word = repository.observeWords().first().single()
            assertEquals("Новый перевод", word.translation("en"))
            assertNull(word.translations["en"])
            assertEquals("Uma frase.", word.translation("pt-BR"))
            db.close()
            db = CatalogDatabase.open(context, name)
            repository = RoomCatalogRepository(db, instrumentation.context.assets, Dispatchers.IO)
            assertEquals(id, repository.observeWords().first().single().id)
            repository.deleteWord(id)
            assertEquals(1L, repository.revision.value)
            assertNull(repository.load().wordById[id])
            assertTrue(repository.observeWords().first().isEmpty())
            assertEquals(1, db.catalogDao().word(id)?.isArchived)
            assertEquals(
                id,
                repository.saveWord(null, edited),
            ) // Re-adding restores the same identity.
            assertEquals(1, repository.observeWords().first().size)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun rejectsCatalogMutationsAndDuplicatesAndPreservesOriginAcrossCatalogUpdates() = runBlocking {
        val context = instrumentation.targetContext
        val name = "personal-protection-${System.nanoTime()}.db"
        val db = CatalogDatabase.open(context, name)
        try {
            val repository =
                RoomCatalogRepository(db, instrumentation.context.assets, Dispatchers.IO)
            val catalog = repository.load()
            val supplied = catalog.words.first()
            val original = db.catalogDao().word(supplied.id)
            val draft =
                WordDraft(
                    supplied.written,
                    supplied.reading,
                    listOf(WordTranslationDraft("ru", "Мой перевод")),
                )
            expect(DictionaryFailure.ReadOnly) { repository.saveWord(supplied.id, draft) }
            expect(DictionaryFailure.ReadOnly) { repository.deleteWord(supplied.id) }
            expect(DictionaryFailure.Duplicate) { repository.saveWord(null, draft) }
            assertEquals(0L, repository.revision.value)
            assertEquals(original, db.catalogDao().word(supplied.id))
            val manualDraft =
                WordDraft(
                    "手作りの例文です。",
                    "てづくりのれいぶんです。",
                    listOf(WordTranslationDraft("ru", "Ручная запись")),
                )
            val id = repository.saveWord(null, manualDraft)
            expect(DictionaryFailure.Duplicate) { repository.saveWord(null, manualDraft) }
            expect(DictionaryFailure.Duplicate) { repository.saveWord(id, draft) }
            assertEquals(1L, repository.revision.value)
            assertEquals(manualDraft.written, db.catalogDao().word(id)?.writtenForm)
            try {
                db.openHelper.writableDatabase.execSQL(
                    "UPDATE words SET is_user_created=1 WHERE id=${supplied.id}",
                )
                fail("Origin must be immutable")
            } catch (_: android.database.sqlite.SQLiteConstraintException) {}
            val root =
                JSONObject(
                    instrumentation.context.assets.open("seed/catalog.json").bufferedReader().use {
                        it.readText()
                    },
                )
            val version = root.getInt("content_version") + 1
            root.put("content_version", version)
            root
                .getJSONObject("tables")
                .getJSONObject("catalog_metadata")
                .getJSONArray("rows")
                .getJSONArray(0)
                .put(2, version)
            CatalogInstaller(db, instrumentation.context.assets).install(root, "personal-update")
            assertEquals(1, db.catalogDao().word(id)?.isUserCreated)
            assertEquals(
                "Ручная запись",
                repository.observeWords().first().single().translation("ru"),
            )
            // A future package cannot silently adopt the same manual identity.
            val conflict = JSONObject(root.toString()).put("content_version", version + 1)
            conflict
                .getJSONObject("tables")
                .getJSONObject("catalog_metadata")
                .getJSONArray("rows")
                .getJSONArray(0)
                .put(2, version + 1)
            val suppliedRows =
                conflict.getJSONObject("tables").getJSONObject("words").getJSONArray("rows")
            suppliedRows.getJSONArray(0).put(1, manualDraft.written).put(2, manualDraft.reading)
            try {
                CatalogInstaller(db, instrumentation.context.assets).install(conflict, "conflict")
                fail("Manual collision must abort")
            } catch (_: IllegalArgumentException) {}
            assertEquals(version, db.catalogDao().catalogMetadata().single().contentVersion)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
