package ru.mobui.kanji.data.catalog

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import ru.mobui.kanji.domain.practice.HintMode
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

class TrainingsStorageTest {
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private fun storage(test: suspend (CatalogDatabase, RoomCatalogRepository) -> Unit) =
        runBlocking {
            val context = instrumentation.targetContext
            val name = "trainings-${System.nanoTime()}.db"
            val db = CatalogDatabase.open(context, name)
            try {
                test(db, RoomCatalogRepository(db, instrumentation.context.assets, Dispatchers.IO))
            } finally {
                db.close()
                context.deleteDatabase(name)
            }
        }

    private fun training(
        id: String,
        wordId: Int?,
        status: TrainingStatus = TrainingStatus.InProgress,
        endedAt: Long? = null,
        taskStatus: TrainingTaskStatus = TrainingTaskStatus.Pending,
    ) =
        Training(
            id = id,
            recipeId = null,
            recipeRevision = 1,
            recipeNames = listOf(TrainingRecipeName("ru", "Демо рецепт")),
            taskCount = 1,
            secondsPerSymbol = 60,
            status = status,
            startedAt = 1_000L,
            endedAt = endedAt,
            tasks =
                listOf(
                    TrainingTask(
                        id = "$id-task",
                        position = 0,
                        wordId = wordId,
                        writtenForm = "日本",
                        reading = "にほん",
                        translations = listOf(TrainingTaskTranslation("ru", "Япония")),
                        status = taskStatus,
                        symbols =
                            listOf(
                                TrainingSymbol(
                                    id = "$id-s0",
                                    position = 0,
                                    occurrenceId = "0",
                                    role = TrainingSymbolRole.Practice,
                                    script = TrainingSymbolScript.Kanji,
                                    glyph = "日",
                                    status = TrainingSymbolStatus.Empty,
                                ),
                                TrainingSymbol(
                                    id = "$id-s1",
                                    position = 1,
                                    occurrenceId = "1",
                                    role = TrainingSymbolRole.Practice,
                                    script = TrainingSymbolScript.Kanji,
                                    glyph = "本",
                                    status = TrainingSymbolStatus.Empty,
                                ),
                            ),
                    ),
                ),
        )

    @Test
    fun schemaIncludesTrainingTablesOnVersion12() = storage { db, catalog ->
        catalog.load()
        val sql = db.openHelper.writableDatabase
        assertEquals(12, sql.version)
        val tables =
            sql.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
                buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
        assertTrue(
            tables.containsAll(
                setOf(
                    "trainings",
                    "trainings_recipe_translations",
                    "training_tasks",
                    "training_task_translations",
                    "training_task_symbols",
                ),
            ),
        )
        assertEquals(
            1,
            sql.query(
                    "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_trainings_one_in_progress'",
                )
                .use { it.count },
        )
    }

    @Test
    fun startInterruptsPreviousAndPersistsAggregate() = storage { db, catalog ->
        catalog.load()
        val word = catalog.saveWord(null,
            ru.mobui.kanji.domain.catalog.WordDraft(
                "日本",
                "にほん",
                listOf(ru.mobui.kanji.domain.catalog.WordTranslationDraft("ru", "Япония")),
            ),
        )
        val trainings = catalog.trainingsRepository
        trainings.startTraining(training("first", word).copy(strokeHintMode = HintMode.FullCharacter))
        val firstStarted = trainings.getInProgress()!!
        assertEquals(HintMode.FullCharacter, firstStarted.strokeHintMode)
        assertEquals("first", firstStarted.id)
        trainings.startTraining(training("second", word).copy(strokeHintMode = HintMode.NextStroke))
        assertEquals("second", trainings.observeInProgress().first()!!.id)
        val first = trainings.getTraining("first")!!
        assertEquals(TrainingStatus.Interrupted, first.status)
        assertEquals(HintMode.FullCharacter, first.strokeHintMode)
        assertNotNull(first.endedAt)
        assertEquals(2, first.tasks.single().symbols.size)
        assertEquals("Япония", first.tasks.single().translations.single().translation)
        assertEquals("Демо рецепт", first.recipeNames.single().name)
        val second = trainings.getTraining("second")!!
        assertEquals(HintMode.NextStroke, second.strokeHintMode)
    }

