package ru.mobui.kanji.domain.handwriting

data class ReferencePoint(val x: Float, val y: Float) {
    init {
        require(x.isFinite() && x in 0f..1f)
        require(y.isFinite() && y in 0f..1f)
    }
}

/** Label baseline in normalized viewport units; source labels may lie just outside the viewport. */
data class ReferenceStrokeNumber(val number: Int, val x: Float, val y: Float) {
    init { require(number > 0 && x.isFinite() && y.isFinite()) }
}

enum class HookRequirement { Unknown, Required, NotRequired, Optional }

/** Source-independent polylines prepared by the caller, in the unit square. */
class ReferenceDrawing(strokes: List<List<ReferencePoint>>, numbers: List<ReferenceStrokeNumber> = emptyList(),
    hookRequirements: List<HookRequirement> = List(strokes.size) { HookRequirement.Unknown },
) {
    private val hookSnapshot = hookRequirements.toTypedArray()
    val hookRequirements: List<HookRequirement> get() = hookSnapshot.toList()
    private val numberSnapshot = numbers.toTypedArray()
    val strokeNumbers: List<ReferenceStrokeNumber> get() = numberSnapshot.toList()
    private val snapshot = strokes.map { it.toTypedArray() }.toTypedArray()
    val strokes: List<List<ReferencePoint>> get() = snapshot.map { it.toList() }
    init {
        require(snapshot.isNotEmpty())
        require(hookSnapshot.size == snapshot.size)
        require(snapshot.all { it.isNotEmpty() })
        require(numberSnapshot.all { it.number <= snapshot.size })
        require(numberSnapshot.map { it.number }.distinct().size == numberSnapshot.size)
    }
}

