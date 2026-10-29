package ru.mobui.kanji.checker.shape.compensation

import ru.mobui.kanji.checker.strokes.geometry.ShapePoint

import ru.mobui.kanji.domain.practice.ShearCompensation
import kotlin.math.*

internal data class ShearedShape(val strokes: List<List<ShapePoint>>, val result: ShearCompensation)

/** Estimates common slant from near-vertical sections, weighted by their vertical lengths. */
internal class ShearCompensator(private val maxDegrees: Float?) {
    init { require(maxDegrees == null || (maxDegrees.isFinite() && maxDegrees in 0f..20f)) }

    fun compensate(strokes: List<List<ShapePoint>>, reference: List<List<ShapePoint>>): ShearedShape {
        val unchanged = ShearedShape(strokes, ShearCompensation(limitDegrees = maxDegrees))
        if (maxDegrees == null) return unchanged
        val referenceSlope = slope(reference, .176327f) ?: return unchanged
        val inputSlope = slope(strokes, .466308f) ?: return unchanged
        val factor = referenceSlope - inputSlope
        val degrees = atan(factor) * 180f / PI.toFloat()
        val points = strokes.flatten()
        val left = points.minOf { it.x }; val top = points.minOf { it.y }
        val shifted = strokes.map { stroke -> stroke.map { p -> ShapePoint(p.x + factor * (p.y - top), p.y) } }
        // Retain the already aligned left edge; this displacement is charged to the shear budget.
        val offset = left - shifted.flatten().minOf { it.x }
        val result = shifted.map { stroke -> stroke.map { p -> ShapePoint(p.x + offset, p.y) } }
        return ShearedShape(result, ShearCompensation(factor, degrees, maxDegrees, isEstimated = true))
    }

    private fun slope(strokes: List<List<ShapePoint>>, maxSlope: Float): Float? {
        val points = strokes.flatten()
        if (points.isEmpty()) return null
        val side = max(points.maxOf { it.x } - points.minOf { it.x }, points.maxOf { it.y } - points.minOf { it.y })
        if (side <= 0f) return null
        val sections = mutableListOf<Pair<Float, Float>>()
        for (stroke in strokes) {
            if (stroke.size < 2) continue
            val start = stroke.first(); val end = stroke.last()
            val dx = end.x - start.x; val dy = end.y - start.y
            val chord = hypot(dx, dy)
            val arc = stroke.zipWithNext().sumOf { (a, b) -> hypot(b.x - a.x, b.y - a.y).toDouble() }
            // Whole long, nearly straight strokes provide stable evidence under uniform scaling.
            // Do not infer vertical slant from a short section of an intended curve/diagonal.
            if (abs(dy) >= side * .35f && abs(dx) <= abs(dy) * maxSlope && chord > 0f && arc <= chord * 1.05f) {
                sections.add(dx / dy to abs(dy))
            }
        }
        if (sections.isEmpty() || sections.sumOf { it.second.toDouble() } < side * .35f) return null
        val halfWeight = sections.sumOf { it.second.toDouble() } / 2
        var weight = 0.0
        for ((value, length) in sections.sortedBy { it.first }) {
            weight += length
            if (weight >= halfWeight) return value
        }
        return null
    }
}
