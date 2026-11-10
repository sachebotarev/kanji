package ru.mobui.kanji.domain.catalog

/**
 * Business rules for which glyphs need handwriting in a training word.
 *
 * Canonical list and change process: [doc/implementation/training-input-rules.md].
 * Host PRACTICE must call these helpers instead of inlining chapter/catalog checks.
 *
 * Independent Prefill gates combine with **AND** for [requiresHandwriting]: the cell is
 * `User` only when every active gate passes; failing any gate → `Prefilled`.
 */
object TrainingInputRules {

    /**
     * Selected textbook sources and their max chapter cap from book materials.
     *
     * Value is:
     * - `Int`: maximum selected chapter for that book.
     * - `null`: book is selected without chapter constraint (whole book).
     *
     * Empty map → book gate off (recipe has no book materials).
     */
    fun bookChapterCaps(recipe: Recipe): Map<Int, Int?> =
        recipe.materials
            .asSequence()
            .filter { it.type == MaterialType.Book && it.sourceId != null }
            .groupBy { requireNotNull(it.sourceId) }
            .mapValues { (_, rows) ->
                if (rows.any { it.chapters.isEmpty() }) null
                else rows.flatMap { it.chapters }.maxOrNull()
            }

    /**
     * Hardest JLPT among exact levels on kanji materials (smallest number: N1…N5).
     *
     * `null` → JLPT gate off. `-1` (no data) does not set a ceiling.
     */
    fun jlptDifficultyCeiling(recipe: Recipe): JlptLevel? =
        recipe.materials
            .asSequence()
            .filter { it.type == MaterialType.Kanji }
            .flatMap { it.jlptLevels }
            .filter { it in 1..5 }
            .minOrNull()
            ?.let { number -> JlptLevel.entries.first { it.number == number } }

    /**
     * True when [kanjiId] is permitted to be handwritten by the selected book materials.
     *
     * Rules:
     * - If [caps] is empty (no book materials in recipe) → gate is inactive, returns true.
     * - If [caps] is not empty:
     *   - The kanji MUST be placed in at least one of the selected textbooks.
     *   - If placed in a textbook with a chapter cap, its chapter must be <= maxChapter.
     *   - If not placed in any selected textbook, returns false (Prefilled).
     */
    fun isWithinBookChapters(
        snapshot: CatalogSnapshot,
        kanjiId: Int,
        caps: Map<Int, Int?>,
    ): Boolean {
        if (caps.isEmpty()) return true
        return caps.any { (sourceId, maxChapter) ->
            val chapter = snapshot.kanjiChapter(sourceId, kanjiId) ?: return@any false
            maxChapter == null || chapter <= maxChapter
        }
    }

    /**
     * True when [kanjiId] is NOT permitted by the book material constraints.
     *
     * Inverse of [isWithinBookChapters].
     */
    fun isBeyondSelectedChapters(
        snapshot: CatalogSnapshot,
        kanjiId: Int,
        caps: Map<Int, Int?>,
    ): Boolean = !isWithinBookChapters(snapshot, kanjiId, caps)

    /**
     * True when the glyph's JLPT is **harder** than the recipe ceiling.
     * Missing catalog JLPT is treated as [JlptLevel.N1].
     */
    fun isBeyondSelectedJlpt(level: JlptLevel?, ceiling: JlptLevel?): Boolean {
        if (ceiling == null) return false
        val effective = level ?: JlptLevel.N1
        return effective.number < ceiling.number
    }

    /**
     * Whether this code point must be handwritten.
     *
     * Gates combine with **AND**: catalog kanji ∧ within book chapters ∧ within JLPT ceiling.
     * A disabled gate (empty caps / null ceiling) is treated as passed.
     * Caller skips whitespace before invoking.
     */
    fun requiresHandwriting(
        codePoint: Int,
        snapshot: CatalogSnapshot,
        bookChapterCaps: Map<Int, Int?>,
        jlptCeiling: JlptLevel? = null,
    ): Boolean {
        val kanji = snapshot.kanjiById[codePoint] ?: return false
        val withinChapters = isWithinBookChapters(snapshot, kanji.id, bookChapterCaps)
        val withinJlpt = !isBeyondSelectedJlpt(kanji.jlptLevel, jlptCeiling)
        return withinChapters && withinJlpt
    }

    /** At least one writable cell. */
    fun hasWritableCell(requiresHandwritingPerCell: Iterable<Boolean>): Boolean =
        requiresHandwritingPerCell.any { it }
}

/** @see TrainingInputRules.bookChapterCaps */
fun Recipe.bookChapterCaps(): Map<Int, Int?> = TrainingInputRules.bookChapterCaps(this)

/** @see TrainingInputRules.jlptDifficultyCeiling */
fun Recipe.jlptDifficultyCeiling(): JlptLevel? = TrainingInputRules.jlptDifficultyCeiling(this)

/** @see TrainingInputRules.isWithinBookChapters */
fun CatalogSnapshot.isKanjiWithinBookChapters(kanjiId: Int, caps: Map<Int, Int?>): Boolean =
    TrainingInputRules.isWithinBookChapters(this, kanjiId, caps)

/** @see TrainingInputRules.isBeyondSelectedChapters */
fun CatalogSnapshot.isKanjiBeyondBookChapters(kanjiId: Int, caps: Map<Int, Int?>): Boolean =
    TrainingInputRules.isBeyondSelectedChapters(this, kanjiId, caps)
