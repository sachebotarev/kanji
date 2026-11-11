package ru.mobui.kanji.feature.practice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.core.writingpad.PadContent
import ru.mobui.kanji.domain.handwriting.InkStroke
import ru.mobui.kanji.domain.handwriting.ReferencePoint
import ru.mobui.kanji.domain.practice.EntryMode
import ru.mobui.kanji.domain.practice.EntryStatus
import ru.mobui.kanji.domain.practice.KanjiId
import ru.mobui.kanji.domain.practice.PracticeCell

@Composable
internal fun KanjiStrip(state: KanjiStripState, activeStroke: () -> InkStroke?, onSelect: (Int) -> Unit) {
    with(state) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Five full cells always fit. Longer words leave a peek of the next cell (carousel cue)
        // plus a soft edge fade — common overflow pattern, without arrow buttons over glyphs.
        val gap = CELL_GAP
        val needsScroll = cells.size > FIT_WITHOUT_SCROLL
        val peek = if (needsScroll) CELL_PEEK else 0.dp
        val fitSlots = minOf(FIT_WITHOUT_SCROLL, cells.size.coerceAtLeast(1))
        val gapCount = (fitSlots - 1).coerceAtLeast(0)
        val cellSide =
            ((maxWidth - peek - gap * gapCount) / fitSlots).coerceIn(MIN_CELL_SIDE, MAX_CELL_SIDE)
        val cellHeight = cellSide + CELL_UNDERLINE_SPACE
        val viewport = with(density) { maxWidth.toPx() }
        val cellPx = with(density) { cellSide.toPx() }
        val gapPx = with(density) { gap.toPx() }
        val fadePx = with(density) { EDGE_FADE.toPx() }
        val step = cellPx + gapPx
        LaunchedEffect(selectedIndex, viewport, cellPx, gapPx, fadePx, cells.size) {
            selectedIndex?.let { index ->
                val left = index * step
                val right = left + cellPx
                // Keep the active cell clear of the overflow fades on either side.
                val inset = if (needsScroll) fadePx else 0f
                val visibleLeft = scroll.value + inset
                val visibleRight = scroll.value + viewport - inset
                val target =
                    when {
                        left < visibleLeft -> left - inset
                        right > visibleRight -> right - (viewport - inset)
                        else -> scroll.value.toFloat()
                    }
                scroll.animateScrollTo(
                    target.toInt().coerceIn(0, scroll.maxValue.coerceAtLeast(0)),
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                )
            }
        }
        // Offscreen + DstOut: fade the cells' own alpha. Overlaying background-color
        // gradients looked muddy/crooked over rounded selected cells.
        Box(
            Modifier.fillMaxWidth()
                .height(cellHeight)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(scroll),
                horizontalArrangement =
                    if (needsScroll) Arrangement.spacedBy(gap)
                    else Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            ) {
                cells.forEachIndexed { index, cell ->
                    val displayStatus =
                        if (selectedIndex == index && gestureActive) EntryStatus.Editing
                        else cell.status
                    KanjiCell(
                        KanjiCellState(
                            cell = cell,
                            index = index,
                            selected = selectedIndex == index,
                            enabled = enabled,
                            displayStatus = displayStatus,
                        ),
                        side = cellSide,
                        height = cellHeight,
                        activeStroke = if (selectedIndex == index) activeStroke else ({ null }),
                        onClick = { onSelect(index) },
                    )
                }
            }
            if (scroll.canScrollBackward) {
                StripEdgeMask(
                    fromStart = true,
                    modifier =
                        Modifier.align(Alignment.CenterStart)
                            .width(EDGE_FADE)
                            .height(cellHeight),
                )
            }
            if (scroll.canScrollForward) {
                StripEdgeMask(
                    fromStart = false,
                    modifier =
                        Modifier.align(Alignment.CenterEnd)
                            .width(EDGE_FADE)
                            .height(cellHeight),
                )
            }
        }
    }

    }
}

