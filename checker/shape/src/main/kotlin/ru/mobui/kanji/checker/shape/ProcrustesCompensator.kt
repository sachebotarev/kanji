package ru.mobui.kanji.checker.shape

import kotlin.math.*

/** Similarity transform about the arc-sampled input centroid, followed by translation. */
internal data class ProcrustesResult(
    val strokes: List<List<ShapePoint>>,
    val translationX: Float,
    val translationY: Float,
    val scaleFactor: Float,
    val rotationDegrees: Float,
    val rootMeanSquaredError: Float,
    val centroid: ShapePoint = ShapePoint(0f, 0f),
)

/** Joint least-squares fit. Stroke orientation is a fitting detail, never an edit to the input. */
internal class ProcrustesCompensator(private val samplesPerStroke: Int = 32) {
    init { require(samplesPerStroke >= 2) }

    fun compensate(
        input: List<List<ShapePoint>>,
        reference: List<List<ShapePoint>>,
        assignment: List<Int>?,
        translate: Boolean = true,
        resize: Boolean = true,
        rotate: Boolean = true,
    ): ProcrustesResult {
        val identity = ProcrustesResult(input, 0f, 0f, 1f, 0f, 0f)
        if (input.isEmpty() || input.size != reference.size || assignment == null ||
            assignment.size != input.size || assignment.toSet() != reference.indices.toSet() ||
            input.any { it.isEmpty() } || reference.any { it.isEmpty() }) return identity

        val source = input.map { sampleArcLength(it) }
        val target = assignment.map { sampleArcLength(reference[it]) }
        val count = source.sumOf { it.size }
        val meanX = source.sumOf { s -> s.sumOf { it.x.toDouble() } } / count
        val meanY = source.sumOf { s -> s.sumOf { it.y.toDouble() } } / count
        val targetX = target.sumOf { s -> s.sumOf { it.x.toDouble() } } / count
        val targetY = target.sumOf { s -> s.sumOf { it.y.toDouble() } } / count
        val shiftX = (if (translate) targetX - meanX else 0.0).let { if (abs(it) < 1e-8) 0.0 else it }
        val shiftY = (if (translate) targetY - meanY else 0.0).let { if (abs(it) < 1e-8) 0.0 else it }
        val norm = source.sumOf { s -> s.sumOf { (it.x - meanX).pow(2) + (it.y - meanY).pow(2) } }
        if (norm <= 1e-15) return identity

        fun fit(reversed: List<Boolean>): ProcrustesResult {
            var dot = 0.0
            var cross = 0.0
            for (i in source.indices) for (k in source[i].indices) {
                val a = source[i][k]
                val b = target[i][if (reversed[i]) samplesPerStroke - 1 - k else k]
                val x = a.x - meanX; val y = a.y - meanY
                val u = b.x - targetX; val v = b.y - targetY
                dot += x * u + y * v
                cross += x * v - y * u
            }
            val angle = (if (rotate) atan2(cross, dot) else 0.0).let { if (abs(it) < 1e-7) 0.0 else it }
            val cosine = cos(angle); val sine = sin(angle)
            val scale = (if (resize) (dot * cosine + cross * sine) / norm else 1.0).let { if (abs(it - 1) < 1e-7) 1.0 else it }
            // A collapsed/negative fit must not turn a malformed drawing into a point.
            if (!scale.isFinite() || scale <= 1e-9) return identity.copy(rootMeanSquaredError = Float.POSITIVE_INFINITY)
            fun transform(p: ShapePoint): ShapePoint {
                val x = p.x - meanX; val y = p.y - meanY
                return ShapePoint(
                    (meanX + shiftX + scale * (x * cosine - y * sine)).toFloat(),
                    (meanY + shiftY + scale * (x * sine + y * cosine)).toFloat(),
                )
            }
            var error = 0.0
            for (i in source.indices) for (k in source[i].indices) {
                val a = transform(source[i][k])
                val b = target[i][if (reversed[i]) samplesPerStroke - 1 - k else k]
                error += (a.x.toDouble() - b.x).pow(2) + (a.y.toDouble() - b.y).pow(2)
            }
            return ProcrustesResult(input.map { s -> s.map(::transform) }, shiftX.toFloat(), shiftY.toFloat(),
                scale.toFloat(), (angle * 180 / PI).toFloat(), sqrt(error / count).toFloat(),
                ShapePoint(meanX.toFloat(), meanY.toFloat()))
        }

        // Determine orientation in each stroke's own frame so translation and size cannot flip it.
        // Then alternate correspondence orientation and joint fitting; bounded iterations keep latency predictable.
        fun orientations(strokes: List<List<ShapePoint>>): List<Boolean> = strokes.indices.map { i ->
            val a = strokes[i]; val b = target[i]
            val ax = a.sumOf { it.x.toDouble() } / a.size
            val ay = a.sumOf { it.y.toDouble() } / a.size
            val bx = b.sumOf { it.x.toDouble() } / b.size
            val by = b.sumOf { it.y.toDouble() } / b.size
            fun error(reverse: Boolean): Double = a.indices.sumOf { k ->
                val q = b[if (reverse) b.lastIndex - k else k]
                (a[k].x - ax - q.x + bx).pow(2) + (a[k].y - ay - q.y + by).pow(2)
            }
            error(true) + 1e-12 < error(false)
        }
        var directions = orientations(source)
        var best = fit(directions)
        repeat(8) {
            val next = orientations(best.strokes.map { sampleArcLength(it) })
            if (next == directions) return best
            val candidate = fit(next)
            if (candidate.rootMeanSquaredError >= best.rootMeanSquaredError - 1e-7f) return best
            directions = next
            best = candidate
        }
        return best
    }

    private fun sampleArcLength(stroke: List<ShapePoint>): List<ShapePoint> {
        val lengths = DoubleArray(stroke.size)
        for (i in 1 until stroke.size) lengths[i] = lengths[i - 1] + hypot(
            (stroke[i].x - stroke[i - 1].x).toDouble(), (stroke[i].y - stroke[i - 1].y).toDouble())
        val total = lengths.last()
        if (total <= 1e-12) return List(samplesPerStroke) { stroke.first() }
        var segment = 1
        return List(samplesPerStroke) { k ->
            val distance = total * k / (samplesPerStroke - 1)
            while (segment < stroke.lastIndex && lengths[segment] < distance) segment++
            val length = lengths[segment] - lengths[segment - 1]
            val t = if (length > 0) ((distance - lengths[segment - 1]) / length).toFloat() else 0f
            val a = stroke[segment - 1]; val b = stroke[segment]
            ShapePoint(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
        }
    }
}
