package ru.mobui.kanji.data.catalog

import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.mobui.kanji.domain.practice.CompletedTrainingTask
import ru.mobui.kanji.domain.practice.StoredCheckVerdict
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
import ru.mobui.kanji.domain.practice.TrainingValidationException
import ru.mobui.kanji.domain.practice.TrainingsRepository
import ru.mobui.kanji.domain.practice.validatedForSave
import ru.mobui.kanji.domain.practice.validatedForStart

internal class RoomTrainingsRepository(
    private val database: CatalogDatabase,
    private val prepare: suspend () -> Unit,
    private val io: CoroutineDispatcher,
) : TrainingsRepository {
    private val dao
        get() = database.trainingsDao()

    private fun <T> installed(observe: () -> Flow<T>): Flow<T> =
        flow {
                prepare()
                emitAll(observe())
            }
            .flowOn(io)

    override fun observeInProgress(): Flow<Training?> = installed {
        dao.observeInProgress().map { it?.toDomain() }
    }

    override fun observeCompletedTasks(): Flow<List<CompletedTrainingTask>> = installed {
        dao.observeCompletedTasks().map { rows ->
            rows.map { CompletedTrainingTask(it.endedAt, it.status) }
        }
    }

    override fun observeHistory(): Flow<List<Training>> = installed {
        dao.observeHistory().map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun getInProgress(): Training? =
        withContext(io) {
            prepare()
            dao.inProgress()?.toDomain()
        }

    override suspend fun getTraining(id: String): Training? =
        withContext(io) {
            prepare()
            dao.training(id)?.toDomain()
        }

    override suspend fun startTraining(training: Training) =
        withContext(io) {
            val valid = training.validatedForStart()
            prepare()
            database.withTransaction {
                val now = System.currentTimeMillis()
                dao.interruptInProgress(now)
                replaceAggregate(valid)
            }
        }

    override suspend fun saveTraining(training: Training) =
        withContext(io) {
            val valid = training.validatedForSave()
            prepare()
            database.withTransaction {
                if (dao.training(valid.id) == null) {
                    throw TrainingValidationException("Training ${valid.id} does not exist")
                }
                if (valid.status == TrainingStatus.InProgress) {
                    val others = dao.inProgressIds().filter { it != valid.id }
                    if (others.isNotEmpty()) {
                        throw TrainingValidationException(
                            "Another in-progress training exists: ${others.joinToString()}",
                        )
                    }
                }
                replaceAggregate(valid)
            }
        }

    override suspend fun deleteTraining(id: String) =
        withContext(io) {
            prepare()
            dao.deleteById(id)
        }

    override suspend fun clearHistory() =
        withContext(io) {
            prepare()
            dao.deleteAll()
        }

    private suspend fun replaceAggregate(training: Training) {
        dao.putTraining(
            TrainingsRow(
                training.id,
                training.recipeId,
                training.recipeRevision,
                training.taskCount,
                training.secondsPerSymbol,
                training.clueMode.key,
                when (training.strokeHintMode) {
                    ru.mobui.kanji.domain.practice.HintMode.NextStroke -> "next_stroke"
                    ru.mobui.kanji.domain.practice.HintMode.FullCharacter -> "full_character"
                },
                training.status.key,
                training.startedAt,
                training.endedAt,
            ),
        )
        dao.clearRecipeNames(training.id)
        if (training.recipeNames.isNotEmpty()) {
            dao.insertRecipeNames(
                training.recipeNames.map {
                    TrainingsRecipeTranslationsRow(training.id, it.languageTag, it.name)
                },
            )
        }
        replaceTasks(training)
    }

    private suspend fun replaceTasks(training: Training) {
        dao.clearTasks(training.id)
        if (training.tasks.isNotEmpty()) {
            dao.insertTasks(
                training.tasks.map { task ->
                    TrainingTasksRow(
                        task.id,
                        training.id,
                        task.position,
                        task.wordId,
                        task.writtenForm,
                        task.reading,
                        task.status.key,
                        task.startedAt,
                        task.endedAt,
                        task.elapsedMs,
                        task.readingHintUsed,
                        task.translationHintUsed,
                    )
                },
            )
            val translations =
                training.tasks.flatMap { task ->
                    task.translations.map {
                        TrainingTaskTranslationsRow(task.id, it.languageTag, it.translation)
                    }
                }
            if (translations.isNotEmpty()) dao.insertTaskTranslations(translations)
            val symbols =
                training.tasks.flatMap { task ->
                    task.symbols.map { symbol ->
                        TrainingTaskSymbolsRow(
                            symbol.id,
                            task.id,
                            symbol.position,
                            symbol.occurrenceId,
                            symbol.role.key,
                            symbol.script.key,
                            symbol.glyph,
                            symbol.kanjiId,
                            symbol.kanjiKey,
                            symbol.status.key,
                            symbol.hintRequestCount,
                            symbol.elapsedMs,
                            symbol.readabilityVerdict?.key,
                            symbol.readabilityGlyph,
                            symbol.shapeVerdict?.key,
                            symbol.shapeSimilarityPercent,
                            symbol.shapeBudgetPercent,
                            symbol.strokeWritingVerdict?.key,
                            symbol.strokeExpected,
                            symbol.strokeActual,
                            symbol.writingSampleJson,
                            symbol.checkReportJson,
                        )
                    }
                }
            if (symbols.isNotEmpty()) dao.insertSymbols(symbols)
        }
    }
}

private fun TrainingAggregate.toDomain(): Training =
    Training(
        training.id,
        training.recipeId,
        training.recipeRevision,
        recipeNames.map { TrainingRecipeName(it.languageTag, it.name) },
        training.taskCount,
        training.secondsPerSymbol,
        ru.mobui.kanji.domain.practice.TaskClueMode.fromKey(training.hintMode),
        ru.mobui.kanji.domain.practice.HintMode.fromKey(training.strokeHintMode),
        TrainingStatus.entries.single { it.key == training.status },
        training.startedAt,
        training.endedAt,
        tasks
            .sortedBy { it.task.position }
            .map { aggregate ->
                TrainingTask(
                    aggregate.task.id,
                    aggregate.task.position,
                    aggregate.task.wordId,
                    aggregate.task.writtenForm,
                    aggregate.task.reading,
                    aggregate.translations.map {
                        TrainingTaskTranslation(it.languageTag, it.translation)
                    },
                    TrainingTaskStatus.entries.single { it.key == aggregate.task.status },
                    aggregate.task.startedAt,
                    aggregate.task.endedAt,
                    aggregate.task.elapsedMs,
                    aggregate.task.readingHintUsed,
                    aggregate.task.translationHintUsed,
                    aggregate.symbols
                        .sortedBy { it.position }
                        .map { symbol ->
                            TrainingSymbol(
                                symbol.id,
                                symbol.position,
                                symbol.occurrenceId,
                                TrainingSymbolRole.entries.single { it.key == symbol.role },
                                TrainingSymbolScript.entries.single { it.key == symbol.script },
                                symbol.glyph,
                                symbol.kanjiId,
                                symbol.kanjiKey,
                                TrainingSymbolStatus.entries.single { it.key == symbol.status },
                                symbol.hintRequestCount,
                                symbol.elapsedMs,
                                symbol.readabilityVerdict?.let { key ->
                                    StoredCheckVerdict.entries.single { it.key == key }
                                },
                                symbol.readabilityGlyph,
                                symbol.shapeVerdict?.let { key ->
                                    StoredCheckVerdict.entries.single { it.key == key }
                                },
                                symbol.shapeSimilarityPercent,
                                symbol.shapeBudgetPercent,
                                symbol.strokeWritingVerdict?.let { key ->
                                    StoredCheckVerdict.entries.single { it.key == key }
                                },
                                symbol.strokeExpected,
                                symbol.strokeActual,
                                symbol.writingSampleJson,
                                symbol.checkReportJson,
                            )
                        },
                )
            },
    )
