package ru.mobui.kanji.feature.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import ru.mobui.kanji.domain.practice.CheckFailure
import ru.mobui.kanji.domain.practice.CheckKind
import ru.mobui.kanji.domain.practice.CheckReport
import ru.mobui.kanji.domain.practice.CheckVerdict
import ru.mobui.kanji.domain.practice.EntryMode
import ru.mobui.kanji.domain.practice.PracticeCell
import ru.mobui.kanji.domain.practice.StrokeRuleRelation

/** Learner-facing check summary for one indicator. No settings or debug entry points. */
@Composable
internal fun CheckDetailsDialog(
    kind: CheckKind,
    report: CheckReport?,
    cell: PracticeCell?,
    onClose: () -> Unit,
) {
    val result = report?.checks?.firstOrNull { it.kind == kind }
    val failures = result?.failures?.takeIf { it.isNotEmpty() } ?: report?.failures.orEmpty()
    val title =
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
                result == null -> R.string.check_pending
                result.verdict == null -> R.string.check_disabled
                else ->
                    when (result.verdict) {
                        CheckVerdict.Correct -> if (result.acceptedWithPenalty) R.string.check_accepted_with_penalty else R.string.check_passed
                        CheckVerdict.Incorrect -> R.string.check_failed
                        CheckVerdict.Incomplete -> R.string.check_incomplete
                        else -> R.string.check_unavailable
                    }
            },
        )
    AlertDialog(
        modifier = Modifier.testTag("check-details-dialog"),
        onDismissRequest = onClose,
        title = { Text(title) },
        text = {
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    status,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (result?.acceptedWithPenalty == true) assistedFeedbackColor() else MaterialTheme.colorScheme.onSurface,
                )
                if (kind == CheckKind.Shape && result?.acceptedWithPenalty == true) {
                    Text(stringResource(R.string.check_shape_soft_explanation, ((1.0 - ru.mobui.kanji.domain.practice.SHAPE_SOFT_ACCEPTANCE_FACTOR) * 100).roundToInt()), style = MaterialTheme.typography.bodyMedium)
                }
                when (kind) {
                    CheckKind.Readability -> {
                        if (result?.verdict != null) {
                            val expected =
                                cell
                                    ?.task
                                    ?.kanjiId
                                    ?.value
                                    ?.removePrefix("u")
                                    ?.toIntOrNull(UNICODE_HEX_RADIX)
                                    ?.takeIf { Character.isValidCodePoint(it) }
                                    ?.let { String(Character.toChars(it)) }
                            // Inspecting a check must not reveal the answer before a hint was used.
                            expected
                                ?.takeIf {
                                    cell?.hintUsed == true ||
                                        cell?.task?.mode == EntryMode.Prefilled
                                }
                                ?.let { DetailRow(stringResource(R.string.check_expected), it) }
                            DetailRow(
                                stringResource(R.string.check_recognized),
                                result.details?.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.check_no_candidate),
                            )
                        }
                    }
                    CheckKind.Shape -> {
                        val actual = cell?.sample?.strokeCount ?: 0
                        val expected = cell?.reference?.strokes?.size
                        DetailRow(
                            stringResource(R.string.check_quantity),
                            expected?.let {
                                stringResource(R.string.check_count_value, actual, it)
                            } ?: actual.toString(),
                            if (result?.verdict != null && expected != null) {
                                if (actual == expected) CheckVerdict.Correct
                                else CheckVerdict.Incorrect
                            } else null,
                        )
                        result?.matchPercent?.let {
                            DetailRow(
                                stringResource(R.string.check_similarity),
                                stringResource(R.string.check_match_percent, it.roundToInt()),
                            )
                        }
                        if (result?.verdict != null) {
                            val used = report?.compensation?.budget?.totalPercent ?: 0f
                            DetailRow(
                                stringResource(R.string.check_compensation),
                                stringResource(R.string.check_match_percent, used.roundToInt()),
                                if (report?.compensation?.budget?.hasError == true) {
                                    CheckVerdict.Incorrect
                                } else {
                                    null
                                },
                            )
                        }
                        val reasons = mutableListOf<String>()
                        val strokeCountFailures = failures.filterIsInstance<CheckFailure.StrokeCount>()
                        if (strokeCountFailures.isNotEmpty()) {
                            strokeCountFailures.forEach { failure ->
                                reasons +=
                                    if (failure.actual < failure.expected) {
                                        stringResource(
                                            R.string.check_shape_incomplete_strokes,
                                            failure.actual,
                                            failure.expected,
                                        )
                                    } else {
                                        stringResource(
                                            R.string.check_shape_extra_strokes,
                                            failure.actual,
                                            failure.expected,
                                        )
                                    }
                            }
                        } else if (expected != null && actual != expected && result?.verdict != CheckVerdict.Correct) {
                            reasons +=
                                if (actual < expected) {
                                    stringResource(R.string.check_shape_incomplete_strokes, actual, expected)
                                } else {
                                    stringResource(R.string.check_shape_extra_strokes, actual, expected)
                                }
                        }
                        failures.filterIsInstance<CheckFailure.ShapeCoverage>().forEach { failure ->
                            reasons +=
                                stringResource(
                                    R.string.check_shape_low_similarity,
                                    failure.actualPercent.roundToInt(),
                                    failure.requiredPercent.roundToInt(),
                                )
                        }
                        failures.filterIsInstance<CheckFailure.MissingHooks>().forEach { failure ->
                            val strokes = failure.referenceStrokes.joinToString(", ")
                            reasons += stringResource(R.string.check_shape_missing_hook, strokes)
                        }
                        failures.filterIsInstance<CheckFailure.KanjiRules>().forEach { failure ->
                            failure.violations.forEach { violation ->
                                val resId =
                                    when (violation.rule.relation) {
                                        StrokeRuleRelation.Crosses -> R.string.check_shape_rule_crosses
                                        StrokeRuleRelation.EndsAt -> R.string.check_shape_rule_ends_at
                                        StrokeRuleRelation.ShorterThan -> R.string.check_shape_rule_shorter_than
                                    }
                                reasons +=
                                    stringResource(
                                        resId,
                                        violation.rule.firstStroke,
                                        violation.rule.secondStroke,
                                    )
                            }
                        }
                        var hasDistortion = false
                        failures.filterIsInstance<CheckFailure.Budget>().forEach {
                            hasDistortion = true
                            reasons += stringResource(R.string.check_shape_budget_exceeded)
                        }
                        failures.filterIsInstance<CheckFailure.Rotation>().forEach {
                            hasDistortion = true
                            reasons += stringResource(R.string.check_shape_rotation_exceeded)
                        }
                        failures.filterIsInstance<CheckFailure.Shear>().forEach {
                            hasDistortion = true
                            reasons += stringResource(R.string.check_shape_shear_exceeded)
                        }
                        failures.filterIsInstance<CheckFailure.Size>().forEach {
                            hasDistortion = true
                            reasons += stringResource(R.string.check_shape_size_exceeded)
                        }
                        failures.filterIsInstance<CheckFailure.Stretch>().forEach {
                            hasDistortion = true
                            reasons += stringResource(R.string.check_shape_stretch_exceeded)
                        }
                        failures.filterIsInstance<CheckFailure.Translation>().forEach {
                            hasDistortion = true
                            reasons += stringResource(R.string.check_shape_translation_exceeded)
                        }
                        if (!hasDistortion && report?.compensation?.budget?.hasError == true) {
                            reasons += stringResource(R.string.check_shape_budget_exceeded)
                        }
                        if (reasons.isEmpty() && result?.verdict != null && result.verdict != CheckVerdict.Correct) {
                            reasons += stringResource(R.string.check_shape_general_mismatch)
                        }
                        if (result?.verdict != CheckVerdict.Correct && reasons.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.check_reasons_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Column(
                                Modifier.testTag("check-shape-reasons"),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                reasons.distinct().forEach { reason ->
                                    Text(
                                        "• $reason",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                    CheckKind.StrokeWriting -> {
                        val actual = cell?.sample?.strokeCount ?: 0
                        val expected = cell?.reference?.strokes?.size
                        DetailRow(
                            stringResource(R.string.check_quantity),
                            expected?.let {
                                stringResource(R.string.check_count_value, actual, it)
                            } ?: actual.toString(),
                            if (result?.verdict != null && expected != null) {
                                if (actual == expected) CheckVerdict.Correct
                                else CheckVerdict.Incorrect
                            } else null,
                        )
                        val evaluated =
                            result?.verdict in
                                listOf(CheckVerdict.Correct, CheckVerdict.Incorrect) &&
                                expected != null &&
                                actual == expected
                        val order = failures.filterIsInstance<CheckFailure.StrokeOrder>()
                        val direction = failures.filterIsInstance<CheckFailure.StrokeDirection>()
                        DetailRow(
                            stringResource(R.string.check_order_label),
                            stringResource(
                                when {
                                    !evaluated -> R.string.check_not_evaluated
                                    order.isNotEmpty() -> R.string.check_violated
                                    else -> R.string.check_passed
                                },
                            ),
                            if (evaluated) {
                                if (order.isEmpty()) CheckVerdict.Correct
                                else CheckVerdict.Incorrect
                            } else null,
                        )
                        if (evaluated && order.isNotEmpty()) {
                            Column(
                                Modifier.testTag("check-order-detail"),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                order
                                    .flatMap { it.mismatches }
                                    .distinct()
                                    .forEach { mismatch ->
                                        Text(
                                            stringResource(
                                                R.string.check_order_mismatch,
                                                mismatch.actualPosition,
                                                mismatch.expectedPosition,
                                            ),
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                            }
                        }
                        DetailRow(
                            stringResource(R.string.check_direction_label),
                            stringResource(
                                when {
                                    !evaluated -> R.string.check_not_evaluated
                                    direction.isNotEmpty() -> R.string.check_violated
                                    else -> R.string.check_passed
                                },
                            ),
                            if (evaluated) {
                                if (direction.isEmpty()) CheckVerdict.Correct
                                else CheckVerdict.Incorrect
                            } else null,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClose, Modifier.testTag("close-check-details")) {
                Text(stringResource(R.string.check_understood))
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String, verdict: CheckVerdict? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f))
        Text(
            value,
            Modifier.weight(1f),
            color =
                when (verdict) {
                    CheckVerdict.Correct -> MaterialTheme.colorScheme.primary
                    CheckVerdict.Incorrect -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
        )
    }
}

private const val UNICODE_HEX_RADIX = 16
