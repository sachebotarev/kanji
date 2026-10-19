package ru.mobui.kanji.domain.practice

import ru.mobui.kanji.domain.handwriting.InkStroke
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.WritingSample

data class PracticeCell(
    val task: KanjiTask,
    val sample: WritingSample = WritingSample(),
    val status: EntryStatus = if (task.mode == EntryMode.Prefilled) EntryStatus.Prefilled else EntryStatus.Empty,
    val revision: Long = 0,
    val elapsedMs: Long = 0,
    val hintUsed: Boolean = false,
    val reference: ReferenceDrawing? = null,
    val referenceUnavailable: Boolean = false,
) {
    val isSatisfied: Boolean get() = status == EntryStatus.Correct ||
        (status == EntryStatus.Prefilled && reference != null)
}

data class PracticeState(
    val task: PracticeTask,
    val cells: List<PracticeCell>,
    val selectedIndex: Int?,
    val elapsedMs: Long = 0,
    val result: PracticeResult? = null,
    val gestureActive: Boolean = false,
) {
    val isComplete: Boolean get() = cells.all { it.isSatisfied }
    val selected: PracticeCell? get() = selectedIndex?.let(cells::get)
}

/** Business rules and per-cell history. Called serially by the screen state owner. */
class PracticeSession(task: PracticeTask) {
    var state = PracticeState(
        task.copy(kanji = task.kanji.toList()),
        task.kanji.map { PracticeCell(it) },
        task.kanji.indexOfFirst { it.mode == EntryMode.User }.takeIf { it >= 0 },
    )
        private set
    private val history = task.kanji.associate { it.occurrenceId to ArrayDeque<PracticeCell>() }

    fun setGestureActive(active: Boolean) { state = state.copy(gestureActive = active && state.result == null) }

    fun canUndo(): Boolean = state.selected?.let { history.getValue(it.task.occurrenceId).isNotEmpty() } == true

    fun select(index: Int) {
        if (state.result == null && index in state.cells.indices && state.cells[index].task.mode == EntryMode.User) {
            state = state.copy(selectedIndex = index)
        }
    }

    fun setReference(id: KanjiId, drawing: ReferenceDrawing?) {
        state = state.copy(cells = state.cells.map {
            if (it.task.kanjiId == id) it.copy(reference = drawing, referenceUnavailable = drawing == null) else it
        })
    }

    fun addStroke(occurrenceId: String, revision: Long, stroke: InkStroke) {
        val cell = state.selected ?: return
        if (state.result != null || cell.task.occurrenceId != occurrenceId || cell.revision != revision) return
        edit {
            val old = it.sample.strokes
            it.copy(sample = WritingSample(old + stroke), status = EntryStatus.Editing)
        }
    }

    fun clear() {
        if (state.selected?.status == EntryStatus.Empty) return
        edit { it.copy(sample = WritingSample(), status = EntryStatus.Empty) }
    }

    /** Usage is cumulative metadata, not an edit and not a successful answer. */
    fun recordHintUsed() {
        val index = state.selectedIndex ?: return
        val cell = state.cells[index]
        if (state.result != null || cell.task.mode != EntryMode.User) return
        replace(index, cell.copy(hintUsed = true))
    }

    fun undo() {
        val index = state.selectedIndex ?: return
        if (state.result != null) return
        val current = state.cells[index]
        val previous = history.getValue(current.task.occurrenceId).removeLastOrNull() ?: return
        replace(index, previous.copy(
            revision = current.revision + 1,
            elapsedMs = current.elapsedMs,
            hintUsed = current.hintUsed || previous.hintUsed,
            reference = current.reference,
            referenceUnavailable = current.referenceUnavailable,
        ))
    }

    /** Settings affect verdicts, not ink/history. Advancing revisions rejects old in-flight checks. */
    fun invalidateChecks() {
        if (state.result != null || state.gestureActive) return
        history.values.forEach { entries ->
            val refreshed = entries.map { it.copy(status = if (it.sample.strokeCount == 0) EntryStatus.Empty else EntryStatus.Editing) }
            entries.clear()
            entries.addAll(refreshed)
        }
        state.cells.forEachIndexed { index, cell ->
            if (cell.task.mode == EntryMode.User && cell.sample.strokeCount > 0) {
                replace(index, cell.copy(status = EntryStatus.Editing, revision = cell.revision + 1))
            }
        }
    }

    fun checkRequests(): List<CheckRequest> = if (state.result != null) emptyList() else state.cells
        .filter { it.status == EntryStatus.Editing || it.status == EntryStatus.Incorrect }
        .map { CheckRequest(state.task.taskId, it.task.occurrenceId, it.task.kanjiId, it.revision, it.sample) }

    fun applyCheck(request: CheckRequest, verdict: CheckVerdict): Boolean {
        if (state.result != null || request.taskId != state.task.taskId) return false
        val index = state.cells.indexOfFirst { it.task.occurrenceId == request.occurrenceId }
        val cell = state.cells.getOrNull(index) ?: return false
        if (cell.revision != request.revision || cell.sample !== request.sample) return false
        replace(index, cell.copy(status = when (verdict) {
            CheckVerdict.Correct -> EntryStatus.Correct
            CheckVerdict.Incorrect -> EntryStatus.Incorrect
            CheckVerdict.Incomplete, CheckVerdict.Unavailable -> EntryStatus.Editing
        }))
        return true
    }

    /** Elapsed monotonic time is supplied, including time in the background. Clamp to the deadline, including the selected cell. */
    fun advanceTime(deltaMs: Long) {
        require(deltaMs >= 0)
        if (state.result != null || (state.isComplete && !state.gestureActive)) return
        val remaining = state.task.timeLimitMs?.minus(state.elapsedMs) ?: Long.MAX_VALUE
        val delta = minOf(deltaMs, remaining)
        val selectedIndex = state.selectedIndex
        state = state.copy(
            elapsedMs = state.elapsedMs + delta,
            cells = state.cells.mapIndexed { index, cell ->
                if (index == selectedIndex) cell.copy(elapsedMs = cell.elapsedMs + delta) else cell
            },
        )
        if (state.task.timeLimitMs != null && state.elapsedMs >= state.task.timeLimitMs!!) {
            finish(AnswerStatus.TimedOut)
        }
    }

    fun submit() {
        if (state.isComplete && !state.gestureActive) finish(if (state.cells.any { it.hintUsed }) AnswerStatus.Assisted else AnswerStatus.Correct)
    }

    fun skip() = finish(AnswerStatus.Skipped)

    private fun finish(status: AnswerStatus) {
        if (state.result != null) return
        state = state.copy(result = PracticeResult(
            state.task.taskId, state.task.russianWord, status,
            state.cells.map { KanjiResult(
                it.task.occurrenceId, it.task.kanjiId, it.task.mode, it.status,
                it.elapsedMs, it.sample, it.hintUsed,
            ) }, state.elapsedMs,
        ))
    }

    private fun edit(transform: (PracticeCell) -> PracticeCell) {
        val index = state.selectedIndex ?: return
        if (state.result != null || state.cells[index].task.mode != EntryMode.User) return
        val current = state.cells[index]
        history.getValue(current.task.occurrenceId).addLast(current)
        replace(index, transform(current).copy(revision = current.revision + 1))
    }

    private fun replace(index: Int, cell: PracticeCell) {
        state = state.copy(cells = state.cells.mapIndexed { i, old -> if (i == index) cell else old })
    }
}
