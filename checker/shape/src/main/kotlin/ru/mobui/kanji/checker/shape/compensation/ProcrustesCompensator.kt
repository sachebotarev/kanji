package ru.mobui.kanji.checker.shape.compensation

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import ru.mobui.kanji.checker.strokes.geometry.ShapePoint

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
    init {
        require(samplesPerStroke >= 2)
    }

    fun compensate(
        input: List<List<ShapePoint>>,
        reference: List<List<ShapePoint>>,
        assignment: List<Int>?,
        translate: Boolean = true,
        resize: Boolean = true,
        rotate: Boolean = true,
    ): ProcrustesResult {
        val identity = ProcrustesResult(input, 0f, 0f, 1f, 0f, 0f)
        return if (validInput(input, reference, assignment)) {
            val source = input.map(::sampleArcLength)
            val target = requireNotNull(assignment).map { sampleArcLength(reference[it]) }
            val fit = ProcrustesFit(input, source, target, translate, resize, rotate)
            if (fit.isDefined) fit.refine(::sampleArcLength) else identity
        } else identity
    }

    private fun validInput(
        input: List<List<ShapePoint>>,
        reference: List<List<ShapePoint>>,
        assignment: List<Int>?,
    ): Boolean {
        val nonempty =
            input.isNotEmpty() && input.none { it.isEmpty() } && reference.none { it.isEmpty() }
        val countsMatch = input.size == reference.size && assignment?.size == input.size
        return nonempty && countsMatch && assignment?.toSet() == reference.indices.toSet()
    }

    private fun sampleArcLength(stroke: List<ShapePoint>): List<ShapePoint> {
        val lengths = DoubleArray(stroke.size)
        for (i in 1 until stroke.size) lengths[i] =
            lengths[i - 1] +
                hypot(
                    (stroke[i].x - stroke[i - 1].x).toDouble(),
                    (stroke[i].y - stroke[i - 1].y).toDouble(),
                )
        val total = lengths.last()
        if (total <= SAMPLING_EPSILON) return List(samplesPerStroke) { stroke.first() }
        var segment = 1
        return List(samplesPerStroke) { k ->
            val distance = total * k / (samplesPerStroke - 1)
            while (segment < stroke.lastIndex && lengths[segment] < distance) segment++
            val length = lengths[segment] - lengths[segment - 1]
            val t = if (length > 0) ((distance - lengths[segment - 1]) / length).toFloat() else 0f
            val a = stroke[segment - 1]
            val b = stroke[segment]
            ShapePoint(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
        }
    }
}

