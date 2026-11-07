package ru.mobui.kanji.feature.dashboard.widgets

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import ru.mobui.kanji.feature.dashboard.R
import ru.mobui.kanji.feature.dashboard.UnfinishedTrainingSummary

/** HOME.D03 — continue unfinished training or start a new one. */
@Composable
fun UnfinishedTrainingDialog(
    summary: UnfinishedTrainingSummary,
    onContinue: () -> Unit,
    onStartNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.training_unfinished_choice)) },
        text = {
            Text("${summary.recipeName} · ${summary.progressLabel}")
        },
        confirmButton = {
            Button(onContinue, Modifier.testTag("continue-training")) {
                Text(stringResource(R.string.training_continue))
            }
        },
        dismissButton = {
            TextButton(onStartNew, Modifier.testTag("start-new-training")) {
                Text(stringResource(R.string.training_start_new))
            }
        },
        modifier = Modifier.testTag("unfinished-choice-dialog"),
    )
}
