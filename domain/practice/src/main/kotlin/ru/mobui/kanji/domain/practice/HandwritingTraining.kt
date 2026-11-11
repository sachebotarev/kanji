package ru.mobui.kanji.domain.practice

import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.WritingSample

data class PracticeCell(
    val task: KanjiTask,
    val sample: WritingSample = WritingSample(),
    val status: EntryStatus =
        if (task.mode == EntryMode.Prefilled) EntryStatus.Prefilled else EntryStatus.Empty,
    val revision: Long = 0,
    val elapsedMs: Long = 0,
    val hintUsed: Boolean = false,
    /** Counted hint presses for this cell (FullCharacter and NextStroke share the budget). */
    val strokeHintCount: Int = 0,
    val shapeAcceptedWithPenalty: Boolean = false,
    /** NextStroke hint budget exhausted: cell stays incorrect with a full red reveal. */
    val strokeHintsExhausted: Boolean = false,
    val reference: ReferenceDrawing? = null,
    val referenceUnavailable: Boolean = false,
) {
    val isSatisfied: Boolean
        get() = status == EntryStatus.Correct || status == EntryStatus.Prefilled

    /** Whether this cell has a fixable penalty (such as soft shape acceptance) that can be improved by redrawing. */
    val hasFixablePenalty: Boolean
        get() = task.mode == EntryMode.User && shapeAcceptedWithPenalty

    /** Cell has a final Correct/Incorrect (exhausted hints)/Prefilled outcome (Next may unlock). */
    val isSettled: Boolean
        get() =
            status == EntryStatus.Correct ||
                (status == EntryStatus.Incorrect && strokeHintsExhausted) ||
                status == EntryStatus.Prefilled
}

data class PracticeState(
    val task: PracticeTask,
    val cells: List<PracticeCell>,
    val selectedIndex: Int?,
    val elapsedMs: Long = 0,
    val result: PracticeResult? = null,
    val gestureActive: Boolean = false,
    /** Reading spoiler opened once for this word; does not mark a kanji cell. */
    val readingHintUsed: Boolean = false,
    /** Translation spoiler opened once for this word; does not mark a kanji cell. */
    val translationHintUsed: Boolean = false,
    /**
     * Word deadline hit: unsettled practice cells are Incorrect, input frozen, «Далее» unlocks;
     * [result] is still null until submit.
     */
    val timedOut: Boolean = false,
) {
    /** Counted assistance or soft shape acceptance reduces the word score. A zero limit alone does not. */
    val hasScorePenalty: Boolean
        get() = readingHintUsed || translationHintUsed || cells.any {
            it.task.mode == EntryMode.User && (it.strokeHintCount > 0 || it.shapeAcceptedWithPenalty)
        }

    /** Fixable penalty (such as soft shape acceptance) that the user can improve by redrawing. Hints are not fixable. */
    val hasFixablePenalty: Boolean
        get() = cells.any { it.hasFixablePenalty }

    /** Total count of hints used for this word/task: header clues (reading/translation) plus all stroke hints. */
    val totalHintCount: Int
        get() = (if (readingHintUsed) 1 else 0) +
            (if (translationHintUsed) 1 else 0) +
            cells.sumOf { it.strokeHintCount }

    /** Every writable cell accepted. */
    val isComplete: Boolean
        get() = cells.all { it.isSatisfied }

    /** Every cell is Correct, Incorrect, or Prefilled — «Далее» may finish the word. */
    val isResolved: Boolean
        get() = timedOut || cells.all { it.isSettled }

    val selected: PracticeCell?
        get() = selectedIndex?.let(cells::get)
}

/** Business rules and per-cell history. Called serially by the screen state owner. */
class HandwritingTraining(task: PracticeTask) {
    var state =
        PracticeState(
            task.copy(kanji = task.kanji.toList()),
            task.kanji.map { PracticeCell(it) },
            task.kanji.indexOfFirst { it.mode == EntryMode.User }.takeIf { it >= 0 },
        )
        internal set

    internal val history = task.kanji.associate { it.occurrenceId to ArrayDeque<PracticeCell>() }

    fun canUndo(): Boolean =
        state.selected?.let { history.getValue(it.task.occurrenceId).isNotEmpty() } == true

    /** Usage is cumulative metadata, not an edit and not a successful answer. */

    /**
     * Settings affect verdicts, not ink/history. Advancing revisions rejects old in-flight checks.
     */
    fun invalidateChecks() {
        if (state.result != null || state.gestureActive) return
        history.values.forEach { entries ->
            val refreshed = entries.map {
                it.copy(
                    shapeAcceptedWithPenalty = false,
                    status =
                        if (it.sample.strokeCount == 0) EntryStatus.Empty else EntryStatus.Editing,
                )
            }
            entries.clear()
            entries.addAll(refreshed)
        }
        state.cells.forEachIndexed { index, cell ->
            if (
                cell.task.mode == EntryMode.User &&
                    cell.sample.strokeCount > 0 &&
                    !cell.strokeHintsExhausted
            ) {
                replace(
                    index,
                    cell.copy(status = EntryStatus.Editing, revision = cell.revision + 1, shapeAcceptedWithPenalty = false),
                )
            }
        }
    }

