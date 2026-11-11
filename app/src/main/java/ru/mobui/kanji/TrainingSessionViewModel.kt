package ru.mobui.kanji

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.mobui.kanji.domain.practice.PracticeResult

internal data class TrainingSessionState(
    val active: ActiveTraining? = null,
    val pendingHubLink: String? = null,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    val suppressResume: Boolean = false,
)

/** Activity-owned state: boundary writes never address a disposed composition. */
internal class TrainingSessionViewModel(
    private val persistence: TrainingPersistence,
    private val scope: CoroutineScope,
) : ViewModel() {
    private val mutable = MutableStateFlow(TrainingSessionState())
    val state = mutable.asStateFlow()
    private var generation = 0L
    private var pending: PendingAnswer? = null

    fun activate(active: ActiveTraining?) {
        generation++
        pending = null
        mutable.value = TrainingSessionState(active = active)
    }

    fun leave() {
        generation++
        pending = null
        mutable.value = TrainingSessionState(suppressResume = true, pendingHubLink = mutable.value.pendingHubLink)
    }

    fun requestHubLink(uri: String) {
        if (mutable.value.active?.training?.status == ru.mobui.kanji.domain.practice.TrainingStatus.InProgress) {
            mutable.value = mutable.value.copy(pendingHubLink = uri)
        } else {
            leave()
            mutable.value = mutable.value.copy(pendingHubLink = uri)
        }
    }

    fun cancelHubLink() { mutable.value = mutable.value.copy(pendingHubLink = null) }

    fun restorationToken(): Long = generation

    fun restore(token: Long, active: ActiveTraining?): Boolean {
        if (token != generation || mutable.value.active != null || mutable.value.suppressResume) return false
        if (active != null) activate(active)
        return true
    }

    fun submit(result: PracticeResult, acknowledge: () -> Unit) {
        if (pending != null || mutable.value.saving) return
        val active = mutable.value.active ?: return
        if (active.task.taskId != result.taskId ||
            active.training.status != ru.mobui.kanji.domain.practice.TrainingStatus.InProgress) return
        pending = PendingAnswer(active.withTaskResult(result, System.currentTimeMillis()), acknowledge)
        retry()
    }

    @Suppress("TooGenericExceptionCaught")
    fun retry() {
        val answer = pending ?: return
        if (mutable.value.saving) return
        val token = generation
        mutable.value = mutable.value.copy(saving = true, saveFailed = false)
        scope.launch {
            try {
                persistence.save(answer.updated.training)
                if (token != generation) return@launch
                answer.acknowledge()
                pending = null
                generation++
                val link = mutable.value.pendingHubLink
                mutable.value = if (link == null) TrainingSessionState(active = answer.updated.advance() ?: answer.updated)
                    else TrainingSessionState(suppressResume = true, pendingHubLink = link)
            } catch (cancelled: CancellationException) {
                if (token == generation) mutable.value = mutable.value.copy(saving = false, saveFailed = true)
                throw cancelled
            } catch (_: Exception) {
                if (token == generation) mutable.value = mutable.value.copy(saving = false, saveFailed = true)
            }
        }
    }

    private data class PendingAnswer(val updated: ActiveTraining, val acknowledge: () -> Unit)
}
