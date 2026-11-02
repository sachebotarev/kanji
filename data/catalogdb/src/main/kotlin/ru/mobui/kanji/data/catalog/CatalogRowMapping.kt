package ru.mobui.kanji.data.catalog

import kotlinx.coroutines.flow.map
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogSource
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.JlptLevel
import ru.mobui.kanji.domain.catalog.ReadingPart
import ru.mobui.kanji.domain.catalog.SchoolStage

internal fun SourcesRow.toCatalogSource(translations: List<SourcesTranslationsRow>): CatalogSource =
    CatalogSource(
        this.id,
        this.originalTitle,
        translations.associate { r -> r.languageTag to r.title },
        translations
            .mapNotNull { r -> r.description?.let { text -> r.languageTag to text } }
            .toMap(),
        this.sourceType,
        this.coverKey,
    )

internal fun WordsRow.toCatalogWord(
    translations: List<WordsTranslationsRow>,
    parts: List<WordReadingPartsRow>,
): CatalogWord =
    CatalogWord(
        this.id,
        this.writtenForm,
        this.reading,
        translations.associate { r ->
            r.languageTag to r.translation
        },
        parts.map { r -> ReadingPart(r.writtenPart, r.readingPart) },
        this.isUserCreated == 1,
        translations.mapNotNull { r -> r.usageNote?.let { note -> r.languageTag to note } }.toMap(),
    )

internal fun KanjiRow.toCatalogKanji(
    meanings: List<KanjiTranslationsRow>,
    readings: List<KanjiReadingsRow>,
): CatalogKanji =
    CatalogKanji(
        this.id,
        this.symbol,
        this.strokeCount,
        this.radicalId,
        this.isKyoiku == 1,
        this.isJoyo == 1,
        this.isJinmeiyo == 1,
        meanings
            .groupBy { r -> r.languageTag }
            .mapValues { (_, rows) -> rows.map { r -> r.meaning } },
        readings.filter { r -> r.readingType == "on" }.map { r -> r.reading },
        readings.filter { r -> r.readingType == "kun" }.map { r -> r.reading },
        when (this.schoolStage) {
            "elementary" -> SchoolStage.Elementary
            "junior_high" -> SchoolStage.JuniorHigh
            "high_school" -> SchoolStage.HighSchool
            else -> null
        },
        this.schoolYear,
        JlptLevel.entries.firstOrNull { level -> level.number == this.jlptLevel },
    )
