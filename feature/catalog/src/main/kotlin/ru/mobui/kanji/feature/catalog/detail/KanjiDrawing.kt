package ru.mobui.kanji.feature.catalog.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.feature.catalog.theme.CatalogJapaneseColor
import ru.mobui.kanji.feature.catalog.theme.CatalogJapaneseStyle

/** Fixed writing cell: preserve source coordinates and whitespace for every glyph. */
@Composable
internal fun KanjiDrawing(
    symbol: String,
    drawing: ReferenceDrawing?,
    modifier: Modifier = Modifier,
) {
    val color = CatalogJapaneseColor
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val guide = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    val measurer = rememberTextMeasurer()
    val paths = remember(drawing) {
        drawing?.strokes.orEmpty().map { points ->
            Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
            }
        }
    }
    val numbers = remember(drawing) { drawing?.strokeNumbers.orEmpty() }
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.width(minOf(maxWidth, 188.dp)).aspectRatio(1f)
                .semantics(mergeDescendants = true) { contentDescription = symbol },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val side = size.width
                drawRect(surface)
                val border = 1.dp.toPx()
                drawRect(
                    outline,
                    topLeft = Offset(border / 2, border / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - border, size.height - border),
                    style = Stroke(border),
                )
                val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
                drawLine(guide, Offset(side / 2, 0f), Offset(side / 2, side), border, pathEffect = dash)
                drawLine(guide, Offset(0f, side / 2), Offset(side, side / 2), border, pathEffect = dash)
                withTransform({ scale(side, side, pivot = Offset.Zero) }) {
                    paths.forEach { path ->
                        drawPath(
                            path, color,
                            style = Stroke(GLYPH_STROKE_WIDTH, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }
                numbers.forEach { number ->
                    val label = measurer.measure(
                        number.number.toString(), TextStyle(color = secondary, fontSize = 14.sp),
                    )
                    drawText(label, topLeft = Offset(number.x * side, number.y * side - label.firstBaseline))
                }
            }
            if (drawing == null) {
                Text(symbol, style = CatalogJapaneseStyle.copy(fontSize = 72.sp), color = color)
            }
        }
    }
}

private const val GLYPH_STROKE_WIDTH = 3f / 109f
