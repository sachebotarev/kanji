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
 * Standard confirmation dialog for delete and destructive actions.
 * Highlights destructive action with MaterialTheme.colorScheme.error.
 */
@Composable
fun KanjiDeleteConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    confirmText: String = stringResource(R.string.core_delete),
    dismissText: String = stringResource(R.string.core_cancel),
    confirmTag: String = "delete-confirm",
    dismissTag: String = "delete-cancel",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = enabled,
                modifier = Modifier.testTag(confirmTag),
            ) {
                Text(
                    confirmText,
                    color = if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(dismissTag),
            ) {
                Text(dismissText)
            }
        },
        modifier = modifier,
    )
}
