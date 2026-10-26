package ru.mobui.kanji.feature.dashboard.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.feature.dashboard.R
import ru.mobui.kanji.feature.dashboard.WordStatistics

@Composable
fun WordStatisticsDialog(state: WordStatistics, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(onDismissRequest = onDismiss, modifier = modifier.testTag("word-statistics-dialog"),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.dashboard_checked)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.dashboard_total, state.total))
                    Text(stringResource(R.string.dashboard_statistics_scope), style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val rows = listOf(
                    Triple(R.string.dashboard_learned, state.learned, MaterialTheme.colorScheme.primary),
                    Triple(R.string.dashboard_assisted, state.assisted, Color(0xFFE4B94F)),
                    Triple(R.string.dashboard_errors, state.errors, MaterialTheme.colorScheme.error),
                )
                rows.forEach { (label, count, color) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(12.dp).background(color, CircleShape))
                        Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text(count.toString(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onDismiss, Modifier.testTag("close-word-statistics")) {
                Text(stringResource(R.string.dashboard_close))
            }
        })
}
