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
internal fun DebugDialog(state: PracticeUiState, onClose: () -> Unit) {
    val currentOnClose by rememberUpdatedState(onClose)
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp) {
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .padding(16.dp)
                    .testTag("debug-dialog"),
            ) {
                Row(
                    modifier =
                        Modifier.testTag("debug-drag-handle").pointerInput(Unit) {
                            var distance = 0f
                            detectVerticalDragGestures(
                                onDragStart = { distance = 0f },
                                onDragEnd = { if (distance > 24.dp.toPx()) currentOnClose() },
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    distance += amount
                                },
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.debug_title),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClose, Modifier.testTag("close-debug")) {
                        Text(stringResource(R.string.debug_close))
                    }
                }
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val cell = state.practice.selected
                    val identity =
                        stringResource(
                            R.string.debug_identity,
                            state.practice.task.taskId,
                            cell?.task?.occurrenceId ?: debugToken("none"),
                            cell?.task?.kanjiId?.value ?: debugToken("none"),
                            cell?.revision ?: 0L,
                        )
                    Text(
                        identity,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(
                            R.string.debug_timing,
                            state.practice.elapsedMs,
                            cell?.elapsedMs ?: 0,
                            debugToken((cell?.hintUsed == true).toString()),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        if (state.isChecking) stringResource(R.string.debug_checking)
                        else
                            state.debugReport?.verdict?.let { debugVerdict(it) }
                                ?: stringResource(R.string.debug_waiting),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (state.debugFailures.isNotEmpty() || state.debugCompensation != null) {
                        val lines = state.debugFailures.map { failure -> checkFailureText(failure) }
                        val correction = state.debugCompensation
                        val correctionLines =
                            if (correction == null) emptyList()
                            else
                                buildList {
                                    if (correction.shear.isApplied)
                                        add(
                                            stringResource(
                                                R.string.debug_shear,
                                                correction.shear.degrees,
                                            ),
                                        )
                                    if (correction.jitter.isApplied)
                                        add(
                                            stringResource(
                                                R.string.debug_jitter,
                                                correction.jitter.removedPoints,
                                                correction.jitter.maxDeviationPercent,
                                            ),
                                        )
                                    if (correction.size.isApplied)
                                        add(
                                            stringResource(
                                                R.string.debug_size,
                                                correction.size.changePercent,
                                                correction.size.scaleFactor,
                                            ),
                                        )
                                    if (
                                        correction.translation.isApplied ||
                                            correction.translation.hasError
                                    )
                                        add(
                                            stringResource(
                                                R.string.debug_translation,
                                                correction.translation.shiftXPercent,
                                                correction.translation.shiftYPercent,
                                                correction.translation.limitPercent,
                                            ),
                                        )
                                    if (
                                        kotlin.math.abs(correction.rotationDegrees) >
                                            DEBUG_COMPENSATION_EPSILON
                                    )
                                        add(
                                            stringResource(
                                                R.string.debug_rotation,
                                                correction.rotationDegrees,
                                            ) + " [${stringResource(R.string.pivot_centroid)}]",
                                        )
                                    if (
                                        kotlin.math.abs(correction.scaleX - 1f) >
                                            DEBUG_COMPENSATION_EPSILON
                                    )
                                        add(
                                            stringResource(
                                                R.string.debug_scale_x,
                                                (correction.scaleX - 1f) * PERCENT_SCALE,
                                            ),
                                        )
                                    if (
                                        kotlin.math.abs(correction.scaleY - 1f) >
                                            DEBUG_COMPENSATION_EPSILON
                                    )
                                        add(
                                            stringResource(
                                                R.string.debug_scale_y,
                                                (correction.scaleY - 1f) * PERCENT_SCALE,
                                            ),
                                        )
                                }
                        Text(
                            (lines + correctionLines).joinToString("\n"),
                            Modifier.fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("practice-debug-feedback")
                                .semantics { liveRegion = LiveRegionMode.Polite },
                            color =
                                if (state.debugFailures.isNotEmpty())
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    state.debugReport?.metrics?.forEach { (key, value) ->
                        Text(
                            "${debugMetricLabel(key)}: ${debugMetricValue(key, value)}",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}


private const val DEBUG_COMPENSATION_EPSILON = .0001f

private const val PERCENT_SCALE = 100
