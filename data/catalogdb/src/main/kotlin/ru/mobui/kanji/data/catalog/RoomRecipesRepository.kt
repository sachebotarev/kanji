package ru.mobui.kanji.data.catalog

import android.icu.lang.UCharacter
import androidx.room.withTransaction
import java.text.Normalizer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.RecipesRepository
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.TagColor
import ru.mobui.kanji.domain.catalog.TagDraft
import ru.mobui.kanji.domain.catalog.TagTranslation
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.RecipeName
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.RecipeValidationException
import ru.mobui.kanji.domain.catalog.WordTagAssignment
import ru.mobui.kanji.domain.catalog.validated

internal class RoomRecipesRepository(
    private val database: CatalogDatabase,
    private val prepare: suspend () -> Unit,
    private val io: CoroutineDispatcher,
) : RecipesRepository {
    private val dao get() = database.recipesDao()

    private fun <T> installed(observe: () -> Flow<T>): Flow<T> = flow {
        prepare()
        emitAll(observe())
    }.flowOn(io)

    override fun observeTags(): Flow<List<DictionaryTag>> = installed {
        dao.observeTags().map { rows -> rows.map { row ->
            DictionaryTag(row.tag.id, TagColor.entries.single { it.key == row.tag.color },
                row.translations.map { TagTranslation(it.languageTag, it.name) })
        } }
    }
    override fun observeWordTags(): Flow<List<WordTagAssignment>> = installed {
        dao.observeWordTags().map { rows -> rows.groupBy { it.wordId }.map { (id, tags) ->
            WordTagAssignment(id, tags.map { it.tagId }.toSet())
        } }
    }
    override fun observeRecipes(): Flow<List<Recipe>> = installed {
        dao.observeRecipes().map { rows -> rows.map { row ->
            Recipe(row.recipe.id,
                row.names.map { RecipeName(it.languageTag, it.name) },
                row.materials.sortedBy { it.material.position }.map { material ->
                    RecipeMaterial(material.material.id,
                        MaterialType.entries.single { it.key == material.material.materialType },
                        material.material.sourceId,
                        material.chapters.map { it.chapterNumber }.toSet(),
                        material.kanji.map { it.kanjiId }.toSet(),
                        material.words.map { it.wordId }.toSet(),
                        material.tags.map { it.tagId }.toSet(),
                        material.jlpt.map { it.jlptLevel }.toSet())
                }, row.recipe.taskCount, row.recipe.secondsPerSymbol, row.recipe.isActive,
                row.recipe.revision)
        } }
    }

    override suspend fun saveTag(id: Long?, draft: TagDraft): Long = withContext(io) {
        val validated = draft.validated()
        prepare()
        database.withTransaction { saveTagInTransaction(database, id, validated) }
    }
    override suspend fun deleteTag(id: Long) = withContext(io) {
        prepare()
        database.withTransaction { dao.deleteTag(id) }
    }
    override suspend fun assignTags(wordId: Int, tagIds: Set<Long>) = withContext(io) {
        prepare()
        database.withTransaction { assignTagsInTransaction(database, wordId, tagIds) }
    }
    override suspend fun saveRecipe(recipe: Recipe) = withContext(io) {
        val valid = recipe.validated()
        prepare()
        database.withTransaction {
            validateMaterials(valid.materials)
            val previous = dao.recipe(valid.id)
            if (previous != null && (previous.isArchived || previous.revision != valid.revision)) {
                throw RecipeValidationException(
                    "Recipe ${valid.id} revision conflict or archived (expected ${valid.revision})",
                )
            }
            val now = System.currentTimeMillis()
            val rows = dao.recipes()
            val active = previous?.isActive ?: (rows.isEmpty() || valid.isActive)
            if (active) dao.deactivateAll()
            dao.putRecipe(RecipesRow(valid.id, previous?.position ?: ((rows.maxOfOrNull { it.position } ?: -1) + 1),
                active, valid.taskCount, secondsPerSymbol = valid.secondsPerSymbol,
                revision = (previous?.revision ?: 0) + 1, createdAt = previous?.createdAt ?: now, updatedAt = now))
            dao.clearNames(valid.id)
            dao.insertNames(valid.names.map { RecipesTranslationsRow(valid.id, it.languageTag, it.name) })
            // Remove children before changing type; all replacements are one transaction.
            dao.clearMaterials(valid.id)
            valid.materials.forEachIndexed { index, material ->
                dao.insertMaterial(RecipeMaterialsRow(material.id, valid.id, index, material.type.key, material.sourceId))
                dao.insertChapters(material.chapters.map { RecipeMaterialChaptersRow(material.id, it) })
                dao.insertKanji(material.kanjiIds.map { RecipeMaterialKanjiRow(material.id, it) })
                dao.insertWords(material.wordIds.map { RecipeMaterialWordsRow(material.id, it) })
                dao.insertTags(material.tagIds.map { RecipeMaterialTagsRow(material.id, it) })
                dao.insertJlpt(material.jlptLevels.map { RecipeMaterialJlptRow(material.id, it) })
            }
        }
    }
    override suspend fun activateRecipe(id: String) = withContext(io) {
        prepare()
        database.withTransaction {
            require(dao.recipe(id)?.isArchived == false)
            dao.deactivateAll()
            dao.activate(id)
        }
    }
    override suspend fun reorderRecipes(ids: List<String>) = withContext(io) {
        prepare()
        database.withTransaction {
            val current = dao.recipes().map { it.id }.toSet()
            require(ids.size == current.size && ids.toSet() == current)
            val now = System.currentTimeMillis()
            ids.forEachIndexed { position, id -> dao.position(id, position, now) }
        }
    }
    override suspend fun deleteRecipe(id: String) = withContext(io) {
        prepare()
        database.withTransaction {
            val previous = requireNotNull(dao.recipe(id))
            require(!previous.isArchived)
            dao.archive(id, System.currentTimeMillis())
        }
    }

    /** Pool validation uses real source membership; a book never borrows another book's links. */
    private suspend fun validateMaterials(materials: List<RecipeMaterial>) {
        materials.forEach { material ->
            val source = resolveMaterialSource(material)
            validateMaterialRefs(material, source)
            if (!materialHasPool(material, source)) {
                throw RecipeValidationException("Material ${material.id} has an empty word pool")
            }
        }
    }

    private suspend fun resolveMaterialSource(material: RecipeMaterial): SourcesRow? {
        val catalog = database.catalogDao()
        val source = material.sourceId?.let { id ->
            catalog.activeSource(id)
                ?: throw RecipeValidationException("Unknown source $id for material ${material.id}")
        }
        when (material.type) {
            MaterialType.Book -> {
                val book = source
                    ?: throw RecipeValidationException("Material ${material.id} needs a textbook source")
                if (book.sourceType !in setOf("textbook", "book")) {
                    throw RecipeValidationException("Material ${material.id} needs a textbook source")
                }
                if (material.chapters.isNotEmpty() &&
                    !catalog.chapterNumbers(book.id).toSet().containsAll(material.chapters)
                ) {
                    throw RecipeValidationException("Material ${material.id} references unknown chapters")
                }
            }
            MaterialType.Dictionary -> {
                if (source?.sourceType != "personal") {
                    throw RecipeValidationException("Material ${material.id} needs a personal dictionary source")
                }
            }
            else -> if (source != null) {
                throw RecipeValidationException("Material ${material.id} must not bind a source")
            }
        }
        return source
    }

    private suspend fun validateMaterialRefs(material: RecipeMaterial, source: SourcesRow?) {
        val catalog = database.catalogDao()
        if (material.kanjiIds.isNotEmpty() &&
            catalog.countKanji(material.kanjiIds.toList()) != material.kanjiIds.size
        ) {
            throw RecipeValidationException("Material ${material.id} references unknown kanji")
        }
        if (material.wordIds.isNotEmpty() &&
            catalog.countActiveWords(material.wordIds.toList()) != material.wordIds.size
        ) {
            throw RecipeValidationException("Material ${material.id} references unknown words")
        }
        if (material.tagIds.isNotEmpty() &&
            dao.countTags(material.tagIds.toList()) != material.tagIds.size
        ) {
            throw RecipeValidationException("Material ${material.id} references unknown tags")
        }
        if (material.type != MaterialType.Book || material.kanjiIds.isEmpty()) return
        val placed = catalog.countSourceKanji(
            requireNotNull(source).id,
            material.kanjiIds.toList(),
            allChapters = if (material.chapters.isEmpty()) 1 else 0,
            chapters = material.chapters.toList().ifEmpty { listOf(-1) },
        )
        if (placed != material.kanjiIds.size) {
            throw RecipeValidationException(
                "Material ${material.id} kanji are outside the selected chapters",
            )
        }
    }

    private suspend fun materialHasPool(material: RecipeMaterial, source: SourcesRow?): Boolean {
        val catalog = database.catalogDao()
        return when (material.type) {
            MaterialType.Book -> catalog.bookPoolExists(
                requireNotNull(source).id,
                allChapters = if (material.chapters.isEmpty()) 1 else 0,
                chapters = material.chapters.toList().ifEmpty { listOf(-1) },
                allKanji = if (material.kanjiIds.isEmpty()) 1 else 0,
                kanjiIds = material.kanjiIds.toList().ifEmpty { listOf(-1) },
            )
            MaterialType.Dictionary -> catalog.dictionaryPoolExists(
                requireNotNull(source).id,
                allTags = if (material.tagIds.isEmpty()) 1 else 0,
                tagIds = material.tagIds.toList().ifEmpty { listOf(-1L) },
                allKanji = if (material.kanjiIds.isEmpty()) 1 else 0,
                kanjiIds = material.kanjiIds.toList().ifEmpty { listOf(-1) },
            )
            MaterialType.Kanji -> if (material.kanjiIds.isEmpty()) catalog.hasActiveWord()
            else catalog.kanjiPoolExists(material.kanjiIds.toList())
            MaterialType.Words -> material.wordIds.isNotEmpty() &&
                catalog.countActiveWords(material.wordIds.toList()) > 0
        }
    }
}

