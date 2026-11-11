package ru.mobui.kanji.feature.practice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

internal data class PracticeScreenContext(
    val state: PracticeUiState,
    val onAction: (PracticeAction) -> Unit,
    val onPadEvent: (PadEvent) -> Unit,
    val activeStroke: () -> InkStroke?,
    val wordLabel: String?,
    val progressLabel: String? = null,
    val isFirstWord: Boolean = true,
    val onBack: (() -> Unit)? = null,
) {
    val practice get() = state.practice
    val selected get() = practice.selected
    val actionsEnabled get() = practice.result == null && !state.gestureActive
    fun padState(): PadUiState {
        val selected = selected
        val state = state
        val practice = practice
        return PadUiState(
    version =
        ContentVersion(
            selected?.task?.occurrenceId ?: "prefilled",
            selected?.revision ?: 0,
        ),
    content = selected?.padContent() ?: PadContent.Empty,
    inputEnabled =
        selected != null &&
            practice.result == null &&
            !practice.timedOut &&
            selected.strokeHintsExhausted.not() &&
            state.hint?.blocksInput != true,
    overlay = state.hint?.overlay,
    animateOverlay =
        state.hint?.playback ==
            ru.mobui.kanji.domain.practice.HintPlayback.AnimateOnce ||
            state.hint?.playback ==
                ru.mobui.kanji.domain.practice.HintPlayback.AnimateAll,
    animateAllStrokes =
        state.hint?.playback ==
            ru.mobui.kanji.domain.practice.HintPlayback.AnimateAll,
    fullHintHoldMillis = state.fullHintDisplayMillis.toLong(),
    overlayAsError =
        state.hint?.style == ru.mobui.kanji.domain.practice.HintOverlayStyle.Exhausted,
    comparisonOverlay =
        state.debugReport?.compensatedDrawing.takeIf {
            state.debugEnabled &&
                state.showCompensatedDrawing &&
                !state.gestureActive
        },
)
    }
}

