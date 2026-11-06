package ru.mobui.kanji.data.catalog

import androidx.room.Query

internal interface ReferenceReadDao {

    @Query("SELECT * FROM radicals") suspend fun radicals(): List<RadicalsRow>

    @Query("SELECT * FROM radicals_translations")
    suspend fun radicalsTranslations(): List<RadicalsTranslationsRow>

    @Query("SELECT * FROM kanji") suspend fun kanji(): List<KanjiRow>

    @Query("SELECT COUNT(*) FROM kanji WHERE id IN (:ids)")
    suspend fun countKanji(ids: List<Int>): Int

    @Query("SELECT * FROM kanji_translations")
    suspend fun kanjiTranslations(): List<KanjiTranslationsRow>

    @Query("SELECT * FROM kanji_readings") suspend fun kanjiReadings(): List<KanjiReadingsRow>
}
