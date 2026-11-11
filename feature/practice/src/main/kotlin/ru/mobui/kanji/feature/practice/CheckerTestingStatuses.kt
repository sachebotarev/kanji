package ru.mobui.kanji.feature.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import ru.mobui.kanji.domain.practice.CheckKind
import ru.mobui.kanji.domain.practice.CheckReport
import ru.mobui.kanji.domain.practice.CheckVerdict
import ru.mobui.kanji.domain.practice.IndividualCheckResult

internal data class PracticeHintStatusState(
    val usedHints: Int,
    val maxHints: Int,
    val accepted: Boolean,
)

/** Color-coded result cards. Accessibility retains explicit status descriptions. */
@Composable
internal fun CheckerTestingStatuses(
    report: CheckReport?,
    checking: Boolean,
    modifier: Modifier = Modifier,
    strokeCount: Int = 0,
    enabled: Boolean = true,
    hintStatus: PracticeHintStatusState? = null,
    onOpen: (CheckKind) -> Unit = {},
    onOpenHint: () -> Unit = {},
) {
    Row(
        modifier.testTag("checker-testing-statuses"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (kind in CheckKind.entries) {
            val result = report?.checks?.firstOrNull { it.kind == kind }
            val label =
                stringResource(
                    when (kind) {
                        CheckKind.Readability -> R.string.check_readability
                        CheckKind.Shape -> R.string.check_shape
                        CheckKind.StrokeWriting -> R.string.check_writing_short
                    },
                )
            val status =
                stringResource(
                    when {
                        result != null && result.verdict == null -> R.string.check_disabled
                        checking -> R.string.check_running
                        result == null -> R.string.check_pending
                        else ->
                            when (result.verdict) {
                                CheckVerdict.Correct -> if (result.acceptedWithPenalty) R.string.check_accepted_with_penalty else R.string.check_passed
                                CheckVerdict.Incorrect -> R.string.check_failed
                                CheckVerdict.Incomplete -> R.string.check_incomplete
                                else -> R.string.check_unavailable
                            }
                    },
                )
            val verdict = result?.verdict.takeUnless { checking }
            val colors = MaterialTheme.colorScheme
            val valueColor =
                when (verdict) {
                    CheckVerdict.Correct -> if (result?.acceptedWithPenalty == true) assistedFeedbackColor() else colors.primary
                    CheckVerdict.Incorrect,
                    CheckVerdict.Unavailable -> colors.error
                    else -> colors.onSurfaceVariant
                }
            val detail =
                when (kind) {
                    CheckKind.Readability ->
                        result?.details?.takeIf { !checking && verdict != null && it.isNotEmpty() }
                    CheckKind.Shape ->
                        result
                            ?.matchPercent
                            ?.takeIf { !checking && verdict != null }
                            ?.let { stringResource(R.string.check_match_percent, it.roundToInt()) }
                    CheckKind.StrokeWriting -> strokeCount.toString()
                }
            // Fixed band height: a recognized kanji must never grow this row and resize the pad
            // (pad size changes cancel the active stroke in UI-PAD).
            Column(
                Modifier.weight(1f)
                    .height(STATUS_BAND_HEIGHT)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = enabled, role = Role.Button, onClickLabel = label) {
                        onOpen(kind)
                    }
                    .testTag("check-indicator-${kind.name}")
                    .clearAndSetSemantics {
                        contentDescription = label
                        stateDescription = status + detail?.let { ": $it" }.orEmpty()
                        role = Role.Button
                        if (enabled)
                            onClick(label = label) {
                                onOpen(kind)
                                true
                            }
                        else disabled()
                    }
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    label,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
                Box(
                    Modifier.fillMaxWidth().height(STATUS_VALUE_HEIGHT),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        detail ?: if (checking) "…" else "—",
                        color = valueColor,
                        style =
                            MaterialTheme.typography.titleLarge.copy(lineHeight = 28.sp),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
        if (hintStatus != null) {
            val label = stringResource(R.string.check_help)
            val used = hintStatus.usedHints
            val colors = MaterialTheme.colorScheme
            val valueColor =
                when (hintUsageTone(used, hintStatus.maxHints, hintStatus.accepted)) {
                    PracticeFeedbackTone.Exhausted -> colors.error
                    PracticeFeedbackTone.Assisted -> assistedFeedbackColor()
                    PracticeFeedbackTone.Success -> colors.primary
                    PracticeFeedbackTone.Neutral -> colors.onSurfaceVariant
                }
            val displayValue = stringResource(R.string.hint_usage_fraction, used, hintStatus.maxHints)
            val statusDescription = stringResource(R.string.hint_usage_description, used, hintStatus.maxHints)

            Column(
                Modifier.weight(1f)
                    .height(STATUS_BAND_HEIGHT)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        enabled = enabled,
                        role = Role.Button,
                        onClickLabel = label,
                    ) {
                        onOpenHint()
                    }
                    .testTag("check-indicator-hint")
                    .clearAndSetSemantics {
                        contentDescription = label
                        stateDescription = statusDescription
                        role = Role.Button
                        if (enabled) {
                            onClick(label = label) {
                                onOpenHint()
                                true
                            }
                        } else {
                            disabled()
                        }
                    }
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    label,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
                Box(
                    Modifier.fillMaxWidth().height(STATUS_VALUE_HEIGHT),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        displayValue,
                        color = valueColor,
                        style =
                            MaterialTheme.typography.titleLarge.copy(lineHeight = 28.sp),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

internal val STATUS_BAND_HEIGHT = 52.dp
internal val STATUS_VALUE_HEIGHT = 28.dp

@Preview
@Composable
private fun CheckerIndicatorsPreview() {
    MaterialTheme {
        CheckerTestingStatuses(
            CheckReport(
                CheckVerdict.Incorrect,
                checks =
                    listOf(
                        IndividualCheckResult(CheckKind.Readability, CheckVerdict.Correct, "日"),
                        IndividualCheckResult(
                            CheckKind.Shape,
                            CheckVerdict.Incorrect,
                            matchPercent = 68f,
                        ),
                        IndividualCheckResult(CheckKind.StrokeWriting, null),
                    ),
            ),
            false,
            strokeCount = 4,
        )
    }
}
