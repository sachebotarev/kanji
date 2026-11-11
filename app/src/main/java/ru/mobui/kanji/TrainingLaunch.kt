package ru.mobui.kanji

import java.util.UUID
import kotlin.random.Random
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.JlptLevel
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.WordTagAssignment
import ru.mobui.kanji.domain.catalog.TrainingInputRules
import ru.mobui.kanji.domain.catalog.recipeWords
import ru.mobui.kanji.domain.catalog.translation
import ru.mobui.kanji.domain.catalog.shuffledCycles
import ru.mobui.kanji.domain.handwriting.toStorageJson
import ru.mobui.kanji.domain.practice.AnswerStatus
import ru.mobui.kanji.domain.practice.EntryMode
import ru.mobui.kanji.domain.practice.EntryStatus
import ru.mobui.kanji.domain.practice.KanjiId
import ru.mobui.kanji.domain.practice.KanjiResult
import ru.mobui.kanji.domain.practice.KanjiTask
import ru.mobui.kanji.domain.practice.PracticeResult
import ru.mobui.kanji.domain.practice.PracticeTask
import ru.mobui.kanji.domain.practice.Training
import ru.mobui.kanji.domain.practice.TrainingRecipeName
import ru.mobui.kanji.domain.practice.TrainingStatus
import ru.mobui.kanji.domain.practice.TrainingSymbol
import ru.mobui.kanji.domain.practice.TrainingSymbolRole
import ru.mobui.kanji.domain.practice.TrainingSymbolScript
import ru.mobui.kanji.domain.practice.TrainingSymbolStatus
import ru.mobui.kanji.domain.practice.TrainingTask
import ru.mobui.kanji.domain.practice.TrainingTaskStatus
import ru.mobui.kanji.domain.practice.TrainingTaskTranslation
import ru.mobui.kanji.domain.practice.wordTimeLimitMs

/** In-memory training run bound to a persisted [Training.id]. */
internal data class ActiveTraining(
    val training: Training,
    val tasks: List<PracticeTask>,
    val index: Int = 0,
) {
    init {
        require(tasks.isNotEmpty() && tasks.size == training.tasks.size)
        require(index in tasks.indices)
    }

    val task: PracticeTask
        get() = tasks[index]

    val hasNext: Boolean
        get() = index < tasks.lastIndex

    val ordinal: Int
        get() = index + 1

    val total: Int
        get() = tasks.size

    fun advance(): ActiveTraining? = if (hasNext) copy(index = index + 1) else null
}

internal data class TrainingLaunch(
    val active: ActiveTraining,
)

/**
 * Builds practice tasks and a persistable [Training] with [shuffledCycles] word order.
 *
 * Which cells need handwriting: [TrainingInputRules] (doc/implementation/training-input-rules.md).
 */
internal fun launchTrainingFromRecipe(
    snapshot: CatalogSnapshot,
    recipe: Recipe,
    assignments: List<WordTagAssignment>,
    maxNextStrokeHints: Int = 3,
    nowMs: Long = System.currentTimeMillis(),
    random: Random = Random.Default,
): TrainingLaunch? {
    val chapterCaps = TrainingInputRules.bookChapterCaps(recipe)
    val jlptCeiling = TrainingInputRules.jlptDifficultyCeiling(recipe)
    val pool =
        snapshot
            .recipeWords(recipe.materials, assignments)
            .mapNotNull { it.toLaunchWord(snapshot, chapterCaps, jlptCeiling) }
    if (pool.isEmpty()) return null
    val count = recipe.taskCount?.coerceAtLeast(1) ?: pool.size
    val ordered = shuffledLaunchWords(pool, count, random)
    return buildLaunch(recipe, ordered, maxNextStrokeHints, nowMs)
}

private data class LaunchWord(
    val word: CatalogWord,
    val cells: List<LaunchCell>,
    val prompt: String,
)

private data class LaunchCell(
    val occurrenceId: String,
    val codePoint: Int,
    val glyph: String,
    val mode: EntryMode,
    val script: TrainingSymbolScript,
    val kanjiId: Int?,
    val kanjiKey: String,
)

private fun CatalogWord.toLaunchWord(
    snapshot: CatalogSnapshot,
    bookChapterCaps: Map<Int, Int?>,
    jlptCeiling: JlptLevel? = null,
): LaunchWord? {
    val cells =
        buildList {
            written.codePoints().toArray().forEachIndexed { index, code ->
                if (Character.isWhitespace(code)) return@forEachIndexed
                val catalog = snapshot.kanjiById[code]
                val mode =
                    if (TrainingInputRules.requiresHandwriting(code, snapshot, bookChapterCaps, jlptCeiling)) {
                        EntryMode.User
                    } else {
                        EntryMode.Prefilled
                    }
                add(
                    LaunchCell(
                        occurrenceId = "$id-$index",
                        codePoint = code,
                        glyph = String(Character.toChars(code)),
                        mode = mode,
                        script = scriptOf(code),
                        kanjiId = catalog?.id,
                        kanjiKey = "u${code.toString(APP_UNICODE_HEX_RADIX)}",
                    ),
                )
            }
        }
    if (!TrainingInputRules.hasWritableCell(cells.map { it.mode == EntryMode.User })) return null
    val prompt = translation("ru")?.takeIf { it.isNotBlank() } ?: written
    return LaunchWord(this, cells, prompt)
}

