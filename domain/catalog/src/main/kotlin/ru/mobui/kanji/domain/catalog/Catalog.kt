package ru.mobui.kanji.domain.catalog

import java.util.Locale

/** Catalog identity never depends on order, translated names or chapter numbers. */
data class CatalogWord(
    val id: Int,
    val written: String,
    val reading: String,
    val translations: Map<String, String>,
    val parts: List<ReadingPart> = emptyList(),
    val isUserCreated: Boolean = false,
    val usageNotes: Map<String, String> = emptyMap(),
)

data class ReadingPart(val written: String, val reading: String)

data class CatalogKanji(
    val id: Int,
    val symbol: String,
    val strokes: Int,
    val radicalId: Int?,
    val kyoiku: Boolean,
    val joyo: Boolean,
    val jinmeiyo: Boolean,
    val meanings: Map<String, List<String>>,
    val on: List<String>,
    val kun: List<String>,
    val schoolStage: SchoolStage? = null,
    val schoolYear: Int? = null,
    val jlptLevel: JlptLevel? = null,
)

data class CatalogRadical(
    val id: Int,
    val symbol: String,
    val strokes: Int,
    val names: Map<String, String>,
)

data class CatalogSource(
    val id: Int,
    val originalTitle: String,
    val titles: Map<String, String>,
    val descriptions: Map<String, String> = emptyMap(),
    val sourceType: String = "unknown",
    val coverKey: String? = null,
)

data class CatalogPlacement(val sourceId: Int, val kanjiId: Int, val number: Int, val chapter: Int)

data class CatalogLink(val sourceId: Int, val wordId: Int, val kanjiId: Int?)

data class CatalogChapter(
    val number: Int,
    val kanjiCount: Int,
    val wordCount: Int,
    val preview: String,
)

data class CatalogSourceCounts(val chapters: Int, val kanji: Int, val words: Int)

data class CatalogWordGroup(val chapter: Int, val total: Int, val words: List<CatalogWord>)

data class CatalogSelection(
    val sourceId: Int? = null,
    val chapter: Int? = null,
    val radicalId: Int? = null,
    val kanjiId: Int? = null,
    val strokes: Int? = null,
    val jlpt: JlptFilter = JlptFilter.AnyLevel,
)

enum class SchoolStage {
    Elementary,
    JuniorHigh,
    HighSchool,
}

enum class JlptLevel(val number: Int) {
    N5(JLPT_BEGINNER_LEVEL),
    N4(JLPT_ELEMENTARY_LEVEL),
    N3(JLPT_INTERMEDIATE_LEVEL),
    N2(2),
    N1(1),
}

sealed interface JlptFilter {
    data object AnyLevel : JlptFilter

    data class Exact(val level: JlptLevel) : JlptFilter

    data object Missing : JlptFilter
}

/** Resolve notes through the translation row, so languages never get mixed. */
fun CatalogWord.usageNote(languageTag: String): String? {
    val tag = languageTag.lowercase(Locale.ROOT)
    val language =
        listOf(tag, tag.substringBefore('-'), "ru").firstOrNull { candidate ->
            translations.keys.any { it.equals(candidate, ignoreCase = true) }
        } ?: return null
    return usageNotes.entries.firstOrNull { it.key.equals(language, ignoreCase = true) }?.value
}

interface CatalogRepository {
    suspend fun load(): CatalogSnapshot
}

/** Requested locale, its base language, then Russian. Missing Russian remains explicit. */
fun <T> Map<String, T>.localized(languageTag: String): T? {
    val entries = entries.associate { it.key.lowercase(Locale.ROOT) to it.value }
    val tag = languageTag.lowercase(Locale.ROOT)
    return entries[tag] ?: entries[tag.substringBefore('-')] ?: entries["ru"]
}

fun CatalogSource.title(languageTag: String): String =
    titles.localized(languageTag) ?: originalTitle

fun CatalogWord.translation(languageTag: String): String? = translations.localized(languageTag)

fun CatalogKanji.meaning(languageTag: String): String? =
    meanings.localized(languageTag)?.joinToString("; ")

fun CatalogRadical.name(languageTag: String): String? = names.localized(languageTag)

private fun searchable(value: String): String = buildString {
    value.lowercase(Locale.ROOT).forEach {
        append(
            if (it in '\u30a1'..'\u30f6') (it.code - KATAKANA_TO_HIRAGANA_OFFSET).toChar() else it,
        )
    }
}

private fun matches(needle: String, vararg values: String?): Boolean {
    return needle.isEmpty() || values.any { it != null && searchable(it).contains(needle) }
}

/**
 * Immutable catalog snapshot; filtering is run on the injected computation dispatcher by
 * presentation.
 */
data class CatalogContent(
    val sources: List<CatalogSource>,
    val words: List<CatalogWord>,
    val kanji: List<CatalogKanji>,
    val radicals: List<CatalogRadical>,
    val placements: List<CatalogPlacement>,
    val links: List<CatalogLink>,
)

class CatalogSnapshot(val version: Int, content: CatalogContent) {
    val sources = content.sources
    val words = content.words
    val kanji = content.kanji
    val radicals = content.radicals
    val placements = content.placements
    val links = content.links

    val wordById = words.associateBy { it.id }
    val kanjiById = kanji.associateBy { it.id }
    val radicalById = radicals.associateBy { it.id }
    val sourceById = sources.associateBy { it.id }
    private val placementByKey = placements.associateBy { it.sourceId to it.kanjiId }

    private val placementsBySource = placements.groupBy { it.sourceId }
    private val linksBySource = links.groupBy { it.sourceId }
    val sourceCounts: Map<Int, CatalogSourceCounts> = sources.associate { source ->
        val rows = placementsBySource[source.id].orEmpty()
        source.id to
            CatalogSourceCounts(
                rows.map { it.chapter }.distinct().size,
                rows.size,
                linksBySource[source.id].orEmpty().map { it.wordId }.distinct().size,
            )
    }

