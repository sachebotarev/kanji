package ru.mobui.kanji.feature.practice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import ru.mobui.kanji.domain.practice.CheckFailure
import ru.mobui.kanji.domain.practice.StrokeRuleRelation

@Composable
internal fun checkFailureText(failure: CheckFailure): String =
    when (failure) {
        CheckFailure.StrokeCorrespondence -> stringResource(R.string.check_stroke_correspondence)
        is CheckFailure.KanjiRules ->
            failure.violations
                .map { violation ->
                    val rule = violation.rule
                    stringResource(
                        when (rule.relation) {
                            StrokeRuleRelation.Crosses -> R.string.rule_crosses
                            StrokeRuleRelation.EndsAt -> R.string.rule_ends_at
                            StrokeRuleRelation.ShorterThan -> R.string.rule_shorter
                        },
                        rule.firstStroke,
                        rule.secondStroke,
                    )
                }
                .joinToString("\n")
        is CheckFailure.RuleConfiguration ->
            stringResource(
                R.string.debug_rule_configuration,
                failure.expectedStrokeCount,
                failure.actualStrokeCount,
            )
        is CheckFailure.StrokeOrder ->
            stringResource(
                R.string.debug_order_error,
                failure.mismatches.joinToString { "${it.actualPosition} → ${it.expectedPosition}" },
            )
        is CheckFailure.MissingHooks ->
            stringResource(R.string.debug_missing_hooks, failure.referenceStrokes.joinToString())
        is CheckFailure.StrokeDirection ->
            stringResource(R.string.debug_direction_error, failure.reversedStrokes.joinToString())
        is CheckFailure.StrokeCount ->
            stringResource(R.string.debug_stroke_count, failure.actual, failure.expected)
        is CheckFailure.ShapeCoverage ->
            stringResource(
                R.string.debug_shape_coverage,
                failure.actualPercent,
                failure.requiredPercent,
            )
        else -> compensationFailureText(failure)
    }

@Composable
private fun compensationFailureText(failure: CheckFailure): String =
    when (failure) {
        is CheckFailure.Shear ->
            stringResource(
                R.string.debug_shear_error,
                failure.compensation.degrees,
                failure.compensation.limitDegrees ?: 0f,
            )
        is CheckFailure.Budget ->
            stringResource(
                R.string.debug_budget_error,
                failure.budget.totalPercent,
                failure.budget.limitPercent ?: 0f,
            )
        is CheckFailure.Rotation ->
            stringResource(
                R.string.debug_rotation_error,
                failure.compensation.degrees,
                failure.compensation.limitDegrees,
            )
        is CheckFailure.Stretch ->
            if (!failure.compensation.isDefined) stringResource(R.string.debug_stretch_undefined)
            else
                stringResource(
                    R.string.debug_stretch_error,
                    failure.compensation.changeXPercent,
                    failure.compensation.changeYPercent,
                    failure.compensation.horizontalLimitPercent,
                    failure.compensation.verticalLimitPercent,
                )
        is CheckFailure.Size ->
            if (!failure.compensation.isDefined) stringResource(R.string.debug_size_undefined)
            else
                stringResource(
                    R.string.debug_size_error,
                    kotlin.math.abs(failure.compensation.changePercent),
                    failure.compensation.limitPercent ?: 0f,
                )
        is CheckFailure.Translation ->
            stringResource(
                R.string.debug_translation_error,
                failure.compensation.maxShiftPercent,
                failure.compensation.limitPercent,
            )
        else -> error("Expected a compensation failure")
    }
