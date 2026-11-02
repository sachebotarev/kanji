package ru.mobui.kanji.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSnapshot

/** Common loading/cancellation lifecycle only; each owner defines its own inputs and result. */
internal abstract class CatalogStateViewModel<I, C>(private val repository: CatalogRepository,
    private val compute: CoroutineDispatcher) : ViewModel() {
    private val mutable = MutableStateFlow<CatalogLoad<C>>(CatalogLoad.Loading)
    val state = mutable.asStateFlow()
    private var job: Job? = null
    protected abstract fun inputs(): Flow<I>
    protected abstract suspend fun content(snapshot: CatalogSnapshot, input: I): C

    fun retry() {
        job?.cancel()
        job = viewModelScope.launch {
            mutable.value = CatalogLoad.Loading
            try {
                val snapshot = repository.load()
                inputs().collectLatest { input ->
                    val next = withContext(compute) { content(snapshot, input) }
                    mutable.value = CatalogLoad.Ready(next)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.value = CatalogLoad.Error }
        }
    }
}
