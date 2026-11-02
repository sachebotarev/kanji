package ru.mobui.kanji.feature.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ru.mobui.kanji.core.writingpad.ContentVersion
import ru.mobui.kanji.core.writingpad.KanjiWritingPad
import ru.mobui.kanji.core.writingpad.PadContent
import ru.mobui.kanji.core.writingpad.PadUiState
import ru.mobui.kanji.domain.practice.CheckVerdict
import ru.mobui.kanji.domain.practice.RecognitionSettings

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
            SettingsHeader(onBack, onReset)
            SettingsPreview(preview)
            Column(
                Modifier.fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsPreviewFailures(preview)
                ParameterGroup(R.string.settings_group_shape, ShapeParameters, settings, ::change)
                ParameterGroup(
                    R.string.settings_group_compensations,
                    CompensationParameters,
                    settings,
                    ::change,
                )
                ParameterGroup(R.string.settings_group_budget, BudgetParameters, settings, ::change)
                EnabledChecks(settings, readabilityAvailable, ::change)
                SettingsGroup(R.string.settings_group_rules) {
                    RuleChoiceRow(
                        R.string.setting_structure,
                        R.string.help_structure,
                        settings.checkStrokeStructure,
                        "check-structure",
                    ) {
                        change(settings.copy(checkStrokeStructure = it))
                    }
                    if (settings.checkStrokeStructure)
                        ParameterFields(RuleParameters, settings, ::change)
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit, onReset: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        TextButton(onBack, Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.back))
        }
        Text(
            stringResource(R.string.checker_settings_title),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
        )
        TextButton(
            onReset,
            Modifier.heightIn(min = 48.dp).testTag("reset-checker-settings"),
        ) {
            Text(stringResource(R.string.checker_settings_reset))
        }
    }
}

