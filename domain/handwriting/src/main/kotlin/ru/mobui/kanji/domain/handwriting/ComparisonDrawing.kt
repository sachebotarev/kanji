package ru.mobui.kanji.domain.handwriting

/** Finite comparison coordinates; compensation may move points outside the input field. */
data class ComparisonPoint(val x: Float, val y: Float) {
    init { require(x.isFinite() && y.isFinite()) }
}

/** Immutable snapshot of the actual geometry used for scoring, independent of input history. */
class ComparisonDrawing(strokes: List<List<ComparisonPoint>>) {
    private val snapshot = strokes.map { it.toTypedArray() }.toTypedArray()
    val strokes: List<List<ComparisonPoint>> get() = snapshot.map { it.toList() }
    init { require(snapshot.all { it.isNotEmpty() }) }
}
