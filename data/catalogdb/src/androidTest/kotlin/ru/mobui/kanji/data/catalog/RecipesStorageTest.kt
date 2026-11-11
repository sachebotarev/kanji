package ru.mobui.kanji.data.catalog

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.RecipeName
import ru.mobui.kanji.domain.catalog.TagColor
import ru.mobui.kanji.domain.catalog.TagDraft
import ru.mobui.kanji.domain.catalog.TagTranslation
import ru.mobui.kanji.domain.catalog.WordDraft
import ru.mobui.kanji.domain.catalog.WordTranslationDraft

class RecipesStorageTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun draft(text: String = "木が見えます。") = WordDraft(text, text,
        listOf(WordTranslationDraft("ru", "Видно дерево.")))
    private fun tag(name: String) = TagDraft(TagColor.Green, listOf(TagTranslation("ru", name)))
    private fun recipe(id: String, wordId: Int) = Recipe(id,
        listOf(RecipeName("ru", "Рецепт $id")),
        listOf(RecipeMaterial("$id-material", MaterialType.Words, wordIds = setOf(wordId))))

    private fun storage(test: suspend (CatalogDatabase, RoomCatalogRepository) -> Unit) = runBlocking {
        val context = instrumentation.targetContext
        val name = "recipes-${System.nanoTime()}.db"
        val db = CatalogDatabase.open(context, name)
        try { test(db, RoomCatalogRepository(db, instrumentation.context.assets, Dispatchers.IO)) }
        finally { db.close(); context.deleteDatabase(name) }
    }

    private suspend fun rejected(action: suspend () -> Unit) {
        try { action(); fail("Expected rejection") }
        catch (expected: IllegalArgumentException) { /* Domain validation. */ }
        catch (expected: android.database.sqlite.SQLiteConstraintException) { /* SQL guard. */ }
    }

    @Test fun schemaUsesRecipeAndMaterialNamesWithoutLegacyConfigurationTables() = storage { db, catalog ->
        catalog.load()
        val sql = db.openHelper.writableDatabase
        assertEquals(1, sql.version)
        val tables = sql.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        assertTrue(tables.containsAll(setOf("recipes", "recipes_translations", "recipe_materials",
            "recipe_materials_chapters", "recipe_materials_kanji", "recipe_materials_words", "recipe_materials_tags")))
        assertTrue(tables.containsAll(setOf("trainings", "training_tasks", "training_task_symbols")))
        listOf("chapters", "kanji", "words", "tags").forEach { suffix ->
            sql.query("PRAGMA foreign_key_list(recipe_materials_$suffix)").use { cursor ->
                var found = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(cursor.getColumnIndexOrThrow("table")) == "recipe_materials") {
                        assertEquals("material_id", cursor.getString(cursor.getColumnIndexOrThrow("from")))
                        found = true
                    }
                }
                assertTrue(found)
            }
        }
    }

    @Test fun wordAndNewTagsAreAtomicAndNamesUseUnicodeCaseFolding() = storage { db, catalog ->
        val recipesRepository = catalog.recipesRepository
        recipesRepository.saveTag(null, tag("Straße"))
        rejected { recipesRepository.saveTag(null, tag("STRASSE")) }
        rejected {
            catalog.saveWord(null, draft().copy(pendingTags = listOf(tag("Новый"), tag("strasse"))))
        }
        assertTrue(catalog.observeWords().first().isEmpty())
        assertEquals(1, recipesRepository.observeTags().first().size)
        val word = catalog.saveWord(null, draft().copy(pendingTags = listOf(tag("Выражения"))))
        val created = recipesRepository.observeTags().first().single { it.name("en") == "Выражения" }
        assertEquals(setOf(created.id), recipesRepository.observeWordTags().first().single().tagIds)
        catalog.saveWord(word, draft().copy(translations = listOf(WordTranslationDraft("ru", "Дерево видно."))))
        assertEquals(setOf(created.id), recipesRepository.observeWordTags().first().single().tagIds)
        recipesRepository.deleteTag(created.id)
        assertNotNull(db.catalogDao().word(word))
        assertTrue(recipesRepository.observeWordTags().first().isEmpty())
    }

    @Test fun recipesPersistReorderWithoutChangingSelectionAndRejectPartialOrStaleWrites() = storage { db, catalog ->
        val word = catalog.saveWord(null, draft())
        val recipesRepository = catalog.recipesRepository
        recipesRepository.saveRecipe(recipe("first", word))
        recipesRepository.saveRecipe(recipe("second", word))
        assertEquals("first", recipesRepository.observeRecipes().first().single { it.isActive }.id)
        recipesRepository.activateRecipe("second")
        recipesRepository.reorderRecipes(listOf("second", "first"))
        assertEquals(listOf("second", "first"), recipesRepository.observeRecipes().first().map { it.id })
        assertEquals("second", recipesRepository.observeRecipes().first().single { it.isActive }.id)
        rejected { recipesRepository.reorderRecipes(listOf("first")) }
        val original = recipesRepository.observeRecipes().first().single { it.id == "second" }
        recipesRepository.saveRecipe(original.copy(taskCount = 25))
        rejected { recipesRepository.saveRecipe(original.copy(taskCount = 50)) }
        assertEquals(25, recipesRepository.observeRecipes().first().single { it.id == "second" }.taskCount)
        recipesRepository.deleteRecipe("second")
        assertFalse(recipesRepository.observeRecipes().first().single().isActive)
        assertNotNull(db.catalogDao().word(word))
        assertEquals(1, db.recipesDao().recipes().size)
    }

    @Test fun dictionaryMaterialsIntersectTagsAndKanjiAndKeepRealSourceIdentity() = storage { db, catalog ->
        val word = catalog.saveWord(null, draft())
        val recipesRepository = catalog.recipesRepository
        val tagId = recipesRepository.saveTag(null, tag("Повторить"))
        recipesRepository.assignTags(word, setOf(tagId))
        val source = db.catalogDao().sources().single { it.sourceType == "personal" }
        val tree = catalog.load().kanji.single { it.symbol == "木" }.id
        val base = Recipe("dictionary", listOf(RecipeName("ru", "Мой словарь")),
            listOf(RecipeMaterial("personal-material", MaterialType.Dictionary, source.id,
                kanjiIds = setOf(tree), tagIds = setOf(tagId))))
        recipesRepository.saveRecipe(base)
        assertEquals(source.id, recipesRepository.observeRecipes().first().single().materials.single().sourceId)
        val otherTag = recipesRepository.saveTag(null, tag("Без слов"))
        recipesRepository.saveRecipe(base.copy(id = "or", materials = base.materials.map {
            it.copy(id = "or-material", tagIds = setOf(tagId, otherTag))
        }))
        rejected { recipesRepository.saveRecipe(base.copy(id = "empty", materials = base.materials.map {
            it.copy(id = "empty-material", tagIds = setOf(otherTag))
        })) }
        rejected { recipesRepository.saveRecipe(base.copy(id = "bad", materials = base.materials.map {
            it.copy(id = "bad-material", sourceId = 9999999)
        })) }
        recipesRepository.deleteTag(tagId)
        assertTrue(recipesRepository.observeRecipes().first().all { it.materials.single().tagIds == emptySet<Long>() || it.id == "or" })
    }

    @Test fun rawSqlGuardsAndForeignKeysAreInstalledOnOpen() = storage { db, catalog ->
        catalog.load()
        val sql = db.openHelper.writableDatabase
        rejected { sql.execSQL("INSERT INTO dictionary_tags(color,position,created_at,updated_at) VALUES('hex',0,0,0)") }
        val id = catalog.recipesRepository.saveTag(null, tag("Тег"))
        val catalogWord = db.catalogDao().words().first { it.isUserCreated == 0 }.id
        rejected { sql.execSQL("INSERT INTO word_tags(word_id,tag_id,created_at) VALUES($catalogWord,$id,0)") }
        val word = catalog.saveWord(null, draft())
        catalog.recipesRepository.saveRecipe(recipe("one", word))
        rejected { sql.execSQL("UPDATE recipes SET task_count=0 WHERE id='one'") }
    }

    @Test fun baselineChecksSurviveReopen() = runBlocking {
        val context = instrumentation.targetContext
        val name = "baseline-${System.nanoTime()}.db"
        try {
            repeat(2) {
                val db = CatalogDatabase.open(context, name)
                try {
                    val sql = db.openHelper.writableDatabase
                    assertEquals(1, sql.version)
                    rejected { sql.execSQL("INSERT INTO dictionary_tags(color,position,created_at,updated_at) VALUES('invalid',0,0,0)") }
                    RoomCatalogRepository(db, instrumentation.context.assets, Dispatchers.IO).load()
                } finally { db.close() }
            }
        } finally { context.deleteDatabase(name) }
    }
}
