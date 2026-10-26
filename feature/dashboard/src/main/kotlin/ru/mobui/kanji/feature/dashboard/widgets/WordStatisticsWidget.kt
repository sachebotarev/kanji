package ru.mobui.kanji.feature.dashboard.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.feature.dashboard.R
import ru.mobui.kanji.feature.dashboard.WordStatistics

@Composable
fun WordStatisticsWidget(state: WordStatistics, onOpenDetails: () -> Unit, modifier: Modifier = Modifier) {
    val colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFFE4B94F), MaterialTheme.colorScheme.error)
    val description = stringResource(R.string.dashboard_stats_summary, state.total)
    val openLabel = stringResource(R.string.dashboard_open_statistics)
    DashboardCard(modifier.testTag("open-word-statistics")
        .clickable(role = Role.Button, onClickLabel = openLabel, onClick = onOpenDetails)
        .semantics(mergeDescendants = true) { contentDescription = description }) {
        Column(Modifier.clearAndSetSemantics {}) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.dashboard_checked), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                IllustratedIcon(DashboardIcon.Expand, Modifier.size(18.dp))
            }
            Box(Modifier.fillMaxWidth().height(116.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(96.dp)) {
                    var start = -90f
                    listOf(state.learned, state.assisted, state.errors).forEachIndexed { index, count ->
                        val sweep = if (state.total > 0) count * 360f / state.total else 0f
                        drawArc(colors[index], start, sweep, false, style = Stroke(14.dp.toPx()))
                        start += sweep
                    }
                }
                Text(state.total.toString(), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
