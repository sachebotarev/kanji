package ru.mobui.kanji.feature.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import ru.mobui.kanji.domain.practice.*
import kotlin.math.roundToInt

/** Color-coded result cards. Accessibility retains explicit status descriptions. */
@Composable
internal fun CheckerTestingStatuses(report: CheckReport?, checking: Boolean, modifier: Modifier = Modifier, strokeCount: Int = 0, enabled: Boolean = true, onOpen: (CheckKind) -> Unit = {}) {
    Row(modifier.testTag("checker-testing-statuses"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (kind in CheckKind.entries) {
            val result = report?.checks?.firstOrNull { it.kind == kind }
            val label = stringResource(when (kind) {
                CheckKind.Readability -> R.string.check_readability
                CheckKind.Shape -> R.string.check_shape
                CheckKind.StrokeWriting -> R.string.check_writing_short
            })
            val status = stringResource(when {
                result != null && result.verdict == null -> R.string.check_disabled
                checking -> R.string.check_running
                result == null -> R.string.check_pending
                else -> when (result.verdict) {
                    CheckVerdict.Correct -> R.string.check_passed
                    CheckVerdict.Incorrect -> R.string.check_failed
                    CheckVerdict.Incomplete -> R.string.check_incomplete
                    else -> R.string.check_unavailable
                }
            })
            val verdict = result?.verdict.takeUnless { checking }
            val colors = MaterialTheme.colorScheme
            val valueColor = when (verdict) {
                CheckVerdict.Correct -> colors.primary
                CheckVerdict.Incorrect, CheckVerdict.Unavailable -> colors.error
                else -> colors.onSurfaceVariant
            }
            val detail = when (kind) {
                CheckKind.Readability -> result?.details?.takeIf { !checking && verdict != null && it.isNotEmpty() }
                CheckKind.Shape -> result?.matchPercent?.takeIf { !checking && verdict != null }
                    ?.let { stringResource(R.string.check_match_percent, it.roundToInt()) }
                CheckKind.StrokeWriting -> strokeCount.toString()
            }
            Column(
                Modifier.weight(1f).heightIn(min = 56.dp).clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = enabled, role = Role.Button, onClickLabel = label) { onOpen(kind) }.testTag("check-indicator-${kind.name}").clearAndSetSemantics {
                        contentDescription = label
                        stateDescription = status + detail?.let { ": $it" }.orEmpty()
                        role = Role.Button
                        if (enabled) onClick(label = label) { onOpen(kind); true } else disabled()
                    }.padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            ) {
                Text(label, color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Text(detail ?: if (checking) "…" else "—", color = valueColor,
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Preview
@Composable
private fun CheckerIndicatorsPreview() {
    MaterialTheme {
        CheckerTestingStatuses(CheckReport(CheckVerdict.Incorrect, checks = listOf(
            IndividualCheckResult(CheckKind.Readability, CheckVerdict.Correct, "日"),
            IndividualCheckResult(CheckKind.Shape, CheckVerdict.Incorrect, matchPercent = 68f),
            IndividualCheckResult(CheckKind.StrokeWriting, null),
        )), false, strokeCount = 4)
    }
}

