package ru.mobui.kanji.core.writingpad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.ReferencePoint

/** Drawing and input only. Completed strokes and all editing history belong to the caller. */
@Composable
fun KanjiWritingPad(state: PadUiState, onEvent: (PadEvent) -> Unit, modifier: Modifier = Modifier) {
    val currentOnEvent by rememberUpdatedState(onEvent)
    val active = remember { ActivePadGesture() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val inset = with(LocalDensity.current) { 8.dp.toPx() }
    val paths = rememberPadPaths(state, canvasSize, inset)
    val shape = RoundedCornerShape(16.dp)
    val label = stringResource(R.string.writing_pad_label)
    val description = padDescription(state.content)
    val ink = MaterialTheme.colorScheme.onSurface
    val guide = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier
            .aspectRatio(1f)
            .onSizeChanged { canvasSize = it }
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, guide, shape)
            .semantics {
                contentDescription = label
                stateDescription = description
                if (!state.inputEnabled) disabled()
            }
            .pointerInput(state.version, state.inputEnabled, canvasSize, inset) {
                if (state.inputEnabled)
                    capturePadGestures(state.version, active) { currentOnEvent(it) }
            },
    ) {
        val side = size.minDimension - inset * 2
        if (side > 0) {
            drawPadGuides(guide, inset, side)
            drawPadContent(paths, active, state.content is PadContent.Reference, ink)
        }
    }
}

@Composable
private fun padDescription(content: PadContent): String {
    val count =
        when (content) {
            PadContent.Empty -> 0
            is PadContent.UserInk -> content.sample.strokeCount
            is PadContent.Reference -> content.drawing.strokes.size
        }
    return if (content is PadContent.Reference) stringResource(R.string.writing_pad_reference)
    else pluralStringResource(R.plurals.writing_pad_strokes, count, count)
}

private data class PadPaths(
    val saved: List<Pair<Path, Offset>>,
    val overlay: List<Pair<Path, Offset>>,
    val comparison: List<Pair<Path, Offset>>,
    val numbers:
        List<Pair<ru.mobui.kanji.domain.handwriting.ReferenceStrokeNumber, TextLayoutResult>>,
)

@Composable
private fun rememberPadPaths(state: PadUiState, canvasSize: IntSize, inset: Float): PadPaths {
    val side = minOf(canvasSize.width, canvasSize.height) - inset * 2
    val saved =
        remember(state.content, canvasSize, inset) {
            val strokes =
                when (val content = state.content) {
                    PadContent.Empty -> emptyList()
                    is PadContent.UserInk ->
                        content.sample.strokes.map { stroke ->
                            stroke.points.map { ReferencePoint(it.x, it.y) }
                        }
                    is PadContent.Reference -> content.drawing.strokes
                }
            drawingPaths(strokes, side, inset) { Offset(it.x, it.y) }
        }
    val overlay =
        remember(state.overlay, canvasSize, inset) {
            drawingPaths(state.overlay?.strokes.orEmpty(), side, inset) { Offset(it.x, it.y) }
        }
    val comparison =
        remember(state.comparisonOverlay, canvasSize, inset) {
            drawingPaths(state.comparisonOverlay?.strokes.orEmpty(), side, inset) {
                Offset(it.x, it.y)
            }
        }
    return PadPaths(saved, overlay, comparison, rememberStrokeLabels(state.overlay))
}

@Composable
private fun rememberStrokeLabels(
    drawing: ReferenceDrawing?,
): List<Pair<ru.mobui.kanji.domain.handwriting.ReferenceStrokeNumber, TextLayoutResult>> {
    val measurer = rememberTextMeasurer()
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    return remember(drawing, measurer, color) {
        drawing?.strokeNumbers.orEmpty().map { number ->
            number to
                measurer.measure(
                    number.number.toString(),
                    TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                )
        }
    }
}

private fun <T> drawingPaths(
    strokes: List<List<T>>,
    side: Float,
    inset: Float,
    position: (T) -> Offset,
): List<Pair<Path, Offset>> = strokes.map { points ->
    val path = Path()
    points.forEachIndexed { index, point ->
        val x = inset + position(point).x * side
        val y = inset + position(point).y * side
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path to
        Offset(inset + position(points.first()).x * side, inset + position(points.first()).y * side)
}

private fun DrawScope.drawPadGuides(guide: Color, inset: Float, side: Float) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
    val mid = inset + side / 2
    fun line(a: Offset, b: Offset) = drawLine(guide, a, b, 1.dp.toPx(), pathEffect = dash)
    line(Offset(mid, inset), Offset(mid, inset + side))
    line(Offset(inset, mid), Offset(inset + side, mid))
    drawLine(
        guide.copy(alpha = .35f),
        Offset(inset, inset),
        Offset(inset + side, inset + side),
        1.dp.toPx(),
        pathEffect = dash,
    )
    drawLine(
        guide.copy(alpha = .35f),
        Offset(inset + side, inset),
        Offset(inset, inset + side),
        1.dp.toPx(),
        pathEffect = dash,
    )
}

private fun DrawScope.drawPadContent(
    paths: PadPaths,
    active: ActivePadGesture,
    reference: Boolean,
    ink: Color,
) {
    val inset = 8.dp.toPx()
    val side = size.minDimension - inset * 2
    val width = 4.dp.toPx()
    paths.overlay.forEach { (path, start) -> drawInk(path, start, ink.copy(alpha = .25f), width) }
    drawStrokeLabels(paths.numbers, inset, side)
    if (!reference || active.path == null) {
        paths.saved.forEach { (path, start) -> drawInk(path, start, ink, width) }
    }
    active.renderedPath?.let { drawInk(it, requireNotNull(active.dot), ink, width) }
    clipRect(inset, inset, inset + side, inset + side) {
        paths.comparison.forEach { (path, start) -> drawInk(path, start, Color.Red, 2.dp.toPx()) }
    }
}

private fun DrawScope.drawStrokeLabels(
    numbers: List<Pair<ru.mobui.kanji.domain.handwriting.ReferenceStrokeNumber, TextLayoutResult>>,
    inset: Float,
    side: Float,
) {
    numbers.forEach { (position, layout) ->
        drawText(
            layout,
            topLeft =
                Offset(
                    (inset + position.x * side).coerceIn(
                        inset,
                        (inset + side - layout.size.width).coerceAtLeast(inset),
                    ),
                    (inset + position.y * side - layout.firstBaseline).coerceIn(
                        inset,
                        (inset + side - layout.size.height).coerceAtLeast(inset),
                    ),
                ),
        )
    }
}

private fun DrawScope.drawInk(path: Path, start: Offset, color: Color, width: Float) {
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawCircle(color, radius = width / 2, center = start)
}

@Preview(showBackground = true)
@Composable
private fun WritingPadPreview() {
    MaterialTheme {
        KanjiWritingPad(PadUiState(ContentVersion("preview", 0)), {}, Modifier.size(300.dp))
    }
}
