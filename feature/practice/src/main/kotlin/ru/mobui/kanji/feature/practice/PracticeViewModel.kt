package ru.mobui.kanji.feature.practice

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.mobui.kanji.core.writingpad.PadEvent
import ru.mobui.kanji.domain.practice.*

data class PracticeUiState(
    val practice: PracticeState,
    val canUndo: Boolean = false,
    val isChecking: Boolean = false,
    val gestureActive: Boolean = false,
    val notice: PracticeNotice? = null,
    val hint: HintPresentation? = null,
    val debugFailures: List<CheckFailure> = emptyList(),
    val debugCompensation: CheckCompensation? = null,
    val debugEnabled: Boolean = false,
    val debugReport: CheckReport? = null,
    val currentCheckReport: CheckReport? = null,
    val showCompensatedDrawing: Boolean = false,
)

enum class PracticeNotice { CheckerUnavailable, Incomplete }
sealed interface PracticeAction {
    data class Select(val index: Int) : PracticeAction
    data object ToggleCompensatedDrawing : PracticeAction
    data object ToggleHint : PracticeAction
    data object Undo : PracticeAction
    data object Clear : PracticeAction
    data object Submit : PracticeAction
    data object Skip : PracticeAction
}

/** Screen-scoped owner. Task and infrastructure are supplied by the caller. */
class PracticeViewModel(
    task: PracticeTask,
    private val references: KanjiReferenceSource,
    private val checker: KanjiChecker,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val hintAlgorithm: HintAlgorithm = TraceReferenceHint(),
    private val showCheckDiagnostics: Boolean = false,
) : ViewModel() {
    private val session = PracticeSession(task)
    private val mutableState = MutableStateFlow(PracticeUiState(session.state, debugEnabled = showCheckDiagnostics))
    val uiState = mutableState.asStateFlow()
    private val mutablePreview = MutableStateFlow<PadEvent.GesturePreview?>(null)
    val preview = mutablePreview.asStateFlow()
    private var ticker: Job? = null
    private var checking: Job? = null
    private var lastTick: Long? = null
    private var screenActive = false
    private var resultDelivered = false
    private var checkGeneration = 0L
    private val checkFailures = mutableMapOf<String, Pair<Long, CheckReport>>()
    private val checkedRevisions = mutableMapOf<String, Long>()

    init {
        task.kanji.map { it.kanjiId }.distinct().forEach { id ->
            viewModelScope.launch {
                val drawing = try { references.load(id) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
                accountTime()
                session.setReference(id, drawing)
                publish()
            }
        }
    }

    fun setActive(active: Boolean) {
        accountTime()
        ticker?.cancel()
        ticker = null
        screenActive = active
        // Keep the monotonic baseline while the screen is hidden. On resume,
        // accountTime includes the whole background interval, even after device sleep.
        if (active && lastTick == null) lastTick = clock()
        if (!active) {
            cancelCheck()
            mutablePreview.value = null
            session.setGestureActive(false)
        } else {
            ticker = viewModelScope.launch {
                while (isActive) { delay(250); accountTime(); publish() }
            }
        }
        publish()
        if (active) scheduleCheck()
    }

    fun onCheckerSettingsChanged() {
        if (session.state.gestureActive || session.state.result != null) return
        cancelCheck()
        checkedRevisions.clear()
        checkFailures.clear()
        session.invalidateChecks()
        publish()
        scheduleCheck()
    }

    fun onAction(action: PracticeAction) {
        accountTime()
        if (session.state.result != null) { publish(); return }
        // UI actions must not commit or change cells under an unfinished gesture.
        if (session.state.gestureActive) return
        when (action) {
            is PracticeAction.Select -> {
                val previous = session.state.selectedIndex
                session.select(action.index)
                if (session.state.selectedIndex != previous) mutableState.value = mutableState.value.copy(hint = null)
            }
            PracticeAction.ToggleCompensatedDrawing -> {
                if (!showCheckDiagnostics || mutableState.value.debugReport?.compensatedDrawing == null) return
                mutableState.value = mutableState.value.copy(showCompensatedDrawing = !mutableState.value.showCompensatedDrawing)
            }
            PracticeAction.ToggleHint -> {
                if (mutableState.value.hint != null) {
                    mutableState.value = mutableState.value.copy(hint = null)
                } else {
                    val cell = session.state.selected ?: return
                    val reference = cell.reference ?: return
                    val hint = hintAlgorithm.create(HintRequest(cell.sample, reference)) ?: return
                    session.recordHintUsed()
                    mutableState.value = mutableState.value.copy(hint = hint)
                }
            }
            PracticeAction.Undo -> { cancelCheck(); session.undo() }
            PracticeAction.Clear -> { cancelCheck(); session.clear() }
            PracticeAction.Submit -> session.submit()
            PracticeAction.Skip -> session.skip()
        }
        publish()
        scheduleCheck()
    }

    fun onPadEvent(event: PadEvent) {
        accountTime()
        if (mutableState.value.hint?.blocksInput == true) return
        val cell = session.state.selected
        when (event) {
            is PadEvent.GesturePreview -> {
                if (cell?.task?.occurrenceId != event.baseVersion.cellId) return
                if (event.stroke != null && (cell.revision != event.baseVersion.revision || session.state.result != null)) return
                if (event.stroke == null && mutablePreview.value?.gestureId != event.gestureId) return
                val wasActive = session.state.gestureActive
                if (event.stroke != null && !wasActive) cancelCheck()
                mutablePreview.value = event.takeIf { it.stroke != null }
                session.setGestureActive(event.stroke != null)
                if (wasActive != session.state.gestureActive) publish()
                if (event.stroke == null) scheduleCheck()
            }
            is PadEvent.StrokeFinished -> {
                cancelCheck()
                session.addStroke(event.baseVersion.cellId, event.baseVersion.revision, event.stroke)
                publish()
                if (!session.state.gestureActive) scheduleCheck()
            }
        }
    }

    /** Synchronous delivery once per ViewModel lifetime; caller can deduplicate by taskId. */
    fun deliverResult(onResult: (PracticeResult) -> Unit) {
        val result = session.state.result ?: return
        if (resultDelivered) return
        resultDelivered = true
        onResult(result)
    }

    /** Coalesce strokes; a pause is a scheduling hint, never proof that a sign is finished. */
    private fun scheduleCheck() {
        cancelCheck()
        if (!screenActive || session.state.result != null || session.state.gestureActive || mutableState.value.hint?.blocksInput == true) return
        val requests = session.checkRequests().filter {
            checkedRevisions[it.occurrenceId] != it.revision
        }
        if (requests.isEmpty()) return
        val generation = checkGeneration
        checking = viewModelScope.launch {
            var notice = mutableState.value.notice
            var unavailable = false
            var incomplete = false
            try {
                delay(350)
                mutableState.value = mutableState.value.copy(isChecking = true)
                for (request in requests) {
                    val report = try { checker.checkWithDetails(request) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { CheckReport(CheckVerdict.Unavailable) }
                    // A provider may finish despite cancellation; never apply that response.
                    if (generation != checkGeneration) return@launch
                    accountTime()
                    val verdict = report.verdict
                    if (session.applyCheck(request, verdict)) {
                        checkFailures[request.occurrenceId] = request.revision to report.copy(failures = report.failures.toList(), metrics = report.metrics.toMap())
                        if (verdict != CheckVerdict.Unavailable) checkedRevisions[request.occurrenceId] = request.revision
                        unavailable = unavailable || verdict == CheckVerdict.Unavailable
                        incomplete = incomplete || verdict == CheckVerdict.Incomplete
                        notice = when {
                            unavailable -> PracticeNotice.CheckerUnavailable
                            incomplete -> PracticeNotice.Incomplete
                            else -> null
                        }
                        publish(notice)
                    }
                }
            } finally {
                if (generation == checkGeneration) {
                    mutableState.value = mutableState.value.copy(isChecking = false)
                    publish(notice)
                }
            }
        }
    }

    private fun cancelCheck() {
        checkGeneration++
        checking?.cancel()
        checking = null
        mutableState.value = mutableState.value.copy(isChecking = false)
    }

    private fun accountTime() {
        val previous = lastTick ?: return
        val now = clock()
        lastTick = now
        session.advanceTime((now - previous).coerceAtLeast(0))
        if (session.state.result != null) {
            mutablePreview.value = null
            session.setGestureActive(false)
        }
    }

    private fun publish(notice: PracticeNotice? = mutableState.value.notice) {
        val currentReport = session.state.selected?.let { cell ->
            checkFailures[cell.task.occurrenceId]?.takeIf {
                it.first == cell.revision && !session.state.gestureActive && session.state.result == null
            }?.second
        }
        val debugReport = currentReport.takeIf { showCheckDiagnostics }
        mutableState.value = mutableState.value.copy(
            practice = session.state,
            currentCheckReport = currentReport,
            canUndo = session.canUndo(),
            gestureActive = session.state.gestureActive,
            notice = notice,
            hint = mutableState.value.hint.takeIf { session.state.result == null },
            debugReport = debugReport,
            showCompensatedDrawing = mutableState.value.showCompensatedDrawing && debugReport?.compensatedDrawing != null &&
                session.state.selected?.task?.occurrenceId == mutableState.value.practice.selected?.task?.occurrenceId &&
                session.state.selected?.revision == mutableState.value.practice.selected?.revision,
            debugFailures = debugReport?.failures.orEmpty(),
            debugCompensation = debugReport?.compensation?.takeIf { it.isApplied },
        )
    }
}
