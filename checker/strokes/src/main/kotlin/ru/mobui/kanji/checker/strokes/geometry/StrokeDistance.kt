package ru.mobui.kanji.checker.strokes.geometry

import kotlin.math.hypot
import kotlin.math.sqrt

/** Distance and arc length of the nearest point, in the same coordinate units as the stroke. */
fun distanceToStroke(point: ShapePoint, stroke: List<ShapePoint>): Pair<Float, Float> {
    if (stroke.isEmpty()) return Float.POSITIVE_INFINITY to 0f
    var best = hypot(point.x - stroke[0].x, point.y - stroke[0].y)
    var position = 0f; var walked = 0f
    for (i in 0 until stroke.size - 1) {
        val a = stroke[i]; val b = stroke[i + 1]
        val dx = b.x - a.x; val dy = b.y - a.y
        val squared = dx * dx + dy * dy
        val length = sqrt(squared)
        val t = if (squared == 0f) 0f else (((point.x - a.x) * dx + (point.y - a.y) * dy) / squared).coerceIn(0f, 1f)
        val distance = hypot(point.x - a.x - t * dx, point.y - a.y - t * dy)
        if (distance < best) { best = distance; position = walked + t * length }
        walked += length
    }
    return best to position
}
