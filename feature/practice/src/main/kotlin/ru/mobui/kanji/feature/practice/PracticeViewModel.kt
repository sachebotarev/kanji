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
import kotlinx.coroutines.withTimeoutOrNull
import ru.mobui.kanji.core.writingpad.PadEvent
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.practice.CheckCompensation
import ru.mobui.kanji.domain.practice.CheckFailure
import ru.mobui.kanji.domain.practice.CheckReport
import ru.mobui.kanji.domain.practice.CheckRequest
import ru.mobui.kanji.domain.practice.CheckVerdict
import ru.mobui.kanji.domain.practice.HandwritingTraining
import ru.mobui.kanji.domain.practice.HintMode
import ru.mobui.kanji.domain.practice.HintOverlayStyle
import ru.mobui.kanji.domain.practice.HintPlayback
import ru.mobui.kanji.domain.practice.HintPresentation
import ru.mobui.kanji.domain.practice.HintRequest
import ru.mobui.kanji.domain.practice.KanjiChecker
import ru.mobui.kanji.domain.practice.KanjiId
import ru.mobui.kanji.domain.practice.KanjiReferenceSource
import ru.mobui.kanji.domain.practice.EntryStatus
import ru.mobui.kanji.domain.practice.PracticeCell
import ru.mobui.kanji.domain.practice.PracticeResult
import ru.mobui.kanji.domain.practice.PracticeState
import ru.mobui.kanji.domain.practice.PracticeTask
import ru.mobui.kanji.domain.practice.TraceReferenceHint
import ru.mobui.kanji.domain.practice.addStroke
import ru.mobui.kanji.domain.practice.clear
import ru.mobui.kanji.domain.practice.exhaustStrokeHints
import ru.mobui.kanji.domain.practice.hintAlgorithmFor
import ru.mobui.kanji.domain.practice.nextUnsatisfiedUserIndex
import ru.mobui.kanji.domain.practice.recordReadingHint
import ru.mobui.kanji.domain.practice.recordTranslationHint
import ru.mobui.kanji.domain.practice.recordStrokeHint
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
    val hintMode: HintMode = HintMode.FullCharacter,
    val maxNextStrokeHints: Int = 3,
    /** Word-level reading spoiler; once revealed it stays open for this task. */
    val readingRevealed: Boolean = false,
    /** Word-level translation spoiler; once revealed it stays open for this task. */
    val translationRevealed: Boolean = true,
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

    /** Tap on the spoiled reading; counts as one hint for the selected cell. */
    data object RevealReading : PracticeAction

    /** Tap on the spoiled translation; counts as one hint for the task. */
    data object RevealTranslation : PracticeAction

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
    private val hintMode: () -> HintMode = { HintMode.FullCharacter },
    private val maxNextStrokeHints: () -> Int = { DEFAULT_MAX_NEXT_STROKE_HINTS },
    private val fullHintDisplayMillis: () -> Int = { DEFAULT_FULL_HINT_DISPLAY_MILLIS },
    private val autoAdvanceOnSuccess: () -> Boolean = { false },
    private val showCheckDiagnostics: Boolean = false,
) : ViewModel() {
    private var training = HandwritingTraining(task)
    private val mutableState =
        MutableStateFlow(
            PracticeUiState(
                training.state,
                readingRevealed = task.clueMode != ru.mobui.kanji.domain.practice.TaskClueMode.Reading,
                translationRevealed = task.clueMode != ru.mobui.kanji.domain.practice.TaskClueMode.Translation,
                debugEnabled = showCheckDiagnostics,
            ),
        )
    val uiState = mutableState.asStateFlow()
    private val mutablePreview = MutableStateFlow<PadEvent.GesturePreview?>(null)
    val preview = mutablePreview.asStateFlow()
    private var ticker: Job? = null
    private var lastTick: Long? = null
    /** Last pad pulse while a gesture is active; used to clear a stuck gesture for MVP demos. */
    private var lastGesturePulse: Long? = null
    private var fullHintHideJob: Job? = null
    private var screenActive = false
    private var resultDelivered = false
    private val coordinator = CheckCoordinator()

    init {
        loadTaskReferences(task, references, viewModelScope) { id, drawing ->
            accountTime()
            training.setReference(id, drawing)
            publish()
        }
    }

    fun setActive(active: Boolean) {
        if (active == screenActive && (!active || ticker != null)) return
        if (!active) {
            accountTime()
            // Stop UI ticks, not elapsed wall time: account for the background gap on resume.
            ticker?.cancel()
            ticker = null
            screenActive = false
            cancelCheck()
            mutablePreview.value = null
            lastGesturePulse = null
            training.setGestureActive(false)
        } else {
            screenActive = true
            accountTime()
            lastTick = clock()
            ticker =
                viewModelScope.launch {
                    while (isActive) {
                        delay(TIMER_TICK_MILLIS)
                        accountTime()
                        clearStaleGesture()
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
        lastGesturePulse = null
        resultDelivered = false
        coordinator.clear()
        training = HandwritingTraining(task)
        mutableState.value =
            PracticeUiState(
                training.state,
                hintMode = hintMode(),
                maxNextStrokeHints = maxNextStrokeHints(),
                debugEnabled = showCheckDiagnostics,
            )
        mutablePreview.value = null
        loadTaskReferences(task, references, viewModelScope) { id, drawing ->
            accountTime()
            training.setReference(id, drawing)
            publish()
        }
        if (screenActive) {
            setActive(true)
        }
    }

    fun onCheckerSettingsChanged() {
        if (training.state.gestureActive || training.state.result != null || training.state.timedOut)
            return
        cancelCheck()
        coordinator.clear()
        training.invalidateChecks()
        publish()
        scheduleCheck()
    }

    fun onAction(action: PracticeAction) {
        accountTime()
        if (training.state.result != null) {
            publish()
            return
        }
        // After the deadline: review cells (Select) or finish (Submit); edits stay frozen.
        if (
            training.state.timedOut &&
                action !is PracticeAction.Submit &&
                action !is PracticeAction.Select
        ) {
            publish()
            return
        }
        // UI actions must not commit or change cells under an unfinished gesture.
        if (training.state.gestureActive) return
        when (action) {
            is PracticeAction.Select -> handleSelectAction(action)
            PracticeAction.ToggleCompensatedDrawing -> handleToggleCompensatedDrawing()
            PracticeAction.ToggleHint -> handleToggleHint()
            PracticeAction.RevealReading -> handleRevealReading()
            PracticeAction.RevealTranslation -> handleRevealTranslation()
            PracticeAction.Undo -> {
                cancelCheck()
                training.undo()
            }
            PracticeAction.Clear -> {
                cancelCheck()
                training.clear()
            }
            PracticeAction.Submit -> training.submit()
            PracticeAction.Skip -> training.skip()
        }
        publish()
        scheduleCheck()
    }

    private fun handleSelectAction(action: PracticeAction.Select) {
        val previous = training.state.selectedIndex
        training.select(action.index)
        if (training.state.selectedIndex != previous) {
            cancelFullHintHide()
            mutableState.value =
                mutableState.value.copy(hint = reviewHint(training.state.selected))
        }
    }

    private fun handleToggleCompensatedDrawing() {
        if (!showCheckDiagnostics || mutableState.value.debugReport?.compensatedDrawing == null) return
        mutableState.value =
            mutableState.value.copy(
                showCompensatedDrawing = !mutableState.value.showCompensatedDrawing,
            )
    }

    private fun handleRevealReading() {
        if (mutableState.value.readingRevealed || training.state.task.reading.isBlank()) return
        training.recordReadingHint()
        mutableState.value = mutableState.value.copy(readingRevealed = true)
    }

    private fun handleRevealTranslation() {
        if (mutableState.value.translationRevealed || training.state.task.russianWord.isBlank()) return
        training.recordTranslationHint()
        mutableState.value = mutableState.value.copy(translationRevealed = true)
    }

    private fun handleToggleHint() {
        val current = mutableState.value.hint
        val mode = hintMode()
        if (current != null) {
            dismissOrRenewHint(current, mode)
        } else {
            activateHint(mode)
        }
    }

    private fun dismissOrRenewHint(current: HintPresentation, mode: HintMode) {
        if (current.style == HintOverlayStyle.Exhausted) return
        // Full-character guide is momentary: retap only restarts the hide timer.
        if (mode == HintMode.FullCharacter && current.style == HintOverlayStyle.Guide) {
            scheduleFullHintHide()
            return
        }
        cancelFullHintHide()
        mutableState.value = mutableState.value.copy(hint = null)
    }

    private fun activateHint(mode: HintMode) {
        val cell = training.state.selected ?: return
        val reference = cell.reference ?: return
        if (cell.strokeHintsExhausted) {
            mutableState.value = mutableState.value.copy(hint = exhaustedHint(reference))
            return
        }
        // Already correct: pale full reference only, never counts toward the budget.
        if (cell.status == EntryStatus.Correct) {
            val hint = TraceReferenceHint().create(HintRequest(cell.sample, reference)) ?: return
            mutableState.value = mutableState.value.copy(hint = hint)
            scheduleFullHintHide()
            return
        }
        // Guide unavailable (e.g. every stroke already drawn in NextStroke).
        if (mode == HintMode.NextStroke && hintAlgorithmFor(mode).create(HintRequest(cell.sample, reference)) == null) {
            return
        }
        cancelCheck()
        val count = training.recordStrokeHint()
        if (count >= maxNextStrokeHints()) {
            revealHintExhausted(cell.task.occurrenceId, reference)
        } else {
            val hint = hintAlgorithmFor(mode).create(HintRequest(cell.sample, reference)) ?: return
            mutableState.value = mutableState.value.copy(hint = hint)
            if (mode == HintMode.FullCharacter) scheduleFullHintHide()
        }
    }

    fun onPadEvent(event: PadEvent) {
        accountTime()
        if (event is PadEvent.HintPlaybackFinished) {
            if (mutableState.value.hint?.playback == HintPlayback.AnimateOnce) {
                mutableState.value = mutableState.value.copy(hint = null)
            }
            return
        }
        if (training.state.result != null || training.state.timedOut) {
            mutablePreview.value = null
            lastGesturePulse = null
            training.setGestureActive(false)
            publish()
            return
        }
        if (mutableState.value.hint?.blocksInput == true) return
        val cell = training.state.selected
        when (event) {
            PadEvent.HintPlaybackFinished -> Unit
            is PadEvent.GesturePreview -> handleGesturePreview(event, cell)
            is PadEvent.StrokeFinished -> handleStrokeFinished(event)
        }
    }

    private fun handleGesturePreview(event: PadEvent.GesturePreview, cell: PracticeCell?) {
        if (cell?.task?.occurrenceId != event.baseVersion.cellId) return
        if (
            event.stroke != null &&
                (cell.revision != event.baseVersion.revision || training.state.result != null)
        )
            return
        if (event.stroke == null && mutablePreview.value?.gestureId != event.gestureId) return
        val wasActive = training.state.gestureActive
        if (event.stroke != null && !wasActive) cancelCheck()
        mutablePreview.value = event.takeIf { it.stroke != null }
        lastGesturePulse = if (event.stroke != null) clock() else null
        training.setGestureActive(event.stroke != null)
        if (wasActive != training.state.gestureActive) publish()
        if (event.stroke == null) scheduleCheck()
    }

    private fun handleStrokeFinished(event: PadEvent.StrokeFinished) {
        cancelCheck()
        lastGesturePulse = null
        training.addStroke(event.baseVersion.cellId, event.baseVersion.revision, event.stroke)
        if (mutableState.value.hint?.playback == HintPlayback.AnimateOnce) {
            mutableState.value = mutableState.value.copy(hint = null)
        }
        publish()
        if (!training.state.gestureActive) scheduleCheck()
    }

    /** Synchronous delivery once per ViewModel lifetime; caller can deduplicate by taskId. */
    fun deliverResult(onResult: (PracticeResult) -> Unit) {
        val result = training.state.result ?: return
        if (resultDelivered) return
        resultDelivered = true
        onResult(result)
    }

    private fun canScheduleCheck(): Boolean =
        screenActive &&
            training.state.result == null &&
            !training.state.timedOut &&
            !training.state.gestureActive &&
            mutableState.value.hint?.blocksInput != true

    /** Coalesce strokes; a pause is a scheduling hint, never proof that a sign is finished. */
    private fun scheduleCheck() {
        cancelCheck()
        if (!canScheduleCheck()) return
        val requests =
            training.checkRequests().filter {
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
                            training,
                            request,
                            report,
                            unavailable,
                            incomplete,
                        )?.let { (u, i, n) ->
                            unavailable = u
                            incomplete = i
                            notice = n
                            // Show Correct (green flash) before moving selection.
                            publish(notice)
                            if (report.verdict == CheckVerdict.Correct) {
                                delay(CORRECT_HOLD_MILLIS)
                                if (generation != coordinator.generation) return@launch
                                advanceToNextUserCell(request.occurrenceId)
                                publish(notice)
                                maybeAutoSubmitOnSuccess()
                            }
                        }
                    }
                } finally {
                    if (generation == coordinator.generation) {
                        mutableState.value = mutableState.value.copy(isChecking = false)
                        publish(notice)
                        maybeAutoSubmitOnSuccess()
                    }
                }
            }
    }

    /** Successful word only; timeout/skip still wait for «Далее». */
    private fun maybeAutoSubmitOnSuccess() {
        if (!autoAdvanceOnSuccess()) return
        if (
            training.state.result != null ||
                training.state.timedOut ||
                training.state.gestureActive ||
                !training.state.isComplete
        )
            return
        training.submit()
        publish()
    }

    private fun cancelCheck() {
        coordinator.cancel()
        mutableState.value = mutableState.value.copy(isChecking = false)
    }

    /**
     * After every enabled checker accepts the current sign, move to the next unfinished user cell.
     * Prefills are skipped; if none remain, selection stays so «Далее» can finish the word.
     */
    private fun exhaustedHint(reference: ReferenceDrawing) =
        HintPresentation(
            overlay = reference,
            blocksInput = true,
            playback = HintPlayback.Static,
            style = HintOverlayStyle.Exhausted,
        )

    /** Brush reference in the pad after timeout or exhausted hints; Correct cells keep their ink. */
    private fun reviewHint(cell: PracticeCell?): HintPresentation? {
        val selected = cell ?: return null
        return when {
            selected.strokeHintsExhausted -> selected.reference?.let(::exhaustedHint)
            training.state.timedOut && selected.status == EntryStatus.Incorrect ->
                selected.reference?.let(::exhaustedHint)
            else -> null
        }
    }

    private fun cancelFullHintHide() {
        fullHintHideJob?.cancel()
        fullHintHideJob = null
    }

    private fun scheduleFullHintHide() {
        cancelFullHintHide()
        val hold = fullHintDisplayMillis().toLong().coerceAtLeast(1L)
        fullHintHideJob =
            viewModelScope.launch {
                delay(hold)
                val hint = mutableState.value.hint ?: return@launch
                if (
                    hint.style == HintOverlayStyle.Guide &&
                        hint.playback == HintPlayback.Static
                ) {
                    mutableState.value = mutableState.value.copy(hint = null)
                }
            }
    }

    private fun revealHintExhausted(occurrenceId: String, reference: ReferenceDrawing) {
        cancelFullHintHide()
        training.exhaustStrokeHints()
        mutablePreview.value = null
        mutableState.value = mutableState.value.copy(hint = exhaustedHint(reference))
        publish()
        viewModelScope.launch {
            delay(HINT_EXHAUST_HOLD_MILLIS)
            if (training.state.result != null) return@launch
            if (training.state.selected?.task?.occurrenceId != occurrenceId) return@launch
            accountTime()
            advanceToNextUserCell(occurrenceId)
            publish()
        }
    }

    private fun advanceToNextUserCell(occurrenceId: String) {
        val current = training.state.cells.indexOfFirst { it.task.occurrenceId == occurrenceId }
        if (current < 0 || training.state.selectedIndex != current) return
        val next = training.nextUnsatisfiedUserIndex(current) ?: return
        training.select(next)
        cancelFullHintHide()
        mutableState.value =
            mutableState.value.copy(hint = reviewHint(training.state.selected))
    }

    private fun accountTime() {
        val previous = lastTick ?: return
        val now = clock()
        lastTick = now
        training.advanceTime((now - previous).coerceAtLeast(0))
        if (training.state.result != null || training.state.timedOut) {
            mutablePreview.value = null
            lastGesturePulse = null
            training.setGestureActive(false)
            if (training.state.timedOut) {
                cancelFullHintHide()
                mutableState.value =
                    mutableState.value.copy(
                        hint = reviewHint(training.state.selected),
                        readingRevealed = true,
                        translationRevealed = true,
                    )
            }
        }
    }

    /** Clears a gesture that stopped receiving pad events (pointer cancel without finally). */
    private fun clearStaleGesture() {
        if (!training.state.gestureActive || training.state.result != null) return
        val pulse = lastGesturePulse ?: return
        if (clock() - pulse < GESTURE_STALE_MILLIS) return
        lastGesturePulse = null
        mutablePreview.value = null
        training.setGestureActive(false)
        scheduleCheck()
    }

    private fun publish(notice: PracticeNotice? = mutableState.value.notice) {
        val currentReport =
            training.state.selected?.let { cell ->
                coordinator.failures[cell.task.occurrenceId]
                    ?.takeIf {
                        it.first == cell.revision &&
                            !training.state.gestureActive &&
                            training.state.result == null
                    }
                    ?.second
            }
        val debugReport = currentReport.takeIf { showCheckDiagnostics }
        mutableState.value =
            mutableState.value.copy(
                practice = training.state,
                currentCheckReport = currentReport,
                canUndo = training.canUndo(),
                gestureActive = training.state.gestureActive,
                notice = notice,
                hint =
                    when {
                        training.state.result != null -> null
                        // Keep the brush reference while reviewing cells after the deadline.
                        training.state.timedOut -> reviewHint(training.state.selected)
                        else -> mutableState.value.hint
                    },
                hintMode = hintMode(),
                maxNextStrokeHints = maxNextStrokeHints(),
                readingRevealed = mutableState.value.readingRevealed,
                translationRevealed = mutableState.value.translationRevealed,
                debugReport = debugReport,
                showCompensatedDrawing =
                    mutableState.value.showCompensatedDrawing &&
                        debugReport?.compensatedDrawing != null &&
                        training.state.selected?.task?.occurrenceId ==
                            mutableState.value.practice.selected?.task?.occurrenceId &&
                        training.state.selected?.revision ==
                            mutableState.value.practice.selected?.revision,
                debugFailures = debugReport?.failures.orEmpty(),
                debugCompensation = debugReport?.compensation?.takeIf { it.isApplied },
            )
    }
}

