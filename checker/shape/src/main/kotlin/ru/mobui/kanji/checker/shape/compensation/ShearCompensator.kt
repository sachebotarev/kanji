package ru.mobui.kanji.checker.shape.compensation

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.hypot
import kotlin.math.max
import ru.mobui.kanji.checker.strokes.geometry.ShapePoint
import ru.mobui.kanji.domain.practice.ShearCompensation

internal data class ShearedShape(val strokes: List<List<ShapePoint>>, val result: ShearCompensation)

/** Estimates common slant from near-vertical sections, weighted by their vertical lengths. */
internal class ShearCompensator(private val maxDegrees: Float?) {
    init {
        require(
            maxDegrees == null || (maxDegrees.isFinite() && maxDegrees in 0f..MAX_SHEAR_DEGREES),
        )
    }

    fun compensate(
        strokes: List<List<ShapePoint>>,
        reference: List<List<ShapePoint>>,
    ): ShearedShape {
        val unchanged = ShearedShape(strokes, ShearCompensation(limitDegrees = maxDegrees))
        val slopes = if (maxDegrees == null) null else {
            slope(reference, MAX_REFERENCE_SLOPE)?.let { referenceSlope ->
                slope(strokes, MAX_INPUT_SLOPE)?.let { inputSlope -> referenceSlope to inputSlope }
            }
        }
        return if (slopes == null) unchanged else applyShear(strokes, slopes.first - slopes.second)
    }

    private fun applyShear(strokes: List<List<ShapePoint>>, factor: Float): ShearedShape {
        val degrees = atan(factor) * HALF_TURN_DEGREES / PI.toFloat()
        val points = strokes.flatten()
        val left = points.minOf { it.x }
        val top = points.minOf { it.y }
        val shifted = strokes.map { stroke ->
            stroke.map { p -> ShapePoint(p.x + factor * (p.y - top), p.y) }
        }
        // Retain the already aligned left edge; this displacement is charged to the shear budget.
        val offset = left - shifted.flatten().minOf { it.x }
        val result = shifted.map { stroke -> stroke.map { p -> ShapePoint(p.x + offset, p.y) } }
        return ShearedShape(
            result,
            ShearCompensation(factor, degrees, maxDegrees, isEstimated = true),
        )
    }

    private fun slope(strokes: List<List<ShapePoint>>, maxSlope: Float): Float? {
        val points = strokes.flatten()
        val side = if (points.isEmpty()) 0f else max(
            points.maxOf { it.x } - points.minOf { it.x },
            points.maxOf { it.y } - points.minOf { it.y },
        )
        if (side <= 0f) return null
        val sections = strokes.mapNotNull { section(it, side, maxSlope) }
        val totalWeight = sections.sumOf { it.second.toDouble() }
        return if (sections.isEmpty() || totalWeight < side * MIN_VERTICAL_SPAN_RATIO) null else {
            val halfWeight = totalWeight / 2
            var weight = 0.0
            sections.sortedBy { it.first }.firstOrNull { (_, length) ->
                weight += length
                weight >= halfWeight
            }?.first
        }
    }

    private fun section(stroke: List<ShapePoint>, side: Float, maxSlope: Float): Pair<Float, Float>? {
        if (stroke.size < 2) return null
        val dx = stroke.last().x - stroke.first().x
        val dy = stroke.last().y - stroke.first().y
        val chord = hypot(dx, dy)
        val arc = stroke.zipWithNext().sumOf { (a, b) -> hypot(b.x - a.x, b.y - a.y).toDouble() }
        // Whole long, nearly straight strokes provide stable evidence under uniform scaling.
        val nearVertical = abs(dy) >= side * MIN_VERTICAL_SPAN_RATIO && abs(dx) <= abs(dy) * maxSlope
        val straight = chord > 0f && arc <= chord * MAX_ARC_CHORD_RATIO
        return if (nearVertical && straight) dx / dy to abs(dy) else null
    }

}

private const val MAX_SHEAR_DEGREES = 20f
private const val MAX_REFERENCE_SLOPE = .176327f
private const val MAX_INPUT_SLOPE = .466308f
private const val HALF_TURN_DEGREES = 180f
private const val MIN_VERTICAL_SPAN_RATIO = .35f
private const val MAX_ARC_CHORD_RATIO = 1.05f