@Composable
private fun KanjiCell(
    state: KanjiCellState,
    side: Dp,
    height: Dp,
    activeStroke: () -> InkStroke?,
    onClick: () -> Unit,
) {
    with(state) {
    val corner = minOf(12.dp, side * 0.2f)
    val shape = RoundedCornerShape(corner)
    val hinted = cell.task.mode == EntryMode.User && cell.strokeHintCount > 0
    val reducedCredit = hinted || cell.shapeAcceptedWithPenalty
    val assisted = displayStatus == EntryStatus.Correct && reducedCredit
    val primary = when {
        reducedCredit -> assistedFeedbackColor()
        else -> MaterialTheme.colorScheme.primary
    }
    val primaryContainer = when {
        reducedCredit -> assistedFeedbackColor().copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val error = MaterialTheme.colorScheme.error
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    // Mild flash on Correct/Incorrect — same strength for both; Correct still holds before advance.
    val celebrate = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    var wasCorrect by rememberSaveable(cell.task.occurrenceId) {
        mutableStateOf(displayStatus == EntryStatus.Correct)
    }
    LaunchedEffect(displayStatus, cell.revision) {
        when (displayStatus) {
            EntryStatus.Correct -> {
                if (!wasCorrect) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    wasCorrect = true
                }
                celebrate.snapTo(1f)
                celebrate.animateTo(
                    0f,
                    animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
                )
            }
            EntryStatus.Incorrect -> {
                wasCorrect = false
                celebrate.snapTo(1f)
                celebrate.animateTo(
                    0f,
                    animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
                )
            }
            else -> {
                wasCorrect = false
                celebrate.snapTo(0f)
            }
        }
    }
    val settled =
        when (displayStatus) {
            EntryStatus.Correct -> primaryContainer
            EntryStatus.Incorrect -> errorContainer
            EntryStatus.Prefilled -> MaterialTheme.colorScheme.surfaceVariant
            else -> if (reducedCredit) primaryContainer else MaterialTheme.colorScheme.surface
        }
    val peak =
        when (displayStatus) {
            EntryStatus.Correct -> primary
            EntryStatus.Incorrect -> error
            else -> settled
        }
    val background =
        if (
            displayStatus == EntryStatus.Correct ||
                displayStatus == EntryStatus.Incorrect
        ) {
            lerp(settled, peak, celebrate.value)
        } else {
            settled
        }
    val border =
        when {
            displayStatus == EntryStatus.Incorrect -> error
            reducedCredit || displayStatus == EntryStatus.Correct -> primary
            displayStatus == EntryStatus.Editing || selected ->
                MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.outlineVariant
        }
    val selectionColor = MaterialTheme.colorScheme.secondary
    val label = stringResource(R.string.cell_description, index + 1, statusLabel(displayStatus))
    val selectedLabel =
        stringResource(if (selected) R.string.practice_selected else R.string.practice_not_selected)
    val assistedLabel = stringResource(R.string.cell_assisted)
    val inkPad = (side * 0.09f).coerceIn(4.dp, 6.dp)
    val pulse = 1f + 0.05f * celebrate.value
    Box(Modifier.width(side).height(height)) {
        Box(
            Modifier.size(side)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .background(background, shape)
                .drawBehind {
                    val width =
                        (if (
                                selected ||
                                    displayStatus == EntryStatus.Correct ||
                                    displayStatus == EntryStatus.Incorrect
                            )
                                2.dp
                            else 1.dp)
                            .toPx()
                    val inset = width / 2
                    // Keep the complete stroke inside the cell; the scroll viewport clips outside
                    // pixels.
                    drawRoundRect(
                        color = border,
                        topLeft = Offset(inset, inset),
                        size =
                            Size(
                                (size.width - width).coerceAtLeast(0f),
                                (size.height - width).coerceAtLeast(0f),
                            ),
                        cornerRadius = CornerRadius((corner.toPx() - inset).coerceAtLeast(0f)),
                        style =
                            Stroke(
                                width = width,
                                pathEffect =
                                    if (displayStatus == EntryStatus.Editing)
                                        PathEffect.dashPathEffect(
                                            floatArrayOf(4.dp.toPx(), 3.dp.toPx()),
                                        )
                                    else null,
                            ),
                    )
                }
                .clickable(enabled = enabled && cell.task.mode == EntryMode.User, onClick = onClick)
                .semantics {
                    contentDescription = label
                    stateDescription = if (assisted) "$selectedLabel. $assistedLabel" else selectedLabel
                    this.selected = state.selected
                }
                .testTag("kanji-cell-$index"),
            contentAlignment = Alignment.Center,
        ) {
            val glyph = cell.task.kanjiId.unicodeGlyphOrNull()
            val isIncorrect = displayStatus == EntryStatus.Incorrect
            // Keep the learner's ink after a failed check. Reveal the reference only when there
            // is nothing to show (empty / hint budget spent).
            val revealAnswer =
                isIncorrect && (cell.strokeHintsExhausted || cell.sample.strokeCount == 0)
            val content =
                when {
                    revealAnswer ->
                        cell.reference?.let(PadContent::Reference) ?: PadContent.Empty
                    else -> cell.padContent()
                }
            if (
                content is PadContent.Empty &&
                    (displayStatus == EntryStatus.Prefilled || revealAnswer) &&
                    glyph != null
            ) {
                Text(
                    glyph,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                    color =
                        if (isIncorrect) error
                        else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            } else {
                InkThumbnail(
                    content,
                    // Don't overlay the live gesture on a revealed incorrect answer.
                    if (revealAnswer) ({ null }) else activeStroke,
                    Modifier.fillMaxSize()
                        .padding(inkPad)
                        .testTag(
                            if (revealAnswer) "kanji-cell-answer-$index"
                            else "kanji-cell-ink-$index",
                        ),
                    color = when {
                        isIncorrect -> error
                        reducedCredit -> assistedFeedbackColor()
                        else -> null
                    },
                )
            }
        }
        if (selected) {
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .width((side * 0.38f).coerceIn(16.dp, 24.dp))
                    .height(3.dp)
                    .background(selectionColor, RoundedCornerShape(2.dp)),
            )
        }
    }

    }
}