@Composable
internal fun PracticeHeader(context: PracticeScreenContext) {
    with(context) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val title = stringResource(R.string.practice_title)
                val headerTitle =
                    if (progressLabel.isNullOrEmpty()) title else "$title $progressLabel"
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("practice-back")) {
                        Icon(
                            painterResource(ru.mobui.kanji.core.designsystem.R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                } else {
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    headerTitle,
                    Modifier.weight(1f)
                        .padding(end = 8.dp)
                        .then(
                            if (progressLabel.isNullOrEmpty()) Modifier
                            else Modifier.testTag("word-progress"),
                        ),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                HeaderTaskTimer(
                    practice.elapsedMs,
                    practice.task.timeLimitMs,
                    Modifier.padding(end = 12.dp),
                )
            }
            Column(
                Modifier.widthIn(max = 488.dp).fillMaxWidth().padding(horizontal = 24.dp),
            ) {
                WordPrompt(
                    word = wordLabel ?: practice.task.russianWord,
                    revealed = state.translationRevealed,
                    enabled = actionsEnabled,
                ) {
                    onAction(PracticeAction.RevealTranslation)
                }
                WordReading(
                    practice.task.reading,
                    revealed = state.readingRevealed,
                    enabled = actionsEnabled,
                ) {
                    onAction(PracticeAction.RevealReading)
                }
                KanjiStrip(
                    KanjiStripState(
                        practice.cells,
                        practice.selectedIndex,
                        actionsEnabled,
                        state.gestureActive,
                    ),
                    activeStroke,
                ) {
                    onAction(PracticeAction.Select(it))
                }
                // Keep the cell underline clear of the status divider below.
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
internal fun PracticeWriting(
    context: PracticeScreenContext,
    onOpenCheck: (CheckKind) -> Unit,
    modifier: Modifier = Modifier,
    /** When true, the pad fills leftover height; tools keep intrinsic height and never shrink. */
    flexiblePad: Boolean = false,
    onOpenHintDetails: () -> Unit = {},
) {
    with(context) {
        val selected = context.selected
        Column(
            modifier.widthIn(max = 488.dp).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            // Air under the kanji strip / divider before the status chips.
            Spacer(Modifier.height(12.dp))
            val hintStatus =
                if (selected != null && selected.task.mode == EntryMode.User) {
                    PracticeHintStatusState(
                        usedHints = practice.totalHintCount,
                        maxHints = state.maxNextStrokeHints,
                        accepted = selected.status == EntryStatus.Correct,
                    )
                } else {
                    null
                }
            PracticeStatus(
                report = state.currentCheckReport,
                checking = state.isChecking,
                strokeCount = selected?.sample?.strokeCount ?: 0,
                enabled = actionsEnabled,
                hintStatus = hintStatus,
                modifier = Modifier.widthIn(max = if (hintStatus != null) 380.dp else 320.dp).fillMaxWidth(),
                onOpen = { onOpenCheck(it) },
                onOpenHintDetails = onOpenHintDetails,
            )
            Spacer(Modifier.height(8.dp))
            val padState = padState()
            val showGestureHint =
                isFirstWord &&
                    state.showPadGestureHints &&
                    selected != null &&
                    selected.task.mode == EntryMode.User &&
                    selected.status == EntryStatus.Empty &&
                    selected.sample.strokeCount == 0 &&
                    !state.gestureActive &&
                    state.hint == null &&
                    padState.overlay == null &&
                    !practice.timedOut &&
                    !practice.isResolved
            if (flexiblePad) {
                // Square pad under the chips; leftover height stays below. Pad size must not change
                // mid-stroke — UI-PAD cancels the gesture when layout size changes.
                BoxWithConstraints(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    val side = minOf(maxWidth, maxHeight)
                    key(practice.task.taskId) {
                        PracticePadBox(
                            state = padState,
                            onEvent = onPadEvent,
                            showGestureHint = showGestureHint,
                            modifier = Modifier.size(side),
                        )
                    }
                }
            } else {
                key(practice.task.taskId) {
                    PracticePadBox(
                        state = padState,
                        onEvent = onPadEvent,
                        showGestureHint = showGestureHint,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PracticePadBox(
    state: PadUiState,
    onEvent: (ru.mobui.kanji.core.writingpad.PadEvent) -> Unit,
    showGestureHint: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        KanjiWritingPad(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.fillMaxSize().testTag("practice-pad"),
        )
        androidx.compose.animation.AnimatedVisibility(
            visible = showGestureHint,
            enter = androidx.compose.animation.fadeIn(
                androidx.compose.animation.core.tween(durationMillis = 250),
            ),
            exit = androidx.compose.animation.fadeOut(
                androidx.compose.animation.core.tween(durationMillis = 150),
            ),
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
        ) {
            PadGestureHint()
        }
    }
}

@Composable
private fun PadGestureHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.testTag("pad-gesture-hint"),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = stringResource(R.string.pad_gesture_hint_undo),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.pad_gesture_hint_clear),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.pad_gesture_hint_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun PracticeContentLayout(
    context: PracticeScreenContext,
    padding: androidx.compose.foundation.layout.PaddingValues,
    onOpenCheck: (CheckKind) -> Unit,
    onOpenHintDetails: () -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
        val padSide = minOf(maxWidth, 488.dp) - 48.dp
        val needsScroll = maxHeight < padSide + 230.dp
        if (needsScroll) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PracticeHeader(context)
                PracticeWriting(context, onOpenCheck, onOpenHintDetails = onOpenHintDetails)
            }
        } else {
            // Header intrinsic; writing fills the rest. Pad uses weight inside leftover height.
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PracticeHeader(context)
                PracticeWriting(
                    context,
                    onOpenCheck,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    flexiblePad = true,
                    onOpenHintDetails = onOpenHintDetails,
                )
            }
        }
    }
}

@Composable
internal fun PracticeDetails(context: PracticeScreenContext, kind: CheckKind, onClose: () -> Unit) {
    CheckDetailsDialog(
        kind,
        context.state.currentCheckReport,
        context.selected,
        onClose = onClose,
    )
}

@Composable
internal fun PracticeBottomBar(context: PracticeScreenContext) {
    with(context) {
        Box(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
                PracticeFeedback(state)
                val cell = selected
                PracticeActions(
                    actionsEnabled = actionsEnabled,
                    isComplete = practice.isComplete,
                    completionTone = practice.completionTone(state.maxNextStrokeHints),
                    isResolved = practice.isResolved,
                    isTimedOut = practice.timedOut,
                    checking = state.isChecking,
                    undoEnabled =
                        actionsEnabled &&
                            !practice.timedOut &&
                            state.canUndo &&
                            cell?.strokeHintsExhausted != true,
                    clearEnabled =
                        actionsEnabled &&
                            !practice.timedOut &&
                            cell != null &&
                            cell.status != EntryStatus.Empty &&
                            !cell.strokeHintsExhausted,
                    hintActive = state.hint != null,
                    hintEnabled =
                        run {
                            val reference = cell?.reference
                            practice.result == null &&
                                !practice.timedOut &&
                                !state.gestureActive &&
                                reference != null &&
                                cell?.strokeHintsExhausted != true &&
                                practice.totalHintCount < state.maxNextStrokeHints
                        },
                    hintMode = state.hintMode,
                    gesturesOnlyMode = state.gesturesOnlyMode,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun WordReading(
    reading: String,
    revealed: Boolean,
    enabled: Boolean,
    onReveal: () -> Unit,
) {
    val trimmed = reading.trim()
    if (trimmed.isEmpty()) {
        Spacer(Modifier.height(12.dp).testTag("reading-hints-space"))
        return
    }
    ReadingSpoiler(
        reading = trimmed,
        revealed = revealed,
        enabled = enabled,
        onReveal = onReveal,
    )
}

