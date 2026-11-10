package ru.mobui.kanji.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
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
        ru.mobui.kanji.core.designsystem.KanjiDeleteConfirmDialog(
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
