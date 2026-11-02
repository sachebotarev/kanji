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

    @Query("SELECT * FROM sources_translations")
    suspend fun sourcesTranslations(): List<SourcesTranslationsRow>

    @Query("SELECT * FROM source_kanji") suspend fun sourceKanji(): List<SourceKanjiRow>

    @Query("SELECT * FROM source_words") suspend fun sourceWords(): List<SourceWordsRow>

    @Query("SELECT * FROM catalog_installation WHERE id=1")
    suspend fun installation(): InstallationRow?
}
