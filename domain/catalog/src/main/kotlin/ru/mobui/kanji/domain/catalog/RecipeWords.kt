package ru.mobui.kanji.domain.catalog

import kotlin.random.Random

/** Preview only: storage still validates the complete aggregate and its real source IDs. */
fun CatalogSnapshot.materialWords(material: RecipeMaterial, assignments: List<WordTagAssignment>): List<CatalogWord> {
    val source = sources.firstOrNull { it.id == material.sourceId }
    return when (material.type) {
        MaterialType.Book -> if (source?.sourceType in setOf("book", "textbook"))
            words(CatalogSelection(sourceId = material.sourceId, chapterIds = material.chapters, kanjiIds = material.kanjiIds), "", "ru")
        else emptyList()
        MaterialType.Dictionary -> {
            if (source?.sourceType != "personal") return emptyList()
            val tagged = assignments.filter { assignment -> assignment.tagIds.any { it in material.tagIds } }.map { it.wordId }.toSet()
            words(CatalogSelection(sourceId = material.sourceId), "", "ru").filter { word ->
                (material.tagIds.isEmpty() || word.id in tagged) &&
                    (material.kanjiIds.isEmpty() || literalKanji(word).any { it.id in material.kanjiIds })
            }
        }
        MaterialType.Kanji -> words.filter { word ->
            val literals = literalKanji(word)
            val jlptOk = material.jlptLevels.isEmpty() ||
                literals.any { sign -> (sign.jlptLevel?.number ?: -1) in material.jlptLevels }
            val kanjiOk = material.kanjiIds.isEmpty() || literals.any { it.id in material.kanjiIds }
            jlptOk && kanjiOk
        }
        MaterialType.Words -> words.filter { it.id in material.wordIds }
    }.distinctBy { it.id }
}

/** Union by stable word ID, preserving catalogue order; the query never changes stored choices. */
fun CatalogSnapshot.recipeWords(materials: List<RecipeMaterial>, assignments: List<WordTagAssignment>): List<CatalogWord> {
    val ids = materials.flatMap { materialWords(it, assignments) }.map { it.id }.toSet()
    return words.filter { it.id in ids }.distinctBy { it.id }
}

/**
 * `shuffled_cycles` queue for a training launch.
 *
 * - [count] ≤ pool: one shuffle, take the prefix (no repeats).
 * - [count] == pool size: one full shuffle.
 * - [count] > pool: repeated shuffled cycles; when the pool has more than one item,
 *   the first word of a new cycle must not equal the last word of the previous cycle.
 *
 * Spec: doc/design-system/05-screens/recipe-builder.md
 */
fun <T> shuffledCycles(pool: List<T>, count: Int, random: Random = Random.Default): List<T> {
    require(count > 0)
    if (pool.isEmpty()) return emptyList()
    if (count <= pool.size) return pool.shuffled(random).take(count)
    val out = ArrayList<T>(count)
    while (out.size < count) {
        var cycle = pool.shuffled(random)
        if (pool.size > 1 && out.isNotEmpty() && cycle.first() == out.last()) {
            val mutable = cycle.toMutableList()
            val swapAt = mutable.indexOfFirst { it != out.last() }.takeIf { it > 0 } ?: 1
            val tmp = mutable[0]
            mutable[0] = mutable[swapAt]
            mutable[swapAt] = tmp
            cycle = mutable
        }
        val take = minOf(count - out.size, cycle.size)
        out.addAll(cycle.take(take))
    }
    return out
}

/** Words selected for one training from a recipe pool and optional [Recipe.taskCount]. */
fun CatalogSnapshot.selectTrainingWords(
    recipe: Recipe,
    assignments: List<WordTagAssignment>,
    random: Random = Random.Default,
): List<CatalogWord> {
    val pool = recipeWords(recipe.materials, assignments)
    if (pool.isEmpty()) return emptyList()
    val count = recipe.taskCount?.coerceAtLeast(1) ?: pool.size
    return shuffledCycles(pool, count, random)
}
