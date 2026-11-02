package ru.mobui.kanji.feature.dictionary

import ru.mobui.kanji.core.wordui.WordStudyItem
import ru.mobui.kanji.domain.catalog.*

/** Mapping is outside the widget; callers supply navigation callbacks, never repositories. */
internal fun wordStudyItems(snapshot: CatalogSnapshot, wordId: Int, language: String, chapterLabel: (Int) -> String): List<WordStudyItem> = buildList {
    snapshot.links.filter { it.wordId == wordId }.groupBy { it.sourceId }.forEach { (sourceId, links) ->
        val source = snapshot.sourceById[sourceId] ?: return@forEach
        add(WordStudyItem(sourceId, null, source.title(language)))
        links.mapNotNull { it.kanjiId }.distinct().forEach { id ->
            val placement = snapshot.placements.firstOrNull { it.sourceId == sourceId && it.kanjiId == id }
            add(WordStudyItem(sourceId, id, snapshot.kanjiById[id]?.symbol.orEmpty(), placement?.let { chapterLabel(it.chapter) }))
        }
    }
}

