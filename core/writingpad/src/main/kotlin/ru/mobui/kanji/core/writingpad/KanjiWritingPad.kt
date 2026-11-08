package ru.mobui.kanji.core.writingpad

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.PathMeasure
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
import kotlinx.coroutines.delay
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.ReferencePoint

/** Drawing and input only. Completed strokes and all editing history belong to the caller. */
@Composable
fun KanjiWritingPad(state: PadUiState, onEvent: (PadEvent) -> Unit, modifier: Modifier = Modifier) {
    val currentOnEvent by rememberUpdatedState(onEvent)
    val active = remember { ActivePadGesture() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val padMetrics =
        remember(density) {
            with(density) {
                PadMetrics(
                    inset = 8.dp.toPx(),
                    strokeWidth = 4.dp.toPx(),
                    comparisonWidth = 2.dp.toPx(),
                    guideWidth = 1.dp.toPx(),
                    dashEffect =
                        PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                )
            }
        }
    val inset = padMetrics.inset
    val paths = rememberPadPaths(state, canvasSize, inset)
    val hintMeasure = remember { PathMeasure() }
    val hintSegment = remember { Path() }
    val overlayProgress = remember { Animatable(1f) }
    val overlayAlpha = remember { Animatable(0.25f) }
    LaunchedEffect(state.overlay, state.animateOverlay) {
        if (state.overlay == null) {
            overlayProgress.snapTo(1f)
            overlayAlpha.snapTo(0.25f)
            return@LaunchedEffect
        }
        if (!state.animateOverlay) {
            overlayProgress.snapTo(1f)
            overlayAlpha.snapTo(0.25f)
            return@LaunchedEffect
        }
        overlayProgress.snapTo(0f)
        overlayAlpha.snapTo(1f)
        overlayProgress.animateTo(1f, animationSpec = tween(700, easing = LinearEasing))
        delay(220)
        overlayAlpha.animateTo(0f, animationSpec = tween(320))
        currentOnEvent(PadEvent.HintPlaybackFinished)
    }
    val shape = RoundedCornerShape(16.dp)
    val label = stringResource(R.string.writing_pad_label)
    val description = padDescription(state.content)
    val ink = MaterialTheme.colorScheme.onSurface
    val guide = MaterialTheme.colorScheme.outlineVariant
    val progress = overlayProgress.value
    val alpha = overlayAlpha.value
    val overlayRender =
        PadOverlayRender(
            progress = progress,
            alpha = alpha,
            animate = state.animateOverlay,
            asError = state.overlayAsError,
            measure = hintMeasure,
            segment = hintSegment,
        )
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
            drawPadGuides(guide, side, padMetrics)
            drawPadContent(
                paths,
                active,
                state.content is PadContent.Reference,
                ink,
                padMetrics,
                overlayRender,
            )
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

private data class PadMetrics(
    val inset: Float,
    val strokeWidth: Float,
    val comparisonWidth: Float,
    val guideWidth: Float,
    val dashEffect: PathEffect,
)

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

private fun DrawScope.drawPadGuides(guide: Color, side: Float, metrics: PadMetrics) {
    val inset = metrics.inset
    val mid = inset + side / 2
    fun line(a: Offset, b: Offset) =
        drawLine(guide, a, b, metrics.guideWidth, pathEffect = metrics.dashEffect)
    line(Offset(mid, inset), Offset(mid, inset + side))
    line(Offset(inset, mid), Offset(inset + side, mid))
    drawLine(
        guide.copy(alpha = .35f),
        Offset(inset, inset),
        Offset(inset + side, inset + side),
        metrics.guideWidth,
        pathEffect = metrics.dashEffect,
    )
    drawLine(
        guide.copy(alpha = .35f),
        Offset(inset + side, inset),
        Offset(inset, inset + side),
        metrics.guideWidth,
        pathEffect = metrics.dashEffect,
    )
}

private fun DrawScope.drawPadContent(
    paths: PadPaths,
    active: ActivePadGesture,
    reference: Boolean,
    ink: Color,
    metrics: PadMetrics,
    overlay: PadOverlayRender,
) {
    val inset = metrics.inset
    val side = size.minDimension - inset * 2
    if (overlay.animate) {
        val yellow = OVERLAY_ANIMATED_COLOR.copy(alpha = overlay.alpha)
        paths.overlay.forEach { (path, start) ->
            drawPartialInk(
                path,
                start,
                yellow,
                metrics.strokeWidth * OVERLAY_ANIMATE_WIDTH_FACTOR,
                overlay.progress,
                overlay.measure,
                overlay.segment,
            )
        }
    } else if (overlay.asError) {
        paths.overlay.forEach { (path, start) ->
            drawInk(path, start, Color.Red, metrics.strokeWidth * OVERLAY_ERROR_WIDTH_FACTOR)
        }
        drawStrokeLabels(paths.numbers, inset, side)
    } else {
        paths.overlay.forEach { (path, start) ->
            drawInk(path, start, ink.copy(alpha = .25f), metrics.strokeWidth)
        }
        drawStrokeLabels(paths.numbers, inset, side)
    }
    if (!reference || active.path == null) {
        paths.saved.forEach { (path, start) -> drawInk(path, start, ink, metrics.strokeWidth) }
    }
    active.renderedPath?.let { drawInk(it, requireNotNull(active.dot), ink, metrics.strokeWidth) }
    clipRect(inset, inset, inset + side, inset + side) {
        paths.comparison.forEach { (path, start) ->
            drawInk(path, start, Color.Red, metrics.comparisonWidth)
        }
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

private fun DrawScope.drawPartialInk(
    path: Path,
    start: Offset,
    color: Color,
    width: Float,
    progress: Float,
    measure: PathMeasure,
    segment: Path,
) {
    if (progress <= PROGRESS_EPSILON) return
    if (progress >= PROGRESS_COMPLETE_THRESHOLD) {
        drawInk(path, start, color, width)
        return
    }
    measure.setPath(path, false)
    val length = measure.length
    if (length <= 0f) return
    segment.rewind()
    measure.getSegment(0f, length * progress, segment, true)
    drawPath(segment, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawCircle(color, radius = width / 2, center = start)
}

@Preview(showBackground = true)
@Composable
private fun WritingPadPreview() {
    MaterialTheme {
        KanjiWritingPad(PadUiState(ContentVersion("preview", 0)), {}, Modifier.size(300.dp))
    }
}

private data class PadOverlayRender(
    val progress: Float,
    val alpha: Float,
    val animate: Boolean,
    val asError: Boolean,
    val measure: PathMeasure,
    val segment: Path,
)

private val OVERLAY_ANIMATED_COLOR = Color(0xFFFFC107)
private const val OVERLAY_ANIMATE_WIDTH_FACTOR = 1.15f
private const val OVERLAY_ERROR_WIDTH_FACTOR = 1.1f
private const val PROGRESS_EPSILON = 0.001f
private const val PROGRESS_COMPLETE_THRESHOLD = 0.999f

