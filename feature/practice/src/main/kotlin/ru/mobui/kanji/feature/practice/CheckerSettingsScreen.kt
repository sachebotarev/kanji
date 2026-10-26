package ru.mobui.kanji.feature.practice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ru.mobui.kanji.core.writingpad.*
import ru.mobui.kanji.domain.practice.*

/** Stateless settings surface; host owns persistence, checker and navigation. */
@Composable
fun CheckerSettingsScreen(
    settings: RecognitionSettings,
    onChange: (RecognitionSettings) -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    preview: PracticeUiState? = null,
    readabilityAvailable: Boolean = true,
) {
    fun change(value: RecognitionSettings) = onChange(value.roundedParameters())
    Scaffold(modifier) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                TextButton(onBack, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.back)) }
                Text(stringResource(R.string.checker_settings_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton(onReset, Modifier.heightIn(min = 48.dp).testTag("reset-checker-settings")) { Text(stringResource(R.string.checker_settings_reset)) }
            }
            val cell = preview?.practice?.selected
            val report = preview?.currentCheckReport ?: preview?.debugReport
            if (cell != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KanjiWritingPad(PadUiState(
                        ContentVersion(cell.task.occurrenceId, cell.revision),
                        PadContent.UserInk(cell.sample), inputEnabled = false,
                        comparisonOverlay = report?.compensatedDrawing,
                    ), {}, Modifier.size(132.dp).testTag("settings-preview"))
                    Column(Modifier.weight(1f).padding(top = 8.dp)) {
                        Text(stringResource(R.string.checker_settings_preview), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(if (preview.isChecking) R.string.debug_checking else when (report?.verdict) {
                            CheckVerdict.Correct -> R.string.checker_preview_correct
                            CheckVerdict.Incorrect -> R.string.checker_preview_incorrect
                            else -> R.string.checker_preview_pending
                        }), Modifier.testTag("settings-verdict"))
                        report?.compensation?.budget?.let { budget ->
                            Text(stringResource(R.string.checker_settings_budget_value, budget.totalPercent), style = MaterialTheme.typography.bodySmall)
                        }
                        if (preview.isChecking) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                report?.failures?.forEach { failure -> Text(checkFailureText(failure), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (preview != null) Text(stringResource(R.string.checker_settings_live), style = MaterialTheme.typography.bodySmall)
                SettingsGroup(R.string.settings_group_shape) {
                    ParameterSlider(R.string.setting_match, R.string.help_match, settings.requiredMatchPercent, 1f..100f, "match") { change(settings.copy(requiredMatchPercent = it)) }
                    ParameterSlider(R.string.setting_distance, R.string.help_distance, settings.distanceTolerance * 100, 1f..15f, "distance") { change(settings.copy(distanceTolerance = it / 100)) }
                }
                SettingsGroup(R.string.settings_group_compensations) {
                    ParameterSlider(R.string.setting_translation_limit, R.string.help_translation, settings.maxTranslationPercent, 0f..100f, "translation") { change(settings.copy(maxTranslationPercent = it)) }
                    ParameterSlider(R.string.setting_size_limit, R.string.help_size, settings.maxSizeCompensationPercent ?: 30f, 0f..200f, "size") { change(settings.copy(maxSizeCompensationPercent = it)) }
                    ParameterSlider(R.string.setting_stretch_x, R.string.help_stretch_x, settings.maxHorizontalStretchPercent, 0f..100f, "stretch-x") { change(settings.copy(maxHorizontalStretchPercent = it)) }
                    ParameterSlider(R.string.setting_stretch_y, R.string.help_stretch_y, settings.maxVerticalStretchPercent, 0f..100f, "stretch-y") { change(settings.copy(maxVerticalStretchPercent = it)) }
                    ParameterSlider(R.string.setting_rotation_limit, R.string.help_rotation, settings.maxRotationDegrees, 0f..20f, "rotation", unit = R.string.setting_degrees_value) { change(settings.copy(maxRotationDegrees = it)) }
                    ParameterSlider(R.string.setting_shear_limit, R.string.help_shear, settings.maxShearDegrees ?: 0f, 0f..20f, "shear", unit = R.string.setting_degrees_value) { change(settings.copy(maxShearDegrees = it)) }
                    ParameterSlider(R.string.setting_jitter_limit, R.string.help_jitter, settings.jitterTolerancePercent, 0f..5f, "jitter", increment = .1f) { change(settings.copy(jitterTolerancePercent = it)) }
                }
                SettingsGroup(R.string.settings_group_budget) {
                    ParameterSlider(R.string.setting_budget, R.string.help_budget, settings.maxCompensationBudgetPercent ?: 0f, 0f..100f, "budget") {
                        change(settings.copy(maxCompensationBudgetPercent = it.takeIf { amount -> amount > 0f }))
                    }
                }
                SettingsGroup(R.string.checks_testing) {
                    RuleChoiceRow(R.string.check_readability, R.string.help_readability, settings.readabilityEnabled && readabilityAvailable, "enable-readability", allowEnable = readabilityAvailable) { change(settings.copy(readabilityEnabled = it)) }
                    if (!readabilityAvailable) {
                        Text(stringResource(R.string.readability_model_required),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("readability-model-required"))
                    }
                    RuleChoiceRow(R.string.check_shape, R.string.help_shape, settings.shapeEnabled, "enable-shape") { change(settings.copy(shapeEnabled = it)) }
                    RuleChoiceRow(R.string.check_writing, R.string.help_writing, settings.strokeWritingEnabled, "enable-writing") { change(settings.copy(strokeWritingEnabled = it)) }
                }
                SettingsGroup(R.string.settings_group_rules) {
                    RuleChoiceRow(R.string.setting_structure, R.string.help_structure, settings.checkStrokeStructure, "check-structure") { change(settings.copy(checkStrokeStructure = it)) }
                    if (settings.checkStrokeStructure) {
                        ParameterSlider(R.string.setting_length_difference, R.string.help_length_difference, settings.lengthDifferenceTolerancePercent, 0f..30f, "length-difference") { change(settings.copy(lengthDifferenceTolerancePercent = it)) }
                        ParameterSlider(R.string.setting_connection, R.string.help_connection, settings.connectionTolerancePercent, 0f..10f, "connection") { change(settings.copy(connectionTolerancePercent = it)) }
                        ParameterSlider(R.string.setting_protrusion, R.string.help_protrusion, settings.protrusionTolerancePercent, 0f..10f, "protrusion") { change(settings.copy(protrusionTolerancePercent = it)) }
                    }

                }

            }
        }
    }
}

@Composable
private fun SettingsGroup(title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider()
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun RuleChoiceRow(label: Int, help: Int, checked: Boolean, tag: String, allowEnable: Boolean = true, onChange: (Boolean) -> Unit) {
    val title = stringResource(label)
    val check = stringResource(R.string.setting_rule_check)
    val ignore = stringResource(R.string.setting_rule_ignore)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SettingHelp(label, help, tag)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((value, text) in listOf(true to check, false to ignore)) {
                val active = value == checked
                OutlinedButton(
                    onClick = { if (!active) onChange(value) },
                    enabled = !value || allowEnable,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .testTag(if (value) tag else "$tag-off")
                        .semantics { selected = active; contentDescription = "$title: $text" },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                        contentColor = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    ),
                ) { Text(text) }
            }
        }
    }
}

@Composable
private fun ParameterSlider(
    label: Int,
    help: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    tag: String,
    enabled: Boolean = true,
    unit: Int = R.string.setting_percent_value,
    increment: Float = 1f,
    onChange: (Float) -> Unit,
) {
    val title = stringResource(label)
    fun snapped(raw: Float) = ((raw / increment).roundToInt() * increment).coerceIn(range)
    val displayed = snapped(value)
    Column {
        val formatted = if (increment == 1f) stringResource(unit, displayed.roundToInt())
            else stringResource(R.string.setting_tenths_percent_value, displayed)
        SettingHelp(label, help, tag, value = formatted)
        Slider(
            value = displayed,
            onValueChange = { onChange(snapped(it)) },
            modifier = Modifier.fillMaxWidth().testTag("parameter-$tag").semantics { contentDescription = title },
            enabled = enabled,
            valueRange = range,
            steps = ((range.endInclusive - range.start) / increment).roundToInt() - 1,
        )
    }
}