private const val DEFAULT_FULL_HINT_DISPLAY_MILLIS = 500
private const val DEFAULT_MAX_NEXT_STROKE_HINTS = 3
private const val TIMER_TICK_MILLIS = 250L
private const val CHECK_DEBOUNCE_MILLIS = 350L
/** Hold on the accepted cell so the green flash is visible before advancing. */
private const val CORRECT_HOLD_MILLIS = 420L
/** Hold on the red exhausted reveal before moving to the next cell. */
private const val HINT_EXHAUST_HOLD_MILLIS = 520L
/** Whole-provider budget so «Далее» cannot stay disabled for an MVP demo. */
private const val CHECK_TIMEOUT_MILLIS = 8_000L
/** No pad pulses while gestureActive — treat as stuck input. */
private const val GESTURE_STALE_MILLIS = 5_000L

private suspend fun executeCheck(
    checker: KanjiChecker,
    request: CheckRequest,
): CheckReport =
    withTimeoutOrNull(CHECK_TIMEOUT_MILLIS) {
        try {
            checker.checkWithDetails(request)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            CheckReport(CheckVerdict.Unavailable)
        }
    } ?: CheckReport(CheckVerdict.Unavailable)

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
        training: HandwritingTraining,
        request: CheckRequest,
        report: CheckReport,
        unavailable: Boolean,
        incomplete: Boolean,
    ): Triple<Boolean, Boolean, PracticeNotice?>? {
        val verdict = report.verdict
        if (!training.applyCheck(request, verdict)) return null
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
