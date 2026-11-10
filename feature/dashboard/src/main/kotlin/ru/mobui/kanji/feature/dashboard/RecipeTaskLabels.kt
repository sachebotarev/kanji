package ru.mobui.kanji.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * Effective number of tasks shown for a recipe: explicit [taskCount], else the material pool size.
 * Never invents a fake zero when the pool is known.
 */
fun effectiveRecipeTaskCount(taskCount: Int?, poolWordCount: Int): Int =
    taskCount ?: poolWordCount.coerceAtLeast(0)

/** Short metric label used on library cards and similar. */
@Composable
fun recipeTaskMetricLabel(taskCount: Int?, poolWordCount: Int): String {
    val explicit = taskCount
    return if (explicit != null) {
        pluralStringResource(R.plurals.library_task_count, explicit, explicit)
    } else {
        val n = poolWordCount.coerceAtLeast(0)
        pluralStringResource(R.plurals.library_all_words, n, n)
    }
}

/** One-line summary under a recipe title (pick list / home card). */
@Composable
fun recipeTrainingSummaryLine(taskCount: Int?, poolWordCount: Int, secondsPerSymbol: Int?): String {
    val words = effectiveRecipeTaskCount(taskCount, poolWordCount)
    val tasksPart =
        if (taskCount == null) {
            recipeTaskMetricLabel(null, poolWordCount)
        } else {
            stringResource(R.string.library_tasks, words)
        }
    return when {
        secondsPerSymbol == null || secondsPerSymbol == 0 ->
            "$tasksPart · ${stringResource(R.string.library_no_timer)}"
        secondsPerSymbol % 60 == 0 ->
            stringResource(
                R.string.recipe_training_summary_minutes,
                words,
                secondsPerSymbol / 60,
            )
        else ->
            stringResource(R.string.recipe_training_summary, words, secondsPerSymbol)
    }
}
