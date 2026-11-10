package ru.mobui.kanji.feature.dashboard.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.mobui.kanji.domain.practice.Training
import ru.mobui.kanji.domain.practice.TrainingsRepository

internal data class TrainingHistoryState(
    val trainings: List<Training> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: Boolean = false,
)

internal class TrainingHistoryViewModel(
    private val saved: SavedStateHandle,
    private val repository: TrainingsRepository,
    initialPeriod: TrainingHistoryPeriod = TrainingHistoryPeriod.All,
) : ViewModel() {
    private val mutable = MutableStateFlow(TrainingHistoryState())
    val state = mutable.asStateFlow()

    private val search = MutableStateFlow(saved.get<String>(QUERY_KEY).orEmpty())
    val query = search.asStateFlow()

    private val period =
        MutableStateFlow(
            saved.get<String>(PERIOD_KEY)?.let(TrainingHistoryPeriod::fromKey) ?: initialPeriod,
        )
    val periodState = period.asStateFlow()

    private var observation: Job? = null

    init {
        saved[PERIOD_KEY] = period.value.key
        retry()
    }

    fun query(value: String) {
        search.value = value
        saved[QUERY_KEY] = value
    }

    fun setPeriod(value: TrainingHistoryPeriod) {
        period.value = value
        saved[PERIOD_KEY] = value.key
    }

    fun clearPeriod() = setPeriod(TrainingHistoryPeriod.All)

    @Suppress("TooGenericExceptionCaught")
    fun retry() {
        observation?.cancel()
        mutable.update { it.copy(loading = true, error = false) }
        observation =
            viewModelScope.launch {
                try {
                    repository.observeHistory().collect { list ->
                        mutable.update {
                            it.copy(trainings = list, loading = false, error = false)
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    logger.log(Level.SEVERE, "Failed to observe history", error)
                    mutable.update { it.copy(loading = false, error = true) }
                }
            }
    }

    @Suppress("TooGenericExceptionCaught")
    fun delete(id: String) {
        if (mutable.value.busy || mutable.value.loading) return
        mutable.update { it.copy(busy = true, error = false) }
        viewModelScope.launch {
            try {
                repository.deleteTraining(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.log(Level.SEVERE, "Failed to delete training $id", error)
                mutable.update { it.copy(error = true) }
            } finally {
                mutable.update { it.copy(busy = false) }
            }
        }
    }

    suspend fun training(id: String): Training? = repository.getTraining(id)

    private companion object {
        val logger: Logger = Logger.getLogger("ru.mobui.kanji.TrainingHistory")
        const val QUERY_KEY = "history_query"
        const val PERIOD_KEY = "history_period"
    }
}
