package ru.mobui.kanji.domain.practice

import ru.mobui.kanji.domain.handwriting.InkStroke
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.WritingSample

/** User input and reference updates; history and state remain owned by the session. */
fun PracticeSession.setGestureActive(active: Boolean) {
    state = state.copy(gestureActive = active && state.result == null)
}

fun PracticeSession.select(index: Int) {
    if (
        state.result == null &&
            index in state.cells.indices &&
            state.cells[index].task.mode == EntryMode.User
    ) {
        state = state.copy(selectedIndex = index)
    }
}

fun PracticeSession.setReference(id: KanjiId, drawing: ReferenceDrawing?) {
    state =
        state.copy(
            cells =
                state.cells.map {
                    if (it.task.kanjiId == id)
                        it.copy(reference = drawing, referenceUnavailable = drawing == null)
                    else it
                },
        )
}

fun PracticeSession.addStroke(occurrenceId: String, revision: Long, stroke: InkStroke) {
    val cell = state.selected ?: return
    if (state.result != null || cell.task.occurrenceId != occurrenceId || cell.revision != revision)
        return
    edit {
        val old = it.sample.strokes
        it.copy(sample = WritingSample(old + stroke), status = EntryStatus.Editing)
    }
}

fun PracticeSession.clear() {
    if (state.selected?.status == EntryStatus.Empty) return
    edit { it.copy(sample = WritingSample(), status = EntryStatus.Empty) }
}

fun PracticeSession.recordHintUsed() {
    val index = state.selectedIndex ?: return
    val cell = state.cells[index]
    if (state.result != null || cell.task.mode != EntryMode.User) return
    replace(index, cell.copy(hintUsed = true))
}

fun PracticeSession.undo() {
    val index = state.selectedIndex?.takeIf { state.result == null } ?: return
    val current = state.cells[index]
    val previous = history.getValue(current.task.occurrenceId).removeLastOrNull() ?: return
    replace(
        index,
        previous.copy(
            revision = current.revision + 1,
            elapsedMs = current.elapsedMs,
            hintUsed = current.hintUsed || previous.hintUsed,
            reference = current.reference,
            referenceUnavailable = current.referenceUnavailable,
        ),
    )
}