    fun chapters(sourceId: Int): List<CatalogChapter> =
        placements
            .filter { it.sourceId == sourceId }
            .groupBy { it.chapter }
            .toSortedMap()
            .map { (chapter, items) ->
                val ids = items.map { it.kanjiId }.toSet()
                CatalogChapter(
                    chapter,
                    items.size,
                    links
                        .filter { it.sourceId == sourceId && it.kanjiId in ids }
                        .map { it.wordId }
                        .toSet()
                        .size,
                    items
                        .sortedBy { it.number }
                        .joinToString(" ") { kanjiById[it.kanjiId]?.symbol.orEmpty() },
                )
            }

    fun kanji(selection: CatalogSelection, query: String, language: String): List<CatalogKanji> {
        val scoped =
            if (selection.sourceId != null)
                placements
                    .filter {
                        it.sourceId == selection.sourceId &&
                            (selection.chapter == null || it.chapter == selection.chapter)
                    }
                    .associate { it.kanjiId to it.number }
            else null
        val needle = searchable(query.trim())
        return kanji
            .asSequence()
            .filter { scoped == null || it.id in scoped }
            .filter { selection.radicalId == null || it.radicalId == selection.radicalId }
            .filter { selection.strokes == null || it.strokes == selection.strokes }
            .filter {
                when (val filter = selection.jlpt) {
                    JlptFilter.AnyLevel -> true
                    JlptFilter.Missing -> it.jlptLevel == null
                    is JlptFilter.Exact -> it.jlptLevel == filter.level
                }
            }
            .filter { needle.isNotEmpty() || it.radicalId != null || scoped != null }
            .filter {
                needle.isEmpty() ||
                    searchable(it.symbol).contains(needle) ||
                    it.meaning(language)?.let { m -> searchable(m).contains(needle) } == true ||
                    it.on.any { o -> searchable(o).contains(needle) } ||
                    it.kun.any { k -> searchable(k).contains(needle) }
            }
            .sortedWith(
                compareBy<CatalogKanji> {
                        if (needle.isNotEmpty() && query.trim() == it.symbol) 0 else 1
                    }
                    .thenBy { scoped?.get(it.id) ?: it.id },
            )
            .toList()
    }

    fun words(selection: CatalogSelection, query: String, language: String): List<CatalogWord> {
        val scoped = links.filter { link ->
            (selection.sourceId == null || link.sourceId == selection.sourceId) &&
                (selection.kanjiId == null || link.kanjiId == selection.kanjiId) &&
                (selection.chapter == null ||
                    placementByKey[link.sourceId to link.kanjiId]?.chapter == selection.chapter) &&
                (selection.radicalId == null ||
                    kanjiById[link.kanjiId]?.radicalId == selection.radicalId)
        }
        val first =
            scoped
                .groupBy { it.wordId }
                .mapValues { (_, rows) ->
                    rows.minOf {
                        placementByKey[it.sourceId to it.kanjiId]?.number ?: Int.MAX_VALUE
                    }
                }
        val needle = searchable(query.trim())
        return words
            .asSequence()
            .filter { it.id in first }
            .filter { matches(needle, it.written, it.reading, it.translation(language)) }
            .sortedWith(compareBy<CatalogWord> { first[it.id] }.thenBy { it.id })
            .toList()
    }

    /** A word belongs to its first eligible placement, never to its literal spelling. */
    fun wordGroups(
        selection: CatalogSelection,
        query: String,
        language: String,
    ): List<CatalogWordGroup> {
        val sourceId = selection.sourceId ?: return emptyList()
        val firstPlacement =
            links
                .asSequence()
                .filter { it.sourceId == sourceId }
                .filter { selection.kanjiId == null || it.kanjiId == selection.kanjiId }
                .mapNotNull { link ->
                    placementByKey[link.sourceId to link.kanjiId]?.let { link.wordId to it }
                }
                .filter { (_, placement) ->
                    selection.chapter == null || placement.chapter == selection.chapter
                }
                .filter { (_, placement) ->
                    selection.radicalId == null ||
                        kanjiById[placement.kanjiId]?.radicalId == selection.radicalId
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, placements) -> placements.minBy { it.number } }
        val all = words(selection, "", language).groupBy { firstPlacement[it.id]?.chapter ?: 0 }
        val needle = searchable(query.trim())
        return all.toSortedMap().mapNotNull { (chapter, entries) ->
            val filtered = entries.filter {
                matches(needle, it.written, it.reading, it.translation(language))
            }
            if (filtered.isEmpty()) null else CatalogWordGroup(chapter, entries.size, filtered)
        }
    }

    /** Facets ignore their own selected value, while respecting every other active constraint. */
    fun radicals(query: String, language: String, strokes: Int? = null): List<CatalogRadical> {
        val needle = searchable(query.trim())
        return radicals
            .filter { strokes == null || it.strokes == strokes }
            .filter { matches(needle, it.symbol, it.name(language), it.id.toString()) }
            .sortedWith(compareBy<CatalogRadical> { it.strokes }.thenBy { it.id })
    }

    fun literalKanji(word: CatalogWord): List<CatalogKanji> =
        word.written.codePoints().toArray().distinct().mapNotNull { kanjiById[it] }
}

private const val JLPT_BEGINNER_LEVEL = 5
private const val JLPT_ELEMENTARY_LEVEL = 4
private const val JLPT_INTERMEDIATE_LEVEL = 3
private const val KATAKANA_TO_HIRAGANA_OFFSET = 0x60
