package ru.mobui.kanji

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal enum class JapaneseModelStatus { Loading, Missing, Ready }

/** Application-owned download; screen navigation never cancels or duplicates it. */
internal class JapaneseModelLoader(
    private val scope: CoroutineScope,
    private val isDownloaded: suspend () -> Boolean,
    private val download: suspend () -> Boolean,
) {
    private val mutableStatus = MutableStateFlow(JapaneseModelStatus.Missing)
    val status = mutableStatus.asStateFlow()
    private val mutableError = MutableStateFlow(false)
    val error = mutableError.asStateFlow()

    fun load() {
        if (mutableStatus.value != JapaneseModelStatus.Missing) return
        mutableStatus.value = JapaneseModelStatus.Loading
        mutableError.value = false
        scope.launch {
            try {
                val ready = isDownloaded() || download()
                mutableStatus.value = if (ready) JapaneseModelStatus.Ready else JapaneseModelStatus.Missing
                mutableError.value = !ready
            } catch (cancelled: CancellationException) {
                mutableStatus.value = JapaneseModelStatus.Missing
                throw cancelled
            } catch (_: Exception) {
                mutableStatus.value = JapaneseModelStatus.Missing
                mutableError.value = true
            }
        }
    }

    fun dismissError() { mutableError.value = false }
}