    fun checkRequests(): List<CheckRequest> =
        if (state.result != null) emptyList()
        else
            state.cells
                .filter {
                    !it.strokeHintsExhausted &&
                        (it.status == EntryStatus.Editing || it.status == EntryStatus.Incorrect)
                }
                .map {
                    CheckRequest(
                        state.task.taskId,
                        it.task.occurrenceId,
                        it.task.kanjiId,
                        it.revision,
                        it.sample,
                    )
                }

    fun applyCheck(request: CheckRequest, verdict: CheckVerdict, shapeAcceptedWithPenalty: Boolean = false): Boolean {
        if (state.result != null || request.taskId != state.task.taskId) return false
        val index = state.cells.indexOfFirst { it.task.occurrenceId == request.occurrenceId }
        val cell = state.cells.getOrNull(index) ?: return false
        if (cell.revision != request.revision || cell.sample !== request.sample) return false
        if (cell.strokeHintsExhausted) return false
        replace(
            index,
            cell.copy(
                shapeAcceptedWithPenalty = verdict == CheckVerdict.Correct && shapeAcceptedWithPenalty,
                status =
                    when (verdict) {
                        CheckVerdict.Correct -> EntryStatus.Correct
                        CheckVerdict.Incorrect -> EntryStatus.Incorrect
                        CheckVerdict.Incomplete,
                        CheckVerdict.Unavailable -> EntryStatus.Editing
                    },
            ),
        )
        return true
    }

    /**
     * Elapsed monotonic time is supplied, including time in the background. Clamp to the deadline,
     * including the selected cell.
     */
    fun advanceTime(deltaMs: Long) {
        require(deltaMs >= 0)
        if (
            state.result != null ||
                state.timedOut ||
                (state.isResolved && !state.gestureActive)
        )
            return
        val remaining = state.task.timeLimitMs?.minus(state.elapsedMs) ?: Long.MAX_VALUE
        val delta = minOf(deltaMs, remaining)
        val selectedIndex = state.selectedIndex
        state =
            state.copy(
                elapsedMs = state.elapsedMs + delta,
                cells =
                    state.cells.mapIndexed { index, cell ->
                        if (index == selectedIndex) cell.copy(elapsedMs = cell.elapsedMs + delta)
                        else cell
                    },
            )
        if (state.task.timeLimitMs != null && state.elapsedMs >= state.task.timeLimitMs!!) {
            settleTimedOut()
        }
    }

    /**
     * Freezes the word at the deadline: Empty/Editing practice cells become Incorrect so «Далее»
     * unlocks; Correct/Incorrect/Prefilled stay. Does not mark text spoilers as used.
     * Does not emit [PracticeResult] until [submit].
     */
    private fun settleTimedOut() {
        if (state.timedOut || state.result != null) return
        state =
            state.copy(
                timedOut = true,
                gestureActive = false,
                cells =
                    state.cells.map { cell ->
                        if (cell.task.mode != EntryMode.User || cell.isSettled) cell
                        else cell.copy(status = EntryStatus.Incorrect)
                    },
            )
    }

    fun submit() {
        if (state.gestureActive || state.result != null || !state.isResolved) return
        when {
            state.timedOut -> finish(AnswerStatus.TimedOut)
            state.isComplete -> {
                val assisted =
                    state.readingHintUsed ||
                        state.translationHintUsed ||
                        state.cells.any {
                            it.task.mode == EntryMode.User && (it.strokeHintCount > 0 || it.shapeAcceptedWithPenalty)
                        }
                finish(if (assisted) AnswerStatus.Assisted else AnswerStatus.Correct)
            }
            else -> finish(AnswerStatus.Skipped)
        }
    }

    fun skip() = finish(if (state.timedOut) AnswerStatus.TimedOut else AnswerStatus.Skipped)

    private fun finish(status: AnswerStatus) {
        if (state.result != null) return
        state =
            state.copy(
                result =
                    PracticeResult(
                        state.task.taskId,
                        state.task.russianWord,
                        status,
                        state.cells.map {
                            KanjiResult(
                                it.task.occurrenceId,
                                it.task.kanjiId,
                                it.task.mode,
                                it.status,
                                it.elapsedMs,
                                it.sample,
                                it.hintUsed || it.strokeHintCount > 0,
                                hintRequestCount = it.strokeHintCount,
                                shapeAcceptedWithPenalty = it.shapeAcceptedWithPenalty,
                            )
                        },
                        state.elapsedMs,
                        readingHintUsed = state.readingHintUsed,
                        translationHintUsed = state.translationHintUsed,
                    ),
            )
    }

    internal fun edit(transform: (PracticeCell) -> PracticeCell) {
        val index = state.selectedIndex ?: return
        if (
            state.result != null ||
                state.timedOut ||
                state.cells[index].task.mode != EntryMode.User
        )
            return
        val current = state.cells[index]
        val entries = history.getValue(current.task.occurrenceId)
        entries.addLast(current)
        while (entries.size > MAX_HISTORY_ENTRIES) entries.removeFirst()
        replace(index, transform(current).copy(revision = current.revision + 1, shapeAcceptedWithPenalty = false))
    }

    internal fun replace(index: Int, cell: PracticeCell) {
        state =
            state.copy(cells = state.cells.mapIndexed { i, old -> if (i == index) cell else old })
    }
}

private const val MAX_HISTORY_ENTRIES = 30
