package ru.mobui.kanji.checker.shape.stroke

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import ru.mobui.kanji.checker.strokes.geometry.ShapePoint

/**
 * Reference-driven terminal feature, measured before cleanup and independently of writing
 * direction.
 */
internal class HookChecker {
    private data class Turn(val angle: Float, val reach: Float)

    fun hasDistinctHook(reference: List<ShapePoint>): Boolean = turns(reference).isNotEmpty()

    fun hasHook(input: List<ShapePoint>, reference: List<ShapePoint>): Boolean {
        val referenceTurns = turns(reference)
        val strongest = referenceTurns.maxByOrNull { abs(it.angle) * it.reach }
        // A rounded reference corner has a range of tangent directions. A sparse handwritten
        // corner must match that feature, not one arbitrarily selected tangent on the curve.
        val salient =
            if (strongest == null) emptyList()
            else
                referenceTurns.filter {
                    it.reach >= strongest.reach * MIN_REFERENCE_REACH_RATIO
                }
        return listOf(input, input.reversed()).any { stroke ->
            if (strongest == null) matchesTerminalProfile(stroke, reference)
            else
                turns(stroke).any { actual ->
                    actual.reach >= strongest.reach * MIN_INPUT_REACH_RATIO &&
                        actual.reach <= strongest.reach * MAX_INPUT_REACH_RATIO &&
                        salient.any { expected ->
                            val difference = actual.angle - expected.angle
                            abs(atan2(sin(difference), cos(difference))) <=
                                PI.toFloat() / HOOK_DIRECTION_DIVISOR
                        }
                }
        }
    }

    private fun turns(stroke: List<ShapePoint>): List<Turn> {
        val path = Arc(stroke)
        if (path.length <= 0f) return emptyList()
        return (TAIL_START_PERCENT..TAIL_LAST_TURN_PERCENT step 2).mapNotNull { percent ->
            val fraction = percent / PERCENT_SCALE_FLOAT
            val anchor = path.at(fraction)
            val before = path.at(fraction - TURN_SAMPLE_SPAN)
            val end = stroke.last()
            val ux = anchor.x - before.x
            val uy = anchor.y - before.y
            val vx = end.x - anchor.x
            val vy = end.y - anchor.y
            val angle = atan2(ux * vy - uy * vx, ux * vx + uy * vy)
            val reach =
                abs(ux * vy - uy * vx) /
                    hypot(ux, uy).coerceAtLeast(MIN_VECTOR_LENGTH) /
                    path.length
            // A shallow hook is meaningful too; tiny sampling wiggles are not.
            if (abs(angle) >= PI / MIN_TURN_DIVISOR && reach >= MIN_TURN_REACH) Turn(angle, reach)
            else null
        }
    }

    /**
     * Some G-labelled SVG paths have no distinct hook. Require their actual terminal profile; do
     * not invent a turn absent from the reference. Report this ambiguity separately.
     */
    private fun matchesTerminalProfile(
        input: List<ShapePoint>,
        reference: List<ShapePoint>,
    ): Boolean {
        val actual = profile(input)
        val expected = profile(reference)
        return actual != null &&
            expected != null &&
            actual.zip(expected).all { (a, b) -> distance(a, b) <= PROFILE_MATCH_TOLERANCE }
    }

    private fun profile(points: List<ShapePoint>): List<ShapePoint>? {
        val path = Arc(points)
        if (path.length <= 0f) return null
        val anchor = path.at(TAIL_ANCHOR_FRACTION)
        val before = path.at(TAIL_DIRECTION_FRACTION)
        val dx = anchor.x - before.x
        val dy = anchor.y - before.y
        val norm = hypot(dx, dy)
        return if (norm <= MIN_VECTOR_LENGTH) null
        else (TAIL_START_PERCENT..PERCENT_SCALE step TAIL_PROFILE_STEP_PERCENT).map {
            val p = path.at(it / PERCENT_SCALE_FLOAT)
            val x = (p.x - anchor.x) / path.length
            val y = (p.y - anchor.y) / path.length
            ShapePoint((x * dx + y * dy) / norm, (dx * y - dy * x) / norm)
        }
    }

    private class Arc(private val points: List<ShapePoint>) {
        private val lengths =
            FloatArray(points.size).also { array ->
                for (i in 1..points.lastIndex) array[i] =
                    array[i - 1] + distance(points[i - 1], points[i])
            }
        val length = lengths.lastOrNull() ?: 0f

        fun at(fraction: Float): ShapePoint {
            if (points.size < 2 || length <= 0f) return points.firstOrNull() ?: ShapePoint(0f, 0f)
            val target = length * fraction
            val end =
                lengths
                    .indexOfFirst { it >= target }
                    .let { if (it < 0) points.lastIndex else it.coerceAtLeast(1) }
            val span = lengths[end] - lengths[end - 1]
            val t = if (span > 0f) (target - lengths[end - 1]) / span else 0f
            val a = points[end - 1]
            val b = points[end]
            return ShapePoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        }
    }

    private companion object {
        fun distance(a: ShapePoint, b: ShapePoint) = hypot(b.x - a.x, b.y - a.y)
    }
}

private const val MIN_REFERENCE_REACH_RATIO = .5f
private const val MIN_INPUT_REACH_RATIO = .35f
private const val MAX_INPUT_REACH_RATIO = 2.5f
private const val HOOK_DIRECTION_DIVISOR = 3
private const val TAIL_START_PERCENT = 60
private const val TAIL_LAST_TURN_PERCENT = 98
private const val PERCENT_SCALE_FLOAT = 100f
private const val TURN_SAMPLE_SPAN = .15f
private const val MIN_VECTOR_LENGTH = .000001f
private const val MIN_TURN_DIVISOR = 9
private const val MIN_TURN_REACH = .005f
private const val TAIL_ANCHOR_FRACTION = .6f
private const val TAIL_DIRECTION_FRACTION = .45f
private const val PERCENT_SCALE = 100
private const val TAIL_PROFILE_STEP_PERCENT = 5
private const val PROFILE_MATCH_TOLERANCE = .02f