    @Test
    fun saveUpdatesProgressAndRejectsSecondInProgress() = storage { _, catalog ->
        catalog.load()
        val trainings = catalog.trainingsRepository
        trainings.startTraining(training("one", wordId = null))
        val updated =
            training(
                    "one",
                    wordId = null,
                    taskStatus = TrainingTaskStatus.InProgress,
                )
                .let { base ->
                    base.copy(
                        tasks =
                            base.tasks.map { task ->
                                task.copy(
                                    status = TrainingTaskStatus.InProgress,
                                    startedAt = 2_000L,
                                    symbols =
                                        task.symbols.mapIndexed { index, symbol ->
                                            if (index == 0) {
                                                symbol.copy(
                                                    status = TrainingSymbolStatus.Editing,
                                                    hintRequestCount = 2,
                                                    writingSampleJson = "[]",
                                                )
                                            } else symbol
                                        },
                                )
                            },
                    )
                }
        trainings.saveTraining(
            updated.copy(
                tasks =
                    updated.tasks.map { task ->
                        task.copy(readingHintUsed = true, status = TrainingTaskStatus.Assisted)
                    },
            ),
        )
        val loaded = trainings.getInProgress()!!
        assertEquals(2, loaded.tasks.single().symbols.first().hintRequestCount)
        assertTrue(loaded.tasks.single().readingHintUsed)
        assertEquals(TrainingTaskStatus.Assisted, loaded.tasks.single().status)
        assertEquals("[]", loaded.tasks.single().symbols.first().writingSampleJson)
        try {
            trainings.saveTraining(training("other", wordId = null))
            fail("Expected rejection of second in-progress via start-only path")
        } catch (_: IllegalArgumentException) {
            /* save requires existing id */
        }
        trainings.startTraining(training("other", wordId = null))
        assertEquals("other", trainings.getInProgress()!!.id)
        assertNull(trainings.getTraining("missing"))
    }

    @Test
    fun clearHistoryRemovesAllTrainings() = storage { _, catalog ->
        catalog.load()
        val trainings = catalog.trainingsRepository
        trainings.startTraining(training("one", wordId = null))
        trainings.startTraining(training("two", wordId = null))
        assertEquals("two", trainings.getInProgress()!!.id)
        assertNotNull(trainings.getTraining("one"))
        trainings.clearHistory()
        assertNull(trainings.getInProgress())
        assertNull(trainings.getTraining("one"))
        assertNull(trainings.getTraining("two"))
    }

    @Test
    fun observeHistoryAndDeleteOneTraining() = storage { _, catalog ->
        catalog.load()
        val trainings = catalog.trainingsRepository
        trainings.startTraining(training("live", wordId = null))
        trainings.saveTraining(
            training(
                "live",
                wordId = null,
                status = TrainingStatus.Completed,
                endedAt = 2_000L,
                taskStatus = TrainingTaskStatus.Correct,
            ),
        )
        trainings.startTraining(training("next", wordId = null))
        val history = trainings.observeHistory().first()
        assertEquals(listOf("next", "live"), history.map { it.id })
        assertEquals(TrainingStatus.InProgress, history.first().status)
        trainings.deleteTraining("next")
        assertNull(trainings.getInProgress())
        assertEquals(listOf("live"), trainings.observeHistory().first().map { it.id })
        trainings.deleteTraining("live")
        assertNull(trainings.getTraining("live"))
        assertTrue(trainings.observeHistory().first().isEmpty())
    }

    @Test
    fun guardsRejectInvalidStatusCombination() = storage { db, catalog ->
        catalog.load()
        val sql = db.openHelper.writableDatabase
        try {
            sql.execSQL(
                "INSERT INTO trainings(id,status,started_at,ended_at) VALUES('bad','in_progress',1,2)",
            )
            fail("Expected SQL rejection")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            /* CHECK via trigger */
        }
        try {
            sql.execSQL(
                "INSERT INTO trainings(id,status,started_at,ended_at) VALUES('bad2','completed',1,NULL)",
            )
            fail("Expected SQL rejection")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            /* CHECK via trigger */
        }
    }
}
