package ru.mobui.kanji.domain.practice

import ru.mobui.kanji.domain.handwriting.InkStroke
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.WritingSample

/** User input and reference updates; history and state remain owned by the training. */
fun HandwritingTraining.setGestureActive(active: Boolean) {
    state = state.copy(gestureActive = active && state.result == null && !state.timedOut)
}

fun HandwritingTraining.select(index: Int) {
    if (
        state.result == null &&
            index in state.cells.indices &&
            state.cells[index].task.mode == EntryMode.User
    ) {
        state = state.copy(selectedIndex = index)
    }
}

/** Next writable cell after [afterIndex] that is not yet Correct/Incorrect, if any. */
fun HandwritingTraining.nextUnsatisfiedUserIndex(afterIndex: Int): Int? =
    state.cells.indices.firstOrNull { index ->
        index > afterIndex &&
            state.cells[index].task.mode == EntryMode.User &&
            !state.cells[index].isSettled
    }

fun HandwritingTraining.setReference(id: KanjiId, drawing: ReferenceDrawing?) {
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

fun HandwritingTraining.addStroke(occurrenceId: String, revision: Long, stroke: InkStroke) {
    val cell = state.selected ?: return
    if (state.result != null || state.timedOut || !cell.canAcceptStroke(occurrenceId, revision)) {
        return
    }
    edit {
        val old = it.sample.strokes
        it.copy(sample = WritingSample(old + stroke), status = EntryStatus.Editing)
    }
}

private fun PracticeCell.canAcceptStroke(occurrenceId: String, revision: Long): Boolean =
    !strokeHintsExhausted && task.occurrenceId == occurrenceId && this.revision == revision

fun HandwritingTraining.clear() {
    val cell = state.selected ?: return
    if (state.timedOut || cell.status == EntryStatus.Empty || cell.strokeHintsExhausted) return
    edit { it.copy(sample = WritingSample(), status = EntryStatus.Empty) }
}

fun HandwritingTraining.recordHintUsed() {
    val index = state.selectedIndex ?: return
    val cell = state.cells[index]
    if (state.result != null || state.timedOut || cell.task.mode != EntryMode.User) return
    replace(index, cell.copy(hintUsed = true))
}

/** Opens the word reading spoiler; does not mark a kanji cell as helped. */
fun HandwritingTraining.recordReadingHint() {
    if (state.result != null || state.timedOut || state.readingHintUsed) return
    state = state.copy(readingHintUsed = true)
}

/** Opens the word translation spoiler; does not mark a kanji cell as helped. */
fun HandwritingTraining.recordTranslationHint() {
    if (state.result != null || state.timedOut || state.translationHintUsed) return
    state = state.copy(translationHintUsed = true)
}

/** Counts one NextStroke hint press; returns the new count. */
fun HandwritingTraining.recordStrokeHint(): Int {
    val index = state.selectedIndex ?: return 0
    val cell = state.cells[index]
    if (
        state.result != null ||
            state.timedOut ||
            cell.task.mode != EntryMode.User ||
            cell.strokeHintsExhausted
    )
        return cell.strokeHintCount
    val count = cell.strokeHintCount + 1
    replace(index, cell.copy(hintUsed = true, strokeHintCount = count))
    return count
}

/** Budget spent: keep the cell incorrect and freeze further ink edits. */
fun HandwritingTraining.exhaustStrokeHints() {
    val index = state.selectedIndex ?: return
    val cell = state.cells[index]
    if (state.result != null || state.timedOut || cell.task.mode != EntryMode.User) return
    replace(
        index,
        cell.copy(
            hintUsed = true,
            strokeHintsExhausted = true,
            status = EntryStatus.Incorrect,
            revision = cell.revision + 1,
        ),
    )
}

fun HandwritingTraining.undo() {
    val index = state.selectedIndex?.takeIf { state.result == null && !state.timedOut } ?: return
    val current = state.cells[index]
    if (current.strokeHintsExhausted) return
    val previous = history.getValue(current.task.occurrenceId).removeLastOrNull() ?: return
    replace(
        index,
        previous.copy(
            revision = current.revision + 1,
            elapsedMs = current.elapsedMs,
            hintUsed = current.hintUsed || previous.hintUsed,
            strokeHintCount = maxOf(current.strokeHintCount, previous.strokeHintCount),
            strokeHintsExhausted = current.strokeHintsExhausted || previous.strokeHintsExhausted,
            reference = current.reference,
            referenceUnavailable = current.referenceUnavailable,
        ),
    )
}
