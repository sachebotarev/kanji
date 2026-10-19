package ru.mobui.kanji.feature.practice

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import ru.mobui.kanji.core.writingpad.*
import ru.mobui.kanji.domain.handwriting.InkStroke
import ru.mobui.kanji.domain.handwriting.ReferencePoint
import ru.mobui.kanji.domain.practice.*

/** Pure screen: navigation, state ownership and providers live outside this composable. */
@Composable
fun PracticeScreen(
    state: PracticeUiState,
    onAction: (PracticeAction) -> Unit,
    onPadEvent: (PadEvent) -> Unit,
    modifier: Modifier = Modifier,
    activeStroke: () -> InkStroke? = { null },
    onBack: () -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
    wordLabel: String? = null,
) {
    val settingsLabel = stringResource(R.string.checker_settings_title)
    val debugLabel = stringResource(R.string.debug_title)
    var debugOpen by rememberSaveable { mutableStateOf(false) }
    val practice = state.practice
    val selected = practice.selected
    val nextIndex = selected?.takeIf { it.status == EntryStatus.Correct }?.let {
        val index = practice.selectedIndex ?: 0
        ((index + 1 until practice.cells.size).toList() + (0 until index).toList())
            .firstOrNull { !practice.cells[it].isSatisfied }
    }
    val actionsEnabled = practice.result == null && !state.gestureActive 
    Scaffold(
        modifier = modifier,
        bottomBar = {
            Box(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
                    PracticeFeedback(state)
                    PracticeActions(
                        enabled = actionsEnabled,
                        complete = practice.isComplete,
                        checking = state.isChecking,
                        onAction = onAction,
                    )
                }
            }
        },
    ) { safePadding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(safePadding)) {
            val header: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, enabled = !state.gestureActive ) {
                    Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back))
                }
                Text(stringResource(R.string.practice_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (onOpenSettings != null) IconButton(onClick = onOpenSettings, enabled = !state.gestureActive, modifier = Modifier.testTag("open-checker-settings")) {
                    Text("⚙", modifier = Modifier.semantics { contentDescription = settingsLabel })
                }
                if (state.debugEnabled) IconButton(onClick = { debugOpen = true }, modifier = Modifier.testTag("open-debug")) {
                    Text("{}", modifier = Modifier.semantics { contentDescription = debugLabel }, fontFamily = FontFamily.Monospace)
                }
            }
            Column(Modifier.widthIn(max = 488.dp).fillMaxWidth().padding(horizontal = 24.dp)) {
                WordPrompt(wordLabel ?: practice.task.russianWord)
                KanjiStrip(practice.cells, practice.selectedIndex, actionsEnabled, state.gestureActive, activeStroke) { onAction(PracticeAction.Select(it)) }
            }
            }
            }
            val writing: @Composable () -> Unit = {
            Column(Modifier.widthIn(max = 488.dp).fillMaxWidth().padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(8.dp))
                PracticeStatus(selected, practice.selectedIndex, practice.cells.size, practice.elapsedMs, practice.task.timeLimitMs,
                    state.gestureActive, state.notice == PracticeNotice.Incomplete, nextIndex.takeIf { actionsEnabled }) {
                    onAction(PracticeAction.Select(it))
                }
                Spacer(Modifier.height(4.dp))
                key(practice.task.taskId) {
                    KanjiWritingPad(
                        state = PadUiState(
                            version = ContentVersion(selected?.task?.occurrenceId ?: "prefilled", selected?.revision ?: 0),
                            content = selected?.padContent() ?: PadContent.Empty,
                            inputEnabled = selected != null && practice.result == null && state.hint?.blocksInput != true,
                            overlay = state.hint?.overlay,
                            comparisonOverlay = state.debugReport?.compensatedDrawing.takeIf { state.debugEnabled && state.showCompensatedDrawing && !state.gestureActive },
                        ),
                        onEvent = onPadEvent,
                        modifier = Modifier.fillMaxWidth().testTag("practice-pad"),
                    )
                }
                Spacer(Modifier.height(12.dp))
                PracticeTools(
                    debugEnabled = state.debugEnabled,
                    comparisonEnabled = state.debugReport?.compensatedDrawing != null && !state.gestureActive,
                    comparisonActive = state.showCompensatedDrawing,
                    hintActive = state.hint != null,
                    hintEnabled = practice.result == null && !state.gestureActive && selected?.reference != null,
                    undoEnabled = actionsEnabled && state.canUndo,
                    clearEnabled = actionsEnabled && selected != null && selected.status != EntryStatus.Empty,
                    onAction = onAction,
                )
                Spacer(Modifier.height(24.dp))
            }            }
            val drawingHeight = minOf(maxWidth, 488.dp) - 48.dp + 144.dp
            if (maxHeight >= drawingHeight + 176.dp) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                        header()
                    }
                    writing()
                }
            } else {
                // Very short windows keep the full-size pad and all controls reachable by scrolling.
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    header()
                    writing()
                }
            }
        }
    }
    if (debugOpen && state.debugEnabled) DebugDialog(state) { debugOpen = false }
}

