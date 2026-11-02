package ru.mobui.kanji.data.catalog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_metadata", primaryKeys = ["singleton"])
internal data class CatalogMetadataRow(
    val singleton: Int,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
    @ColumnInfo(name = "content_version") val contentVersion: Int,
)

@Entity(
    tableName = "radicals",
    primaryKeys = ["id"],
    indices = [Index(value = ["symbol"], unique = true)],
)
internal data class RadicalsRow(
    val id: Int,
    val symbol: String,
    @ColumnInfo(name = "stroke_count") val strokeCount: Int,
)

@Entity(
    tableName = "radicals_translations",
    primaryKeys = ["radical_id", "language_tag"],
    foreignKeys =
        [
            ForeignKey(
                entity = RadicalsRow::class,
                parentColumns = ["id"],
                childColumns = ["radical_id"],
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.RESTRICT,
            ),
        ],
)
internal data class RadicalsTranslationsRow(
    @ColumnInfo(name = "radical_id") val radicalId: Int,
    @ColumnInfo(name = "language_tag", collate = ColumnInfo.NOCASE) val languageTag: String,
    val name: String,
)

@Entity(
    tableName = "kanji",
    primaryKeys = ["id"],
    foreignKeys =
        [
            ForeignKey(
                entity = RadicalsRow::class,
                parentColumns = ["id"],
                childColumns = ["radical_id"],
                onDelete = ForeignKey.RESTRICT,
                onUpdate = ForeignKey.RESTRICT,
            ),
        ],
    indices =
        [
            Index(value = ["radical_id"], unique = false, name = "kanji_radical_id"),
            Index(value = ["symbol"], unique = true),
        ],
)
internal data class KanjiRow(
    val id: Int,
    val symbol: String,
    @ColumnInfo(name = "stroke_count") val strokeCount: Int,
    @ColumnInfo(name = "radical_id") val radicalId: Int?,
    @ColumnInfo(name = "is_kyoiku") val isKyoiku: Int,
    @ColumnInfo(name = "is_joyo") val isJoyo: Int,
    @ColumnInfo(name = "is_jinmeiyo") val isJinmeiyo: Int,
    @ColumnInfo(name = "school_stage") val schoolStage: String? = null,
    @ColumnInfo(name = "school_year") val schoolYear: Int? = null,
    @ColumnInfo(name = "jlpt_level") val jlptLevel: Int? = null,
)

@Entity(
    tableName = "kanji_translations",
    primaryKeys = ["kanji_id", "language_tag", "position"],
    foreignKeys =
        [
            ForeignKey(
                entity = KanjiRow::class,
                parentColumns = ["id"],
                childColumns = ["kanji_id"],
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.RESTRICT,
            ),
        ],
)
internal data class KanjiTranslationsRow(
    @ColumnInfo(name = "kanji_id") val kanjiId: Int,
    @ColumnInfo(name = "language_tag", collate = ColumnInfo.NOCASE) val languageTag: String,
    val position: Int,
    val meaning: String,
)

@Entity(
    tableName = "kanji_readings",
    primaryKeys = ["kanji_id", "reading_type", "position"],
    foreignKeys =
        [
            ForeignKey(
                entity = KanjiRow::class,
                parentColumns = ["id"],
                childColumns = ["kanji_id"],
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.RESTRICT,
            ),
        ],
    indices = [Index(value = ["reading"], unique = false, name = "kanji_readings_reading")],
)
internal data class KanjiReadingsRow(
    @ColumnInfo(name = "kanji_id") val kanjiId: Int,
    @ColumnInfo(name = "reading_type") val readingType: String,
    val position: Int,
    val reading: String,
)

@Entity(
    tableName = "words",
    primaryKeys = ["id"],
    indices = [Index(value = ["written_form", "reading"], unique = true)],
)
internal data class WordsRow(
    val id: Int,
    @ColumnInfo(name = "written_form") val writtenForm: String,
    val reading: String,
    @ColumnInfo(name = "is_archived", defaultValue = "0") val isArchived: Int,
    @ColumnInfo(name = "is_user_created", defaultValue = "0") val isUserCreated: Int = 0,
)

@Entity(
    tableName = "words_translations",
    primaryKeys = ["word_id", "language_tag"],
    foreignKeys =
        [
            ForeignKey(
                entity = WordsRow::class,
                parentColumns = ["id"],
                childColumns = ["word_id"],
                onDelete = ForeignKey.RESTRICT,
                onUpdate = ForeignKey.RESTRICT,
            ),
        ],
)
internal data class WordsTranslationsRow(
    @ColumnInfo(name = "word_id") val wordId: Int,
    @ColumnInfo(name = "language_tag", collate = ColumnInfo.NOCASE) val languageTag: String,
    val translation: String,
    @ColumnInfo(name = "usage_note") val usageNote: String? = null,
)

