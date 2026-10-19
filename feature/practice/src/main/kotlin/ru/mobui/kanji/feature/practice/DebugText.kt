package ru.mobui.kanji.feature.practice

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import ru.mobui.kanji.domain.practice.CheckVerdict
import java.text.NumberFormat

/** Localize at the presentation boundary; diagnostic keys remain stable for tooling. */
private val metricLabels = mapOf(
    "pipelineType" to R.string.metric_pipeline_type,
    "coverage.global.percent" to R.string.metric_coverage_global_percent,
    "coverage.strokes.percent" to R.string.metric_coverage_strokes_percent,
    "coverage.gain.percent" to R.string.metric_coverage_gain_percent,
    "hooks.referenceAmbiguous" to R.string.metric_hooks_reference_ambiguous,
    "algorithm" to R.string.metric_algorithm,
    "input.strokes" to R.string.metric_input_strokes,
    "reference.strokes" to R.string.metric_reference_strokes,
    "input.points" to R.string.metric_input_points,
    "reference.points" to R.string.metric_reference_points,
    "input.bounds" to R.string.metric_input_bounds,
    "input.width" to R.string.metric_input_width,
    "input.height" to R.string.metric_input_height,
    "input.aspect" to R.string.metric_input_aspect,
    "input.strokeDuration.ms" to R.string.metric_input_strokeduration_ms,
    "input.tools" to R.string.metric_input_tools,
    "enabledCompensations" to R.string.metric_enabledcompensations,
    "checkStrokeStructure" to R.string.metric_checkstrokestructure,
    "checkStrokeOrder" to R.string.metric_checkstrokeorder,
    "checkStrokeDirection" to R.string.metric_checkstrokedirection,
    "connectionTolerancePercent" to R.string.metric_connectiontolerancepercent,
    "protrusionTolerancePercent" to R.string.metric_protrusiontolerancepercent,
    "lengthDifferenceTolerancePercent" to R.string.metric_lengthdifferencetolerancepercent,
    "dictionary.rules" to R.string.metric_dictionary_rules,
    "maxCompensationBudgetPercent" to R.string.metric_maxcompensationbudgetpercent,
    "jitterTolerancePercent" to R.string.metric_jittertolerancepercent,
    "maxSizeCompensationPercent" to R.string.metric_maxsizecompensationpercent,
    "maxTranslationPercent" to R.string.metric_maxtranslationpercent,
    "requiredMatchPercent" to R.string.metric_requiredmatchpercent,
    "distanceTolerance" to R.string.metric_distancetolerance,
    "maxHorizontalStretchPercent" to R.string.metric_maxhorizontalstretchpercent,
    "maxVerticalStretchPercent" to R.string.metric_maxverticalstretchpercent,
    "maxShearDegrees" to R.string.metric_maxsheardegrees,
    "maxRotationDegrees" to R.string.metric_maxrotationdegrees,
    "endpointArtifactTolerance" to R.string.metric_endpointartifacttolerance,
    "dictionary.violations" to R.string.metric_dictionary_violations,
    "strokeOrder.mismatches" to R.string.metric_strokeorder_mismatches,
    "hooks.required" to R.string.metric_hooks_required,
    "hooks.missing" to R.string.metric_hooks_missing,
    "strokeDirection.reversed" to R.string.metric_strokedirection_reversed,
    "coverage.baseline.percent" to R.string.metric_coverage_baseline_percent,
    "coverage.beforeEndpointCleanup.percent" to R.string.metric_coverage_beforeendpointcleanup_percent,
    "coverage.final.percent" to R.string.metric_coverage_final_percent,
    "coverage.input.percent" to R.string.metric_coverage_input_percent,
    "coverage.reference.percent" to R.string.metric_coverage_reference_percent,
    "coverage.margin.percent" to R.string.metric_coverage_margin_percent,
    "budget.shear.percent" to R.string.metric_budget_shear_percent,
    "budget.total.percent" to R.string.metric_budget_total_percent,
    "budget.translation.percent" to R.string.metric_budget_translation_percent,
    "budget.size.percent" to R.string.metric_budget_size_percent,
    "budget.stretch.percent" to R.string.metric_budget_stretch_percent,
    "budget.rotation.percent" to R.string.metric_budget_rotation_percent,
    "budget.jitter.percent" to R.string.metric_budget_jitter_percent,
    "budget.endpoints.percent" to R.string.metric_budget_endpoints_percent,
    "budget.hasError" to R.string.metric_budget_haserror,
    "shear.degrees" to R.string.metric_shear_degrees,
    "shear.factor" to R.string.metric_shear_factor,
    "shear.hasError" to R.string.metric_shear_haserror,
    "shear.isEstimated" to R.string.metric_shear_isestimated,
    "rotation.pivot" to R.string.metric_rotation_pivot,
    "rotation.hasError" to R.string.metric_rotation_haserror,
    "rotation.degrees" to R.string.metric_rotation_degrees,
    "jitter.removedPoints" to R.string.metric_jitter_removedpoints,
    "jitter.maxDeviation.percent" to R.string.metric_jitter_maxdeviation_percent,
    "stretch.x.percent" to R.string.metric_stretch_x_percent,
    "stretch.y.percent" to R.string.metric_stretch_y_percent,
    "stretch.hasError" to R.string.metric_stretch_haserror,
    "size.factor" to R.string.metric_size_factor,
    "size.change.percent" to R.string.metric_size_change_percent,
    "size.hasError" to R.string.metric_size_haserror,
    "size.isDefined" to R.string.metric_size_isdefined,
    "translation.x.percent" to R.string.metric_translation_x_percent,
    "translation.y.percent" to R.string.metric_translation_y_percent,
    "translation.hasError" to R.string.metric_translation_haserror,
    "strokeCountMatches" to R.string.metric_strokecountmatches,
    "evaluatedCandidates" to R.string.metric_evaluatedcandidates,
    "check.duration.ms" to R.string.metric_check_duration_ms,
)

