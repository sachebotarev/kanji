package ru.mobui.kanji.feature.dashboard

/** Presentation snapshot of a reusable recipe configuration, not a running training. */
data class RecipeOverview(
    val id: String,
    val name: String,
    /** Explicit task cap from the recipe; null means use the full material pool. */
    val taskCount: Int? = null,
    /** Unique words currently available for this recipe's materials. */
    val poolWordCount: Int = 0,
    val secondsPerSymbol: Int? = null,
    val completedTrainingCount: Int = 0,
) {
    val wordsPerTraining: Int
        get() = effectiveRecipeTaskCount(taskCount, poolWordCount)
}
