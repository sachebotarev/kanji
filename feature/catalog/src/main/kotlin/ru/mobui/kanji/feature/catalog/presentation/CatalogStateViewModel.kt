package ru.mobui.kanji.feature.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSnapshot

/** Common loading/cancellation lifecycle only; each owner defines its own inputs and result. */
internal abstract class CatalogStateViewModel<I, C>(
    private val repository: CatalogRepository,
    private val compute: CoroutineDispatcher,
    initialLanguage: String,
) : ViewModel() {
    private val mutable = MutableStateFlow<CatalogLoad<C>>(CatalogLoad.Loading)
    val state = mutable.asStateFlow()
    private val context = MutableStateFlow(initialLanguage to 0L)
    private var job: Job? = null

    protected abstract fun inputs(): Flow<I>

    protected abstract suspend fun content(snapshot: CatalogSnapshot, input: I, language: String): C

    fun updateContext(language: String, revision: Long = 0L) {
        context.value = language to revision
    }

    @Suppress("TooGenericExceptionCaught")
    fun retry() {
        job?.cancel()
        job = viewModelScope.launch {
            mutable.value = CatalogLoad.Loading
            context.collectLatest { (language, _) ->
                try {
                    val snapshot = repository.load()
                    inputs().collectLatest { input ->
                        val next = withContext(compute) { content(snapshot, input, language) }
                        mutable.value = CatalogLoad.Ready(next)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    logger.log(Level.SEVERE, "Failed to load catalog", error)
                    mutable.value = CatalogLoad.Error
                }
            }
        }
    }

    private companion object {
        val logger: Logger = Logger.getLogger("ru.mobui.kanji.Catalog")
    }
}