/** One sampled correspondence problem, with shared statistics for every orientation candidate. */
private class ProcrustesFit(
    private val input: List<List<ShapePoint>>,
    private val source: List<List<ShapePoint>>,
    private val target: List<List<ShapePoint>>,
    private val translate: Boolean,
    private val resize: Boolean,
    private val rotate: Boolean,
) {
    private val count = source.sumOf { it.size }
    private val meanX = source.sumOf { s -> s.sumOf { it.x.toDouble() } } / count
    private val meanY = source.sumOf { s -> s.sumOf { it.y.toDouble() } } / count
    private val targetX = target.sumOf { s -> s.sumOf { it.x.toDouble() } } / count
    private val targetY = target.sumOf { s -> s.sumOf { it.y.toDouble() } } / count
    private val shiftX =
        (if (translate) targetX - meanX else 0.0).let {
            if (abs(it) < TRANSLATION_EPSILON) 0.0 else it
        }
    private val shiftY =
        (if (translate) targetY - meanY else 0.0).let {
            if (abs(it) < TRANSLATION_EPSILON) 0.0 else it
        }
    private val norm = source.sumOf { s ->
        s.sumOf { (it.x - meanX).pow(2) + (it.y - meanY).pow(2) }
    }
    val isDefined: Boolean
        get() = norm > MIN_SOURCE_NORM

    // Bounded iterations retain the original convergence and latency contract.
    fun refine(sample: (List<ShapePoint>) -> List<ShapePoint>): ProcrustesResult {
        var directions = orientations(source)
        var best = fit(directions)
        var iterations = 0
        var converged = false
        while (iterations < MAX_FIT_ITERATIONS && !converged) {
            val next = orientations(best.strokes.map(sample))
            if (next == directions) {
                converged = true
            } else {
                val candidate = fit(next)
                if (
                    candidate.rootMeanSquaredError >=
                        best.rootMeanSquaredError - MIN_FIT_IMPROVEMENT
                ) {
                    converged = true
                } else {
                    directions = next
                    best = candidate
                }
            }
            iterations++
        }
        return best
    }

    private fun correlations(reversed: List<Boolean>): Pair<Double, Double> {
        var dot = 0.0
        var cross = 0.0
        source.forEachIndexed { i, stroke ->
            stroke.forEachIndexed { k, a ->
                val b = targetPoint(i, k, reversed[i])
                val x = a.x - meanX
                val y = a.y - meanY
                val u = b.x - targetX
                val v = b.y - targetY
                dot += x * u + y * v
                cross += x * v - y * u
            }
        }
        return dot to cross
    }

    private fun fit(reversed: List<Boolean>): ProcrustesResult {
        val (dot, cross) = correlations(reversed)
        val angle =
            (if (rotate) atan2(cross, dot) else 0.0).let {
                if (abs(it) < TRANSFORM_EPSILON) 0.0 else it
            }
        val scale =
            (if (resize) (dot * cos(angle) + cross * sin(angle)) / norm else 1.0).let {
                if (abs(it - 1) < TRANSFORM_EPSILON) 1.0 else it
            }
        // A collapsed/negative fit must not turn a malformed drawing into a point.
        return if (!scale.isFinite() || scale <= MIN_SCALE) {
            ProcrustesResult(input, 0f, 0f, 1f, 0f, Float.POSITIVE_INFINITY)
        } else {
            val transform = SimilarityTransform(meanX, meanY, shiftX, shiftY, scale, angle)
            ProcrustesResult(
                input.map { s -> s.map(transform::apply) },
                shiftX.toFloat(),
                shiftY.toFloat(),
                scale.toFloat(),
                (angle * HALF_TURN_DEGREES / PI).toFloat(),
                sqrt(squaredError(reversed, transform) / count).toFloat(),
                ShapePoint(meanX.toFloat(), meanY.toFloat()),
            )
        }
    }

    private fun squaredError(reversed: List<Boolean>, transform: SimilarityTransform): Double {
        var error = 0.0
        source.forEachIndexed { i, stroke ->
            stroke.forEachIndexed { k, point ->
                val a = transform.apply(point)
                val b = targetPoint(i, k, reversed[i])
                error += (a.x.toDouble() - b.x).pow(2) + (a.y.toDouble() - b.y).pow(2)
            }
        }
        return error
    }

    private fun targetPoint(i: Int, k: Int, reverse: Boolean): ShapePoint =
        target[i][if (reverse) source[i].lastIndex - k else k]

    // Determine orientation in each stroke's own frame; translation/size cannot flip it.
    private fun orientations(strokes: List<List<ShapePoint>>): List<Boolean> =
        strokes.indices.map { i ->
            val a = strokes[i]
            val b = target[i]
            val ax = a.sumOf { it.x.toDouble() } / a.size
            val ay = a.sumOf { it.y.toDouble() } / a.size
            val bx = b.sumOf { it.x.toDouble() } / b.size
            val by = b.sumOf { it.y.toDouble() } / b.size
            fun error(reverse: Boolean): Double =
                a.indices.sumOf { k ->
                    val q = b[if (reverse) b.lastIndex - k else k]
                    (a[k].x - ax - q.x + bx).pow(2) + (a[k].y - ay - q.y + by).pow(2)
                }
            error(true) + SAMPLING_EPSILON < error(false)
        }
}

private class SimilarityTransform(
    private val meanX: Double,
    private val meanY: Double,
    private val shiftX: Double,
    private val shiftY: Double,
    private val scale: Double,
    angle: Double,
) {
    private val cosine = cos(angle)
    private val sine = sin(angle)

    fun apply(p: ShapePoint): ShapePoint {
        val x = p.x - meanX
        val y = p.y - meanY
        return ShapePoint(
            (meanX + shiftX + scale * (x * cosine - y * sine)).toFloat(),
            (meanY + shiftY + scale * (x * sine + y * cosine)).toFloat(),
        )
    }
}

private const val TRANSLATION_EPSILON = 1e-8

private const val MIN_SOURCE_NORM = 1e-15
private const val TRANSFORM_EPSILON = 1e-7
private const val MIN_SCALE = 1e-9
private const val HALF_TURN_DEGREES = 180
private const val SAMPLING_EPSILON = 1e-12
private const val MAX_FIT_ITERATIONS = 8
private const val MIN_FIT_IMPROVEMENT = 1e-7f
