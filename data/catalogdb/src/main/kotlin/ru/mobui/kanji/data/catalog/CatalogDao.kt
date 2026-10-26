package ru.mobui.kanji.data.catalog

import androidx.room.*
import kotlinx.coroutines.flow.Flow

internal data class PersonalWordRow(
    @Embedded val word: WordsRow,
    @Relation(parentColumn = "id", entityColumn = "word_id") val translations: List<WordsTranslationsRow>,
    @Relation(parentColumn = "id", entityColumn = "word_id") val parts: List<WordReadingPartsRow>
)


@Dao
internal interface CatalogDao {
    @Transaction
    @Query("""SELECT words.* FROM words WHERE is_archived=0 AND id IN
        (SELECT word_id FROM source_words JOIN sources ON sources.id=source_words.source_id
         WHERE sources.source_type='personal' AND sources.is_archived=0)
        ORDER BY (SELECT MIN(source_words.id) FROM source_words JOIN sources ON sources.id=source_words.source_id
                  WHERE source_words.word_id=words.id AND sources.source_type='personal') ASC""")
    fun observePersonalWords(): Flow<List<PersonalWordRow>>
    @Query("SELECT * FROM words WHERE id=:id") suspend fun word(id: Int): WordsRow?
    @Query("SELECT * FROM words WHERE written_form=:written AND reading=:reading AND id != :excluded")
    suspend fun duplicate(written: String, reading: String, excluded: Int): WordsRow?
    @Query("SELECT MIN(id) FROM words") suspend fun minimumWordId(): Int?
    @Query("SELECT MIN(id) FROM sources") suspend fun minimumSourceId(): Int?
    @Query("SELECT MIN(id) FROM source_words") suspend fun minimumLinkId(): Int?
    @Query("DELETE FROM words_translations WHERE word_id=:id") suspend fun clearWordTranslations(id: Int)
    @Query("UPDATE words SET is_archived=1 WHERE id=:id AND is_user_created=1") suspend fun archiveManual(id: Int)
    @Query("DELETE FROM source_words WHERE word_id=:id AND source_id IN (SELECT id FROM sources WHERE source_type='personal')")
    suspend fun clearPersonalLinks(id: Int)

    @Query("SELECT * FROM catalog_metadata") suspend fun catalog_metadata(): List<CatalogMetadataRow>
    @Upsert suspend fun put_catalog_metadata(rows: List<CatalogMetadataRow>)
    @Query("SELECT * FROM radicals") suspend fun radicals(): List<RadicalsRow>
    @Upsert suspend fun put_radicals(rows: List<RadicalsRow>)
    @Query("SELECT * FROM radicals_translations") suspend fun radicals_translations(): List<RadicalsTranslationsRow>
    @Upsert suspend fun put_radicals_translations(rows: List<RadicalsTranslationsRow>)
    @Query("SELECT * FROM kanji") suspend fun kanji(): List<KanjiRow>
    @Upsert suspend fun put_kanji(rows: List<KanjiRow>)
    @Query("SELECT * FROM kanji_translations") suspend fun kanji_translations(): List<KanjiTranslationsRow>
    @Upsert suspend fun put_kanji_translations(rows: List<KanjiTranslationsRow>)
    @Query("SELECT * FROM kanji_readings") suspend fun kanji_readings(): List<KanjiReadingsRow>
    @Upsert suspend fun put_kanji_readings(rows: List<KanjiReadingsRow>)
    @Query("SELECT * FROM words") suspend fun words(): List<WordsRow>
    @Upsert suspend fun put_words(rows: List<WordsRow>)
    @Query("SELECT * FROM words_translations") suspend fun words_translations(): List<WordsTranslationsRow>
    @Upsert suspend fun put_words_translations(rows: List<WordsTranslationsRow>)
    @Query("SELECT * FROM word_reading_parts") suspend fun word_reading_parts(): List<WordReadingPartsRow>
    @Upsert suspend fun put_word_reading_parts(rows: List<WordReadingPartsRow>)
    @Query("SELECT * FROM sources") suspend fun sources(): List<SourcesRow>
    @Upsert suspend fun put_sources(rows: List<SourcesRow>)
    @Query("SELECT * FROM sources_translations") suspend fun sources_translations(): List<SourcesTranslationsRow>
    @Upsert suspend fun put_sources_translations(rows: List<SourcesTranslationsRow>)
    @Query("SELECT * FROM source_kanji") suspend fun source_kanji(): List<SourceKanjiRow>
    @Upsert suspend fun put_source_kanji(rows: List<SourceKanjiRow>)
    @Query("SELECT * FROM source_words") suspend fun source_words(): List<SourceWordsRow>
    @Upsert suspend fun put_source_words(rows: List<SourceWordsRow>)
    @Query("DELETE FROM kanji_readings WHERE kanji_id IN (:ids)") suspend fun clearReadings(ids: List<Int>)
    @Query("DELETE FROM kanji_translations WHERE kanji_id=:id AND language_tag=:language") suspend fun clearMeanings(id: Int, language: String)
    @Query("DELETE FROM word_reading_parts WHERE word_id IN (:ids)") suspend fun clearParts(ids: List<Int>)
    @Query("SELECT * FROM catalog_installation WHERE id=1") suspend fun installation(): InstallationRow?
    @Upsert suspend fun putInstallation(row: InstallationRow)
    @Query("DELETE FROM source_words WHERE source_id IN (:ids)") suspend fun clearLinks(ids: List<Int>)
    @Query("DELETE FROM source_kanji WHERE source_id IN (:ids)") suspend fun clearPlacements(ids: List<Int>)
}
