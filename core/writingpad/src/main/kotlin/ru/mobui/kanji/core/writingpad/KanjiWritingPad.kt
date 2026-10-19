package ru.mobui.kanji.core.writingpad

import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.domain.handwriting.ReferencePoint
import ru.mobui.kanji.domain.handwriting.InkPoint
import ru.mobui.kanji.domain.handwriting.InkStroke
import ru.mobui.kanji.domain.handwriting.InkTool

/** Drawing and input only. Completed strokes and all editing history belong to the caller. */
@Composable
fun KanjiWritingPad(
    state: PadUiState,
    onEvent: (PadEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnEvent by rememberUpdatedState(onEvent)
    var activePath by remember { mutableStateOf<Path?>(null) }
    var activeDot by remember { mutableStateOf<Offset?>(null) }
    var drawRevision by remember { mutableLongStateOf(0L) }
    val gestureCounter = remember { longArrayOf(0L) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val insetPx = with(density) { 8.dp.toPx() }
    val savedPaths = remember(state.content, canvasSize, insetPx) {
        val side = minOf(canvasSize.width, canvasSize.height) - insetPx * 2
        val strokes = when (val content = state.content) {
            PadContent.Empty -> emptyList()
            is PadContent.UserInk -> content.sample.strokes.map { stroke ->
                stroke.points.map { ReferencePoint(it.x, it.y) }
            }
            is PadContent.Reference -> content.drawing.strokes
        }
        strokes.map { points ->
            val path = Path()
            points.forEachIndexed { index, point ->
                val x = insetPx + point.x * side
                val y = insetPx + point.y * side
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path to Offset(insetPx + points.first().x * side, insetPx + points.first().y * side)
        }
    }
    val overlayPaths = remember(state.overlay, canvasSize, insetPx) {
        val side = minOf(canvasSize.width, canvasSize.height) - insetPx * 2
        state.overlay?.strokes.orEmpty().map { points ->
            val path = Path()
            points.forEachIndexed { index, point ->
                val x = insetPx + point.x * side
                val y = insetPx + point.y * side
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path to Offset(insetPx + points.first().x * side, insetPx + points.first().y * side)
        }
    }
    val comparisonPaths = remember(state.comparisonOverlay, canvasSize, insetPx) {
        val side = minOf(canvasSize.width, canvasSize.height) - insetPx * 2
        state.comparisonOverlay?.strokes.orEmpty().map { points ->
            val path = Path()
            points.forEachIndexed { index, p ->
                val x = insetPx + p.x * side; val y = insetPx + p.y * side
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path to Offset(insetPx + points.first().x * side, insetPx + points.first().y * side)
        }
    }
    val textMeasurer = rememberTextMeasurer()
    val numberColor = MaterialTheme.colorScheme.onSurfaceVariant
    val numberLabels = remember(state.overlay, textMeasurer, numberColor) {
        state.overlay?.strokeNumbers.orEmpty().map { number ->
            number to textMeasurer.measure(number.number.toString(),
                TextStyle(color = numberColor, fontSize = 12.sp, fontWeight = FontWeight.Medium))
        }
    }
    val label = stringResource(R.string.writing_pad_label)
    val strokeCount = when (val content = state.content) {
        PadContent.Empty -> 0
        is PadContent.UserInk -> content.sample.strokeCount
        is PadContent.Reference -> content.drawing.strokes.size
    }
    val description = if (state.content is PadContent.Reference) {
        stringResource(R.string.writing_pad_reference)
    } else {
        pluralStringResource(R.plurals.writing_pad_strokes, strokeCount, strokeCount)
    }
    val shape = RoundedCornerShape(16.dp)
    val ink = MaterialTheme.colorScheme.onSurface
    val guide = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .onSizeChanged { canvasSize = it }
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .semantics {
                contentDescription = label
                stateDescription = description
                if (!state.inputEnabled) disabled()
            }
            .pointerInput(state.version, state.inputEnabled, canvasSize, insetPx) {
                if (!state.inputEnabled) return@pointerInput
                val inset = 8.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    if (down.type == PointerType.Mouse && !currentEvent.buttons.isPrimaryPressed) {
                        return@awaitEachGesture
                    }
                    val startSize = size
                    val side = minOf(size.width, size.height).toFloat() - 2 * inset
                    if (side <= 0f) return@awaitEachGesture
                    val bounds = Rect(inset, inset, inset + side, inset + side)
                    if (!bounds.contains(down.position)) return@awaitEachGesture
                    down.consume()
                    val version = state.version
                    val id = ++gestureCounter[0]
                    val tool = when (down.type) {
                        PointerType.Touch -> InkTool.Finger
                        PointerType.Stylus -> InkTool.Stylus
                        PointerType.Mouse -> InkTool.Mouse
                        else -> InkTool.Unknown
                    }
                    val points = mutableListOf<InkPoint>()
                    val path = Path()
                    fun append(position: Offset, time: Long) {
                        points.add(InkPoint(
                            x = ((position.x - inset) / side).coerceIn(0f, 1f),
                            y = ((position.y - inset) / side).coerceIn(0f, 1f),
                            elapsedMs = (time - down.uptimeMillis).coerceAtLeast(0),
                        ))
                        if (points.size == 1) path.moveTo(position.x, position.y)
                        else path.lineTo(position.x, position.y)
                        activeDot = down.position
                        activePath = path
                        drawRevision++
                    }
                    try {
                        append(down.position, down.uptimeMillis)
                        currentOnEvent(PadEvent.GesturePreview(version, id, InkStroke(points, tool)))
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (size != startSize || change.isConsumed || !bounds.contains(change.position)) break
                            change.consume()
                            append(change.position, change.uptimeMillis)
                            if (!change.pressed) {
                                currentOnEvent(PadEvent.StrokeFinished(version, id, InkStroke(points, tool)))
                                break
                            }
                            currentOnEvent(PadEvent.GesturePreview(version, id, InkStroke(points, tool)))
                        }
                    } finally {
                        activePath = null
                        activeDot = null
                        currentOnEvent(PadEvent.GesturePreview(version, id, null))
                    }
                }
            },
    ) {
        val inset = 8.dp.toPx()
        val side = size.minDimension - inset * 2
        if (side <= 0) return@Canvas
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        val mid = inset + side / 2
        fun guideLine(a: Offset, b: Offset) = drawLine(
            guide, a, b, strokeWidth = 1.dp.toPx(), pathEffect = dash,
        )
        guideLine(Offset(mid, inset), Offset(mid, inset + side))
        guideLine(Offset(inset, mid), Offset(inset + side, mid))
        drawLine(guide.copy(alpha = .35f), Offset(inset, inset), Offset(inset + side, inset + side), 1.dp.toPx(), pathEffect = dash)
        drawLine(guide.copy(alpha = .35f), Offset(inset + side, inset), Offset(inset, inset + side), 1.dp.toPx(), pathEffect = dash)
        val width = 4.dp.toPx()
        // The tracing guide is beneath committed and active user ink.
        overlayPaths.forEach { (path, start) -> drawInk(path, start, ink.copy(alpha = 0.25f), width) }
        numberLabels.forEach { (position, layout) ->
            drawText(layout, topLeft = Offset(
                (inset + position.x * side).coerceIn(inset, (inset + side - layout.size.width).coerceAtLeast(inset)),
                (inset + position.y * side - layout.firstBaseline).coerceIn(inset, (inset + side - layout.size.height).coerceAtLeast(inset)),
            ))
        }
        // A new gesture replaces a reference visually; the owner commits the replacement.
        if (state.content !is PadContent.Reference || activePath == null) {
            savedPaths.forEach { (path, start) -> drawInk(path, start, ink, width) }
        }
        // Read in the draw phase: pointer moves must not recompose the screen.
        @Suppress("UNUSED_VARIABLE") val revision = drawRevision
        activePath?.let { drawInk(it, activeDot!!, ink, width) }
        clipRect(inset, inset, inset + side, inset + side) {
            comparisonPaths.forEach { (path, start) -> drawInk(path, start, Color.Red, 2.dp.toPx()) }
        }
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
