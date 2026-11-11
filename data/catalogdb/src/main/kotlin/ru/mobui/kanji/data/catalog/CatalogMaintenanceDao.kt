package ru.mobui.kanji.data.catalog

import androidx.room.Query

internal interface CatalogMaintenanceDao {

    @Query("DELETE FROM kanji_translations WHERE kanji_id IN (:ids)")
    suspend fun clearKanjiTranslations(ids: List<Int>)

    @Query("DELETE FROM radicals_translations WHERE radical_id IN (:ids)")
    suspend fun clearRadicalTranslations(ids: List<Int>)

    @Query("DELETE FROM words_translations WHERE word_id IN (:ids)")
    suspend fun clearWordsTranslations(ids: List<Int>)

    @Query("DELETE FROM sources_translations WHERE source_id IN (:ids)")
    suspend fun clearSourcesTranslations(ids: List<Int>)

    @Query("DELETE FROM kanji_readings WHERE kanji_id IN (:ids)")
    suspend fun clearReadings(ids: List<Int>)

    @Query("DELETE FROM kanji_translations WHERE kanji_id=:id AND language_tag=:language")
    suspend fun clearMeanings(id: Int, language: String)

    @Query("DELETE FROM word_reading_parts WHERE word_id IN (:ids)")
    suspend fun clearParts(ids: List<Int>)

    @Query("DELETE FROM source_words WHERE source_id IN (:ids)")
    suspend fun clearLinks(ids: List<Int>)

    @Query("DELETE FROM source_kanji WHERE source_id IN (:ids)")
    suspend fun clearPlacements(ids: List<Int>)
}
