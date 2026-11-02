package ru.mobui.kanji.data.catalog

import androidx.room.Upsert

internal interface LearningWriteDao {

    @Upsert suspend fun putCatalogMetadata(rows: List<CatalogMetadataRow>)

    @Upsert suspend fun putWords(rows: List<WordsRow>)

    @Upsert suspend fun putWordsTranslations(rows: List<WordsTranslationsRow>)

    @Upsert suspend fun putWordReadingParts(rows: List<WordReadingPartsRow>)

    @Upsert suspend fun putSources(rows: List<SourcesRow>)

    @Upsert suspend fun putSourcesTranslations(rows: List<SourcesTranslationsRow>)

    @Upsert suspend fun putSourceKanji(rows: List<SourceKanjiRow>)

    @Upsert suspend fun putSourceWords(rows: List<SourceWordsRow>)

    @Upsert suspend fun putInstallation(row: InstallationRow)
}
