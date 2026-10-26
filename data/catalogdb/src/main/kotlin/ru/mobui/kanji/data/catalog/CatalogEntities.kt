package ru.mobui.kanji.data.catalog

import androidx.room.*

@Entity(tableName = "catalog_metadata", primaryKeys = ["singleton"])
internal data class CatalogMetadataRow(
    val singleton: Int,
    val schema_version: Int,
    val content_version: Int
)

@Entity(tableName = "radicals", primaryKeys = ["id"], indices = [Index(value = ["symbol"], unique = true)])
internal data class RadicalsRow(
    val id: Int,
    val symbol: String,
    val stroke_count: Int
)

@Entity(tableName = "radicals_translations", primaryKeys = ["radical_id", "language_tag"], foreignKeys = [
    ForeignKey(entity = RadicalsRow::class, parentColumns = ["id"], childColumns = ["radical_id"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.RESTRICT)
])
internal data class RadicalsTranslationsRow(
    val radical_id: Int,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val language_tag: String,
    val name: String
)

@Entity(tableName = "kanji", primaryKeys = ["id"], foreignKeys = [
    ForeignKey(entity = RadicalsRow::class, parentColumns = ["id"], childColumns = ["radical_id"], onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.RESTRICT)
], indices = [Index(value = ["radical_id"], unique = false, name = "kanji_radical_id"), Index(value = ["symbol"], unique = true)])
internal data class KanjiRow(
    val id: Int,
    val symbol: String,
    val stroke_count: Int,
    val radical_id: Int?,
    val is_kyoiku: Int,
    val is_joyo: Int,
    val is_jinmeiyo: Int
)

@Entity(tableName = "kanji_translations", primaryKeys = ["kanji_id", "language_tag", "position"], foreignKeys = [
    ForeignKey(entity = KanjiRow::class, parentColumns = ["id"], childColumns = ["kanji_id"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.RESTRICT)
])
internal data class KanjiTranslationsRow(
    val kanji_id: Int,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val language_tag: String,
    val position: Int,
    val meaning: String
)

@Entity(tableName = "kanji_readings", primaryKeys = ["kanji_id", "reading_type", "position"], foreignKeys = [
    ForeignKey(entity = KanjiRow::class, parentColumns = ["id"], childColumns = ["kanji_id"], onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.RESTRICT)
], indices = [Index(value = ["reading"], unique = false, name = "kanji_readings_reading")])
internal data class KanjiReadingsRow(
    val kanji_id: Int,
    val reading_type: String,
    val position: Int,
    val reading: String
)

@Entity(tableName = "words", primaryKeys = ["id"], indices = [Index(value = ["written_form", "reading"], unique = true)])
internal data class WordsRow(
    val id: Int,
    val written_form: String,
    val reading: String,
    @ColumnInfo(defaultValue = "0") val is_archived: Int,
    @ColumnInfo(defaultValue = "0") val is_user_created: Int = 0
)

@Entity(tableName = "words_translations", primaryKeys = ["word_id", "language_tag"], foreignKeys = [
    ForeignKey(entity = WordsRow::class, parentColumns = ["id"], childColumns = ["word_id"], onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.RESTRICT)
])
internal data class WordsTranslationsRow(
    val word_id: Int,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val language_tag: String,
    val translation: String
)

@Entity(tableName = "word_reading_parts", primaryKeys = ["word_id", "position"], foreignKeys = [
    ForeignKey(entity = WordsRow::class, parentColumns = ["id"], childColumns = ["word_id"], onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.RESTRICT)
])
internal data class WordReadingPartsRow(
    val word_id: Int,
    val position: Int,
    val written_part: String,
    val reading_part: String
)

@Entity(tableName = "sources", primaryKeys = ["id"])
internal data class SourcesRow(
    val id: Int,
    val source_type: String,
    val original_title: String,
    @ColumnInfo(defaultValue = "0") val is_archived: Int
)

@Entity(tableName = "sources_translations", primaryKeys = ["source_id", "language_tag"], foreignKeys = [
    ForeignKey(entity = SourcesRow::class, parentColumns = ["id"], childColumns = ["source_id"], onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.RESTRICT)
])
internal data class SourcesTranslationsRow(
    val source_id: Int,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val language_tag: String,
    val title: String
)

@Entity(tableName = "source_kanji", primaryKeys = ["source_id", "kanji_id"], foreignKeys = [
    ForeignKey(entity = KanjiRow::class, parentColumns = ["id"], childColumns = ["kanji_id"], onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.RESTRICT),
    ForeignKey(entity = SourcesRow::class, parentColumns = ["id"], childColumns = ["source_id"], onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.RESTRICT)
], indices = [Index(value = ["kanji_id"], name = "source_kanji_kanji_id"), Index(value = ["source_id", "chapter_number", "kanji_number"], unique = false, name = "source_kanji_chapter"), Index(value = ["source_id", "kanji_number"], unique = true)])
internal data class SourceKanjiRow(
    val source_id: Int,
    val kanji_id: Int,
    val kanji_number: Int,
    val chapter_number: Int
)

@Entity(tableName = "source_words", primaryKeys = ["id"], foreignKeys = [
    ForeignKey(entity = SourceKanjiRow::class, parentColumns = ["source_id", "kanji_id"], childColumns = ["source_id", "kanji_id"], onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.RESTRICT),
    ForeignKey(entity = WordsRow::class, parentColumns = ["id"], childColumns = ["word_id"], onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.RESTRICT),
    ForeignKey(entity = SourcesRow::class, parentColumns = ["id"], childColumns = ["source_id"], onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.RESTRICT)
], indices = [Index(value = ["source_id", "kanji_id"], unique = false, name = "source_words_kanji"), Index(value = ["word_id"], unique = false, name = "source_words_word")])
internal data class SourceWordsRow(
    val id: Int,
    val source_id: Int,
    val word_id: Int,
    val kanji_id: Int?
)

@Entity(tableName = "catalog_installation")
internal data class InstallationRow(@PrimaryKey val id: Int, val payloadHash: String)