internal suspend fun saveTagInTransaction(database: CatalogDatabase, id: Long?, draft: TagDraft): Long {
    val valid = draft.validated()
    val dao = database.recipesDao()
    val now = System.currentTimeMillis()
    val old = id?.let { requireNotNull(dao.tag(it)) }
    val tagId = if (old == null) dao.insertTag(DictionaryTagsRow(color = valid.color.key,
        position = dao.nextTagPosition(), createdAt = now, updatedAt = now))
    else old.id.also { dao.updateTag(old.copy(color = valid.color.key, updatedAt = now)) }
    dao.clearTagTranslations(tagId)
    dao.insertTagTranslations(valid.translations.map {
        DictionaryTagsTranslationsRow(tagId, it.languageTag, it.name,
            UCharacter.foldCase(Normalizer.normalize(it.name, Normalizer.Form.NFKC), true))
    })
    return tagId
}

internal suspend fun assignTagsInTransaction(database: CatalogDatabase, wordId: Int, tagIds: Set<Long>) {
    val word = requireNotNull(database.catalogDao().word(wordId))
    require(word.isUserCreated == 1 && word.isArchived == 0)
    val dao = database.recipesDao()
    // One COUNT instead of N lookups; empty selection clears tags.
    require(tagIds.isEmpty() || dao.countTags(tagIds.toList()) == tagIds.size)
    dao.clearWordTags(wordId)
    dao.insertWordTags(tagIds.map { WordTagsRow(wordId, it, System.currentTimeMillis()) })
}
