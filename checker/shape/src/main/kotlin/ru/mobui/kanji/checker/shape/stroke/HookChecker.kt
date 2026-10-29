package ru.mobui.kanji.checker.shape.stroke

import ru.mobui.kanji.checker.strokes.geometry.ShapePoint

import kotlin.math.*

/** Reference-driven terminal feature, measured before cleanup and independently of writing direction. */
internal class HookChecker {
    private data class Turn(val angle: Float, val reach: Float)

    fun hasDistinctHook(reference: List<ShapePoint>): Boolean = turns(reference).isNotEmpty()

    fun hasHook(input: List<ShapePoint>, reference: List<ShapePoint>): Boolean {
        val referenceTurns = turns(reference)
        val strongest = referenceTurns.maxByOrNull { abs(it.angle) * it.reach }
        // A rounded reference corner has a range of tangent directions. A sparse handwritten
        // corner must match that feature, not one arbitrarily selected tangent on the curve.
        val salient = if (strongest == null) emptyList() else referenceTurns.filter {
            it.reach >= strongest.reach * .5f
        }
        return listOf(input, input.reversed()).any { stroke ->
            if (strongest == null) matchesTerminalProfile(stroke, reference)
            else turns(stroke).any { actual ->
                actual.reach >= strongest.reach * .35f && actual.reach <= strongest.reach * 2.5f &&
                    salient.any { expected ->
                        val difference = actual.angle - expected.angle
                        abs(atan2(sin(difference), cos(difference))) <= PI.toFloat() / 3
                    }
            }
        }
    }

    private fun turns(stroke: List<ShapePoint>): List<Turn> {
        val path = Arc(stroke)
        if (path.length <= 0f) return emptyList()
        return (60..98 step 2).mapNotNull { percent ->
            val fraction = percent / 100f
            val anchor = path.at(fraction)
            val before = path.at(fraction - .15f)
            val end = stroke.last()
            val ux = anchor.x - before.x; val uy = anchor.y - before.y
            val vx = end.x - anchor.x; val vy = end.y - anchor.y
            val angle = atan2(ux * vy - uy * vx, ux * vx + uy * vy)
            val reach = abs(ux * vy - uy * vx) / hypot(ux, uy).coerceAtLeast(.000001f) / path.length
            // A shallow hook is meaningful too; tiny sampling wiggles are not.
            if (abs(angle) >= PI / 9 && reach >= .005f) Turn(angle, reach) else null
        }
    }

    /** Some G-labelled SVG paths have no distinct hook. Require their actual terminal profile;
     * do not invent a turn absent from the reference. Report this ambiguity separately. */
    private fun matchesTerminalProfile(input: List<ShapePoint>, reference: List<ShapePoint>): Boolean {
        fun profile(points: List<ShapePoint>): List<ShapePoint>? {
            val path = Arc(points)
            if (path.length <= 0f) return null
            val anchor = path.at(.6f); val before = path.at(.45f)
            val dx = anchor.x - before.x; val dy = anchor.y - before.y
            val norm = hypot(dx, dy)
            if (norm <= .000001f) return null
            return (60..100 step 5).map {
                val p = path.at(it / 100f)
                val x = (p.x - anchor.x) / path.length; val y = (p.y - anchor.y) / path.length
                ShapePoint((x * dx + y * dy) / norm, (dx * y - dy * x) / norm)
            }
        }
        val actual = profile(input) ?: return false
        val expected = profile(reference) ?: return false
        return actual.zip(expected).all { (a, b) -> distance(a, b) <= .02f }
    }

    private class Arc(private val points: List<ShapePoint>) {
        private val lengths = FloatArray(points.size).also { array ->
            for (i in 1..points.lastIndex) array[i] = array[i - 1] + distance(points[i - 1], points[i])
        }
        val length = lengths.lastOrNull() ?: 0f
        fun at(fraction: Float): ShapePoint {
            if (points.size < 2 || length <= 0f) return points.firstOrNull() ?: ShapePoint(0f, 0f)
            val target = length * fraction
            val end = lengths.indexOfFirst { it >= target }.let { if (it < 0) points.lastIndex else it.coerceAtLeast(1) }
            val span = lengths[end] - lengths[end - 1]
            val t = if (span > 0f) (target - lengths[end - 1]) / span else 0f
            val a = points[end - 1]; val b = points[end]
            return ShapePoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        }
    }

    private companion object {
        fun distance(a: ShapePoint, b: ShapePoint) = hypot(b.x - a.x, b.y - a.y)
    }
}
