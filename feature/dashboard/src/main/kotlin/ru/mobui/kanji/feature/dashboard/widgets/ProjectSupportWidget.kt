package ru.mobui.kanji.feature.dashboard.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.feature.dashboard.R

@Composable
fun ProjectSupportWidget(modifier: Modifier = Modifier) {
    Row(modifier.heightIn(min = 48.dp).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        IllustratedIcon(DashboardIcon.Support, Modifier.size(20.dp))
        Text(stringResource(R.string.dashboard_coffee), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
