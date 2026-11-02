package ru.mobui.kanji.feature.catalog

import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import ru.mobui.kanji.domain.catalog.CatalogChapter
import ru.mobui.kanji.domain.catalog.CatalogRadical
import ru.mobui.kanji.domain.catalog.CatalogSource
import ru.mobui.kanji.domain.catalog.CatalogSourceCounts
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.CatalogWordGroup

@Keep
@Serializable
internal enum class SourceTab {
    Chapters,
    Kanji,
    Words,
}

internal sealed interface BrowseRequest {
    data object Home : BrowseRequest

    data class Source(
        val sourceId: Int,
        val chapter: Int? = null,
        val initialTab: SourceTab = if (chapter == null) SourceTab.Chapters else SourceTab.Kanji,
    ) : BrowseRequest

    data object AllKanji : BrowseRequest

    data object AllRadicals : BrowseRequest

    data class RelatedWords(val kanjiId: Int, val sourceId: Int?) : BrowseRequest
}

internal data class SourceSummary(val source: CatalogSource, val counts: CatalogSourceCounts)

internal data class WordRows(
    val entries: List<CatalogWord>,
    val groups: List<CatalogWordGroup>,
    val total: Int,
)

internal sealed interface SourceRows {
    data class Chapters(val entries: List<CatalogChapter>, val range: IntRange?) : SourceRows

    data class Kanji(val rows: KanjiRows, val filters: KanjiFilters, val radicalSymbol: String?) :
        SourceRows

    data class Words(
        val rows: WordRows,
        val filters: WordFilters,
        val kanjiSymbol: String?,
        val showEnd: Boolean,
    ) : SourceRows
}

internal sealed interface BrowseContent {
    data class Home(val sources: List<SourceSummary>) : BrowseContent

    data class Source(
        val request: BrowseRequest.Source,
        val source: CatalogSource?,
        val tab: SourceTab,
        val rows: SourceRows,
    ) : BrowseContent

    data class AllKanji(
        val rows: KanjiRows,
        val filters: KanjiFilters,
        val radicalSymbol: String?,
    ) : BrowseContent

    data class AllRadicals(val entries: List<CatalogRadical>, val total: Int, val strokes: Int?) :
        BrowseContent

    data class RelatedWords(
        val symbol: String,
        val sourceId: Int?,
        val source: CatalogSource?,
        val rows: WordRows,
    ) : BrowseContent
}

internal sealed interface BrowseNavigation {
    data class Source(val sourceId: Int, val chapter: Int? = null) : BrowseNavigation

    data class Word(val wordId: Int, val sourceId: Int?) : BrowseNavigation

    data class Kanji(val kanjiId: Int, val sourceId: Int?) : BrowseNavigation

    data class Radical(val radicalId: Int) : BrowseNavigation

    data object AllKanji : BrowseNavigation

    data object AllRadicals : BrowseNavigation

    data class Filter(val request: FilterRequest) : BrowseNavigation
}
