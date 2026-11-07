package ru.mobui.kanji

import android.util.Log
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
        withContext(Dispatchers.IO) {
            mutex.withLock { repository.startTraining(training) }
        }
    }

    /** Awaited boundary save for MVP reliability (task complete / leave). */
    suspend fun save(training: Training) {
        withContext(Dispatchers.IO) {
            mutex.withLock { repository.saveTraining(training) }
        }
    }

    fun saveAsync(training: Training) {
        scope.launch {
            runCatching { save(training) }
                .onFailure { error ->
                    Log.e(TAG, "Failed to save training ${training.id}", error)
                }
        }
    }

    private companion object {
        const val TAG = "TrainingPersistence"
    }
}
