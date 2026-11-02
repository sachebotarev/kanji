package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import kotlin.math.max
import kotlin.math.min

/** Source-independent rendering; loading and fallback selection belong to the caller. */
@Composable
internal fun KanjiDrawing(symbol: String, drawing: ReferenceDrawing?, modifier: Modifier = Modifier) {
    if (drawing == null) {
        Box(modifier.fillMaxWidth().heightIn(min = 72.dp, max = 120.dp), contentAlignment = Alignment.Center) {
            Text(symbol, style = CatalogJapaneseStyle.copy(fontSize = 72.sp), color = CatalogJapaneseColor)
        }
        return
    }
    val bounds = remember(drawing) { drawingBounds(drawing) }
    val color = CatalogJapaneseColor
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val square = minOf(maxWidth, 188.dp)
        val span = (bounds.bottom - bounds.top).coerceIn(0.08f, 1f)
        val height = square * span
        Canvas(Modifier.width(square).height(height).semantics { contentDescription = symbol }) {
            val side = size.width
            val shiftY = bounds.top * side
            drawing.strokes.forEach { points ->
                val path = Path()
                points.forEachIndexed { index, point ->
                    val x = point.x * side
                    val y = point.y * side - shiftY
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(side * 3f / 109f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            drawing.strokeNumbers.forEach { number ->
                val label = measurer.measure(number.number.toString(), TextStyle(color = secondary, fontSize = 14.sp))
                drawText(label, topLeft = Offset(number.x * side, number.y * side - shiftY - label.firstBaseline))
            }
        }
    }
}

private data class DrawingBounds(val top: Float, val bottom: Float)

private fun drawingBounds(drawing: ReferenceDrawing): DrawingBounds {
    var minY = Float.POSITIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    fun include(y: Float) {
        minY = min(minY, y)
        maxY = max(maxY, y)
    }
    drawing.strokes.forEach { stroke -> stroke.forEach { include(it.y) } }
    drawing.strokeNumbers.forEach { include(it.y) }
    val pad = 3f / 109f + 0.02f
    return DrawingBounds(minY - pad, maxY + pad)
}
