package ru.mobui.kanji.data.catalog

import androidx.room.Upsert

internal interface ReferenceWriteDao {

    @Upsert suspend fun putRadicals(rows: List<RadicalsRow>)

    @Upsert suspend fun putRadicalsTranslations(rows: List<RadicalsTranslationsRow>)

    @Upsert suspend fun putKanji(rows: List<KanjiRow>)

    @Upsert suspend fun putKanjiTranslations(rows: List<KanjiTranslationsRow>)

    @Upsert suspend fun putKanjiReadings(rows: List<KanjiReadingsRow>)
}
