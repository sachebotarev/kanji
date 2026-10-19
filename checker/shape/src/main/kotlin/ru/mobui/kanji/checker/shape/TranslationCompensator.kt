package ru.mobui.kanji.checker.shape

import ru.mobui.kanji.domain.practice.TranslationCompensation

internal data class TranslatedShape(
    val strokes: List<List<ShapePoint>>,
    val result: TranslationCompensation,
)

/** First compensation: align the original glyph bounding boxes before any other transforms. */
internal class TranslationCompensator(private val maxTranslationPercent: Float) {
    init { require(maxTranslationPercent.isFinite() && maxTranslationPercent in 0f..100f) }

    fun compensate(strokes: List<List<ShapePoint>>, reference: List<List<ShapePoint>>): TranslatedShape {
        val inputPoints = strokes.flatten()
        val referencePoints = reference.flatten()
        if (inputPoints.isEmpty() || referencePoints.isEmpty()) {
            return TranslatedShape(strokes, TranslationCompensation(limitPercent = maxTranslationPercent))
        }
        val dx = referencePoints.minOf { it.x } - inputPoints.minOf { it.x }
        val dy = referencePoints.minOf { it.y } - inputPoints.minOf { it.y }
        val result = TranslationCompensation(dx * 100f, dy * 100f, maxTranslationPercent)
        val aligned = strokes.map { stroke -> stroke.map { ShapePoint(it.x + dx, it.y + dy) } }
        return TranslatedShape(aligned, result)
    }
}
