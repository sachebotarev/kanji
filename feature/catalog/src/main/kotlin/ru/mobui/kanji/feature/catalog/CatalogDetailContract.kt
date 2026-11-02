package ru.mobui.kanji.feature.catalog

import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogRadical
import ru.mobui.kanji.domain.catalog.CatalogSource
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing

internal sealed interface DetailRequest {
    val sourceId: Int?

    data class Word(val wordId: Int, override val sourceId: Int?) : DetailRequest

    data class Kanji(val kanjiId: Int, override val sourceId: Int?) : DetailRequest

    data class Radical(val radicalId: Int, override val sourceId: Int?) : DetailRequest
}

internal sealed interface DetailContent {
    val source: CatalogSource?

    data class Word(
        val word: CatalogWord?,
        val kanji: List<CatalogKanji>,
        override val source: CatalogSource?,
    ) : DetailContent

    data class Kanji(
        val kanji: CatalogKanji?,
        val drawing: ReferenceDrawing?,
        val radical: CatalogRadical?,
        val wordCount: Int,
        override val source: CatalogSource?,
    ) : DetailContent

    data class Radical(
        val radical: CatalogRadical?,
        val rows: KanjiRows,
        override val source: CatalogSource?,
    ) : DetailContent
}

internal sealed interface DetailNavigation {
    data class Kanji(val kanjiId: Int, val sourceId: Int?) : DetailNavigation

    data class Radical(val radicalId: Int, val sourceId: Int?) : DetailNavigation

    data class Words(val kanjiId: Int, val sourceId: Int?) : DetailNavigation
}
