package ru.mobui.kanji.feature.dashboard.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.feature.dashboard.R

@Composable
fun SessionHistoryWidget(modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IllustratedIcon(DashboardIcon.History, Modifier.size(34.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.dashboard_history), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.dashboard_history_detail), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
