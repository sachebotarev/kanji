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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
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

/** Pure screen: navigation, state ownership and providers live outside this composable. */
@Composable
fun PracticeScreen(
    state: PracticeUiState,
    onAction: (PracticeAction) -> Unit,
    onPadEvent: (PadEvent) -> Unit,
    modifier: Modifier = Modifier,
    activeStroke: () -> InkStroke? = { null },
    wordLabel: String? = null,
    progressLabel: String? = null,
    onBack: (() -> Unit)? = null,
) {
    val practice = state.practice
    val selected = practice.selected
    var openCheck by
        remember(selected?.task?.occurrenceId, selected?.revision) {
            mutableStateOf<CheckKind?>(null)
        }
    LaunchedEffect(state.gestureActive) { if (state.gestureActive) openCheck = null }
    val context =
        PracticeScreenContext(
            state,
            onAction,
            onPadEvent,
            activeStroke,
            wordLabel,
            progressLabel,
            onBack = onBack,
        )
    Scaffold(modifier = modifier, bottomBar = { PracticeBottomBar(context) }) { padding ->
        PracticeContentLayout(context, padding) { openCheck = it }
    }
    if (openCheck != null && !state.gestureActive) {
        PracticeDetails(context, openCheck!!, { openCheck = null })
    }
}

@Composable
internal fun PracticeStatus(
    report: CheckReport?,
    checking: Boolean,
    strokeCount: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onOpen: (CheckKind) -> Unit,
) {
    // Fixed-height host so check details never push/shrink the writing pad.
    Row(
        modifier.height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckerTestingStatuses(
            report,
            checking,
            Modifier.fillMaxWidth(),
            strokeCount = strokeCount,
            enabled = enabled,
            onOpen = onOpen,
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
    checking: Boolean,
    undoEnabled: Boolean,
    clearEnabled: Boolean,
    hintActive: Boolean,
    hintEnabled: Boolean,
    hintMode: ru.mobui.kanji.domain.practice.HintMode,
    strokeHintCount: Int = 0,
    maxNextStrokeHints: Int = 3,
    onAction: (PracticeAction) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val hintDescription =
        stringResource(
            when {
                hintActive &&
                    hintMode == ru.mobui.kanji.domain.practice.HintMode.NextStroke ->
                    R.string.hint_next_stroke_active
                hintActive -> R.string.hint_active
                hintMode == ru.mobui.kanji.domain.practice.HintMode.NextStroke ->
                    R.string.hint_next_stroke_idle
                else -> R.string.hint_idle
            },
        )
    val hintIconTint = hintBudgetIconTint(strokeHintCount, maxNextStrokeHints)
    LaunchedEffect(hintActive) {
        if (hintActive) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PracticeRoundTool(
            icon = R.drawable.ic_undo,
            label = R.string.undo,
            enabled = undoEnabled,
            onClick = { onAction(PracticeAction.Undo) },
        )
        PracticeRoundTool(
            icon = R.drawable.ic_clear,
            label = R.string.clear,
            enabled = clearEnabled,
            onClick = { onAction(PracticeAction.Clear) },
        )
        PracticeRoundTool(
            icon = R.drawable.ic_hint,
            label = R.string.hint,
            enabled = hintEnabled,
            // Keep the tonal background stable; only the bulb tint shows budget warnings.
            active = false,
            iconTint = hintIconTint,
            modifier =
                Modifier.testTag("trace-hint").semantics {
                    // Full-character hint is momentary — never latch the control as checked.
                    if (hintMode == ru.mobui.kanji.domain.practice.HintMode.FullCharacter) {
                        role = Role.Button
                    } else {
                        role = Role.Checkbox
                        toggleableState =
                            if (hintActive) ToggleableState.On else ToggleableState.Off
                    }
                    stateDescription = hintDescription
                },
            onClick = { onAction(PracticeAction.ToggleHint) },
        )
        Button(
            onClick = { onAction(PracticeAction.Submit) },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            enabled = enabled && !checking,
        ) {
            Text(stringResource(R.string.next), maxLines = 1)
        }
    }
}

private val HINT_BUDGET_CRITICAL_TINT = Color(0xFFE53935)
private val HINT_BUDGET_WARNING_TINT = Color(0xFFFFC107)

internal fun hintBudgetIconTint(used: Int, max: Int): Color? {
    if (max <= 0) return HINT_BUDGET_CRITICAL_TINT
    return when {
        used >= max -> HINT_BUDGET_CRITICAL_TINT
        used >= (max + 1) / 2 -> HINT_BUDGET_WARNING_TINT
        else -> null
    }
}

@Composable
private fun PracticeRoundTool(
    icon: Int,
    label: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    iconTint: Color? = null,
) {
    val defaultContent =
        if (active) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSecondaryContainer
    val content = iconTint ?: defaultContent
    val colors =
        if (active) {
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = content,
                disabledContainerColor =
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                disabledContentColor = content.copy(alpha = 0.38f),
            )
        } else {
            IconButtonDefaults.filledTonalIconButtonColors(
                contentColor = content,
                disabledContentColor = content.copy(alpha = 0.38f),
            )
        }
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        enabled = enabled,
        colors = colors,
    ) {
        Icon(
            painterResource(icon),
            stringResource(label),
            Modifier.size(22.dp),
        )
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
                reading = "にほん",
            )
        PracticeScreen(PracticeUiState(HandwritingTraining(task).state), {}, {})
    }
}


