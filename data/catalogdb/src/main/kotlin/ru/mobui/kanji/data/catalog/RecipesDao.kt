package ru.mobui.kanji.data.catalog

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

internal data class TagAggregate(
    @Embedded val tag: DictionaryTagsRow,
    @Relation(parentColumn = "id", entityColumn = "tag_id") val translations: List<DictionaryTagsTranslationsRow>,
)
internal data class MaterialAggregate(
    @Embedded val material: RecipeMaterialsRow,
    @Relation(parentColumn = "id", entityColumn = "material_id") val chapters: List<RecipeMaterialChaptersRow>,
    @Relation(parentColumn = "id", entityColumn = "material_id") val kanji: List<RecipeMaterialKanjiRow>,
    @Relation(parentColumn = "id", entityColumn = "material_id") val words: List<RecipeMaterialWordsRow>,
    @Relation(parentColumn = "id", entityColumn = "material_id") val tags: List<RecipeMaterialTagsRow>,
    @Relation(parentColumn = "id", entityColumn = "material_id") val jlpt: List<RecipeMaterialJlptRow>,
)
internal data class RecipeAggregate(
    @Embedded val recipe: RecipesRow,
    @Relation(parentColumn = "id", entityColumn = "recipe_id") val names: List<RecipesTranslationsRow>,
    @Relation(entity = RecipeMaterialsRow::class, parentColumn = "id", entityColumn = "recipe_id") val materials: List<MaterialAggregate>,
)

@Dao
internal interface RecipesDao {
    @Transaction @Query("SELECT * FROM dictionary_tags ORDER BY position, id")
    fun observeTags(): Flow<List<TagAggregate>>
    @Query("SELECT word_tags.* FROM word_tags JOIN words ON words.id=word_tags.word_id WHERE words.is_archived=0 AND words.is_user_created=1")
    fun observeWordTags(): Flow<List<WordTagsRow>>
    @Query("SELECT * FROM dictionary_tags WHERE id=:id") suspend fun tag(id: Long): DictionaryTagsRow?
    @Query("SELECT COUNT(*) FROM dictionary_tags WHERE id IN (:ids)")
    suspend fun countTags(ids: List<Long>): Int
    @Query("SELECT COALESCE(MAX(position), -1)+1 FROM dictionary_tags") suspend fun nextTagPosition(): Int
    @Insert suspend fun insertTag(row: DictionaryTagsRow): Long
    @Upsert suspend fun updateTag(row: DictionaryTagsRow)
    @Query("DELETE FROM dictionary_tags_translations WHERE tag_id=:id") suspend fun clearTagTranslations(id: Long)
    @Insert suspend fun insertTagTranslations(rows: List<DictionaryTagsTranslationsRow>)
    @Query("DELETE FROM dictionary_tags WHERE id=:id") suspend fun deleteTag(id: Long)
    @Query("DELETE FROM word_tags WHERE word_id=:id") suspend fun clearWordTags(id: Int)
    @Insert suspend fun insertWordTags(rows: List<WordTagsRow>)
    @Query("SELECT * FROM word_tags") suspend fun allWordTags(): List<WordTagsRow>

    @Transaction @Query("SELECT * FROM recipes WHERE is_archived=0 ORDER BY position,id")
    fun observeRecipes(): Flow<List<RecipeAggregate>>
    @Query("SELECT * FROM recipes WHERE is_archived=0 ORDER BY position,id") suspend fun recipes(): List<RecipesRow>
    @Query("SELECT * FROM recipes WHERE id=:id") suspend fun recipe(id: String): RecipesRow?
    @Upsert suspend fun putRecipe(row: RecipesRow)
    @Query("UPDATE recipes SET is_active=0 WHERE is_active=1") suspend fun deactivateAll()
    @Query("UPDATE recipes SET is_active=1 WHERE id=:id AND is_archived=0") suspend fun activate(id: String)
    @Query("UPDATE recipes SET position=:position, updated_at=:now WHERE id=:id") suspend fun position(id: String, position: Int, now: Long)
    @Query("UPDATE recipes SET is_archived=1,is_active=0,archived_at=:now,updated_at=:now WHERE id=:id") suspend fun archive(id: String, now: Long)
    @Query("DELETE FROM recipes_translations WHERE recipe_id=:id") suspend fun clearNames(id: String)
    @Insert suspend fun insertNames(rows: List<RecipesTranslationsRow>)
    @Query("DELETE FROM recipe_materials WHERE recipe_id=:id") suspend fun clearMaterials(id: String)
    @Insert suspend fun insertMaterial(row: RecipeMaterialsRow)
    @Insert suspend fun insertChapters(rows: List<RecipeMaterialChaptersRow>)
    @Insert suspend fun insertKanji(rows: List<RecipeMaterialKanjiRow>)
    @Insert suspend fun insertWords(rows: List<RecipeMaterialWordsRow>)
    @Insert suspend fun insertTags(rows: List<RecipeMaterialTagsRow>)
    @Insert suspend fun insertJlpt(rows: List<RecipeMaterialJlptRow>)
}
