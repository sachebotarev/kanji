package ru.mobui.kanji.domain.practice

import java.util.Locale
import kotlinx.coroutines.flow.Flow

enum class TrainingStatus(val key: String) {
    InProgress("in_progress"),
    Completed("completed"),
    Interrupted("interrupted"),
}

enum class TrainingTaskStatus(val key: String) {
    Pending("pending"),
    InProgress("in_progress"),
    Correct("correct"),
    Assisted("assisted"),
    Skipped("skipped"),
    TimedOut("timed_out"),
}

enum class TrainingSymbolStatus(val key: String) {
    Empty("empty"),
    Editing("editing"),
    Correct("correct"),
    Incorrect("incorrect"),
    Prefilled("prefilled"),
}

enum class TrainingSymbolRole(val key: String) {
    Practice("practice"),
    Display("display"),
}

enum class TrainingSymbolScript(val key: String) {
    Kanji("kanji"),
    Hiragana("hiragana"),
    Katakana("katakana"),
    Other("other"),
}

enum class StoredCheckVerdict(val key: String) {
    Correct("correct"),
    Incorrect("incorrect"),
    Incomplete("incomplete"),
    Unavailable("unavailable"),
}

data class TrainingRecipeName(val languageTag: String, val name: String)

data class TrainingTaskTranslation(val languageTag: String, val translation: String)

data class TrainingSymbol(
    val id: String,
    val position: Int,
    val occurrenceId: String,
    val role: TrainingSymbolRole,
    val script: TrainingSymbolScript,
    val glyph: String,
    val kanjiId: Int? = null,
    val kanjiKey: String? = null,
    val status: TrainingSymbolStatus,
    val hintRequestCount: Int = 0,
    val elapsedMs: Long = 0,
    val readabilityVerdict: StoredCheckVerdict? = null,
    val readabilityGlyph: String? = null,
    val shapeVerdict: StoredCheckVerdict? = null,
    val shapeSimilarityPercent: Float? = null,
    val shapeBudgetPercent: Float? = null,
    val strokeWritingVerdict: StoredCheckVerdict? = null,
    val strokeExpected: Int? = null,
    val strokeActual: Int? = null,
    /** Opaque JSON snapshot of WritingSample; null when empty or display-only. */
    val writingSampleJson: String? = null,
    val checkReportJson: String? = null,
)

data class TrainingTask(
    val id: String,
    val position: Int,
    val wordId: Int? = null,
    val writtenForm: String,
    val reading: String,
    val translations: List<TrainingTaskTranslation>,
    val status: TrainingTaskStatus = TrainingTaskStatus.Pending,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val elapsedMs: Long = 0,
    /** Reading spoiler was opened during this attempt. */
    val readingHintUsed: Boolean = false,
    /** Translation spoiler was opened during this attempt. */
    val translationHintUsed: Boolean = false,
    val symbols: List<TrainingSymbol>,
)

data class Training(
    val id: String,
    val recipeId: String? = null,
    val recipeRevision: Long? = null,
    val recipeNames: List<TrainingRecipeName>,
    val taskCount: Int? = null,
    val secondsPerSymbol: Int? = null,
    val clueMode: TaskClueMode = TaskClueMode.Reading,
    val strokeHintMode: HintMode = HintMode.NextStroke,
    val status: TrainingStatus,
    val startedAt: Long,
    val endedAt: Long? = null,
    val tasks: List<TrainingTask>,
)

class TrainingValidationException(message: String) : IllegalArgumentException(message)

/** Terminal task snapshot for home statistics (`ended_at` + status key). */
data class CompletedTrainingTask(val endedAt: Long, val statusKey: String)

/**
 * Persistent trainings: at most one [TrainingStatus.InProgress]. Starting a new training
 * interrupts the previous in-progress row in the same transaction.
 */
interface TrainingsRepository {
    fun observeInProgress(): Flow<Training?>

    /**
     * Terminal tasks (`correct`, `assisted`, `skipped`, `timed_out`) with epoch-ms `ended_at`.
     * Empty when there is no completed work yet.
     */
    fun observeCompletedTasks(): Flow<List<CompletedTrainingTask>>

    /**
     * Trainings for footprints: in-progress first, then completed/interrupted by
     * `ended_at` / `started_at` descending. Long in-progress runs stay visible here.
     */
    fun observeHistory(): Flow<List<Training>>

    suspend fun getInProgress(): Training?

