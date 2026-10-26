package ru.mobui.kanji.domain.catalog

import java.util.Locale

/** Catalog identity never depends on order, translated names or chapter numbers. */
data class CatalogWord(val id: Int, val written: String, val reading: String,
    val translations: Map<String, String>, val parts: List<ReadingPart> = emptyList(),
    val isUserCreated: Boolean = false)
data class ReadingPart(val written: String, val reading: String)
data class CatalogKanji(val id: Int, val symbol: String, val strokes: Int, val radicalId: Int?,
    val kyoiku: Boolean, val joyo: Boolean, val jinmeiyo: Boolean,
    val meanings: Map<String, List<String>>, val on: List<String>, val kun: List<String>)
data class CatalogRadical(val id: Int, val symbol: String, val strokes: Int, val names: Map<String, String>)
data class CatalogSource(val id: Int, val originalTitle: String, val titles: Map<String, String>)
data class CatalogPlacement(val sourceId: Int, val kanjiId: Int, val number: Int, val chapter: Int)
data class CatalogLink(val sourceId: Int, val wordId: Int, val kanjiId: Int?)
data class CatalogChapter(val number: Int, val kanjiCount: Int, val wordCount: Int, val preview: String)
data class CatalogSelection(val sourceId: Int? = null, val chapter: Int? = null,
    val radicalId: Int? = null, val kanjiId: Int? = null)

interface CatalogRepository { suspend fun load(): CatalogSnapshot }

/** Requested locale, its base language, then Russian. Missing Russian remains explicit. */
fun <T> Map<String, T>.localized(languageTag: String): T? {
    val entries = entries.associate { it.key.lowercase(Locale.ROOT) to it.value }
    val tag = languageTag.lowercase(Locale.ROOT)
    return entries[tag] ?: entries[tag.substringBefore('-')] ?: entries["ru"]
}
fun CatalogSource.title(languageTag: String): String = titles.localized(languageTag) ?: originalTitle
fun CatalogWord.translation(languageTag: String): String? = translations.localized(languageTag)
fun CatalogKanji.meaning(languageTag: String): String? = meanings.localized(languageTag)?.joinToString("; ")
fun CatalogRadical.name(languageTag: String): String? = names.localized(languageTag)

private fun searchable(value: String): String = buildString {
    value.lowercase(Locale.ROOT).forEach { append(if (it in '\u30a1'..'\u30f6') (it.code - 0x60).toChar() else it) }
}
private fun matches(query: String, vararg values: String?): Boolean {
    val needle = searchable(query.trim())
    return needle.isEmpty() || values.any { it != null && searchable(it).contains(needle) }
}

/** Immutable catalog snapshot; filtering is run on the injected computation dispatcher by presentation. */
class CatalogSnapshot(val version: Int, val sources: List<CatalogSource>, val words: List<CatalogWord>,
    val kanji: List<CatalogKanji>, val radicals: List<CatalogRadical>,
    val placements: List<CatalogPlacement>, val links: List<CatalogLink>) {
    val wordById = words.associateBy { it.id }
    val kanjiById = kanji.associateBy { it.id }
    val radicalById = radicals.associateBy { it.id }
    val sourceById = sources.associateBy { it.id }
    private val placementByKey = placements.associateBy { it.sourceId to it.kanjiId }

    fun chapters(sourceId: Int): List<CatalogChapter> = placements.filter { it.sourceId == sourceId }
        .groupBy { it.chapter }.toSortedMap().map { (chapter, items) ->
            val ids = items.map { it.kanjiId }.toSet()
            CatalogChapter(chapter, items.size,
                links.filter { it.sourceId == sourceId && it.kanjiId in ids }.map { it.wordId }.toSet().size,
                items.sortedBy { it.number }.joinToString(" ") { kanjiById[it.kanjiId]?.symbol.orEmpty() })
        }

    fun kanji(selection: CatalogSelection, query: String, language: String): List<CatalogKanji> {
        val scoped = if (selection.sourceId != null) placements.filter {
            it.sourceId == selection.sourceId && (selection.chapter == null || it.chapter == selection.chapter)
        }.associate { it.kanjiId to it.number } else null
        return kanji.asSequence().filter { scoped == null || it.id in scoped }
            .filter { selection.radicalId == null || it.radicalId == selection.radicalId }
            .filter { matches(query, it.symbol, it.meaning(language), *it.on.toTypedArray(), *it.kun.toTypedArray()) }
            .sortedWith(compareBy<CatalogKanji> { if (query.trim() == it.symbol) 0 else 1 }
                .thenBy { scoped?.get(it.id) ?: it.id }).toList()
    }

    fun words(selection: CatalogSelection, query: String, language: String): List<CatalogWord> {
        val scoped = links.filter { link ->
            (selection.sourceId == null || link.sourceId == selection.sourceId) &&
                (selection.kanjiId == null || link.kanjiId == selection.kanjiId) &&
                (selection.chapter == null || placementByKey[link.sourceId to link.kanjiId]?.chapter == selection.chapter) &&
                (selection.radicalId == null || kanjiById[link.kanjiId]?.radicalId == selection.radicalId)
        }
        val first = scoped.groupBy { it.wordId }.mapValues { (_, rows) ->
            rows.minOf { placementByKey[it.sourceId to it.kanjiId]?.number ?: Int.MAX_VALUE }
        }
        return words.asSequence().filter { it.id in first }
            .filter { matches(query, it.written, it.reading, it.translation(language)) }
            .sortedWith(compareBy<CatalogWord> { first[it.id] }.thenBy { it.id }).toList()
    }

    /** Facets ignore their own selected value, while respecting every other active constraint. */
    fun availableRadicals(selection: CatalogSelection, query: String, language: String): List<CatalogRadical> {
        val ids = kanji(selection.copy(radicalId = null), "", language)
            .filter { selection.kanjiId == null || it.id == selection.kanjiId }.mapNotNull { it.radicalId }.toSet()
        return radicals(query, language).filter { it.id in ids }
    }

    fun availableChapters(selection: CatalogSelection): List<CatalogChapter> {
        val source = selection.sourceId ?: return emptyList()
        val eligible = kanji(selection.copy(chapter = null), "", "ru")
            .filter { selection.kanjiId == null || it.id == selection.kanjiId }.map { it.id }.toSet()
        return chapters(source).mapNotNull { chapter ->
            val rows = placements.filter { it.sourceId == source && it.chapter == chapter.number && it.kanjiId in eligible }
            if (rows.isEmpty()) null else chapter.copy(kanjiCount = rows.size,
                wordCount = words(selection.copy(chapter = chapter.number), "", "ru").size,
                preview = rows.sortedBy { it.number }.joinToString(" ") { kanjiById[it.kanjiId]?.symbol.orEmpty() })
        }
    }

    fun radicals(query: String, language: String): List<CatalogRadical> = radicals
        .filter { matches(query, it.symbol, it.name(language), it.id.toString()) }
        .sortedWith(compareBy<CatalogRadical> { it.strokes }.thenBy { it.id })

    fun literalKanji(word: CatalogWord): List<CatalogKanji> = word.written.codePoints().toArray()
        .distinct().mapNotNull { kanjiById[it] }
}