@Composable
private fun SettingsPreview(preview: PracticeUiState?) {
    val cell = preview?.practice?.selected
    val report = preview?.currentCheckReport ?: preview?.debugReport
    if (cell != null) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KanjiWritingPad(
                PadUiState(
                    ContentVersion(cell.task.occurrenceId, cell.revision),
                    PadContent.UserInk(cell.sample),
                    inputEnabled = false,
                    comparisonOverlay = report?.compensatedDrawing,
                ),
                {},
                Modifier.size(132.dp).testTag("settings-preview"),
            )
            SettingsPreviewStatus(preview, report, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SettingsPreviewStatus(
    preview: PracticeUiState,
    report: ru.mobui.kanji.domain.practice.CheckReport?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(top = 8.dp)) {
        Text(
            stringResource(R.string.checker_settings_preview),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(
                if (preview.isChecking) R.string.debug_checking
                else
                    when (report?.verdict) {
                        CheckVerdict.Correct -> R.string.checker_preview_correct
                        CheckVerdict.Incorrect -> R.string.checker_preview_incorrect
                        else -> R.string.checker_preview_pending
                    },
            ),
            Modifier.testTag("settings-verdict"),
        )
        report?.compensation?.budget?.let { budget ->
            Text(
                stringResource(
                    R.string.checker_settings_budget_value,
                    budget.totalPercent,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (preview.isChecking) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
private fun SettingsPreviewFailures(preview: PracticeUiState?) {
    val report = preview?.currentCheckReport ?: preview?.debugReport
    report?.failures?.forEach { failure ->
        Text(
            checkFailureText(failure),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (preview != null)
        Text(
            stringResource(R.string.checker_settings_live),
            style = MaterialTheme.typography.bodySmall,
        )
}

@Composable
private fun EnabledChecks(
    settings: RecognitionSettings,
    available: Boolean,
    change: (RecognitionSettings) -> Unit,
) {
    SettingsGroup(R.string.checks_testing) {
        RuleChoiceRow(
            R.string.check_readability,
            R.string.help_readability,
            settings.readabilityEnabled && available,
            "enable-readability",
            allowEnable = available,
        ) {
            change(settings.copy(readabilityEnabled = it))
        }
        if (!available)
            Text(
                stringResource(R.string.readability_model_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("readability-model-required"),
            )
        RuleChoiceRow(
            R.string.check_shape,
            R.string.help_shape,
            settings.shapeEnabled,
            "enable-shape",
        ) {
            change(settings.copy(shapeEnabled = it))
        }
        RuleChoiceRow(
            R.string.check_writing,
            R.string.help_writing,
            settings.strokeWritingEnabled,
            "enable-writing",
        ) {
            change(settings.copy(strokeWritingEnabled = it))
        }
    }
}

private data class ParameterSpec(
    val label: Int,
    val help: Int,
    val tag: String,
    val range: ClosedFloatingPointRange<Float>,
    val read: (RecognitionSettings) -> Float,
    val write: (RecognitionSettings, Float) -> RecognitionSettings,
    val unit: Int = R.string.setting_percent_value,
    val increment: Float = 1f,
)

@Composable
private fun ParameterGroup(
    title: Int,
    specs: List<ParameterSpec>,
    settings: RecognitionSettings,
    change: (RecognitionSettings) -> Unit,
) {
    SettingsGroup(title) { ParameterFields(specs, settings, change) }
}

@Composable
private fun ParameterFields(
    specs: List<ParameterSpec>,
    settings: RecognitionSettings,
    change: (RecognitionSettings) -> Unit,
) {
    specs.forEach { spec ->
        ParameterSlider(spec, spec.read(settings)) { change(spec.write(settings, it)) }
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
private fun RuleChoiceRow(
    label: Int,
    help: Int,
    checked: Boolean,
    tag: String,
    allowEnable: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
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
                    modifier =
                        Modifier.weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag(if (value) tag else "$tag-off")
                            .semantics {
                                selected = active
                                contentDescription = "$title: $text"
                            },
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor =
                                if (active) MaterialTheme.colorScheme.secondaryContainer
                                else Color.Transparent,
                            contentColor =
                                if (active) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                        ),
                ) {
                    Text(text)
                }
            }
        }
    }
}

@Composable
private fun ParameterSlider(spec: ParameterSpec, value: Float, onChange: (Float) -> Unit) {
    with(spec) {
        val title = stringResource(label)
        fun snapped(raw: Float) = ((raw / increment).roundToInt() * increment).coerceIn(range)
        val displayed = snapped(value)
        Column {
            val formatted =
                if (increment == 1f) stringResource(unit, displayed.roundToInt())
                else stringResource(R.string.setting_tenths_percent_value, displayed)
            SettingHelp(label, help, tag, value = formatted)
            Slider(
                value = displayed,
                onValueChange = { onChange(snapped(it)) },
                modifier =
                    Modifier.fillMaxWidth().testTag("parameter-$tag").semantics {
                        contentDescription = title
                    },
                enabled = true,
                valueRange = range,
                steps = ((range.endInclusive - range.start) / increment).roundToInt() - 1,
            )
        }
    }
}

private val ShapeParameters =
    listOf(
        ParameterSpec(
            R.string.setting_match,
            R.string.help_match,
            "match",
            1f..100f,
            { it.requiredMatchPercent },
            { settings, value -> settings.copy(requiredMatchPercent = value) },
        ),
        ParameterSpec(
            R.string.setting_distance,
            R.string.help_distance,
            "distance",
            1f..15f,
            { it.distanceTolerance * 100 },
            { settings, value -> settings.copy(distanceTolerance = value / 100) },
        ),
    )

private val CompensationParameters =
    listOf(
        ParameterSpec(
            R.string.setting_translation_limit,
            R.string.help_translation,
            "translation",
            0f..100f,
            { it.maxTranslationPercent },
            { settings, value -> settings.copy(maxTranslationPercent = value) },
        ),
        ParameterSpec(
            R.string.setting_size_limit,
            R.string.help_size,
            "size",
            0f..200f,
            { it.maxSizeCompensationPercent ?: 30f },
            { settings, value -> settings.copy(maxSizeCompensationPercent = value) },
        ),
        ParameterSpec(
            R.string.setting_stretch_x,
            R.string.help_stretch_x,
            "stretch-x",
            0f..100f,
            { it.maxHorizontalStretchPercent },
            { settings, value -> settings.copy(maxHorizontalStretchPercent = value) },
        ),
        ParameterSpec(
            R.string.setting_stretch_y,
            R.string.help_stretch_y,
            "stretch-y",
            0f..100f,
            { it.maxVerticalStretchPercent },
            { settings, value -> settings.copy(maxVerticalStretchPercent = value) },
        ),
        ParameterSpec(
            R.string.setting_rotation_limit,
            R.string.help_rotation,
            "rotation",
            0f..20f,
            { it.maxRotationDegrees },
            { settings, value -> settings.copy(maxRotationDegrees = value) },
            unit = R.string.setting_degrees_value,
        ),
        ParameterSpec(
            R.string.setting_shear_limit,
            R.string.help_shear,
            "shear",
            0f..20f,
            { it.maxShearDegrees ?: 0f },
            { settings, value -> settings.copy(maxShearDegrees = value) },
            unit = R.string.setting_degrees_value,
        ),
        ParameterSpec(
            R.string.setting_jitter_limit,
            R.string.help_jitter,
            "jitter",
            0f..5f,
            { it.jitterTolerancePercent },
            { settings, value -> settings.copy(jitterTolerancePercent = value) },
            increment = .1f,
        ),
    )

private val BudgetParameters =
    listOf(
        ParameterSpec(
            R.string.setting_budget,
            R.string.help_budget,
            "budget",
            0f..100f,
            { it.maxCompensationBudgetPercent ?: 0f },
            { settings, value ->
                settings.copy(maxCompensationBudgetPercent = value.takeIf { it > 0f })
            },
        ),
    )

private val RuleParameters =
    listOf(
        ParameterSpec(
            R.string.setting_length_difference,
            R.string.help_length_difference,
            "length-difference",
            0f..30f,
            { it.lengthDifferenceTolerancePercent },
            { settings, value -> settings.copy(lengthDifferenceTolerancePercent = value) },
        ),
        ParameterSpec(
            R.string.setting_connection,
            R.string.help_connection,
            "connection",
            0f..10f,
            { it.connectionTolerancePercent },
            { settings, value -> settings.copy(connectionTolerancePercent = value) },
        ),
        ParameterSpec(
            R.string.setting_protrusion,
            R.string.help_protrusion,
            "protrusion",
            0f..10f,
            { it.protrusionTolerancePercent },
            { settings, value -> settings.copy(protrusionTolerancePercent = value) },
        ),
    )
