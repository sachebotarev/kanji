package ru.mobui.kanji.feature.practice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.domain.practice.*
import kotlin.math.roundToInt

/** Provider-specific details; an unevaluated subcheck never appears as passed. */
@Composable
internal fun CheckDetailsDialog(kind: CheckKind, report: CheckReport?, cell: PracticeCell?, onClose: () -> Unit,
    onOpenSettings: (() -> Unit)? = null, diagnostics: @Composable () -> Unit = {}) {
    val result = report?.checks?.firstOrNull { it.kind == kind }
    val failures = result?.failures.orEmpty()
    val title = stringResource(when (kind) {
        CheckKind.Readability -> R.string.check_readability
        CheckKind.Shape -> R.string.check_shape
        CheckKind.StrokeWriting -> R.string.check_writing_short
    })
    val status = stringResource(when {
        result == null -> R.string.check_pending
        result.verdict == null -> R.string.check_disabled
        else -> when (result.verdict) {
            CheckVerdict.Correct -> R.string.check_passed
            CheckVerdict.Incorrect -> R.string.check_failed
            CheckVerdict.Incomplete -> R.string.check_incomplete
            else -> R.string.check_unavailable
        }
    })
    AlertDialog(
        modifier = Modifier.testTag("check-details-dialog"), onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(status)
                when (kind) {
                    CheckKind.Readability -> {
                        if (result?.verdict != null) {
                            val expected = cell?.task?.kanjiId?.value?.removePrefix("u")?.toIntOrNull(16)
                                ?.takeIf { Character.isValidCodePoint(it) }?.let { String(Character.toChars(it)) }
                            // Inspecting a check must not reveal the answer before a hint was used.
                            expected?.takeIf { cell?.hintUsed == true || cell?.task?.mode == EntryMode.Prefilled }
                                ?.let { DetailRow(stringResource(R.string.check_expected), it) }
                            DetailRow(stringResource(R.string.check_recognized), result.details?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.check_no_candidate))
                        }
                    }
                    CheckKind.Shape -> {
                        result?.matchPercent?.let { DetailRow(stringResource(R.string.check_similarity),
                            stringResource(R.string.check_match_percent, it.roundToInt())) }
                        failures.forEach { Text(checkFailureText(it)) }
                    }
                    CheckKind.StrokeWriting -> {
                        val actual = cell?.sample?.strokeCount ?: 0
                        val expected = cell?.reference?.strokes?.size
                        DetailRow(stringResource(R.string.check_quantity), expected?.let {
                            stringResource(R.string.check_count_value, actual, it)
                        } ?: actual.toString(), if (result?.verdict != null && expected != null) {
                            if (actual == expected) CheckVerdict.Correct else CheckVerdict.Incorrect
                        } else null)
                        val evaluated = result?.verdict in listOf(CheckVerdict.Correct, CheckVerdict.Incorrect) &&
                            expected != null && actual == expected
                        val order = failures.filterIsInstance<CheckFailure.StrokeOrder>()
                        val direction = failures.filterIsInstance<CheckFailure.StrokeDirection>()
                        DetailRow(stringResource(R.string.check_order_label), stringResource(when {
                            !evaluated -> R.string.check_not_evaluated
                            order.isNotEmpty() -> R.string.check_violated
                            else -> R.string.check_passed
                        }), if (evaluated) { if (order.isEmpty()) CheckVerdict.Correct else CheckVerdict.Incorrect } else null)
                        DetailRow(stringResource(R.string.check_direction_label), stringResource(when {
                            !evaluated -> R.string.check_not_evaluated
                            direction.isNotEmpty() -> R.string.check_violated
                            else -> R.string.check_passed
                        }), if (evaluated) { if (direction.isEmpty()) CheckVerdict.Correct else CheckVerdict.Incorrect } else null)
                        if (failures.isNotEmpty()) HorizontalDivider()
                        failures.forEach { Text(checkFailureText(it)) }
                    }
                }
                diagnostics()
            }
        },
        dismissButton = {
            if (onOpenSettings != null) TextButton(onOpenSettings, Modifier.testTag("open-checker-settings")) {
                Text(stringResource(R.string.check_settings_short))
            }
        },
        confirmButton = { TextButton(onClose, Modifier.testTag("close-check-details")) { Text(stringResource(R.string.check_understood)) } },
    )
}

@Composable
private fun DetailRow(label: String, value: String, verdict: CheckVerdict? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f))
        Text(value, Modifier.weight(1f), color = when (verdict) {
            CheckVerdict.Correct -> MaterialTheme.colorScheme.primary
            CheckVerdict.Incorrect -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        })
    }
}
