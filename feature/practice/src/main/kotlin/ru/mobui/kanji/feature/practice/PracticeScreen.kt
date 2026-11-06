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

/** Pure screen: navigation, state ownership and providers live outside this composable. */
@Composable
fun PracticeScreen(
    state: PracticeUiState,
    onAction: (PracticeAction) -> Unit,
    onPadEvent: (PadEvent) -> Unit,
    modifier: Modifier = Modifier,
    activeStroke: () -> InkStroke? = { null },
    onOpenSettings: (() -> Unit)? = null,
    wordLabel: String? = null,
) {
    var debugOpen by rememberSaveable { mutableStateOf(false) }
    val practice = state.practice
    val selected = practice.selected
    var openCheck by
        remember(selected?.task?.occurrenceId, selected?.revision) {
            mutableStateOf<CheckKind?>(null)
        }
    LaunchedEffect(state.gestureActive) { if (state.gestureActive) openCheck = null }
    val context = PracticeScreenContext(state, onAction, onPadEvent, activeStroke, wordLabel)
    Scaffold(modifier = modifier, bottomBar = { PracticeBottomBar(context) }) { padding ->
        PracticeContentLayout(context, padding) { openCheck = it }
    }
    if (debugOpen && state.debugEnabled) DebugDialog(state) { debugOpen = false }
    if (openCheck != null && !state.gestureActive) {
        PracticeDetails(context, openCheck!!, { openCheck = null }, onOpenSettings) {
            openCheck = null
            debugOpen = true
        }
    }

}

@Composable
internal fun PracticeStatus(
    report: CheckReport?,
    checking: Boolean,
    strokeCount: Int,
    enabled: Boolean,
    onOpen: (CheckKind) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckerTestingStatuses(
                report,
                checking,
                Modifier.weight(1f),
                strokeCount = strokeCount,
                enabled = enabled,
                onOpen = onOpen,
            )
        }
    }
}

@Composable
internal fun PracticeTools(
    hintActive: Boolean,
    hintEnabled: Boolean,
    undoEnabled: Boolean,
    clearEnabled: Boolean,
    onAction: (PracticeAction) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val hintDescription =
        stringResource(if (hintActive) R.string.hint_active else R.string.hint_idle)
    LaunchedEffect(hintActive) {
        if (hintActive) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WorkspaceTool(
            R.drawable.ic_hint,
            R.string.hint,
            hintEnabled,
            Modifier.weight(1f)
                .testTag("trace-hint")
                .toggleable(hintActive, enabled = hintEnabled, role = Role.Checkbox) {
                    onAction(PracticeAction.ToggleHint)
                }
                .semantics { stateDescription = hintDescription },
            hintActive,
            R.string.hint_used,
        )
        WorkspaceTool(
            R.drawable.ic_undo,
            R.string.undo,
            undoEnabled,
            Modifier.weight(1f).clickable(enabled = undoEnabled) { onAction(PracticeAction.Undo) },
            caption = R.string.tool_undo,
        )
        WorkspaceTool(
            R.drawable.ic_clear,
            R.string.clear,
            clearEnabled,
            Modifier.weight(1f).clickable(enabled = clearEnabled) {
                onAction(PracticeAction.Clear)
            },
            caption = R.string.tool_clear,
        )
    }
}

@Composable
private fun WorkspaceTool(
    icon: Int,
    label: Int,
    enabled: Boolean,
    modifier: Modifier,
    active: Boolean = false,
    caption: Int = label,
) {
    val color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else .38f)
    Row(
        modifier
            .heightIn(min = 48.dp)
            .background(
                if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), stringResource(label), Modifier.size(20.dp), tint = color)
        Text(
            stringResource(caption),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/** Feedback stays above the fixed action row, outside the scrolling drawing area. */
@Composable
internal fun PracticeFeedback(state: PracticeUiState) {
    val notice =
        when {
            state.notice == PracticeNotice.CheckerUnavailable -> R.string.checker_unavailable
            state.practice.cells.any { it.referenceUnavailable } -> R.string.reference_unavailable
            else -> null
        }
    notice?.let {
        Text(
            stringResource(it),
            modifier =
                Modifier.fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("practice-notice"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun PracticeActions(
    enabled: Boolean,
    complete: Boolean,
    checking: Boolean,
    onAction: (PracticeAction) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(
            { onAction(PracticeAction.Skip) },
            Modifier.weight(1f).heightIn(min = 48.dp),
            enabled,
            colors =
                ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
        ) {
            Text(stringResource(R.string.skip))
        }
        Button(
            { onAction(PracticeAction.Submit) },
            Modifier.widthIn(min = 124.dp).heightIn(min = 48.dp),
            enabled = enabled && !checking && complete,
        ) {
            Text(stringResource(R.string.next))
        }
    }
}

internal fun PracticeCell.padContent(): PadContent =
    when (status) {
        EntryStatus.Prefilled -> reference?.let(PadContent::Reference) ?: PadContent.Empty
        EntryStatus.Empty -> PadContent.Empty
        else -> PadContent.UserInk(sample)
    }

@Composable
internal fun statusLabel(status: EntryStatus): String =
    stringResource(
        when (status) {
            EntryStatus.Empty -> R.string.empty
            EntryStatus.Editing -> R.string.editing
            EntryStatus.Correct -> R.string.correct
            EntryStatus.Incorrect -> R.string.incorrect
            EntryStatus.Prefilled -> R.string.prefilled
        },
    )

@Preview(showBackground = true, widthDp = 392, heightDp = 844)
@Composable
private fun PracticePreview() {
    MaterialTheme {
        val task =
            PracticeTask(
                "preview",
                "Япония",
                listOf(
                    KanjiTask("first", KanjiId("sun"), EntryMode.User),
                    KanjiTask("second", KanjiId("book"), EntryMode.Prefilled),
                ),
            )
        PracticeScreen(PracticeUiState(HandwritingTraining(task).state), {}, {})
    }
}


