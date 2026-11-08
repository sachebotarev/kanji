package ru.mobui.kanji.feature.dashboard.history

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.practice.Training
import ru.mobui.kanji.domain.practice.TrainingGradeLevel
import ru.mobui.kanji.domain.practice.TrainingRecipeName
import ru.mobui.kanji.domain.practice.TrainingStatus
import ru.mobui.kanji.domain.practice.TrainingTask
import ru.mobui.kanji.domain.practice.TrainingTaskStatus
import ru.mobui.kanji.domain.practice.TrainingTaskTranslation
import ru.mobui.kanji.domain.practice.summarize

enum class TrainingHistoryPeriod(val key: String) {
    Today("today"),
    Days7("7"),
    Days30("30"),
    All("all"),
    ;

    companion object {
        fun fromKey(value: String): TrainingHistoryPeriod =
            entries.firstOrNull { it.key == value } ?: All
    }
}

data class TrainingTaskCounts(val correct: Int, val assisted: Int, val missed: Int) {
    val total: Int
        get() = correct + assisted + missed
}

data class TrainingHistoryDay(val day: LocalDate, val items: List<Training>)

fun Training.taskCounts(): TrainingTaskCounts {
    var correct = 0
    var assisted = 0
    var missed = 0
    tasks.forEach { task ->
        when (task.status) {
            TrainingTaskStatus.Correct -> correct++
            TrainingTaskStatus.Assisted -> assisted++
            TrainingTaskStatus.Skipped,
            TrainingTaskStatus.TimedOut -> missed++
            TrainingTaskStatus.Pending,
            TrainingTaskStatus.InProgress -> Unit
        }
    }
    return TrainingTaskCounts(correct, assisted, missed)
}

fun Training.durationMs(nowMs: Long = System.currentTimeMillis()): Long {
    val end = endedAt ?: nowMs
    return (end - startedAt).coerceAtLeast(0L)
}

/** 1-based current word index and total tasks for an unfinished training. */
fun Training.wordProgress(): Pair<Int, Int> {
    val done = tasks.count { it.isTerminal() }
    val total = tasks.size.coerceAtLeast(1)
    val current = (done + 1).coerceAtMost(total)
    return current to total
}

fun List<TrainingRecipeName>.localizedName(language: String, fallback: String): String =
    firstOrNull { it.languageTag.equals(language, ignoreCase = true) }?.name
        ?: firstOrNull { it.languageTag.equals("ru", ignoreCase = true) }?.name
        ?: firstOrNull()?.name
        ?: fallback

fun List<TrainingTaskTranslation>.localizedText(language: String): String =
    firstOrNull { it.languageTag.equals(language, ignoreCase = true) }?.translation
        ?: firstOrNull { it.languageTag.equals("ru", ignoreCase = true) }?.translation
        ?: firstOrNull()?.translation
        ?: ""

fun filterHistory(
    trainings: List<Training>,
    period: TrainingHistoryPeriod,
    query: String,
    language: String,
    zone: ZoneId,
    today: LocalDate = LocalDate.now(zone),
): List<Training> {
    val q = query.trim().lowercase()
    val cutoff =
        when (period) {
            TrainingHistoryPeriod.Today -> today
            TrainingHistoryPeriod.Days7 -> today.minusDays(DAYS_7_OFFSET)
            TrainingHistoryPeriod.Days30 -> today.minusDays(DAYS_30_OFFSET)
            TrainingHistoryPeriod.All -> null
        }
    return trainings.filter { training ->
        val name = training.recipeNames.localizedName(language, "").lowercase()
        val matchesQuery = q.isEmpty() || name.contains(q)
        if (!matchesQuery) return@filter false
        // Long in-progress runs stay visible for any period filter.
        if (training.status == TrainingStatus.InProgress) return@filter true
        val day = Instant.ofEpochMilli(training.startedAt).atZone(zone).toLocalDate()
        cutoff == null || !day.isBefore(cutoff)
    }
}

fun groupHistoryByDay(
    trainings: List<Training>,
    zone: ZoneId,
): List<TrainingHistoryDay> {
    val grouped =
        trainings
            .groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
            .toSortedMap(compareByDescending { it })
    return grouped.map { (day, items) ->
        TrainingHistoryDay(
            day,
            items.sortedWith(
                compareByDescending<Training> { it.status == TrainingStatus.InProgress }
                    .thenByDescending { it.startedAt },
            ),
        )
    }
}

fun dayRelation(day: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(day, today)

fun TrainingTask.isTerminal(): Boolean =
    status == TrainingTaskStatus.Correct ||
        status == TrainingTaskStatus.Assisted ||
        status == TrainingTaskStatus.Skipped ||
        status == TrainingTaskStatus.TimedOut

fun Training.isExcellent(languageTag: String = "ru"): Boolean =
    status != TrainingStatus.InProgress &&
        tasks.isNotEmpty() &&
        tasks.all { it.isTerminal() } &&
        runCatching { summarize(languageTag).grade == TrainingGradeLevel.Excellent }.getOrDefault(false)

/**
 * Finds IDs of recipes that have at least one completed excellent training
 * matching the recipe's current revision. If the recipe was edited, its revision
 * increased and the stamp resets until a new excellent training is completed.
 */
fun findExcellentRecipeIds(
    recipes: List<Recipe>,
    history: List<Training>,
    languageTag: String = "ru",
): Set<String> =
    recipes.filter { recipe ->
        history.any {
            it.recipeId == recipe.id &&
                it.recipeRevision == recipe.revision &&
                it.isExcellent(languageTag)
        }
    }.map { it.id }.toSet()

private const val DAYS_7_OFFSET = 6L
private const val DAYS_30_OFFSET = 29L

