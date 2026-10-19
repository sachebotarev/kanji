package ru.mobui.kanji.domain.handwriting

/** A sample in the unit square: origin top-left, X rightward and Y downward. */
data class InkPoint(val x: Float, val y: Float, val elapsedMs: Long) {
    init {
        require(x.isFinite() && x in 0f..1f)
        require(y.isFinite() && y in 0f..1f)
        require(elapsedMs >= 0)
    }
}

enum class InkTool { Finger, Stylus, Mouse, Unknown }

/** An immutable snapshot, ordered from pointer down to pointer up. */
class InkStroke(points: List<InkPoint>, val tool: InkTool) {
    private val snapshot = points.toTypedArray()
    val points: List<InkPoint> get() = snapshot.toList()

    init {
        require(snapshot.isNotEmpty())
        require(snapshot.first().elapsedMs == 0L)
        require(snapshot.asList().zipWithNext().all { (a, b) -> a.elapsedMs <= b.elapsedMs })
    }
}

/** Completed strokes in writing order. Independent of recognition providers. */
class WritingSample(strokes: List<InkStroke> = emptyList()) {
    private val snapshot = strokes.toTypedArray()
    val strokes: List<InkStroke> get() = snapshot.toList()
    val strokeCount: Int get() = snapshot.size
}