private fun shuffledLaunchWords(
    pool: List<LaunchWord>,
    count: Int,
    random: Random,
): List<LaunchWord> = shuffledCycles(pool, count, random)

private fun ru.mobui.kanji.domain.catalog.RecipeHintMode.toTaskClueMode(): ru.mobui.kanji.domain.practice.TaskClueMode =
    when (this) {
        ru.mobui.kanji.domain.catalog.RecipeHintMode.Translation -> ru.mobui.kanji.domain.practice.TaskClueMode.Translation
        ru.mobui.kanji.domain.catalog.RecipeHintMode.Reading -> ru.mobui.kanji.domain.practice.TaskClueMode.Reading
        ru.mobui.kanji.domain.catalog.RecipeHintMode.None -> ru.mobui.kanji.domain.practice.TaskClueMode.None
    }

private fun ru.mobui.kanji.domain.catalog.RecipeStrokeHintMode.toHintMode(): ru.mobui.kanji.domain.practice.HintMode =
    when (this) {
        ru.mobui.kanji.domain.catalog.RecipeStrokeHintMode.NextStroke -> ru.mobui.kanji.domain.practice.HintMode.NextStroke
        ru.mobui.kanji.domain.catalog.RecipeStrokeHintMode.FullCharacter -> ru.mobui.kanji.domain.practice.HintMode.FullCharacter
    }

private fun buildLaunch(
    recipe: Recipe,
    words: List<LaunchWord>,
    maxNextStrokeHints: Int,
    nowMs: Long,
): TrainingLaunch {
    val clueMode = recipe.hintMode.toTaskClueMode()
    val strokeHintMode = recipe.strokeHintMode.toHintMode()
    val trainingId = UUID.randomUUID().toString()
    val practiceTasks = ArrayList<PracticeTask>(words.size)
    val trainingTasks = ArrayList<TrainingTask>(words.size)
    words.forEachIndexed { position, launch ->
        val taskId = UUID.randomUUID().toString()
        val kanjiTasks =
            launch.cells.map { cell ->
                KanjiTask(cell.occurrenceId, KanjiId(cell.kanjiKey), cell.mode)
            }
        val practiceCount = launch.cells.count { it.mode == EntryMode.User }
        practiceTasks +=
            PracticeTask(
                taskId = taskId,
                russianWord = launch.prompt,
                kanji = kanjiTasks,
                reading = launch.word.reading.trim(),
                timeLimitMs = wordTimeLimitMs(recipe.secondsPerSymbol, practiceCount),
                clueMode = clueMode,
                strokeHintMode = strokeHintMode,
            )
        trainingTasks +=
            TrainingTask(
                id = taskId,
                position = position,
                wordId = launch.word.id,
                writtenForm = launch.word.written,
                reading = launch.word.reading,
                translations =
                    launch.word.translations.map { (tag, text) ->
                        TrainingTaskTranslation(tag, text)
                    }.ifEmpty {
                        listOf(TrainingTaskTranslation("ru", launch.prompt))
                    },
                status = TrainingTaskStatus.Pending,
                symbols =
                    launch.cells.mapIndexed { cellIndex, cell ->
                        TrainingSymbol(
                            id = UUID.randomUUID().toString(),
                            position = cellIndex,
                            occurrenceId = cell.occurrenceId,
                            role =
                                if (cell.mode == EntryMode.User) TrainingSymbolRole.Practice
                                else TrainingSymbolRole.Display,
                            script = cell.script,
                            glyph = cell.glyph,
                            kanjiId = cell.kanjiId,
                            kanjiKey = cell.kanjiKey,
                            status =
                                if (cell.mode == EntryMode.User) TrainingSymbolStatus.Empty
                                else TrainingSymbolStatus.Prefilled,
                        )
                    },
            )
    }
    val training =
        Training(
            id = trainingId,
            recipeId = recipe.id,
            recipeRevision = recipe.revision,
            recipeNames = recipe.names.map { TrainingRecipeName(it.languageTag, it.name) },
            taskCount = recipe.taskCount,
            secondsPerSymbol = recipe.secondsPerSymbol,
            clueMode = clueMode,
            strokeHintMode = strokeHintMode,
            maxNextStrokeHints = maxNextStrokeHints,
            status = TrainingStatus.InProgress,
            startedAt = nowMs,
            endedAt = null,
            tasks = trainingTasks,
        )
    return TrainingLaunch(ActiveTraining(training, practiceTasks))
}

