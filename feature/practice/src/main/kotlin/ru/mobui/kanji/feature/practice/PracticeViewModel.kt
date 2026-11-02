package ru.mobui.kanji.feature.practice

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.mobui.kanji.core.writingpad.PadEvent
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.practice.CheckCompensation
import ru.mobui.kanji.domain.practice.CheckFailure
import ru.mobui.kanji.domain.practice.CheckReport
import ru.mobui.kanji.domain.practice.CheckRequest
import ru.mobui.kanji.domain.practice.CheckVerdict
import ru.mobui.kanji.domain.practice.HintAlgorithm
import ru.mobui.kanji.domain.practice.HintPresentation
import ru.mobui.kanji.domain.practice.HintRequest
import ru.mobui.kanji.domain.practice.KanjiChecker
import ru.mobui.kanji.domain.practice.KanjiId
import ru.mobui.kanji.domain.practice.KanjiReferenceSource
import ru.mobui.kanji.domain.practice.PracticeResult
import ru.mobui.kanji.domain.practice.PracticeSession
import ru.mobui.kanji.domain.practice.PracticeState
import ru.mobui.kanji.domain.practice.PracticeTask
import ru.mobui.kanji.domain.practice.TraceReferenceHint
import ru.mobui.kanji.domain.practice.addStroke
import ru.mobui.kanji.domain.practice.clear
import ru.mobui.kanji.domain.practice.recordHintUsed
import ru.mobui.kanji.domain.practice.select
import ru.mobui.kanji.domain.practice.setGestureActive
import ru.mobui.kanji.domain.practice.setReference
import ru.mobui.kanji.domain.practice.undo

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

