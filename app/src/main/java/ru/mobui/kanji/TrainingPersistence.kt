package ru.mobui.kanji

import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.mobui.kanji.domain.practice.Training
import ru.mobui.kanji.domain.practice.TrainingsRepository

/**
 * Persists training progress off the UI thread. Saves are serialized; stroke-level updates are
 * never requested from the host — only task boundaries and start/interrupt/complete.
 */
internal class TrainingPersistence(
    private val repository: TrainingsRepository,
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()

    suspend fun start(training: Training) {
        mutex.withLock { withContext(Dispatchers.IO) { repository.startTraining(training) } }
    }

    /** Awaited boundary save for MVP reliability (task complete / leave). */
    suspend fun save(training: Training) {
        mutex.withLock { withContext(Dispatchers.IO) { repository.saveTraining(training) } }
    }

    suspend fun clearHistory() {
        mutex.withLock { withContext(Dispatchers.IO) { repository.clearHistory() } }
    }

    fun saveAsync(training: Training) {
        scope.launch {
            runCatching { save(training) }
                .onFailure { error ->
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    logger.log(Level.SEVERE, "Failed to save training ${training.id}", error)
                }
        }
    }

    private companion object {
        val logger: Logger = Logger.getLogger("ru.mobui.kanji.TrainingPersistence")
    }
}
