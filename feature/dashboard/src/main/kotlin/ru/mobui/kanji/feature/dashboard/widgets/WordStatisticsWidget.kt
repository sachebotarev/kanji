package ru.mobui.kanji.feature.dashboard.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
            ru.mobui.kanji.core.designsystem.KanjiColorAssisted,
            MaterialTheme.colorScheme.error,
        )
    val emptyRing = MaterialTheme.colorScheme.surfaceVariant
    val description =
        stringResource(
            R.string.dashboard_stats_summary,
            state.total,
            state.correct,
            state.assisted,
            state.missed,
        )
    val openLabel = stringResource(R.string.dashboard_open_statistics)
    DashboardCard(
        modifier
            .testTag("open-word-statistics")
            .clickable(role = Role.Button, onClickLabel = openLabel, onClick = onOpenDetails)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Column(Modifier.clearAndSetSemantics {}) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.dashboard_checked),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        stringResource(R.string.dashboard_stats_period),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IllustratedIcon(DashboardIcon.Expand, Modifier.size(18.dp))
            }
            Box(
                Modifier.fillMaxWidth().height(116.dp).padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(96.dp)) {
                    val stroke = Stroke(14.dp.toPx())
                    if (state.total <= 0) {
                        drawArc(emptyRing, 0f, FULL_TURN_DEGREES, false, style = stroke)
                    } else {
                        var start = -RING_START_DEGREES
                        listOf(state.correct, state.assisted, state.missed).forEachIndexed {
                            index,
                            count ->
                            val sweep = count * FULL_TURN_DEGREES / state.total
                            if (sweep > 0f) {
                                drawArc(colors[index], start, sweep, false, style = stroke)
                            }
                            start += sweep
                        }
                    }
                }
                Text(state.total.toString(), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

private const val FULL_TURN_DEGREES = 360f

private const val RING_START_DEGREES = 90f
