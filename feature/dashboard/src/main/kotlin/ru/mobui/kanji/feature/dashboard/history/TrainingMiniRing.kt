package ru.mobui.kanji.feature.dashboard.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun TrainingMiniRing(
    counts: TrainingTaskCounts,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    stroke: Dp = 5.dp,
) {
    val colors =
        listOf(
            MaterialTheme.colorScheme.primary,
            ru.mobui.kanji.core.designsystem.KanjiColorAssisted,
            MaterialTheme.colorScheme.error,
        )
    val empty = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val style = Stroke(stroke.toPx())
            if (counts.total <= 0) {
                drawArc(empty, 0f, 360f, false, style = style)
            } else {
                var start = -90f
                listOf(counts.correct, counts.assisted, counts.missed).forEachIndexed { index, count ->
                    val sweep = count * 360f / counts.total
                    if (sweep > 0f) drawArc(colors[index], start, sweep, false, style = style)
                    start += sweep
                }
            }
        }
        Text(counts.total.toString(), style = MaterialTheme.typography.labelMedium)
    }
}
