package ru.mobui.kanji.data.catalog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "dictionary_tags")
internal data class DictionaryTagsRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val color: String,
    val position: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(tableName = "dictionary_tags_translations", primaryKeys = ["tag_id", "language_tag"],
    foreignKeys = [ForeignKey(entity = DictionaryTagsRow::class, parentColumns = ["id"], childColumns = ["tag_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["language_tag", "name_key"], unique = true)])
internal data class DictionaryTagsTranslationsRow(
    @ColumnInfo(name = "tag_id") val tagId: Long,
    @ColumnInfo(name = "language_tag", collate = ColumnInfo.NOCASE) val languageTag: String,
    val name: String,
    @ColumnInfo(name = "name_key") val nameKey: String,
)

@Entity(tableName = "word_tags", primaryKeys = ["word_id", "tag_id"],
    foreignKeys = [
        ForeignKey(entity = WordsRow::class, parentColumns = ["id"], childColumns = ["word_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DictionaryTagsRow::class, parentColumns = ["id"], childColumns = ["tag_id"], onDelete = ForeignKey.CASCADE),
    ], indices = [Index("tag_id")])
internal data class WordTagsRow(
    @ColumnInfo(name = "word_id") val wordId: Int,
    @ColumnInfo(name = "tag_id") val tagId: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(tableName = "recipes", indices = [Index(value = ["is_archived", "position", "id"])])
internal data class RecipesRow(
    @PrimaryKey val id: String,
    val position: Int,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "task_count") val taskCount: Int?,
    @ColumnInfo(name = "selection_mode") val selectionMode: String = "shuffled_cycles",
    @ColumnInfo(name = "seconds_per_symbol") val secondsPerSymbol: Int?,
    @ColumnInfo(name = "hint_mode") val hintMode: String = "none",
    @ColumnInfo(name = "stroke_hint_mode") val strokeHintMode: String = "next_stroke",
    val revision: Long,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "archived_at") val archivedAt: Long? = null,
)

@Entity(tableName = "recipes_translations", primaryKeys = ["recipe_id", "language_tag"],
    foreignKeys = [ForeignKey(entity = RecipesRow::class, parentColumns = ["id"], childColumns = ["recipe_id"], onDelete = ForeignKey.CASCADE)])
internal data class RecipesTranslationsRow(
    @ColumnInfo(name = "recipe_id") val recipeId: String,
    @ColumnInfo(name = "language_tag", collate = ColumnInfo.NOCASE) val languageTag: String,
    val name: String,
)

@Entity(tableName = "recipe_materials", indices = [Index(value = ["recipe_id", "position"], unique = true), Index("source_id")],
    foreignKeys = [
        ForeignKey(entity = RecipesRow::class, parentColumns = ["id"], childColumns = ["recipe_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SourcesRow::class, parentColumns = ["id"], childColumns = ["source_id"], onDelete = ForeignKey.RESTRICT),
    ])
internal data class RecipeMaterialsRow(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "recipe_id") val recipeId: String,
    val position: Int,
    @ColumnInfo(name = "material_type") val materialType: String,
    @ColumnInfo(name = "source_id") val sourceId: Int?,
)

@Entity(tableName = "recipe_materials_chapters", primaryKeys = ["material_id", "chapter_number"],
    foreignKeys = [ForeignKey(entity = RecipeMaterialsRow::class, parentColumns = ["id"], childColumns = ["material_id"], onDelete = ForeignKey.CASCADE)])
internal data class RecipeMaterialChaptersRow(
    @ColumnInfo(name = "material_id") val materialId: String,
    @ColumnInfo(name = "chapter_number") val chapterNumber: Int,
)

@Entity(tableName = "recipe_materials_kanji", primaryKeys = ["material_id", "kanji_id"], indices = [Index("kanji_id")],
    foreignKeys = [
        ForeignKey(entity = RecipeMaterialsRow::class, parentColumns = ["id"], childColumns = ["material_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = KanjiRow::class, parentColumns = ["id"], childColumns = ["kanji_id"], onDelete = ForeignKey.RESTRICT),
    ])
internal data class RecipeMaterialKanjiRow(
    @ColumnInfo(name = "material_id") val materialId: String,
    @ColumnInfo(name = "kanji_id") val kanjiId: Int,
)

@Entity(tableName = "recipe_materials_words", primaryKeys = ["material_id", "word_id"], indices = [Index("word_id")],
    foreignKeys = [
        ForeignKey(entity = RecipeMaterialsRow::class, parentColumns = ["id"], childColumns = ["material_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = WordsRow::class, parentColumns = ["id"], childColumns = ["word_id"], onDelete = ForeignKey.RESTRICT),
    ])
internal data class RecipeMaterialWordsRow(
    @ColumnInfo(name = "material_id") val materialId: String,
    @ColumnInfo(name = "word_id") val wordId: Int,
)

@Entity(tableName = "recipe_materials_tags", primaryKeys = ["material_id", "tag_id"], indices = [Index("tag_id")],
    foreignKeys = [
        ForeignKey(entity = RecipeMaterialsRow::class, parentColumns = ["id"], childColumns = ["material_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DictionaryTagsRow::class, parentColumns = ["id"], childColumns = ["tag_id"], onDelete = ForeignKey.CASCADE),
    ])
internal data class RecipeMaterialTagsRow(
    @ColumnInfo(name = "material_id") val materialId: String,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)

@Entity(tableName = "recipe_materials_jlpt", primaryKeys = ["material_id", "jlpt_level"],
    foreignKeys = [ForeignKey(entity = RecipeMaterialsRow::class, parentColumns = ["id"], childColumns = ["material_id"], onDelete = ForeignKey.CASCADE)])
internal data class RecipeMaterialJlptRow(
    @ColumnInfo(name = "material_id") val materialId: String,
    /** Exact N1…N5 as 1…5, or `-1` when the material includes unclassified kanji. */
    @ColumnInfo(name = "jlpt_level") val jlptLevel: Int,
)