    suspend fun getTraining(id: String): Training?

    /** Inserts [training] as in-progress after marking any previous in-progress as interrupted. */
    suspend fun startTraining(training: Training)

    /** Replaces the aggregate for resume and progress updates. */
    suspend fun saveTraining(training: Training)

    /** Deletes one training and cascaded tasks/symbols. Study materials stay. */
    suspend fun deleteTraining(id: String)

    /** Deletes all trainings and cascaded tasks/symbols. */
    suspend fun clearHistory()
}

fun Training.validatedForStart(): Training {
    require(id.isNotBlank())
    require(status == TrainingStatus.InProgress && endedAt == null)
    require(startedAt > 0)
    require(taskCount == null || taskCount > 0)
    require(secondsPerSymbol == null || secondsPerSymbol > 0)
    require(recipeRevision == null || recipeRevision > 0)
    require(tasks.isNotEmpty())
    require(tasks.map { it.position }.distinct().size == tasks.size)
    require(tasks.map { it.id }.distinct().size == tasks.size)
    val names =
        recipeNames.map { TrainingRecipeName(validLanguage(it.languageTag), it.name.trim()) }
    require(names.isEmpty() || names.all { it.name.length in 1..120 })
    require(names.map { it.languageTag.lowercase(Locale.ROOT) }.distinct().size == names.size)
    return copy(recipeNames = names, tasks = tasks.map { it.validated() }.sortedBy { it.position })
}

fun Training.validatedForSave(): Training {
    require(id.isNotBlank())
    require(startedAt > 0)
    require(taskCount == null || taskCount > 0)
    require(secondsPerSymbol == null || secondsPerSymbol > 0)
    require(recipeRevision == null || recipeRevision > 0)
    when (status) {
        TrainingStatus.InProgress -> require(endedAt == null)
        TrainingStatus.Completed,
        TrainingStatus.Interrupted -> require(endedAt != null && endedAt >= startedAt)
    }
    require(tasks.isNotEmpty())
    require(tasks.map { it.position }.distinct().size == tasks.size)
    require(tasks.map { it.id }.distinct().size == tasks.size)
    val names =
        recipeNames.map { TrainingRecipeName(validLanguage(it.languageTag), it.name.trim()) }
    require(names.isEmpty() || names.all { it.name.length in 1..120 })
    require(names.map { it.languageTag.lowercase(Locale.ROOT) }.distinct().size == names.size)
    return copy(recipeNames = names, tasks = tasks.map { it.validated() }.sortedBy { it.position })
}

private fun TrainingTask.validated(): TrainingTask {
    require(id.isNotBlank() && position >= 0)
    require(writtenForm.isNotBlank() && reading.isNotBlank())
    require(elapsedMs >= 0)
    require(symbols.isNotEmpty())
    require(symbols.map { it.position }.distinct().size == symbols.size)
    require(symbols.map { it.occurrenceId }.distinct().size == symbols.size)
    val translations =
        translations.map {
            TrainingTaskTranslation(validLanguage(it.languageTag), it.translation.trim())
        }
    require(translations.isEmpty() || translations.all { it.translation.isNotBlank() })
    require(
        translations.map { it.languageTag.lowercase(Locale.ROOT) }.distinct().size ==
            translations.size
    )
    return copy(
        translations = translations,
        symbols = symbols.map { it.validated() }.sortedBy { it.position },
    )
}

private fun TrainingSymbol.validated(): TrainingSymbol {
    require(id.isNotBlank() && occurrenceId.isNotBlank() && position >= 0)
    require(glyph.isNotBlank())
    require(hintRequestCount >= 0 && elapsedMs >= 0)
    require(kanjiKey == null || kanjiKey.isNotBlank())
    require(
        shapeSimilarityPercent == null ||
            shapeSimilarityPercent in 0f..100f
    )
    require(shapeBudgetPercent == null || shapeBudgetPercent >= 0f)
    require(strokeExpected == null || strokeExpected >= 0)
    require(strokeActual == null || strokeActual >= 0)
    if (role == TrainingSymbolRole.Display) {
        require(status == TrainingSymbolStatus.Prefilled)
    }
    return this
}

private fun validLanguage(value: String): String {
    val locale = Locale.Builder().setLanguageTag(value.trim()).build()
    require(locale.language.isNotBlank() && locale.language != "und")
    return locale.toLanguageTag()
}
