package ru.mobui.kanji.feature.practice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toString
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.shape
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import ru.mobui.kanji.core.writingpad.ContentVersion
import ru.mobui.kanji.core.writingpad.KanjiWritingPad
import ru.mobui.kanji.core.writingpad.PadContent
import ru.mobui.kanji.core.writingpad.PadEvent
import ru.mobui.kanji.core.writingpad.PadUiState
import ru.mobui.kanji.domain.handwriting.InkStroke
import ru.mobui.kanji.domain.handwriting.ReferencePoint
import ru.mobui.kanji.domain.practice.CheckKind
import ru.mobui.kanji.domain.practice.CheckReport
import ru.mobui.kanji.domain.practice.EntryMode
import ru.mobui.kanji.domain.practice.EntryStatus
import ru.mobui.kanji.domain.practice.KanjiId
import ru.mobui.kanji.domain.practice.KanjiTask
import ru.mobui.kanji.domain.practice.PracticeCell
import ru.mobui.kanji.domain.practice.HandwritingTraining
import ru.mobui.kanji.domain.practice.PracticeTask

@Composable
internal fun KanjiStrip(state: KanjiStripState, activeStroke: () -> InkStroke?, onSelect: (Int) -> Unit) {
    with(state) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val viewport = with(density) { maxWidth.toPx() }
        val step = with(density) { 76.dp.toPx() }
        LaunchedEffect(selectedIndex, viewport) {
            selectedIndex?.let { index ->
                val left = index * step
                val right = left + with(density) { 64.dp.toPx() }
                val target =
                    when {
                        left < scroll.value -> left
                        right > scroll.value + viewport -> right - viewport
                        else -> scroll.value.toFloat()
                    }
                scroll.animateScrollTo(target.toInt().coerceAtLeast(0))
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    activeStroke = if (selectedIndex == index) activeStroke else ({ null }),
                    onClick = { onSelect(index) },
                )
            }
        }
        if (scroll.canScrollBackward) {
            IconButton(
                onClick = {
                    scope.launch {
                        scroll.animateScrollTo((scroll.value - viewport).toInt().coerceAtLeast(0))
                    }
                },
                modifier =
                    Modifier.align(Alignment.CenterStart)
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = .9f),
                            RoundedCornerShape(12.dp),
                        ),
            ) {
                Icon(
                    painterResource(R.drawable.ic_previous),
                    stringResource(R.string.previous_cells),
                )
            }
        }
        if (scroll.canScrollForward) {
            IconButton(
                onClick = {
                    scope.launch {
                        scroll.animateScrollTo(
                            (scroll.value + viewport).toInt().coerceAtMost(scroll.maxValue),
                        )
                    }
                },
                modifier =
                    Modifier.align(Alignment.CenterEnd)
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = .9f),
                            RoundedCornerShape(12.dp),
                        ),
            ) {
                Icon(painterResource(R.drawable.ic_next), stringResource(R.string.next_cells))
            }
        }
    }

    }
}

@Composable
private fun KanjiCell(state: KanjiCellState, activeStroke: () -> InkStroke?, onClick: () -> Unit) {
    with(state) {
    val shape = RoundedCornerShape(12.dp)
    val background =
        when (displayStatus) {
            EntryStatus.Editing -> MaterialTheme.colorScheme.tertiaryContainer
            EntryStatus.Correct -> MaterialTheme.colorScheme.primaryContainer
            EntryStatus.Incorrect -> MaterialTheme.colorScheme.errorContainer
            EntryStatus.Prefilled -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        }
    val border =
        when {
            displayStatus == EntryStatus.Incorrect -> MaterialTheme.colorScheme.error
            displayStatus == EntryStatus.Correct -> MaterialTheme.colorScheme.primary
            displayStatus == EntryStatus.Editing || selected -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.outlineVariant
        }
    val selectionColor = MaterialTheme.colorScheme.tertiary
    val label = stringResource(R.string.cell_description, index + 1, statusLabel(displayStatus))
    val selectedLabel =
        stringResource(if (selected) R.string.practice_selected else R.string.practice_not_selected)
    Box(Modifier.width(64.dp).height(72.dp)) {
        Box(
            Modifier.size(64.dp)
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
                        cornerRadius = CornerRadius((12.dp.toPx() - inset).coerceAtLeast(0f)),
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
                    stateDescription = selectedLabel
                    this.selected = state.selected
                }
                .testTag("kanji-cell-$index"),
        ) {
            InkThumbnail(cell.padContent(), activeStroke, Modifier.fillMaxSize().padding(6.dp))
        }
        if (selected) {
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .width(24.dp)
                    .height(3.dp)
                    .background(selectionColor, RoundedCornerShape(2.dp)),
            )
        }
    }

    }
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
    Canvas(modifier) {
        fun drawPoints(points: List<ReferencePoint>) {
            if (points.isEmpty()) return
            val path = Path()
            points.forEachIndexed { i, p ->
                if (i == 0) path.moveTo(p.x * size.width, p.y * size.height)
                else path.lineTo(p.x * size.width, p.y * size.height)
            }
            val width = 2.dp.toPx()
            drawPath(
                path,
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
        active?.let { drawPoints(it.points.map { p -> ReferencePoint(p.x, p.y) }) }
    }
}