@Composable
private fun WordPrompt(word: String) {
    Box(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
        Text(word, style = MaterialTheme.typography.headlineLarge.copy(
            lineBreak = LineBreak.Heading, hyphens = Hyphens.Auto, localeList = LocaleList("ru"),
        ), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun KanjiStrip(
    cells: List<PracticeCell>, selectedIndex: Int?, enabled: Boolean, gestureActive: Boolean,
    activeStroke: () -> InkStroke?, onSelect: (Int) -> Unit,
) {
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
                val target = when {
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
                KanjiCell(cell, index, selectedIndex == index, enabled,
                    displayStatus = if (selectedIndex == index && gestureActive) EntryStatus.Editing else cell.status,
                    activeStroke = if (selectedIndex == index) activeStroke else ({ null }),
                    onClick = { onSelect(index) },
                )
            }
        }
        if (scroll.canScrollBackward) {
            IconButton(
                onClick = { scope.launch { scroll.animateScrollTo((scroll.value - viewport).toInt().coerceAtLeast(0)) } },
                modifier = Modifier.align(Alignment.CenterStart).size(48.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = .9f), RoundedCornerShape(12.dp)),
            ) { Icon(painterResource(R.drawable.ic_previous), stringResource(R.string.previous_cells)) }
        }
        if (scroll.canScrollForward) {
            IconButton(
                onClick = { scope.launch { scroll.animateScrollTo((scroll.value + viewport).toInt().coerceAtMost(scroll.maxValue)) } },
                modifier = Modifier.align(Alignment.CenterEnd).size(48.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = .9f), RoundedCornerShape(12.dp)),
            ) { Icon(painterResource(R.drawable.ic_next), stringResource(R.string.next_cells)) }
        }
    }
}

