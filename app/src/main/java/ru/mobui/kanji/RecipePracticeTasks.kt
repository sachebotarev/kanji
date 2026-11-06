package ru.mobui.kanji

import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.WordTagAssignment
import ru.mobui.kanji.domain.catalog.recipeWords
import ru.mobui.kanji.domain.catalog.translation
import ru.mobui.kanji.domain.practice.EntryMode
import ru.mobui.kanji.domain.practice.KanjiId
import ru.mobui.kanji.domain.practice.KanjiTask
import ru.mobui.kanji.domain.practice.PracticeTask

/**
 * Active recipe word pool → practice queue in catalogue order.
 * Takes [Recipe.taskCount] convertible words, or the whole pool when count is unset.
 */
internal fun recipePracticeTasks(
    snapshot: CatalogSnapshot,
    recipe: Recipe,
    assignments: List<WordTagAssignment>,
): List<PracticeTask> {
    val words = snapshot.recipeWords(recipe.materials, assignments)
    val limit = recipe.taskCount?.coerceAtLeast(1) ?: Int.MAX_VALUE
    val timeLimitMs = recipe.secondsPerWord?.let { it.toLong() * MILLIS_PER_SECOND }
    return words.asSequence()
        .mapNotNull { word -> word.toPracticeTask(snapshot, timeLimitMs) }
        .take(limit)
        .toList()
}

private fun CatalogWord.toPracticeTask(
    snapshot: CatalogSnapshot,
    timeLimitMs: Long?,
): PracticeTask? {
    val cells = buildList {
        written.codePoints().toArray().forEachIndexed { index, code ->
            if (Character.isWhitespace(code)) return@forEachIndexed
            val isCatalogKanji = snapshot.kanjiById[code] != null
            add(
                KanjiTask(
                    occurrenceId = "$id-$index",
                    kanjiId = KanjiId("u${code.toString(APP_UNICODE_HEX_RADIX)}"),
                    // Kana and other non-kanji stay visible as given cells; only catalog kanji
                    // are written by the learner.
                    mode = if (isCatalogKanji) EntryMode.User else EntryMode.Prefilled,
                ),
            )
        }
    }
    if (cells.none { it.mode == EntryMode.User }) return null
    val prompt = translation("ru")?.takeIf { it.isNotBlank() } ?: written
    return PracticeTask(
        taskId = "recipe-word-$id",
        russianWord = prompt,
        kanji = cells,
        reading = reading.trim(),
        timeLimitMs = timeLimitMs,
    )
}

private const val MILLIS_PER_SECOND = 1000L
