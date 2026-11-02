package ru.mobui.kanji.feature.dashboard.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.feature.dashboard.R
import ru.mobui.kanji.feature.dashboard.WordStatistics

@Composable
fun WordStatisticsWidget(
    state: WordStatistics,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors =
        listOf(
            MaterialTheme.colorScheme.primary,
            Color(color = 0xFFE4B94F),
            MaterialTheme.colorScheme.error,
        )
    val description = stringResource(R.string.dashboard_stats_summary, state.total)
    val openLabel = stringResource(R.string.dashboard_open_statistics)
    DashboardCard(
        modifier
            .testTag("open-word-statistics")
            .clickable(role = Role.Button, onClickLabel = openLabel, onClick = onOpenDetails)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Column(Modifier.clearAndSetSemantics {}) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(R.string.dashboard_checked),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                )
                IllustratedIcon(DashboardIcon.Expand, Modifier.size(18.dp))
            }
            Box(Modifier.fillMaxWidth().height(116.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(96.dp)) {
                    var start = -RING_START_DEGREES
                    listOf(state.learned, state.assisted, state.errors).forEachIndexed {
                        index,
                        count ->
                        val sweep =
                            if (state.total > 0) count * FULL_TURN_DEGREES / state.total else 0f
                        drawArc(colors[index], start, sweep, false, style = Stroke(14.dp.toPx()))
                        start += sweep
                    }
                }
                Text(state.total.toString(), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

private const val FULL_TURN_DEGREES = 360f

private const val RING_START_DEGREES = 90f
