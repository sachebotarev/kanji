package ru.mobui.kanji.domain.catalog

fun CatalogSnapshot.availableRadicals(
    selection: CatalogSelection,
    query: String,
    language: String,
): List<CatalogRadical> {
    val ids =
        kanji(selection.copy(radicalId = null, radicalIds = emptySet()), "", language)
            .filter { selection.acceptsKanji(it.id) }
            .mapNotNull { it.radicalId }
            .toSet()
    return radicals(query, language).filter { it.id in ids }
}

fun CatalogSnapshot.availableChapters(selection: CatalogSelection): List<CatalogChapter> {
    val source = selection.sourceId ?: return emptyList()
    val eligible =
        kanji(selection.copy(chapter = null, chapterIds = emptySet()), "", "ru")
            .filter { selection.acceptsKanji(it.id) }
            .map { it.id }
            .toSet()
    return chapters(source).mapNotNull { chapter ->
        val rows = placements.filter {
            it.sourceId == source && it.chapter == chapter.number && it.kanjiId in eligible
        }
        if (rows.isEmpty()) null
        else
            chapter.copy(
                kanjiCount = rows.size,
                wordCount = words(selection.copy(chapter = chapter.number, chapterIds = emptySet()), "", "ru").size,
                preview =
                    rows
                        .sortedBy { it.number }
                        .joinToString(" ") { kanjiById[it.kanjiId]?.symbol.orEmpty() },
            )
    }
}

fun CatalogSnapshot.strokeCounts(selection: CatalogSelection): Map<Int, Int> =
    kanji(selection.copy(strokes = null, strokeIds = emptySet()), "", "ru").groupingBy { it.strokes }.eachCount()

fun CatalogSnapshot.jlptCounts(selection: CatalogSelection): Map<JlptLevel?, Int> =
    kanji(selection.copy(jlpt = JlptFilter.AnyLevel, jlptFilters = emptySet()), "", "ru")
        .groupingBy { it.jlptLevel }
        .eachCount()

fun CatalogSnapshot.availableStrokes(selection: CatalogSelection): List<Int> =
    kanji(selection.copy(strokes = null, strokeIds = emptySet()), "", "ru").map { it.strokes }.distinct().sorted()

fun CatalogSnapshot.radicalStrokeCounts(): Map<Int, Int> =
    radicals.groupingBy { it.strokes }.eachCount()

fun CatalogSnapshot.availableJlpt(selection: CatalogSelection): List<JlptFilter> {
    val levels =
        kanji(selection.copy(jlpt = JlptFilter.AnyLevel, jlptFilters = emptySet()), "", "ru").map { it.jlptLevel }.toSet()
    return JlptLevel.entries.filter { it in levels }.map { JlptFilter.Exact(it) } +
        if (null in levels) listOf(JlptFilter.Missing) else emptyList()
}
