package ru.mobui.kanji.data.catalog

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.domain.catalog.*

class PersonalDictionaryStorageTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private suspend fun expect(failure: DictionaryFailure, action: suspend () -> Unit) {
        try { action(); fail("Expected $failure") }
        catch (error: DictionaryException) { assertEquals(failure, error.failure) }
    }
    @Test fun savesObservesUpdatesDeletesAndRestoresManualWordWithoutLosingTranslations() = runBlocking {
        val context = instrumentation.targetContext
        val name = "personal-crud-${System.nanoTime()}.db"
        var db = CatalogDatabase.open(context, name)
        try {
            var repository = RoomCatalogRepository(db, instrumentation.context.assets, Dispatchers.IO)
            val draft = WordDraft("木漏れ日の例文です。", "こもれびのれいぶんです。", listOf(
                WordTranslationDraft("ru", "Предложение о солнечном свете."), WordTranslationDraft("en", "A sentence about sunlight.")))
            val pending = async { withTimeout(15_000) { repository.observeWords().first { it.isNotEmpty() } } }
            val id = repository.saveWord(null, draft)
            assertTrue(pending.await().single().isUserCreated)
            assertTrue(id < 0)
            assertEquals("Предложение о солнечном свете.", repository.observeWords().first().single().translation("de"))
            val edited = draft.copy(translations = listOf(WordTranslationDraft("ru", "Новый перевод"), WordTranslationDraft("pt-br", "Uma frase.")))
            repository.saveWord(id, edited)
            val word = repository.observeWords().first().single()
            assertEquals("Новый перевод", word.translation("en"))
            assertNull(word.translations["en"])
            assertEquals("Uma frase.", word.translation("pt-BR"))
            db.close()
            db = CatalogDatabase.open(context, name)
            repository = RoomCatalogRepository(db, instrumentation.context.assets, Dispatchers.IO)
            assertEquals(id, repository.observeWords().first().single().id)
            repository.deleteWord(id)
            assertTrue(repository.observeWords().first().isEmpty())
            assertEquals(1, db.catalogDao().word(id)?.is_archived)
            assertEquals(id, repository.saveWord(null, edited)) // Re-adding restores the same identity.
            assertEquals(1, repository.observeWords().first().size)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun rejectsCatalogMutationsAndDuplicatesAndPreservesOriginAcrossCatalogUpdates() = runBlocking {
        val context = instrumentation.targetContext
        val name = "personal-protection-${System.nanoTime()}.db"
        val db = CatalogDatabase.open(context, name)
        try {
            val repository = RoomCatalogRepository(db, instrumentation.context.assets, Dispatchers.IO)
            val catalog = repository.load()
            val supplied = catalog.words.first()
            val original = db.catalogDao().word(supplied.id)
            val draft = WordDraft(supplied.written, supplied.reading, listOf(WordTranslationDraft("ru", "Мой перевод")))
            expect(DictionaryFailure.ReadOnly) { repository.saveWord(supplied.id, draft) }
            expect(DictionaryFailure.ReadOnly) { repository.deleteWord(supplied.id) }
            expect(DictionaryFailure.Duplicate) { repository.saveWord(null, draft) }
            assertEquals(original, db.catalogDao().word(supplied.id))
            val manualDraft = WordDraft("手作りの例文です。", "てづくりのれいぶんです。", listOf(WordTranslationDraft("ru", "Ручная запись")))
            val id = repository.saveWord(null, manualDraft)
            expect(DictionaryFailure.Duplicate) { repository.saveWord(null, manualDraft) }
            expect(DictionaryFailure.Duplicate) { repository.saveWord(id, draft) }
            assertEquals(manualDraft.written, db.catalogDao().word(id)?.written_form)
            try {
                db.openHelper.writableDatabase.execSQL("UPDATE words SET is_user_created=1 WHERE id=${supplied.id}")
                fail("Origin must be immutable")
            } catch (_: android.database.sqlite.SQLiteConstraintException) { }
            val root = JSONObject(instrumentation.context.assets.open("seed/catalog.json").bufferedReader().use { it.readText() })
            val version = root.getInt("content_version") + 1
            root.put("content_version", version)
            root.getJSONObject("tables").getJSONObject("catalog_metadata").getJSONArray("rows").getJSONArray(0).put(2, version)
            CatalogInstaller(db, instrumentation.context.assets).install(root, "personal-update")
            assertEquals(1, db.catalogDao().word(id)?.is_user_created)
            assertEquals("Ручная запись", repository.observeWords().first().single().translation("ru"))
            // A future package cannot silently adopt the same manual identity.
            val conflict = JSONObject(root.toString()).put("content_version", version + 1)
            conflict.getJSONObject("tables").getJSONObject("catalog_metadata").getJSONArray("rows").getJSONArray(0).put(2, version + 1)
            val suppliedRows = conflict.getJSONObject("tables").getJSONObject("words").getJSONArray("rows")
            suppliedRows.getJSONArray(0).put(1, manualDraft.written).put(2, manualDraft.reading)
            try { CatalogInstaller(db, instrumentation.context.assets).install(conflict, "conflict"); fail("Manual collision must abort") }
            catch (_: IllegalArgumentException) { }
            assertEquals(version, db.catalogDao().catalog_metadata().single().content_version)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun migratesVersionOnePreservingDataAndExistingConstraints() = runBlocking {
        val context = instrumentation.targetContext
        val name = "personal-migration-${System.nanoTime()}.db"
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        val schema = JSONObject(instrumentation.context.assets.open("ru.mobui.kanji.data.catalog.CatalogDatabase/1.json")
            .bufferedReader().use { it.readText() }).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { sql ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                sql.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) sql.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) sql.execSQL(setup.getString(i))
            instrumentation.targetContext.assets.open("catalog-checks.sql").bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() }.forEach(sql::execSQL)
            }
            sql.execSQL("CREATE UNIQUE INDEX source_words_with_kanji ON source_words(source_id,word_id,kanji_id) WHERE kanji_id IS NOT NULL")
            sql.execSQL("CREATE UNIQUE INDEX source_words_without_kanji ON source_words(source_id,word_id) WHERE kanji_id IS NULL")
            sql.execSQL("INSERT INTO words VALUES(1,'猫','ねこ',0)")
            sql.execSQL("INSERT INTO words_translations VALUES(1,'ru','кошка')")
            sql.execSQL("INSERT INTO sources VALUES(-1,'personal','personal',0)")
            sql.execSQL("INSERT INTO source_words VALUES(-1,-1,1,NULL)")
            sql.version = 1
        }
        val db = CatalogDatabase.open(context, name)
        try {
            val dao = db.catalogDao()
            assertEquals(0, dao.word(1)?.is_user_created)
            assertEquals("кошка", dao.words_translations().single().translation)
            assertEquals(1, dao.source_words().size)
            assertEquals(2, db.openHelper.writableDatabase.version)
            try { db.openHelper.writableDatabase.execSQL("INSERT INTO source_words VALUES(-2,-1,1,NULL)"); fail("Partial unique index must survive") }
            catch (_: android.database.sqlite.SQLiteConstraintException) { }
            try { db.openHelper.writableDatabase.execSQL("INSERT INTO words VALUES(-1,'','ねこ',0,1)"); fail("Validation trigger must survive") }
            catch (_: android.database.sqlite.SQLiteConstraintException) { }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
