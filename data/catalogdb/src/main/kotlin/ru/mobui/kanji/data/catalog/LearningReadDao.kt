package ru.mobui.kanji.data.catalog

import androidx.room.Query

internal interface LearningReadDao {

    @Query("SELECT * FROM catalog_metadata") suspend fun catalogMetadata(): List<CatalogMetadataRow>

    @Query("SELECT * FROM words") suspend fun words(): List<WordsRow>

    @Query("SELECT * FROM words_translations")
    suspend fun wordsTranslations(): List<WordsTranslationsRow>

    @Query("SELECT * FROM word_reading_parts")
    suspend fun wordReadingParts(): List<WordReadingPartsRow>

    @Query("SELECT * FROM sources") suspend fun sources(): List<SourcesRow>

    @Query("SELECT * FROM sources WHERE id=:id AND is_archived=0")
    suspend fun activeSource(id: Int): SourcesRow?

    @Query("SELECT * FROM sources_translations")
    suspend fun sourcesTranslations(): List<SourcesTranslationsRow>

    @Query("SELECT * FROM source_kanji") suspend fun sourceKanji(): List<SourceKanjiRow>

    @Query("SELECT DISTINCT chapter_number FROM source_kanji WHERE source_id=:sourceId")
    suspend fun chapterNumbers(sourceId: Int): List<Int>

    @Query(
        """
        SELECT COUNT(*) FROM source_kanji
        WHERE source_id=:sourceId AND kanji_id IN (:kanjiIds)
          AND (:allChapters=1 OR chapter_number IN (:chapters))
        """,
    )
    suspend fun countSourceKanji(
        sourceId: Int,
        kanjiIds: List<Int>,
        allChapters: Int,
        chapters: List<Int>,
    ): Int

    @Query("SELECT * FROM source_words") suspend fun sourceWords(): List<SourceWordsRow>

    @Query("SELECT COUNT(*) FROM words WHERE is_archived=0 AND id IN (:ids)")
    suspend fun countActiveWords(ids: List<Int>): Int

    @Query("SELECT EXISTS(SELECT 1 FROM words WHERE is_archived=0)")
    suspend fun hasActiveWord(): Boolean

    @Query(
        """
        SELECT EXISTS(
          SELECT 1 FROM words w
          WHERE w.is_archived=0 AND EXISTS(
            SELECT 1 FROM kanji k WHERE k.id IN (:kanjiIds) AND instr(w.written_form, k.symbol)>0
          )
        )
        """,
    )
    suspend fun kanjiPoolExists(kanjiIds: List<Int>): Boolean

    @Query(
        """
        SELECT EXISTS(
          SELECT 1 FROM source_words sw
          INNER JOIN words w ON w.id=sw.word_id AND w.is_archived=0
          WHERE sw.source_id=:sourceId
            AND (:allChapters=1 OR sw.kanji_id IN (
              SELECT sk.kanji_id FROM source_kanji sk
              WHERE sk.source_id=:sourceId AND sk.chapter_number IN (:chapters)
            ))
            AND (:allKanji=1 OR sw.kanji_id IN (:kanjiIds))
        )
        """,
    )
    suspend fun bookPoolExists(
        sourceId: Int,
        allChapters: Int,
        chapters: List<Int>,
        allKanji: Int,
        kanjiIds: List<Int>,
    ): Boolean

    @Query(
        """
        SELECT EXISTS(
          SELECT 1 FROM source_words sw
          INNER JOIN words w ON w.id=sw.word_id AND w.is_archived=0
          WHERE sw.source_id=:sourceId
            AND (:allTags=1 OR EXISTS(
              SELECT 1 FROM word_tags wt WHERE wt.word_id=sw.word_id AND wt.tag_id IN (:tagIds)
            ))
            AND (:allKanji=1 OR EXISTS(
              SELECT 1 FROM kanji k WHERE k.id IN (:kanjiIds) AND instr(w.written_form, k.symbol)>0
            ))
        )
        """,
    )
    suspend fun dictionaryPoolExists(
        sourceId: Int,
        allTags: Int,
        tagIds: List<Long>,
        allKanji: Int,
        kanjiIds: List<Int>,
    ): Boolean

    @Query("SELECT * FROM catalog_installation WHERE id=1")
    suspend fun installation(): InstallationRow?
}
