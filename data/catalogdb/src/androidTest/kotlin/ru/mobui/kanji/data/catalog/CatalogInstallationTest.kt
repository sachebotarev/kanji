package ru.mobui.kanji.data.catalog

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CatalogInstallationTest {
    @Test
    fun installsVerifiedAssetsAndUpdatesWithoutLosingPersonalLinksOrStatistics() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "catalog-test-${System.nanoTime()}.db"
        val db = CatalogDatabase.open(context, name)
        try {
            val installer = CatalogInstaller(db, instrumentation.context.assets)
            installer.install()
            val dao = db.catalogDao()
            val baseVersion = dao.catalogMetadata().single().contentVersion
            val nextVersion = baseVersion + 1
            assertEquals(2491, dao.words().size)
            assertEquals(3955, dao.sourceWords().size)
            assertEquals(3001, dao.kanji().size)
            assertEquals(2211, dao.kanji().count { it.jlptLevel != null })
            assertEquals(1026, dao.kanji().count { it.schoolStage == "elementary" })
            assertEquals(5, dao.kanji().single { it.id == 19968 }.jlptLevel)
            assertTrue(dao.wordsTranslations().any { !it.usageNote.isNullOrBlank() })
            assertTrue(dao.sourcesTranslations().any { !it.description.isNullOrBlank() })
            installer.install() // Same-version install is idempotent.
            dao.putSources(listOf(SourcesRow(-1, "personal", "personal", 0)))
            dao.putWords(listOf(WordsRow(-1, "私の例", "わたしのれい", 0)))
            dao.putSourceWords(
                listOf(SourceWordsRow(-1, -1, 1, null), SourceWordsRow(-2, -1, -1, null)),
            )
            val sql = db.openHelper.writableDatabase
            sql.execSQL(
                "CREATE TABLE user_statistics(word_id INTEGER PRIMARY KEY REFERENCES words(id), " +
                    "attempts INTEGER NOT NULL)",
            )
            sql.execSQL("INSERT INTO user_statistics VALUES(1,7)")
            val root =
                JSONObject(
                    instrumentation.context.assets.open("seed/catalog.json").bufferedReader().use {
                        it.readText()
                    },
                )
            root.put("content_version", nextVersion)
            root
                .getJSONObject("tables")
                .getJSONObject("catalog_metadata")
                .getJSONArray("rows")
                .getJSONArray(0)
                .put(2, nextVersion)
            val placements =
                root.getJSONObject("tables").getJSONObject("source_kanji").getJSONArray("rows")
            val first = placements.getJSONArray(0)
            first.put(3, 50) // Move chapter; identities and statistics stay unchanged.
            val readings =
                root.getJSONObject("tables").getJSONObject("kanji_readings").getJSONArray("rows")
            readings.remove(readings.length() - 1)
            installer.install(root, "version-two")
            assertTrue(dao.words().any { it.id == -1 })
            assertEquals(2, dao.sourceWords().count { it.sourceId == -1 })
            assertEquals(nextVersion, dao.catalogMetadata().single().contentVersion)
            assertTrue(dao.sourceKanji().any { it.chapterNumber == 50 })
            assertEquals(readings.length(), dao.kanjiReadings().size)
            sql.query("SELECT attempts FROM user_statistics WHERE word_id=1").use {
                assertTrue(it.moveToFirst())
                assertEquals(7, it.getInt(0))
            }
            installer.install(root, "version-two")
            assertEquals(2492, dao.words().size)
            val bad = JSONObject(root.toString()).put("content_version", nextVersion + 1)
            bad.getJSONObject("tables")
                .getJSONObject("catalog_metadata")
                .getJSONArray("rows")
                .getJSONArray(0)
                .put(2, nextVersion + 1)
            bad.getJSONObject("tables")
                .getJSONObject("source_words")
                .getJSONArray("rows")
                .getJSONArray(0)
                .put(2, 9999999)
            try {
                installer.install(bad, "bad-update")
                fail("Invalid foreign key must abort update")
            } catch (_: android.database.sqlite.SQLiteConstraintException) {}
            assertEquals(nextVersion, dao.catalogMetadata().single().contentVersion)
            assertEquals(2, dao.sourceWords().count { it.sourceId == -1 })
            assertEquals(3957, dao.sourceWords().size)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
