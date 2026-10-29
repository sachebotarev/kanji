package ru.mobui.kanji.checker.shape.coverage

import ru.mobui.kanji.checker.strokes.geometry.ShapePoint

import kotlin.math.*

/** Bidirectional coverage in a shared coordinate frame. No implicit geometric compensation. */
internal class RasterCoverageCalculator(private val distanceTolerance: Float) {
    class Frame(val left: Float, val top: Float, val side: Float)

    class Target(val mask: BooleanArray, val distances: FloatArray, val frame: Frame, val tolerancePixels: Float, val reference: List<List<ShapePoint>>) {
        val isEmpty: Boolean get() = mask.none { it }
    }

    data class Coverage(val inputPercent: Float, val referencePercent: Float)

    fun prepare(reference: List<List<ShapePoint>>, candidates: List<List<List<ShapePoint>>> = emptyList()): Target {
        val points = reference.flatten()
        val all = points + candidates.flatMap { it.flatten() }
        val left = all.minOfOrNull { it.x } ?: 0f
        val top = all.minOfOrNull { it.y } ?: 0f
        val side = max((all.maxOfOrNull { it.x } ?: left) - left, (all.maxOfOrNull { it.y } ?: top) - top)
        val frame = Frame(left, top, side)
        val referenceSide = if (points.isEmpty()) 0f else max(
            points.maxOf { it.x } - points.minOf { it.x }, points.maxOf { it.y } - points.minOf { it.y },
        )
        val mask = if (referenceSide <= 0f) BooleanArray(SIZE * SIZE) else raster(reference, frame)
        val tolerance = if (side > 0f) distanceTolerance * referenceSide / side * (SIZE - 1) else 0f
        return Target(mask, distanceField(mask), frame, tolerance, reference)
    }

    fun compare(strokes: List<List<ShapePoint>>, target: Target): Coverage? {
        val points = strokes.flatten()
        if (points.isEmpty() || (points.all { it == points.first() })) return null
        // A rejected/extreme candidate must not change the resolution used for other candidates.
        val frame = target.frame
        val fits = points.all { it.x >= frame.left && it.x <= frame.left + frame.side && it.y >= frame.top && it.y <= frame.top + frame.side }
        val common = if (fits) target else prepare(target.reference, listOf(strokes))
        val input = raster(strokes, common.frame)
        if (input.none { it }) return null
        return Coverage(coverage(input, common.distances, common.tolerancePixels), coverage(common.mask, distanceField(input), common.tolerancePixels))
    }

    private fun raster(strokes: List<List<ShapePoint>>, frame: Frame): BooleanArray {
        val mask = BooleanArray(SIZE * SIZE)
        if (frame.side <= 0f) return mask
        fun normalize(p: ShapePoint) = ShapePoint(
            (p.x - frame.left) / frame.side * (SIZE - 1),
            (p.y - frame.top) / frame.side * (SIZE - 1),
        )
        fun plot(p: ShapePoint) { mask[p.y.roundToInt().coerceIn(0, SIZE - 1) * SIZE + p.x.roundToInt().coerceIn(0, SIZE - 1)] = true }
        strokes.forEach { stroke ->
            val normalized = stroke.map(::normalize)
            normalized.firstOrNull()?.let(::plot)
            normalized.zipWithNext().forEach { (a, b) ->
                val steps = ceil(max(abs(b.x - a.x), abs(b.y - a.y)) * 2).toInt().coerceAtLeast(1)
                for (i in 1..steps) {
                    val t = i.toFloat() / steps
                    plot(ShapePoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t))
                }
            }
        }
        return mask
    }

    private fun distanceField(to: BooleanArray): FloatArray {
        // Two-pass chamfer distance field: O(grid area), independent of input point count.
        val distance = FloatArray(to.size) { if (to[it]) 0f else Float.POSITIVE_INFINITY }
        val diagonal = sqrt(2f)
        for (y in 0 until SIZE) for (x in 0 until SIZE) {
            val i = y * SIZE + x
            if (x > 0) distance[i] = min(distance[i], distance[i - 1] + 1)
            if (y > 0) {
                distance[i] = min(distance[i], distance[i - SIZE] + 1)
                if (x > 0) distance[i] = min(distance[i], distance[i - SIZE - 1] + diagonal)
                if (x < SIZE - 1) distance[i] = min(distance[i], distance[i - SIZE + 1] + diagonal)
            }
        }
        for (y in SIZE - 1 downTo 0) for (x in SIZE - 1 downTo 0) {
            val i = y * SIZE + x
            if (x < SIZE - 1) distance[i] = min(distance[i], distance[i + 1] + 1)
            if (y < SIZE - 1) {
                distance[i] = min(distance[i], distance[i + SIZE] + 1)
                if (x > 0) distance[i] = min(distance[i], distance[i + SIZE - 1] + diagonal)
                if (x < SIZE - 1) distance[i] = min(distance[i], distance[i + SIZE + 1] + diagonal)
            }
        }
        return distance
    }

    private fun coverage(from: BooleanArray, distance: FloatArray, tolerancePixels: Float): Float {
        var total = 0; var hits = 0
        from.indices.forEach { i -> if (from[i]) {
            total++
            if (distance[i] <= tolerancePixels) hits++
        } }
        return if (total == 0) 0f else 100f * hits / total
    }
    private companion object { const val SIZE = 128 }
}
