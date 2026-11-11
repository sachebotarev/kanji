package ru.mobui.kanji.feature.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ru.mobui.kanji.domain.practice.TrainingSymbolStatus
import ru.mobui.kanji.domain.practice.practiceSymbolPoints

@Composable
internal fun HintDetailsDialog(
    remaining: Int,
    max: Int,
    used: Int,
    exhausted: Boolean,
    shapeAcceptedWithPenalty: Boolean = false,
    onClose: () -> Unit,
) {
    val feedbackColor = when (hintUsageTone(used, max, accepted = false)) {
        PracticeFeedbackTone.Exhausted -> MaterialTheme.colorScheme.error
        PracticeFeedbackTone.Assisted -> assistedFeedbackColor()
        else -> MaterialTheme.colorScheme.onSurface
    }
    val scorePercent =
        (100 * practiceSymbolPoints(TrainingSymbolStatus.Correct, used, max) *
            if (shapeAcceptedWithPenalty) ru.mobui.kanji.domain.practice.SHAPE_SOFT_ACCEPTANCE_FACTOR else 1.0).roundToInt()
    AlertDialog(
        modifier = Modifier.testTag("hint-details-dialog"),
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.check_help)) },
        text = {
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HintDetailRow(
                    label = stringResource(R.string.hint_remaining_label),
                    value = stringResource(R.string.hint_count_of_max, if (exhausted) 0 else remaining, max),
                    color = feedbackColor,
                )
                HintDetailRow(
                    label = stringResource(R.string.hint_score_label),
                    value = stringResource(R.string.check_match_percent, scorePercent),
                    color = feedbackColor,
                )
                if (used > 0 || shapeAcceptedWithPenalty) {
                    Text(
                        text = stringResource(R.string.hint_penalty_reasons_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Column(
                        Modifier.testTag("hint-penalty-reasons"),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (used > 0) {
                            Text(
                                text = "• " + stringResource(R.string.hint_penalty_hints, used),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (shapeAcceptedWithPenalty) {
                            val shapePenaltyPercent = ((1.0 - ru.mobui.kanji.domain.practice.SHAPE_SOFT_ACCEPTANCE_FACTOR) * 100).roundToInt()
                            Text(
                                text = "• " + stringResource(R.string.hint_penalty_shape, shapePenaltyPercent),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else if (max == 0) {
                    Text(
                        text = stringResource(R.string.hint_explanation_disabled),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.hint_explanation_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClose, Modifier.testTag("close-hint-details")) {
                Text(stringResource(R.string.check_understood))
            }
        },
    )
}

@Composable
private fun HintDetailRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f))
        Text(value, color = color)
    }
}
