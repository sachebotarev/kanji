package ru.mobui.kanji.feature.practice

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.core.designsystem.R as DesignR
import ru.mobui.kanji.domain.practice.TrainingGradeLevel
import ru.mobui.kanji.domain.practice.TrainingSummary

/** PRACTICE.01 — training completion: grade, brief stats, grade-specific mascot. */
@Composable
fun TrainingCompleteScreen(
    summary: TrainingSummary,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradeColor =
        when (summary.grade) {
            TrainingGradeLevel.Excellent -> MaterialTheme.colorScheme.primary
            TrainingGradeLevel.Steady,
            TrainingGradeLevel.KeepGoing -> MaterialTheme.colorScheme.onSurface
        }
    Scaffold(modifier.testTag("training-complete-screen")) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            Text(
                stringResource(R.string.training_complete_title),
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painterResource(mascotFor(summary.grade)),
                    contentDescription = null,
                    Modifier.size(168.dp).testTag("training-complete-mascot"),
                )
                if (summary.recipeName.isNotBlank()) {
                    Text(
                        summary.recipeName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    stringResource(gradeTitle(summary.grade)),
                    style = MaterialTheme.typography.headlineMedium,
                    color = gradeColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("training-complete-grade"),
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        summary.percent.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "%",
                        Modifier.padding(bottom = 10.dp, start = 2.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    stringResource(gradeHint(summary.grade)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("training-complete-stats"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatRow(
                        stringResource(R.string.training_stat_correct),
                        summary.correctCount.toString(),
                    )
                    StatRow(
                        stringResource(R.string.training_stat_assisted),
                        summary.assistedCount.toString(),
                    )
                    StatRow(
                        stringResource(R.string.training_stat_skipped),
                        summary.skippedCount.toString(),
                    )
                    StatRow(
                        stringResource(R.string.training_stat_timed_out),
                        summary.timedOutCount.toString(),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    StatRow(
                        stringResource(R.string.training_stat_time),
                        formatElapsed(summary.elapsedMs),
                    )
                }
            }
            Button(
                onClick = onDone,
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .testTag("training-complete-done"),
            ) {
                Text(stringResource(R.string.training_complete_home))
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun mascotFor(grade: TrainingGradeLevel): Int =
    when (grade) {
        TrainingGradeLevel.Excellent -> DesignR.drawable.mascot_sticker_completed_sheet
        TrainingGradeLevel.Steady -> DesignR.drawable.mascot_sticker_encourage
        TrainingGradeLevel.KeepGoing -> DesignR.drawable.mascot_sticker_jp_shodo_retry
    }

private fun gradeTitle(grade: TrainingGradeLevel): Int =
    when (grade) {
        TrainingGradeLevel.Excellent -> R.string.training_grade_excellent
        TrainingGradeLevel.Steady -> R.string.training_grade_steady
        TrainingGradeLevel.KeepGoing -> R.string.training_grade_keep_going
    }

private fun gradeHint(grade: TrainingGradeLevel): Int =
    when (grade) {
        TrainingGradeLevel.Excellent -> R.string.training_grade_excellent_hint
        TrainingGradeLevel.Steady -> R.string.training_grade_steady_hint
        TrainingGradeLevel.KeepGoing -> R.string.training_grade_keep_going_hint
    }

private fun formatElapsed(elapsedMs: Long): String {
    val totalSec = (elapsedMs / MILLIS_PER_SECOND).coerceAtLeast(0L)
    val minutes = totalSec / SECONDS_PER_MINUTE
    val seconds = totalSec % SECONDS_PER_MINUTE
    return "%d:%02d".format(minutes, seconds)
}

private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L

@Preview(locale = "ru")
@Composable
private fun ExcellentPreview() {
    KanjiTheme(false) {
        TrainingCompleteScreen(
            TrainingSummary("Умный микс", 10, 8, 1, 0, 1, 412_000, 92, TrainingGradeLevel.Excellent),
            {},
        )
    }
}

@Preview(locale = "ru")
@Composable
private fun KeepGoingPreview() {
    KanjiTheme(true) {
        TrainingCompleteScreen(
            TrainingSummary("Умный микс", 10, 2, 2, 4, 2, 510_000, 38, TrainingGradeLevel.KeepGoing),
            {},
        )
    }
}