enum class PracticeNotice {
    CheckerUnavailable,
    Incomplete,
}

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
    private val task: PracticeTask,
    private val references: KanjiReferenceSource,
    private val checker: KanjiChecker,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val hintAlgorithm: HintAlgorithm = TraceReferenceHint(),
    private val showCheckDiagnostics: Boolean = false,
) : ViewModel() {
    private var session = PracticeSession(task)
    private val mutableState =
        MutableStateFlow(PracticeUiState(session.state, debugEnabled = showCheckDiagnostics))
    val uiState = mutableState.asStateFlow()
    private val mutablePreview = MutableStateFlow<PadEvent.GesturePreview?>(null)
    val preview = mutablePreview.asStateFlow()
    private var ticker: Job? = null
    private var lastTick: Long? = null
    private var screenActive = false
    private var resultDelivered = false
    private val coordinator = CheckCoordinator()

    init {
        loadTaskReferences(task, references, viewModelScope) { id, drawing ->
            accountTime()
            session.setReference(id, drawing)
            publish()
        }
    }

    fun setActive(active: Boolean) {
        if (!active) {
            accountTime()
            lastTick = null
            ticker?.cancel()
            ticker = null
            screenActive = false
            cancelCheck()
            mutablePreview.value = null
            session.setGestureActive(false)
        } else {
            screenActive = true
            lastTick = clock()
            ticker =
                viewModelScope.launch {
                    while (isActive) {
                        delay(TIMER_TICK_MILLIS)
                        accountTime()
                        publish()
                    }
                }
            scheduleCheck()
        }
        publish()
    }

    fun restart() {
        cancelCheck()
        ticker?.cancel()
        ticker = null
        lastTick = null
        resultDelivered = false
        coordinator.clear()
        session = PracticeSession(task)
        mutableState.value =
            PracticeUiState(session.state, debugEnabled = showCheckDiagnostics)
        mutablePreview.value = null
        loadTaskReferences(task, references, viewModelScope) { id, drawing ->
            accountTime()
            session.setReference(id, drawing)
            publish()
        }
        if (screenActive) {
            setActive(true)
        }
    }

    fun onCheckerSettingsChanged() {
        if (session.state.gestureActive || session.state.result != null) return
        cancelCheck()
        coordinator.clear()
        session.invalidateChecks()
        publish()
        scheduleCheck()
    }

    fun onAction(action: PracticeAction) {
        accountTime()
        if (session.state.result != null) {
            publish()
            return
        }
        // UI actions must not commit or change cells under an unfinished gesture.
        if (session.state.gestureActive) return
        when (action) {
            is PracticeAction.Select -> {
                val previous = session.state.selectedIndex
                session.select(action.index)
                if (session.state.selectedIndex != previous)
                    mutableState.value = mutableState.value.copy(hint = null)
            }
            PracticeAction.ToggleCompensatedDrawing -> {
                if (
                    !showCheckDiagnostics ||
                        mutableState.value.debugReport?.compensatedDrawing == null
                )
                    return
                mutableState.value =
                    mutableState.value.copy(
                        showCompensatedDrawing = !mutableState.value.showCompensatedDrawing,
                    )
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
            PracticeAction.Undo -> {
                cancelCheck()
                session.undo()
            }
            PracticeAction.Clear -> {
                cancelCheck()
                session.clear()
            }
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
                if (
                    event.stroke != null &&
                        (cell.revision != event.baseVersion.revision ||
                            session.state.result != null)
                )
                    return
                if (event.stroke == null && mutablePreview.value?.gestureId != event.gestureId)
                    return
                val wasActive = session.state.gestureActive
                if (event.stroke != null && !wasActive) cancelCheck()
                mutablePreview.value = event.takeIf { it.stroke != null }
                session.setGestureActive(event.stroke != null)
                if (wasActive != session.state.gestureActive) publish()
                if (event.stroke == null) scheduleCheck()
            }
            is PadEvent.StrokeFinished -> {
                cancelCheck()
                session.addStroke(
                    event.baseVersion.cellId,
                    event.baseVersion.revision,
                    event.stroke,
                )
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
        if (
            !screenActive ||
                session.state.result != null ||
                session.state.gestureActive ||
                mutableState.value.hint?.blocksInput == true
        )
            return
        val requests =
            session.checkRequests().filter {
                coordinator.revisions[it.occurrenceId] != it.revision
            }
        if (requests.isEmpty()) return
        val generation = coordinator.generation
        coordinator.job =
            viewModelScope.launch {
                var notice = mutableState.value.notice
                var unavailable = false
                var incomplete = false
                try {
                    delay(CHECK_DEBOUNCE_MILLIS)
                    mutableState.value = mutableState.value.copy(isChecking = true)
                    for (request in requests) {
                        val report = executeCheck(checker, request)
                        // A provider may finish despite cancellation; never apply that response.
                        if (generation != coordinator.generation) return@launch
                        accountTime()
                        coordinator.applyResult(
                            session,
                            request,
                            report,
                            unavailable,
                            incomplete,
                        )?.let { (u, i, n) ->
                            unavailable = u
                            incomplete = i
                            notice = n
                            publish(notice)
                        }
                    }
                } finally {
                    if (generation == coordinator.generation) {
                        mutableState.value = mutableState.value.copy(isChecking = false)
                        publish(notice)
                    }
                }
            }
    }

    private fun cancelCheck() {
        coordinator.cancel()
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
        val currentReport =
            session.state.selected?.let { cell ->
                coordinator.failures[cell.task.occurrenceId]
                    ?.takeIf {
                        it.first == cell.revision &&
                            !session.state.gestureActive &&
                            session.state.result == null
                    }
                    ?.second
            }
        val debugReport = currentReport.takeIf { showCheckDiagnostics }
        mutableState.value =
            mutableState.value.copy(
                practice = session.state,
                currentCheckReport = currentReport,
                canUndo = session.canUndo(),
                gestureActive = session.state.gestureActive,
                notice = notice,
                hint = mutableState.value.hint.takeIf { session.state.result == null },
                debugReport = debugReport,
                showCompensatedDrawing =
                    mutableState.value.showCompensatedDrawing &&
                        debugReport?.compensatedDrawing != null &&
                        session.state.selected?.task?.occurrenceId ==
                            mutableState.value.practice.selected?.task?.occurrenceId &&
                        session.state.selected?.revision ==
                            mutableState.value.practice.selected?.revision,
                debugFailures = debugReport?.failures.orEmpty(),
                debugCompensation = debugReport?.compensation?.takeIf { it.isApplied },
            )
    }
}

private const val TIMER_TICK_MILLIS = 250L
private const val CHECK_DEBOUNCE_MILLIS = 350L

private suspend fun executeCheck(
    checker: KanjiChecker,
    request: CheckRequest,
): CheckReport =
    try {
        checker.checkWithDetails(request)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        CheckReport(CheckVerdict.Unavailable)
    }

private fun loadTaskReferences(
    task: PracticeTask,
    references: KanjiReferenceSource,
    scope: CoroutineScope,
    onLoaded: (KanjiId, ReferenceDrawing?) -> Unit,
) {
    task.kanji
        .map { it.kanjiId }
        .distinct()
        .forEach { id ->
            scope.launch {
                val drawing =
                    try {
                        references.load(id)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                onLoaded(id, drawing)
            }
        }
}

private class CheckCoordinator {
    var generation = 0L
    var job: Job? = null
    val failures = mutableMapOf<String, Pair<Long, CheckReport>>()
    val revisions = mutableMapOf<String, Long>()

    fun cancel() {
        generation++
        job?.cancel()
        job = null
    }

    fun clear() {
        failures.clear()
        revisions.clear()
    }

    fun applyResult(
        session: PracticeSession,
        request: CheckRequest,
        report: CheckReport,
        unavailable: Boolean,
        incomplete: Boolean,
    ): Triple<Boolean, Boolean, PracticeNotice?>? {
        val verdict = report.verdict
        if (!session.applyCheck(request, verdict)) return null
        failures[request.occurrenceId] =
            request.revision to
                report.copy(
                    failures = report.failures.toList(),
                    metrics = report.metrics.toMap(),
                )
        if (verdict != CheckVerdict.Unavailable) {
            revisions[request.occurrenceId] = request.revision
        }
        val isUnavailable = unavailable || verdict == CheckVerdict.Unavailable
        val isIncomplete = incomplete || verdict == CheckVerdict.Incomplete
        val notice =
            when {
                isUnavailable -> PracticeNotice.CheckerUnavailable
                isIncomplete -> PracticeNotice.Incomplete
                else -> null
            }
        return Triple(isUnavailable, isIncomplete, notice)
    }
}
