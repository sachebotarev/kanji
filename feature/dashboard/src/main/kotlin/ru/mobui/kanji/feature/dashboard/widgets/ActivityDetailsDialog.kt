package ru.mobui.kanji.feature.dashboard.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.feature.dashboard.ActivityOverview
import ru.mobui.kanji.feature.dashboard.R

/** Read-only explanation of the same demo snapshot shown by the activity grid. */
@Composable
fun ActivityDetailsDialog(
    state: ActivityOverview,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = state.dailyCounts.takeLast(VISIBLE_DAYS)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("activity-details-dialog"),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.dashboard_activity)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    pluralStringResource(
                        R.plurals.dashboard_streak,
                        state.streakDays,
                        state.streakDays,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(stringResource(R.string.dashboard_activity_period))
                Text(
                    stringResource(
                        R.string.dashboard_active_days,
                        days.count { it > 0 },
                        days.size,
                    ),
                )
                Text(stringResource(R.string.dashboard_total, days.sum()))
            }
        },
        confirmButton = {
            TextButton(onDismiss, Modifier.testTag("close-activity-details")) {
                Text(stringResource(R.string.dashboard_close))
            }
        },
    )
}

private const val VISIBLE_DAYS = 42