internal fun ActiveTraining.withTaskResult(result: PracticeResult, nowMs: Long): ActiveTraining {
    require(result.taskId == task.taskId)
    val taskStatus =
        when (result.status) {
            AnswerStatus.Correct -> TrainingTaskStatus.Correct
            AnswerStatus.Assisted -> TrainingTaskStatus.Assisted
            AnswerStatus.Skipped -> TrainingTaskStatus.Skipped
            AnswerStatus.TimedOut -> TrainingTaskStatus.TimedOut
        }
    val updatedTasks =
        training.tasks.mapIndexed { position, trainingTask ->
            if (position != index) return@mapIndexed trainingTask
            trainingTask.copy(
                status = taskStatus,
                startedAt = trainingTask.startedAt ?: training.startedAt,
                endedAt = nowMs,
                elapsedMs = result.elapsedMs,
                readingHintUsed = result.readingHintUsed,
                translationHintUsed = result.translationHintUsed,
                symbols =
                    trainingTask.symbols.map { symbol ->
                        val cell =
                            result.kanji.firstOrNull { it.occurrenceId == symbol.occurrenceId }
                                ?: return@map symbol
                        symbol.fromKanjiResult(cell)
                    },
            )
        }
    val completed = index >= tasks.lastIndex
    val updatedTraining =
        training.copy(
            tasks = updatedTasks,
            status = if (completed) TrainingStatus.Completed else TrainingStatus.InProgress,
            endedAt = if (completed) nowMs else null,
        )
    return copy(training = updatedTraining)
}

internal fun ActiveTraining.interrupted(nowMs: Long): ActiveTraining =
    copy(
        training =
            training.copy(
                status = TrainingStatus.Interrupted,
                endedAt = nowMs,
            ),
    )

/**
 * Rebuilds an in-memory run from a persisted [Training]. Resumes at the first non-terminal task;
 * ink is not restored (fresh pad on the current word).
 */
internal fun resumeActiveTraining(training: Training): ActiveTraining? {
    if (training.status != TrainingStatus.InProgress || training.tasks.isEmpty()) return null
    val index =
        training.tasks
            .indexOfFirst {
                it.status == TrainingTaskStatus.Pending || it.status == TrainingTaskStatus.InProgress
            }
            .takeIf { it >= 0 } ?: return null
    val practiceTasks =
        training.tasks.map { task ->
            val practiceCount =
                task.symbols.count { it.role == TrainingSymbolRole.Practice }
            PracticeTask(
                taskId = task.id,
                russianWord =
                    task.translations
                        .firstOrNull { it.languageTag.equals("ru", ignoreCase = true) }
                        ?.translation
                        ?.takeIf { it.isNotBlank() }
                        ?: task.translations.firstOrNull()?.translation?.takeIf { it.isNotBlank() }
                        ?: task.writtenForm,
                kanji =
                    task.symbols.map { symbol ->
                        KanjiTask(
                            symbol.occurrenceId,
                            KanjiId(symbol.kanjiKey ?: "u0"),
                            if (symbol.role == TrainingSymbolRole.Practice) EntryMode.User
                            else EntryMode.Prefilled,
                        )
                    },
                reading = task.reading,
                timeLimitMs = wordTimeLimitMs(training.secondsPerSymbol, practiceCount),
                clueMode = training.clueMode,
                strokeHintMode = training.strokeHintMode,
            )
        }
    return ActiveTraining(training, practiceTasks, index)
}

private fun TrainingSymbol.fromKanjiResult(result: KanjiResult): TrainingSymbol =
    copy(
        status =
            when (result.status) {
                EntryStatus.Empty -> TrainingSymbolStatus.Empty
                EntryStatus.Editing -> TrainingSymbolStatus.Editing
                EntryStatus.Correct -> TrainingSymbolStatus.Correct
                EntryStatus.Incorrect -> TrainingSymbolStatus.Incorrect
                EntryStatus.Prefilled -> TrainingSymbolStatus.Prefilled
            },
        hintRequestCount = maxOf(hintRequestCount, result.hintRequestCount),
        shapeVerdict = when {
            result.shapeAcceptedWithPenalty -> ru.mobui.kanji.domain.practice.StoredCheckVerdict.AcceptedWithPenalty
            shapeVerdict == ru.mobui.kanji.domain.practice.StoredCheckVerdict.AcceptedWithPenalty -> ru.mobui.kanji.domain.practice.StoredCheckVerdict.Correct
            else -> shapeVerdict
        },
        elapsedMs = result.elapsedMs,
        writingSampleJson =
            if (result.mode == EntryMode.User && result.sample.strokeCount > 0) {
                result.sample.toStorageJson()
            } else null,
    )

private fun scriptOf(codePoint: Int): TrainingSymbolScript =
    when (Character.UnicodeScript.of(codePoint)) {
        Character.UnicodeScript.HIRAGANA -> TrainingSymbolScript.Hiragana
        Character.UnicodeScript.KATAKANA -> TrainingSymbolScript.Katakana
        Character.UnicodeScript.HAN -> TrainingSymbolScript.Kanji
        else -> TrainingSymbolScript.Other
    }
