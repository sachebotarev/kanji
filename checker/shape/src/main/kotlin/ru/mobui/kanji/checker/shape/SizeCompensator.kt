package ru.mobui.kanji.checker.shape

import ru.mobui.kanji.domain.practice.SizeCompensation
import kotlin.math.max

internal data class ResizedShape(
    val strokes: List<List<ShapePoint>>,
    val result: SizeCompensation,
)

/** Uniform resize about the aligned upper-left corner; preserves the glyph's aspect ratio. */
internal class SizeCompensator(private val maxCompensationPercent: Float?) {
    init { require(maxCompensationPercent == null || (maxCompensationPercent.isFinite() && maxCompensationPercent >= 0f)) }

    fun compensate(strokes: List<List<ShapePoint>>, reference: List<List<ShapePoint>>): ResizedShape {
        val points = strokes.flatten()
        val target = reference.flatten()
        val inputSide = longestSide(points)
        val targetSide = longestSide(target)
        if (inputSide <= 0f || targetSide <= 0f) {
            return ResizedShape(strokes, SizeCompensation(limitPercent = maxCompensationPercent, isDefined = false))
        }
        val factor = targetSide / inputSide
        if (!factor.isFinite() || factor <= 0f) {
            return ResizedShape(strokes, SizeCompensation(limitPercent = maxCompensationPercent, isDefined = false))
        }
        val left = points.minOf { it.x }
        val top = points.minOf { it.y }
        val resized = strokes.map { stroke -> stroke.map { p ->
            ShapePoint(left + (p.x - left) * factor, top + (p.y - top) * factor)
        } }
        return ResizedShape(resized, SizeCompensation(factor, maxCompensationPercent))
    }

    private fun longestSide(points: List<ShapePoint>): Float = if (points.isEmpty()) 0f else max(
        points.maxOf { it.x } - points.minOf { it.x },
        points.maxOf { it.y } - points.minOf { it.y },
    )
}