/** Full-height alpha mask at the strip edge (DstOut inside an offscreen layer). */
@Composable
private fun StripEdgeMask(fromStart: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        // Soft stop — full black at the rim looked like a hard crooked cut over rounded cells.
        val stops =
            if (fromStart) {
                arrayOf(
                    0f to Color.Black.copy(alpha = 0.92f),
                    0.55f to Color.Black.copy(alpha = 0.35f),
                    1f to Color.Transparent,
                )
            } else {
                arrayOf(
                    0f to Color.Transparent,
                    0.45f to Color.Black.copy(alpha = 0.35f),
                    1f to Color.Black.copy(alpha = 0.92f),
                )
            }
        drawRect(
            brush = Brush.horizontalGradient(colorStops = stops),
            blendMode = BlendMode.DstOut,
        )
    }
}

private const val FIT_WITHOUT_SCROLL = 5
private val MAX_CELL_SIDE = 64.dp
private val MIN_CELL_SIDE = 44.dp
private val CELL_GAP = 12.dp
private val CELL_UNDERLINE_SPACE = 8.dp
/** Visible sliver of the next cell when the strip overflows (carousel peek). */
private val CELL_PEEK = 20.dp
/** Soft mask over the peek — standard “more content” cue without covering a full glyph. */
private val EDGE_FADE = 20.dp

/** Host keys like `u304a` decode to a visible glyph for prefilled kana cells. */
internal fun KanjiId.unicodeGlyphOrNull(): String? {
    val hex = value.removePrefix("u").removePrefix("U")
    val code = hex.toIntOrNull(16) ?: return null
    if (!Character.isValidCodePoint(code)) return null
    return String(Character.toChars(code))
}

@Composable
internal fun InkThumbnail(
    content: PadContent,
    activeStroke: () -> InkStroke?,
    modifier: Modifier,
    color: Color? = null,
) {
    val ink = color ?: MaterialTheme.colorScheme.onSurface
    val strokes =
        remember(content) {
            when (content) {
                PadContent.Empty -> emptyList()
                is PadContent.Reference -> content.drawing.strokes
                is PadContent.UserInk ->
                    content.sample.strokes.map { stroke ->
                        stroke.points.map { ReferencePoint(it.x, it.y) }
                    }
            }
        }
    val reusablePath = remember { Path() }
    Canvas(modifier) {
        fun drawPoints(points: List<ReferencePoint>) {
            if (points.isEmpty()) return
            reusablePath.rewind()
            points.forEachIndexed { i, p ->
                if (i == 0) reusablePath.moveTo(p.x * size.width, p.y * size.height)
                else reusablePath.lineTo(p.x * size.width, p.y * size.height)
            }
            val width = 2.dp.toPx()
            drawPath(
                reusablePath,
                ink,
                style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawCircle(
                ink,
                width / 2,
                Offset(points.first().x * size.width, points.first().y * size.height),
            )
        }
        val active = activeStroke()
        if (content !is PadContent.Reference || active == null) strokes.forEach(::drawPoints)
        active?.let { stroke ->
            val points = stroke.points
            if (points.isNotEmpty()) {
                reusablePath.rewind()
                points.forEachIndexed { i, p ->
                    if (i == 0) reusablePath.moveTo(p.x * size.width, p.y * size.height)
                    else reusablePath.lineTo(p.x * size.width, p.y * size.height)
                }
                val width = 2.dp.toPx()
                drawPath(
                    reusablePath,
                    ink,
                    style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                drawCircle(
                    ink,
                    width / 2,
                    Offset(points.first().x * size.width, points.first().y * size.height),
                )
            }
        }
    }
}

