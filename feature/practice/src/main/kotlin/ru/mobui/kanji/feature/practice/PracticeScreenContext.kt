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
import ru.mobui.kanji.domain.practice.PracticeSession
import ru.mobui.kanji.domain.practice.PracticeTask

internal data class PracticeScreenContext(
    val state: PracticeUiState,
    val onAction: (PracticeAction) -> Unit,
    val onPadEvent: (PadEvent) -> Unit,
    val activeStroke: () -> InkStroke?,
    val wordLabel: String?,
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
            state.hint?.blocksInput != true,
    overlay = state.hint?.overlay,
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
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.practice_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                )
                HeaderTaskTimer(
                    practice.elapsedMs,
                    practice.task.timeLimitMs,
                    Modifier.padding(end = 16.dp),
                )
            }
            Column(
                Modifier.widthIn(max = 488.dp).fillMaxWidth().padding(horizontal = 24.dp),
            ) {
                WordPrompt(wordLabel ?: practice.task.russianWord)
                Spacer(Modifier.height(24.dp).testTag("reading-hints-space"))
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
            }
        }
    }
}

@Composable
internal fun PracticeWriting(context: PracticeScreenContext, onOpenCheck: (CheckKind) -> Unit) {
    with(context) {
        val selected = context.selected
        Column(Modifier.widthIn(max = 488.dp).fillMaxWidth().padding(horizontal = 24.dp)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))
            PracticeStatus(
                state.currentCheckReport,
                state.isChecking,
                selected?.sample?.strokeCount ?: 0,
                actionsEnabled,
            ) {
                onOpenCheck(it)
            }
            Spacer(Modifier.height(20.dp))
            key(practice.task.taskId) {
                KanjiWritingPad(
                    state =
                        context.padState(),
                    onEvent = onPadEvent,
                    modifier = Modifier.fillMaxWidth().testTag("practice-pad"),
                )
            }
            Spacer(Modifier.height(12.dp))
            PracticeTools(
                hintActive = state.hint != null,
                hintEnabled =
                    practice.result == null &&
                        !state.gestureActive &&
                        selected?.reference != null,
                undoEnabled = actionsEnabled && state.canUndo,
                clearEnabled =
                    actionsEnabled &&
                        selected != null &&
                        selected.status != EntryStatus.Empty,
                onAction = onAction,
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
internal fun PracticeContentLayout(
    context: PracticeScreenContext,
    padding: androidx.compose.foundation.layout.PaddingValues,
    onOpenCheck: (CheckKind) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
        val drawingHeight = minOf(maxWidth, 488.dp) - 48.dp + 180.dp
        if (maxHeight >= drawingHeight + 112.dp) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PracticeHeader(context)
                }
                PracticeWriting(context, onOpenCheck)
            }
        } else {
            // Very short windows keep the full-size pad and all controls reachable by
            // scrolling.
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PracticeHeader(context)
                PracticeWriting(context, onOpenCheck)
            }
        }
    }
}

@Composable
internal fun PracticeDetails(context: PracticeScreenContext, kind: CheckKind, onClose: () -> Unit,
    onOpenSettings: (() -> Unit)?, onDebug: () -> Unit) {
    with(context) {
        CheckDetailsDialog(
            kind,
            state.currentCheckReport,
            selected,
            onClose = { onClose() },
            onOpenSettings =
                onOpenSettings?.let { callback ->
                    {
                        onClose()
                        callback()
                    }
                },
            diagnostics = {
                if (kind == CheckKind.Shape && state.debugEnabled) {
                    TextButton(
                        onClick = {
                            onClose()
                            onDebug()
                        },
                        modifier = Modifier.testTag("open-debug"),
                    ) {
                        Text(stringResource(R.string.debug_title))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.debug_compensated_drawing),
                            Modifier.weight(1f),
                        )
                        Switch(
                            checked = state.showCompensatedDrawing,
                            onCheckedChange = { onAction(PracticeAction.ToggleCompensatedDrawing) },
                            enabled =
                                state.debugReport?.compensatedDrawing != null &&
                                    !state.gestureActive,
                            modifier = Modifier.testTag("compensated-drawing"),
                        )
                    }
                }
            },
        )
    }
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
                PracticeActions(
                    enabled = actionsEnabled,
                    complete = practice.isComplete,
                    checking = state.isChecking,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun WordPrompt(word: String) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            word,
            style =
                MaterialTheme.typography.headlineMedium.copy(
                    lineBreak = LineBreak.Heading,
                    hyphens = Hyphens.Auto,
                    localeList = LocaleList("ru"),
                ),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

