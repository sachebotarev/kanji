package ru.mobui.kanji

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mobui.kanji.domain.practice.CompletedTrainingTask
import ru.mobui.kanji.domain.practice.PracticeResult
import ru.mobui.kanji.domain.practice.AnswerStatus
import ru.mobui.kanji.domain.practice.TrainingsRepository
import ru.mobui.kanji.domain.practice.HintMode
import ru.mobui.kanji.domain.practice.TaskClueMode
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

class TrainingSessionViewModelTest {
    @Test
    fun newObserverSeesCommitAndStaleRestoreCannotReplaceIt() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val repository = Repository {
            entered.complete(Unit)
            release.await()
        }
        val model = TrainingSessionViewModel(TrainingPersistence(repository, this), this)
        val active = requireNotNull(resumeActiveTraining(fixture()))
        val restoreToken = model.restorationToken()
        model.activate(active)
        var acknowledgements = 0
        model.submit(answer(active)) { acknowledgements++ }
        entered.await()
        assertTrue(model.state.value.saving)
        // A new composition observes this same owner while disk IO is suspended.
        assertEquals(active, model.state.first().active)
        release.complete(Unit)
        val committed = withTimeout(5_000) { model.state.first { !it.saving } }
        assertEquals(TrainingStatus.Completed, committed.active!!.training.status)
        assertEquals(1, acknowledgements)
        assertFalse(model.restore(restoreToken, active))
        assertEquals(committed, model.state.value)
    }

    @Test
    fun failedWriteRemainsRetryableForNewObserver() = runBlocking {
        var writes = 0
        val repository = Repository { if (++writes == 1) throw java.io.IOException("disk") }
        val model = TrainingSessionViewModel(TrainingPersistence(repository, this), this)
        val active = requireNotNull(resumeActiveTraining(fixture()))
        model.activate(active)
        var acknowledgements = 0
        model.submit(answer(active)) { acknowledgements++ }
        withTimeout(5_000) { model.state.first { it.saveFailed } }
        assertEquals(0, acknowledgements)
        assertTrue(model.state.first().saveFailed)
        model.retry()
        model.retry()
        withTimeout(5_000) { model.state.first { it.active?.training?.status == TrainingStatus.Completed } }
        assertEquals(2, writes)
        assertEquals(1, acknowledgements)
    }

    @Test
    fun deepLinkWaitsForTheWordCommitAndClearsTheSession() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val repository = Repository { gate.await() }
        val model = TrainingSessionViewModel(TrainingPersistence(repository, this), this)
        val active = requireNotNull(resumeActiveTraining(fixture()))
        model.activate(active)
        model.requestHubLink(AppDeepLinks.SETTINGS)
        assertEquals(active, model.state.value.active)
        model.submit(answer(active)) {}
        assertTrue(model.state.value.saving)
        assertEquals(AppDeepLinks.SETTINGS, model.state.value.pendingHubLink)
        gate.complete(Unit)
        val ready = withTimeout(5_000) { model.state.first { it.active == null } }
        assertTrue(ready.suppressResume)
        assertEquals(AppDeepLinks.SETTINGS, ready.pendingHubLink)
    }

    @Test
    fun historyDeletionWaitsForAnAlreadyRequestedSave() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val events = java.util.Collections.synchronizedList(mutableListOf<String>())
        val repository = Repository(clear = { events.add("clear"); Unit }) {
            entered.complete(Unit)
            release.await()
            events.add("save")
        }
        val persistence = TrainingPersistence(repository, this)
        persistence.saveAsync(fixture())
        entered.await()
        val clearing = launch { persistence.clearHistory() }
        yield()
        assertTrue(events.isEmpty())
        release.complete(Unit)
        clearing.join()
        assertEquals(listOf("save", "clear"), events)
    }

    private fun answer(active: ActiveTraining) = PracticeResult(
        active.task.taskId, active.task.russianWord, AnswerStatus.Skipped, emptyList(), 1L,
    )

    private class Repository(private val clear: suspend () -> Unit = {}, private val save: suspend () -> Unit) : TrainingsRepository {
        override fun observeInProgress() = flowOf<Training?>(null)
        override fun observeCompletedTasks() = flowOf(emptyList<CompletedTrainingTask>())
        override fun observeHistory() = flowOf(emptyList<Training>())
        override suspend fun getInProgress(): Training? = null
        override suspend fun getTraining(id: String): Training? = null
        override suspend fun startTraining(training: Training) = Unit
        override suspend fun saveTraining(training: Training) = save()
        override suspend fun deleteTraining(id: String) = Unit
        override suspend fun clearHistory() = clear()
    }

    private fun fixture(): Training =
Training(
                id = "run",
                recipeNames = listOf(TrainingRecipeName("ru", "Луна")),
                clueMode = TaskClueMode.None,
                strokeHintMode = HintMode.FullCharacter,
                maxNextStrokeHints = 5,
                status = TrainingStatus.InProgress,
                startedAt = 1L,
                tasks =
                    listOf(
                        TrainingTask(
                            id = "word",
                            position = 0,
                            writtenForm = "日",
                            reading = "ひ",
                            translations = listOf(TrainingTaskTranslation("ru", "солнце")),
                            status = TrainingTaskStatus.Pending,
                            symbols =
                                listOf(
                                    TrainingSymbol(
                                        id = "s",
                                        position = 0,
                                        occurrenceId = "cell",
                                        role = TrainingSymbolRole.Practice,
                                        script = TrainingSymbolScript.Kanji,
                                        glyph = "日",
                                        kanjiKey = "u65e5",
                                        status = TrainingSymbolStatus.Empty,
                                    ),
                                ),
                        ),
                    ),
            )
}
