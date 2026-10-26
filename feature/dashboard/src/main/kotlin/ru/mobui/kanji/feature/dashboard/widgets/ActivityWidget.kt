package ru.mobui.kanji.feature.dashboard.widgets

import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.mobui.kanji.feature.dashboard.ActivityOverview
import ru.mobui.kanji.feature.dashboard.R

@Composable
fun ActivityWidget(state: ActivityOverview, onOpenDetails: () -> Unit, modifier: Modifier = Modifier) {
    val low = MaterialTheme.colorScheme.surfaceVariant
    val high = MaterialTheme.colorScheme.primary
    val labelStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
    val textMeasurer = rememberTextMeasurer()
    val weekdayLabels = listOf(
        0 to stringResource(R.string.dashboard_monday),
        2 to stringResource(R.string.dashboard_wednesday),
        4 to stringResource(R.string.dashboard_friday),
    )
    val description = stringResource(R.string.dashboard_activity_description)
    val iconSize = with(LocalDensity.current) { 18.sp.toDp() }
    val openLabel = stringResource(R.string.dashboard_open_activity)
    DashboardCard(modifier.testTag("open-activity-details")
        .clickable(role = Role.Button, onClickLabel = openLabel, onClick = onOpenDetails)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Image(painterResource(R.drawable.streak_flame), null, Modifier.size(iconSize))
            Text(pluralStringResource(R.plurals.dashboard_streak, state.streakDays, state.streakDays),
                modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            IllustratedIcon(DashboardIcon.Expand, Modifier.size(18.dp))
        }
        Canvas(Modifier.fillMaxWidth().height(116.dp).padding(vertical = 12.dp).semantics { contentDescription = description }) {
            val gap = 3.dp.toPx()
            val labels = weekdayLabels.map { (day, text) -> day to textMeasurer.measure(text, labelStyle) }
            val labelWidth = labels.maxOf { it.second.size.width }.toFloat()
            val labelGap = 5.dp.toPx()
            val cell = minOf((size.width - labelWidth - labelGap - gap * 5) / 6, (size.height - gap * 6) / 7)
            val groupWidth = labelWidth + labelGap + cell * 6 + gap * 5
            val left = (size.width - groupWidth) / 2
            val gridLeft = left + labelWidth + labelGap
            val top = (size.height - (cell * 7 + gap * 6)) / 2
            labels.forEach { (day, layout) ->
                drawText(layout, topLeft = Offset(left + labelWidth - layout.size.width,
                    top + day * (cell + gap) + (cell - layout.size.height) / 2))
            }
            state.dailyCounts.takeLast(42).forEachIndexed { index, count ->
                drawRoundRect(lerp(low, high, (count / 9f).coerceIn(0f, 1f)),
                    Offset(gridLeft + index / 7 * (cell + gap), top + index % 7 * (cell + gap)),
                    Size(cell, cell), CornerRadius(2.dp.toPx()))
            }
        }
    }
}
