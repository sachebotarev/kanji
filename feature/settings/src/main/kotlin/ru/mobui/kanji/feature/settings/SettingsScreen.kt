package ru.mobui.kanji.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.core.designsystem.KanjiTheme

/** Navigation is owned by the app; this screen only presents available settings sections. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenChecker: () -> Unit,
    modifier: Modifier = Modifier,
    modelLoaded: Boolean = false,
    modelLoading: Boolean = false,
    onDownloadModel: () -> Unit = {},
    onClearTrainingHistory: () -> Unit = {},
) {
    var confirmClearHistory by rememberSaveable { mutableStateOf(false) }
    Scaffold(modifier.testTag("settings-screen")) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onBack, Modifier.testTag("settings-back")) {
                    Icon(
                        painterResource(R.drawable.ic_back),
                        stringResource(R.string.settings_back),
                    )
                }
                Text(
                    stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            CheckerSettingsEntry(onOpenChecker, Modifier.fillMaxWidth().padding(18.dp))
            ClearTrainingHistoryEntry(
                onClick = { confirmClearHistory = true },
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            )
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ru.mobui.kanji.core.designsystem.KanjiSectionTitle(
                    stringResource(R.string.settings_japanese_model),
                )
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
            }
        }
    }
    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            modifier = Modifier.testTag("clear-training-history-dialog"),
            title = { Text(stringResource(R.string.settings_clear_history_title)) },
            text = { Text(stringResource(R.string.settings_clear_history_message)) },
            confirmButton = {
                TextButton(
                    {
                        confirmClearHistory = false
                        onClearTrainingHistory()
                    },
                    Modifier.testTag("clear-training-history-confirm"),
                ) {
                    Text(
                        stringResource(R.string.settings_clear_history_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    { confirmClearHistory = false },
                    Modifier.testTag("clear-training-history-cancel"),
                ) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
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

@Preview(locale = "ru")
@Composable
private fun LightSettingsPreview() {
    KanjiTheme(false) { SettingsScreen({}, {}) }
}

@Preview(locale = "ru")
@Composable
private fun DarkSettingsPreview() {
    KanjiTheme(true) { SettingsScreen({}, {}) }
}