@Composable
private fun KanjiCell(
    cell: PracticeCell, index: Int, selected: Boolean, enabled: Boolean,
    displayStatus: EntryStatus,
    activeStroke: () -> InkStroke?, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val background = when (displayStatus) {
        EntryStatus.Editing -> MaterialTheme.colorScheme.tertiaryContainer
        EntryStatus.Correct -> MaterialTheme.colorScheme.primaryContainer
        EntryStatus.Incorrect -> MaterialTheme.colorScheme.errorContainer
        EntryStatus.Prefilled -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val border = when {
        displayStatus == EntryStatus.Incorrect -> MaterialTheme.colorScheme.error
        displayStatus == EntryStatus.Correct -> MaterialTheme.colorScheme.primary
        displayStatus == EntryStatus.Editing || selected -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val selectionColor = MaterialTheme.colorScheme.tertiary
    val label = stringResource(R.string.cell_description, index + 1, statusLabel(displayStatus))
    val selectedLabel = stringResource(if (selected) R.string.practice_selected else R.string.practice_not_selected)
    Box(Modifier.width(64.dp).height(72.dp)) {
        Box(
            Modifier.size(64.dp).background(background, shape)
                .drawBehind {
                    val width = (if (selected || displayStatus == EntryStatus.Correct || displayStatus == EntryStatus.Incorrect) 2.dp else 1.dp).toPx()
                    val inset = width / 2
                    // Keep the complete stroke inside the cell; the scroll viewport clips outside pixels.
                    drawRoundRect(
                        color = border,
                        topLeft = Offset(inset, inset),
                        size = Size((size.width - width).coerceAtLeast(0f), (size.height - width).coerceAtLeast(0f)),
                        cornerRadius = CornerRadius((12.dp.toPx() - inset).coerceAtLeast(0f)),
                        style = Stroke(
                            width = width,
                            pathEffect = if (displayStatus == EntryStatus.Editing) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null,
                        ),
                    )
                }
                .clickable(enabled = enabled && cell.task.mode == EntryMode.User, onClick = onClick)
                .semantics { contentDescription = label; stateDescription = selectedLabel }
                .testTag("kanji-cell-$index"),
        ) {
            InkThumbnail(cell.padContent(), activeStroke, Modifier.fillMaxSize().padding(8.dp))
        }
        if (selected) {
            Box(Modifier.align(Alignment.BottomCenter).width(24.dp).height(3.dp)
                .background(selectionColor, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
private fun InkThumbnail(content: PadContent, activeStroke: () -> InkStroke?, modifier: Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val strokes = remember(content) {
        when (content) {
            PadContent.Empty -> emptyList()
            is PadContent.Reference -> content.drawing.strokes
            is PadContent.UserInk -> content.sample.strokes.map { stroke -> stroke.points.map { ReferencePoint(it.x, it.y) } }
        }
    }
    Canvas(modifier) {
        fun drawPoints(points: List<ReferencePoint>) {
            if (points.isEmpty()) return
            val path = Path()
            points.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x * size.width, p.y * size.height) else path.lineTo(p.x * size.width, p.y * size.height) }
            val width = 2.dp.toPx()
            drawPath(path, ink, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(ink, width / 2, Offset(points.first().x * size.width, points.first().y * size.height))
        }
        val active = activeStroke()
        if (content !is PadContent.Reference || active == null) strokes.forEach(::drawPoints)
        active?.let { drawPoints(it.points.map { p -> ReferencePoint(p.x, p.y) }) }
    }
}

@Composable
private fun PracticeStatus(cell: PracticeCell?, selectedIndex: Int?, count: Int, elapsedMs: Long, limitMs: Long?, gestureActive: Boolean, incomplete: Boolean, nextIndex: Int?, onNext: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(selectedIndex?.let { stringResource(R.string.character_number, it + 1, count) } ?: stringResource(R.string.all_prefilled), style = MaterialTheme.typography.bodySmall)
            if (limitMs != null) Text(stringResource(R.string.time_remaining, formatTime((limitMs - elapsedMs).coerceAtLeast(0))),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
        }
        val status = when {
            gestureActive -> stringResource(R.string.input_in_progress)
            cell?.status == EntryStatus.Editing -> stringResource(if (incomplete) R.string.incomplete_short else R.string.editing)
            cell?.status == EntryStatus.Correct -> stringResource(R.string.correct)
            cell?.status == EntryStatus.Incorrect -> stringResource(R.string.incorrect)
            else -> null
        }
        val statusColor = when {
            gestureActive || cell?.status == EntryStatus.Editing -> MaterialTheme.colorScheme.tertiary
            cell?.status == EntryStatus.Incorrect -> MaterialTheme.colorScheme.error
            cell?.status == EntryStatus.Correct -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        status?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            ) {
                if (!gestureActive && (cell?.status == EntryStatus.Correct || cell?.status == EntryStatus.Incorrect)) {
                    TeacherMark(correct = cell.status == EntryStatus.Correct, color = statusColor)
                }
                Text(it, color = statusColor, fontWeight = FontWeight.SemiBold)
            }
        }
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (nextIndex != null) IconButton(onClick = { onNext(nextIndex) }, modifier = Modifier.testTag("next-kanji")) {
                Icon(painterResource(R.drawable.ic_next), stringResource(R.string.next_kanji))
            }
        }
    }
}

/** Maru/batsu drawn like a teacher's pencil mark, deliberately asymmetric. Decorative. */
@Composable
private fun TeacherMark(correct: Boolean, color: androidx.compose.ui.graphics.Color) {
    val path = remember(correct) {
        Path().apply {
            if (correct) {
                moveTo(15.5f, 3f)
                cubicTo(8f, .5f, 2.4f, 5.6f, 2.8f, 12.8f)
                cubicTo(3.2f, 21f, 14f, 24f, 19.8f, 17.7f)
                cubicTo(25.1f, 11.3f, 20.2f, 2.9f, 12.8f, 3.5f)
            } else {
                moveTo(4.2f, 3.6f)
                cubicTo(8.3f, 8f, 14.8f, 15f, 20.3f, 20.6f)
                moveTo(20.6f, 3.2f)
                cubicTo(16.1f, 8.1f, 9.4f, 16.3f, 3.3f, 20.8f)
            }
        }
    }
    Canvas(Modifier.size(26.dp).clearAndSetSemantics {}) {
        val factor = size.minDimension / 24f
        withTransform({ scale(factor, factor, pivot = Offset.Zero) }) {
            drawPath(path, color, style = Stroke(2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun PracticeTools(debugEnabled: Boolean, comparisonEnabled: Boolean, comparisonActive: Boolean, hintActive: Boolean, hintEnabled: Boolean, undoEnabled: Boolean, clearEnabled: Boolean, onAction: (PracticeAction) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
        val comparisonLabel = stringResource(R.string.debug_compensated_drawing)
        TraceHintButton(hintEnabled, hintActive, onAction)
        if (debugEnabled) {
            IconToggleButton(
                checked = comparisonActive, onCheckedChange = { onAction(PracticeAction.ToggleCompensatedDrawing) },
                modifier = Modifier.size(48.dp).testTag("compensated-drawing"), enabled = comparisonEnabled,
            ) {
                Text("↔", color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics {
                    contentDescription = comparisonLabel
                })
            }
        }
        Spacer(Modifier.weight(1f))
        ToolButton(R.drawable.ic_undo, R.string.undo, undoEnabled) { onAction(PracticeAction.Undo) }
        IconButton(onClick = { onAction(PracticeAction.Clear) }, modifier = Modifier.size(48.dp), enabled = clearEnabled) {
            Icon(painterResource(R.drawable.ic_clear), stringResource(R.string.clear), Modifier.size(24.dp))
        }
    }
}

@Composable
private fun TraceHintButton(enabled: Boolean, active: Boolean, onAction: (PracticeAction) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val description = stringResource(if (active) R.string.hint_active else R.string.hint_idle)
    LaunchedEffect(active) { if (active) haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
    FilledTonalIconToggleButton(
        checked = active, onCheckedChange = { onAction(PracticeAction.ToggleHint) },
        modifier = Modifier.size(48.dp).testTag("trace-hint").semantics { stateDescription = description }, enabled = enabled,
        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.primary,
            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
        ), shape = RoundedCornerShape(16.dp),
    ) {
        Icon(painterResource(R.drawable.ic_hint), stringResource(R.string.hint), Modifier.size(24.dp))
    }
}

@Composable
private fun ToolButton(icon: Int, label: Int, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(onClick, Modifier.size(48.dp), enabled, shape = RoundedCornerShape(16.dp)) {
        Icon(painterResource(icon), stringResource(label), Modifier.size(24.dp))
    }
}

/** Feedback stays above the fixed action row, outside the scrolling drawing area. */
@Composable
private fun PracticeFeedback(state: PracticeUiState) {
    val notice = when {
        state.notice == PracticeNotice.CheckerUnavailable -> R.string.checker_unavailable
        state.practice.cells.any { it.referenceUnavailable } -> R.string.reference_unavailable
        else -> null
    }
    notice?.let {
        Text(
            stringResource(it),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }.testTag("practice-notice"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DebugDialog(state: PracticeUiState, onClose: () -> Unit) {
    val currentOnClose by rememberUpdatedState(onClose)
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).padding(16.dp).testTag("debug-dialog")) {
                Row(modifier = Modifier.testTag("debug-drag-handle").pointerInput(Unit) {
                    var distance = 0f
                    detectVerticalDragGestures(
                        onDragStart = { distance = 0f },
                        onDragEnd = { if (distance > 24.dp.toPx()) currentOnClose() },
                        onVerticalDrag = { change, amount -> change.consume(); distance += amount },
                    )
                }, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.debug_title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClose, Modifier.testTag("close-debug")) { Text(stringResource(R.string.debug_close)) }
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val cell = state.practice.selected
                    val identity = stringResource(R.string.debug_identity, state.practice.task.taskId, cell?.task?.occurrenceId ?: debugToken("none"), cell?.task?.kanjiId?.value ?: debugToken("none"), cell?.revision ?: 0L)
                    Text(identity, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.debug_timing, state.practice.elapsedMs, cell?.elapsedMs ?: 0, debugToken((cell?.hintUsed == true).toString())), style = MaterialTheme.typography.bodySmall)
                    Text(if (state.isChecking) stringResource(R.string.debug_checking) else state.debugReport?.verdict?.let { debugVerdict(it) } ?: stringResource(R.string.debug_waiting), style = MaterialTheme.typography.bodyMedium)
    if (state.debugFailures.isNotEmpty() || state.debugCompensation != null) {
        val lines = state.debugFailures.map { failure -> checkFailureText(failure) }
        val correction = state.debugCompensation
        val correctionLines = if (correction == null) emptyList() else buildList {
            if (correction.shear.isApplied)
                add(stringResource(R.string.debug_shear, correction.shear.degrees))
            if (correction.jitter.isApplied)
                add(stringResource(R.string.debug_jitter, correction.jitter.removedPoints, correction.jitter.maxDeviationPercent))
            if (correction.size.isApplied)
                add(stringResource(R.string.debug_size, correction.size.changePercent, correction.size.scaleFactor))
            if (correction.translation.isApplied || correction.translation.hasError)
                add(stringResource(R.string.debug_translation, correction.translation.shiftXPercent, correction.translation.shiftYPercent, correction.translation.limitPercent))
            if (kotlin.math.abs(correction.rotationDegrees) > .0001f)
                add(stringResource(R.string.debug_rotation, correction.rotationDegrees) + " [${stringResource(R.string.pivot_centroid)}]")
            if (kotlin.math.abs(correction.scaleX - 1f) > .0001f)
                add(stringResource(R.string.debug_scale_x, (correction.scaleX - 1f) * 100))
            if (kotlin.math.abs(correction.scaleY - 1f) > .0001f)
                add(stringResource(R.string.debug_scale_y, (correction.scaleY - 1f) * 100))
        }
        Text((lines + correctionLines).joinToString("\n"), Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .testTag("practice-debug-feedback").semantics { liveRegion = LiveRegionMode.Polite },
            color = if (state.debugFailures.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
                    state.debugReport?.metrics?.forEach { (key, value) ->
                        Text("${debugMetricLabel(key)}: ${debugMetricValue(key, value)}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeActions(enabled: Boolean, complete: Boolean, checking: Boolean, onAction: (PracticeAction) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton({ onAction(PracticeAction.Skip) }, Modifier.weight(1f).heightIn(min = 48.dp), enabled,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) { Text(stringResource(R.string.skip)) }
        Button(
            { onAction(PracticeAction.Submit) },
            Modifier.weight(1f).heightIn(min = 48.dp),
            enabled = enabled && !checking && complete,
        ) { Text(stringResource(R.string.next)) }
    }
}

internal fun PracticeCell.padContent(): PadContent = when (status) {
    EntryStatus.Prefilled -> reference?.let(PadContent::Reference) ?: PadContent.Empty
    EntryStatus.Empty -> PadContent.Empty
    else -> PadContent.UserInk(sample)
}

@Composable
internal fun statusLabel(status: EntryStatus): String = stringResource(when (status) {
    EntryStatus.Empty -> R.string.empty
    EntryStatus.Editing -> R.string.editing
    EntryStatus.Correct -> R.string.correct
    EntryStatus.Incorrect -> R.string.incorrect
    EntryStatus.Prefilled -> R.string.prefilled
})

internal fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    return if (minutes < 60) "%02d:%02d".format(minutes, seconds % 60)
    else "%d:%02d:%02d".format(minutes / 60, minutes % 60, seconds % 60)
}

@Preview(showBackground = true, widthDp = 392, heightDp = 844)
@Composable
private fun PracticePreview() {
    MaterialTheme {
        val task = PracticeTask("preview", "Япония", listOf(KanjiTask("first", KanjiId("sun"), EntryMode.User), KanjiTask("second", KanjiId("book"), EntryMode.Prefilled)))
        PracticeScreen(PracticeUiState(PracticeSession(task).state), {}, {})
    }
}

@Composable
internal fun checkFailureText(failure: CheckFailure): String = when (failure) {
                is CheckFailure.KanjiRules -> failure.violations.map { violation ->
                    val rule = violation.rule
                    stringResource(when (rule.relation) {
                        StrokeRuleRelation.Crosses -> R.string.rule_crosses
                        StrokeRuleRelation.EndsAt -> R.string.rule_ends_at
                        StrokeRuleRelation.ShorterThan -> R.string.rule_shorter
                    }, rule.firstStroke, rule.secondStroke)
                }.joinToString("\n")
                is CheckFailure.RuleConfiguration -> stringResource(R.string.debug_rule_configuration, failure.expectedStrokeCount, failure.actualStrokeCount)
                is CheckFailure.StrokeOrder -> stringResource(R.string.debug_order_error, failure.mismatches.joinToString { "${it.actualPosition} → ${it.expectedPosition}" })
                is CheckFailure.MissingHooks -> stringResource(R.string.debug_missing_hooks, failure.referenceStrokes.joinToString())
                is CheckFailure.StrokeDirection -> stringResource(R.string.debug_direction_error, failure.reversedStrokes.joinToString())
                is CheckFailure.Shear -> stringResource(R.string.debug_shear_error, failure.compensation.degrees, failure.compensation.limitDegrees ?: 0f)
                is CheckFailure.Budget -> stringResource(R.string.debug_budget_error, failure.budget.totalPercent, failure.budget.limitPercent ?: 0f)
                is CheckFailure.Rotation -> stringResource(R.string.debug_rotation_error, failure.compensation.degrees, failure.compensation.limitDegrees)
                is CheckFailure.Stretch -> if (!failure.compensation.isDefined) stringResource(R.string.debug_stretch_undefined)
                    else stringResource(R.string.debug_stretch_error, failure.compensation.changeXPercent, failure.compensation.changeYPercent,
                        failure.compensation.horizontalLimitPercent, failure.compensation.verticalLimitPercent)
                is CheckFailure.Size -> if (!failure.compensation.isDefined) stringResource(R.string.debug_size_undefined)
                    else stringResource(R.string.debug_size_error, kotlin.math.abs(failure.compensation.changePercent), failure.compensation.limitPercent ?: 0f)
                is CheckFailure.Translation -> stringResource(R.string.debug_translation_error, failure.compensation.maxShiftPercent, failure.compensation.limitPercent)
                is CheckFailure.StrokeCount -> stringResource(R.string.debug_stroke_count, failure.actual, failure.expected)
                is CheckFailure.ShapeCoverage -> stringResource(R.string.debug_shape_coverage, failure.actualPercent, failure.requiredPercent)
            }