@Entity(
    tableName = "word_reading_parts",
    primaryKeys = ["word_id", "position"],
    foreignKeys =
        [
            ForeignKey(
                entity = WordsRow::class,
                parentColumns = ["id"],
                childColumns = ["word_id"],
                onDelete = ForeignKey.RESTRICT,
                onUpdate = ForeignKey.RESTRICT,
            ),
        ],
)
internal data class WordReadingPartsRow(
    @ColumnInfo(name = "word_id") val wordId: Int,
    val position: Int,
    @ColumnInfo(name = "written_part") val writtenPart: String,
    @ColumnInfo(name = "reading_part") val readingPart: String,
)

@Entity(tableName = "sources", primaryKeys = ["id"])
internal data class SourcesRow(
    val id: Int,
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "original_title") val originalTitle: String,
    @ColumnInfo(name = "is_archived", defaultValue = "0") val isArchived: Int,
    @ColumnInfo(name = "cover_key") val coverKey: String? = null,
)

@Entity(
    tableName = "sources_translations",
    primaryKeys = ["source_id", "language_tag"],
    foreignKeys =
        [
            ForeignKey(
                entity = SourcesRow::class,
                parentColumns = ["id"],
                childColumns = ["source_id"],
                onDelete = ForeignKey.RESTRICT,
                onUpdate = ForeignKey.RESTRICT,
            ),
        ],
)
internal data class SourcesTranslationsRow(
    @ColumnInfo(name = "source_id") val sourceId: Int,
    @ColumnInfo(name = "language_tag", collate = ColumnInfo.NOCASE) val languageTag: String,
    val title: String,
    val description: String? = null,
)

@Entity(
    tableName = "source_kanji",
    primaryKeys = ["source_id", "kanji_id"],
    foreignKeys =
        [
            ForeignKey(
                entity = KanjiRow::class,
                parentColumns = ["id"],
                childColumns = ["kanji_id"],
                onDelete = ForeignKey.RESTRICT,
                onUpdate = ForeignKey.RESTRICT,
            ),
            ForeignKey(
                entity = SourcesRow::class,
                parentColumns = ["id"],
                childColumns = ["source_id"],
                onDelete = ForeignKey.RESTRICT,
                onUpdate = ForeignKey.RESTRICT,
            ),
        ],
    indices =
        [
            Index(value = ["kanji_id"], name = "source_kanji_kanji_id"),
            Index(
                value = ["source_id", "chapter_number", "kanji_number"],
                unique = false,
                name = "source_kanji_chapter",
            ),
            Index(value = ["source_id", "kanji_number"], unique = true),
        ],
)
internal data class SourceKanjiRow(
    @ColumnInfo(name = "source_id") val sourceId: Int,
    @ColumnInfo(name = "kanji_id") val kanjiId: Int,
    @ColumnInfo(name = "kanji_number") val kanjiNumber: Int,
    @ColumnInfo(name = "chapter_number") val chapterNumber: Int,
)

@Entity(
    tableName = "source_words",
    primaryKeys = ["id"],
    foreignKeys =
        [
            ForeignKey(
                entity = SourceKanjiRow::class,
                parentColumns = ["source_id", "kanji_id"],
                childColumns = ["source_id", "kanji_id"],
                onDelete = ForeignKey.RESTRICT,
                onUpdate = ForeignKey.RESTRICT,
            ),
            ForeignKey(
                entity = WordsRow::class,
                parentColumns = ["id"],
                childColumns = ["word_id"],
                onDelete = ForeignKey.RESTRICT,
                onUpdate = ForeignKey.RESTRICT,
            ),
            ForeignKey(
                entity = SourcesRow::class,
                parentColumns = ["id"],
                childColumns = ["source_id"],
                onDelete = ForeignKey.RESTRICT,
                onUpdate = ForeignKey.RESTRICT,
            ),
        ],
    indices =
        [
            Index(value = ["source_id", "kanji_id"], unique = false, name = "source_words_kanji"),
            Index(value = ["word_id"], unique = false, name = "source_words_word"),
        ],
)
internal data class SourceWordsRow(
    val id: Int,
    @ColumnInfo(name = "source_id") val sourceId: Int,
    @ColumnInfo(name = "word_id") val wordId: Int,
    @ColumnInfo(name = "kanji_id") val kanjiId: Int?,
)

@Entity(tableName = "catalog_installation")
internal data class InstallationRow(@PrimaryKey val id: Int, val payloadHash: String)
