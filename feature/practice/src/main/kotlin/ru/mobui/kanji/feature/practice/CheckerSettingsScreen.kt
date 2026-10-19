package ru.mobui.kanji.feature.practice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
) {
    val defaultBudget = requireNotNull(RecognitionSettings.touchInput().maxCompensationBudgetPercent)
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
                Text(stringResource(R.string.checker_settings_live), style = MaterialTheme.typography.bodySmall)
                ToggleRow(R.string.setting_structure, settings.checkStrokeStructure, "check-structure") { onChange(settings.copy(checkStrokeStructure = it)) }
                if (settings.checkStrokeStructure) {
                    ParameterSlider(R.string.setting_length_difference, settings.lengthDifferenceTolerancePercent, 0f..30f, "length-difference") { onChange(settings.copy(lengthDifferenceTolerancePercent = it)) }
                    ParameterSlider(R.string.setting_connection, settings.connectionTolerancePercent, 0f..10f, "connection") { onChange(settings.copy(connectionTolerancePercent = it)) }
                    ParameterSlider(R.string.setting_protrusion, settings.protrusionTolerancePercent, 0f..10f, "protrusion") { onChange(settings.copy(protrusionTolerancePercent = it)) }
                }
                ToggleRow(R.string.setting_order, settings.checkStrokeOrder, "check-order") { onChange(settings.copy(checkStrokeOrder = it)) }
                ToggleRow(R.string.setting_direction, settings.checkStrokeDirection, "check-direction") { onChange(settings.copy(checkStrokeDirection = it)) }
                ParameterSlider(R.string.setting_match, settings.requiredMatchPercent, 1f..100f, "match") { onChange(settings.copy(requiredMatchPercent = it)) }
                ParameterSlider(R.string.setting_distance, settings.distanceTolerance * 100, .1f..15f, "distance") { onChange(settings.copy(distanceTolerance = it / 100)) }
                ToggleRow(R.string.setting_budget_limit, settings.maxCompensationBudgetPercent != null, "budget-limit") {
                    onChange(settings.copy(maxCompensationBudgetPercent = if (it) defaultBudget else null))
                }
                ParameterSlider(R.string.setting_budget, settings.maxCompensationBudgetPercent ?: defaultBudget, 0f..100f, "budget", settings.maxCompensationBudgetPercent != null) {
                    onChange(settings.copy(maxCompensationBudgetPercent = it))
                }
                StageSection(CompensationStage.Translation, R.string.setting_translation, settings, onChange) {
                    ParameterSlider(R.string.setting_translation_limit, settings.maxTranslationPercent, 0f..100f, "translation") { onChange(settings.copy(maxTranslationPercent = it)) }
                }
                StageSection(CompensationStage.Shear, R.string.setting_shear, settings, onChange) {
                    ParameterSlider(R.string.setting_shear_limit, settings.maxShearDegrees ?: 5f, 0f..20f, "shear", unit = R.string.setting_degrees_value) { onChange(settings.copy(maxShearDegrees = it)) }
                }
                StageSection(CompensationStage.Size, R.string.setting_size, settings, onChange) {
                    ToggleRow(R.string.setting_size_limit_enabled, settings.maxSizeCompensationPercent != null, "size-limit") { onChange(settings.copy(maxSizeCompensationPercent = if (it) 30f else null)) }
                    ParameterSlider(R.string.setting_size_limit, settings.maxSizeCompensationPercent ?: 30f, 0f..200f, "size", settings.maxSizeCompensationPercent != null) { onChange(settings.copy(maxSizeCompensationPercent = it)) }
                }
                StageSection(CompensationStage.Stretch, R.string.setting_stretch, settings, onChange) {
                    ParameterSlider(R.string.setting_stretch_x, settings.maxHorizontalStretchPercent, 0f..100f, "stretch-x") { onChange(settings.copy(maxHorizontalStretchPercent = it)) }
                    ParameterSlider(R.string.setting_stretch_y, settings.maxVerticalStretchPercent, 0f..100f, "stretch-y") { onChange(settings.copy(maxVerticalStretchPercent = it)) }
                }
                StageSection(CompensationStage.Rotation, R.string.setting_rotation, settings, onChange) {
                    ParameterSlider(R.string.setting_rotation_limit, settings.maxRotationDegrees, 0f..20f, "rotation", unit = R.string.setting_degrees_value) { onChange(settings.copy(maxRotationDegrees = it)) }
                }
                StageSection(CompensationStage.Jitter, R.string.setting_jitter, settings, onChange) {
                    ParameterSlider(R.string.setting_jitter_limit, settings.jitterTolerancePercent, 0f..5f, "jitter") { onChange(settings.copy(jitterTolerancePercent = it)) }
                }

            }
        }
    }
}

@Composable
private fun StageSection(stage: CompensationStage, label: Int, settings: RecognitionSettings, onChange: (RecognitionSettings) -> Unit, content: @Composable () -> Unit) {
    val enabled = stage in settings.enabledCompensations
    HorizontalDivider()
    ToggleRow(label, enabled, "stage-${stage.name}") { checked ->
        onChange(settings.copy(enabledCompensations = if (checked) settings.enabledCompensations + stage else settings.enabledCompensations - stage,
            maxShearDegrees = if (stage == CompensationStage.Shear && checked && settings.maxShearDegrees == null) 5f else settings.maxShearDegrees))
    }
    if (enabled) content()
}

@Composable
private fun ToggleRow(label: Int, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    val title = stringResource(label)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        Switch(checked, onChange, Modifier.testTag(tag).semantics { contentDescription = title })
    }
}

@Composable
private fun ParameterSlider(label: Int, value: Float, range: ClosedFloatingPointRange<Float>, tag: String, enabled: Boolean = true, unit: Int = R.string.setting_percent_value, onChange: (Float) -> Unit) {
    val title = stringResource(label)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(unit, value), style = MaterialTheme.typography.bodyMedium)
        }
        Slider(value.coerceIn(range), onChange, Modifier.fillMaxWidth().testTag("parameter-$tag").semantics { contentDescription = title }, enabled = enabled, valueRange = range)
    }
}