@Composable
internal fun debugVerdict(verdict: CheckVerdict): String = stringResource(when (verdict) {
    CheckVerdict.Correct -> R.string.correct
    CheckVerdict.Incorrect -> R.string.incorrect
    CheckVerdict.Incomplete -> R.string.incomplete_short
    CheckVerdict.Unavailable -> R.string.debug_unavailable
})

@Composable
internal fun debugMetricLabel(key: String): String = metricLabels[key]?.let { stringResource(it) } ?: key

@Composable
internal fun debugToken(value: String): String = when (value) {
    "true" -> stringResource(R.string.debug_yes)
    "false" -> stringResource(R.string.debug_no)
    "none", "" -> stringResource(R.string.debug_none)
    "unrestricted" -> stringResource(R.string.debug_unrestricted)
    "undefined" -> stringResource(R.string.debug_undefined)
    "disabled" -> stringResource(R.string.debug_disabled)
    "Centroid" -> stringResource(R.string.pivot_centroid)
    "Cascade" -> stringResource(R.string.pipeline_cascade)
    "Procrustes" -> stringResource(R.string.pipeline_procrustes)
    "Center" -> stringResource(R.string.pivot_center)
    "TopLeft" -> stringResource(R.string.pivot_top_left)
    "TopRight" -> stringResource(R.string.pivot_top_right)
    "BottomLeft" -> stringResource(R.string.pivot_bottom_left)
    "BottomRight" -> stringResource(R.string.pivot_bottom_right)
    "Finger" -> stringResource(R.string.tool_finger)
    "Stylus" -> stringResource(R.string.tool_stylus)
    "Mouse" -> stringResource(R.string.tool_mouse)
    "Unknown" -> stringResource(R.string.tool_unknown)
    "Translation" -> stringResource(R.string.setting_translation)
    "Shear" -> stringResource(R.string.setting_shear)
    "Size" -> stringResource(R.string.setting_size)
    "Stretch" -> stringResource(R.string.setting_stretch)
    "Rotation" -> stringResource(R.string.setting_rotation)
    "Jitter" -> stringResource(R.string.setting_jitter)
    "Endpoints" -> stringResource(R.string.setting_endpoints)
    else -> value
}

@Composable
internal fun debugMetricValue(key: String, value: String): String {
    if (key == "algorithm") return stringResource(R.string.debug_algorithm)
    if (key == "dictionary.rules" && value != "none") {
        return value.split(", ").joinToString("\n") { it }.let { rules ->
            val parts = rules.split("\n").map { it.split(' ') }
            parts.map { rule ->
                val label = when (rule.getOrNull(1)) {
                    "Crosses" -> R.string.rule_crosses
                    "EndsAt" -> R.string.rule_ends_at
                    "ShorterThan" -> R.string.rule_shorter
                    else -> null
                }
                if (label == null) rule.joinToString(" ")
                else stringResource(label, rule.first().toInt(), rule.last().toInt())
            }.joinToString("\n")
        }
    }
    if (key == "enabledCompensations" || key == "input.tools")
        return value.split(", ").map { debugToken(it) }.joinToString(", ")
    val number = value.toDoubleOrNull()
    if (number != null && number.isFinite()) {
        return NumberFormat.getNumberInstance(LocalConfiguration.current.locales[0]).apply {
            maximumFractionDigits = 4
            isGroupingUsed = false
        }.format(number)
    }
    return debugToken(value)
}
