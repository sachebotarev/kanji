package ru.mobui.kanji.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ru.mobui.kanji.core.designsystem.KanjiDeleteConfirmDialog
import ru.mobui.kanji.core.designsystem.KanjiSectionTitle
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.practice.RecognitionSettings

/** Navigation is owned by the app; this screen presents all general settings sections. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenChecker: () -> Unit,
    modifier: Modifier = Modifier,
    settings: RecognitionSettings = RecognitionSettings.touchInput(),
    onChangeSettings: (RecognitionSettings) -> Unit = {},
    readabilityAvailable: Boolean = true,
    showCalibration: Boolean = false,
    modelLoaded: Boolean = false,
    modelLoading: Boolean = false,
    onDownloadModel: () -> Unit = {},
    onClearTrainingHistory: () -> Unit = {},
) {
    var confirmClearHistory by rememberSaveable { mutableStateOf(false) }

    Scaffold(modifier.testTag("settings-screen")) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onBack, Modifier.testTag("settings-back")) {
                    Icon(
                        painterResource(ru.mobui.kanji.core.designsystem.R.drawable.ic_arrow_back),
                        stringResource(R.string.settings_back),
                        Modifier.size(24.dp),
                    )
                }
                Text(
                    stringResource(R.string.settings_title),
                    Modifier.weight(1f).padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Column(
                Modifier.fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Секция «Ход тренировки»
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KanjiSectionTitle(stringResource(R.string.settings_group_flow))
                    BinaryChoiceRow(
                        label = R.string.setting_auto_advance,
                        help = R.string.help_auto_advance,
                        checked = settings.autoAdvanceOnSuccess,
                        tag = "auto-advance",
                        allowEnable = true,
                        onChange = { onChangeSettings(settings.copy(autoAdvanceOnSuccess = it)) },
                    )
                }

                HorizontalDivider()

                // Секция «Подсказки»
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KanjiSectionTitle(stringResource(R.string.settings_group_hint))
                    SliderSetting(
                        label = R.string.setting_max_next_stroke_hints,
                        help = R.string.help_max_next_stroke_hints,
                        tag = "max-next-stroke-hints",
                        value = settings.maxNextStrokeHints.toFloat(),
                        range = 1f..12f,
                        increment = 1f,
                        unit = R.string.setting_count_value,
                        onChange = { onChangeSettings(settings.copy(maxNextStrokeHints = it.roundToInt())) },
                    )
                    SliderSetting(
                        label = R.string.setting_full_hint_display,
                        help = R.string.help_full_hint_display,
                        tag = "full-hint-display",
                        value = settings.fullHintDisplayMillis.toFloat(),
                        range = 200f..3000f,
                        increment = 50f,
                        unit = R.string.setting_millis_value,
                        onChange = { onChangeSettings(settings.copy(fullHintDisplayMillis = it.roundToInt())) },
                    )
                }

                HorizontalDivider()

                // Секция «Модули проверки»
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KanjiSectionTitle(stringResource(R.string.settings_group_checkers))
                    BinaryChoiceRow(
                        label = R.string.check_readability,
                        help = R.string.help_readability,
                        checked = settings.readabilityEnabled && readabilityAvailable,
                        tag = "enable-readability",
                        allowEnable = readabilityAvailable,
                        onChange = { onChangeSettings(settings.copy(readabilityEnabled = it)) },
                    )
                    if (!readabilityAvailable) {
                        Text(
                            stringResource(R.string.readability_model_required),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("readability-model-required"),
                        )
                    }
                    Button(
                        onClick = onDownloadModel,
                        enabled = !modelLoaded && !modelLoading,
                        modifier = Modifier.fillMaxWidth().testTag("download-japanese-model"),
                    ) {
                        Text(
                            stringResource(
                                when {
                                    modelLoaded -> R.string.settings_model_loaded
                                    modelLoading -> R.string.settings_model_loading
                                    else -> R.string.settings_model_missing
                                },
                            ),
                        )
                    }
                    BinaryChoiceRow(
                        label = R.string.check_writing,
                        help = R.string.help_writing,
                        checked = settings.strokeWritingEnabled,
                        tag = "enable-writing",
                        allowEnable = true,
                        onChange = { onChangeSettings(settings.copy(strokeWritingEnabled = it)) },
                    )
                    BinaryChoiceRow(
                        label = R.string.check_shape,
                        help = R.string.help_shape,
                        checked = settings.shapeEnabled,
                        tag = "enable-shape",
                        allowEnable = true,
                        onChange = { onChangeSettings(settings.copy(shapeEnabled = it)) },
                    )
                }

                // Секция «Калибровка формы кандзи» — доступно только в develop/debug сборке
                if (showCalibration) {
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        KanjiSectionTitle(stringResource(R.string.settings_group_calibration))
                        CheckerSettingsEntry(onOpenChecker, Modifier.fillMaxWidth())
                    }
                }

                HorizontalDivider()

                // Секция «Данные и память»
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KanjiSectionTitle(stringResource(R.string.settings_group_data))
                    ClearTrainingHistoryEntry(
                        onClick = { confirmClearHistory = true },
                        Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (confirmClearHistory) {
        KanjiDeleteConfirmDialog(
            title = stringResource(R.string.settings_clear_history_title),
            message = stringResource(R.string.settings_clear_history_message),
            onConfirm = {
                confirmClearHistory = false
                onClearTrainingHistory()
            },
            onDismiss = { confirmClearHistory = false },
            confirmText = stringResource(R.string.settings_clear_history_confirm),
            dismissText = stringResource(R.string.settings_cancel),
            confirmTag = "clear-training-history-confirm",
            dismissTag = "clear-training-history-cancel",
            modifier = Modifier.testTag("clear-training-history-dialog"),
        )
    }
}

@Composable
internal fun CheckerSettingsEntry(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick,
        modifier.testTag("open-app-checker-settings"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_checker),
                null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_checker),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.settings_checker_detail),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ClearTrainingHistoryEntry(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick,
        modifier.testTag("clear-training-history"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.settings_clear_history),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.settings_clear_history_detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BinaryChoiceRow(
    label: Int,
    help: Int,
    checked: Boolean,
    tag: String,
    allowEnable: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val title = stringResource(label)
    val onLabel = stringResource(R.string.setting_on)
    val offLabel = stringResource(R.string.setting_off)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SettingHelp(label, help, tag)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((value, text) in listOf(true to onLabel, false to offLabel)) {
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
private fun SliderSetting(
    label: Int,
    help: Int,
    tag: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    increment: Float,
    unit: Int,
    onChange: (Float) -> Unit,
) {
    val title = stringResource(label)
    fun snapped(raw: Float) = ((raw / increment).roundToInt() * increment).coerceIn(range)
    val displayed = snapped(value)

    Column {
        val formatted = stringResource(unit, displayed.roundToInt())
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

@Composable
internal fun SettingHelp(
    label: Int,
    help: Int,
    tag: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    initiallyExpanded: Boolean = false,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val title = stringResource(label)
    val action = stringResource(if (expanded) R.string.help_collapse else R.string.help_expand, title)
    val description = stringResource(if (expanded) R.string.help_expanded else R.string.help_collapsed)

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("help-toggle-$tag")
                .clickable(role = Role.Button, onClickLabel = action) { expanded = !expanded }
                .semantics { stateDescription = description },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            value?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Icon(
                painterResource(
                    if (expanded) R.drawable.ic_help_collapse else R.drawable.ic_setting_info,
                ),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        if (expanded) {
            Surface(
                modifier =
                    Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("help-content-$tag"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Text(
                    stringResource(help),
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Preview(locale = "ru")
@Composable
private fun LightSettingsPreview() {
    KanjiTheme(false) { SettingsScreen({}, {}, showCalibration = true) }
}

@Preview(locale = "ru")
@Composable
private fun DarkSettingsPreview() {
    KanjiTheme(true) { SettingsScreen({}, {}, showCalibration = true) }
}
