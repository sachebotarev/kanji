package ru.mobui.kanji.core.designsystem

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

/**
 * Standard informational/alert dialog with a single dismissal button ("Закрыть").
 * Ensures consistent typography, spacing, and dismissal handling across modules.
 */
@Composable
fun KanjiInfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = stringResource(R.string.core_close),
    testTag: String? = null,
    buttonTag: String? = testTag?.let { "$it-close" },
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = if (buttonTag != null) Modifier.testTag(buttonTag) else Modifier,
            ) {
                Text(buttonText)
            }
        },
        modifier = if (testTag != null) modifier.testTag(testTag) else modifier,
    )
}
