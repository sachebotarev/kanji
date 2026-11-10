package ru.mobui.kanji.feature.dashboard.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import ru.mobui.kanji.domain.practice.Training
import ru.mobui.kanji.domain.practice.TrainingGradeLevel
import ru.mobui.kanji.domain.practice.TrainingStatus
import ru.mobui.kanji.domain.practice.TrainingTask
import ru.mobui.kanji.domain.practice.TrainingTaskStatus
import ru.mobui.kanji.domain.practice.summarize
import ru.mobui.kanji.feature.dashboard.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrainingDetailScreen(
    training: Training?,
    language: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val locale = remember(language) { Locale.forLanguageTag(language) }
    val timeFormatter =
        remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    val dateFormatter =
        remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale) }
    val title =
        training?.recipeNames?.localizedName(
            language,
            stringResource(R.string.dashboard_mix),
        ) ?: stringResource(R.string.history_detail_title)
    Scaffold(
        modifier.testTag("training-detail-screen"),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onBack, Modifier.testTag("training-detail-back")) {
                        Icon(
                            painterResource(ru.mobui.kanji.core.designsystem.R.drawable.ic_arrow_back),
                            stringResource(R.string.recipe_back),
                        )
                    }
                },
            )
        },
    ) { insets ->
        if (training == null) {
            ru.mobui.kanji.core.designsystem.KanjiEmptyState(
                title = stringResource(R.string.history_empty),
                modifier = Modifier.padding(insets),
            )
            return@Scaffold
        }
        val counts = remember(training) { training.taskCounts() }
        val unfinished = training.status == TrainingStatus.InProgress
        val started = Instant.ofEpochMilli(training.startedAt).atZone(zone)
        val today = LocalDate.now(zone)
        val dayLabel =
            when (dayRelation(started.toLocalDate(), today)) {
                0L -> stringResource(R.string.history_today)
                1L -> stringResource(R.string.history_yesterday)
                else -> dateFormatter.format(started.toLocalDate())
            }
        val minutes = ((training.durationMs() + 59_999L) / 60_000L).coerceAtLeast(1L)
        val whenLabel =
            stringResource(
                R.string.history_detail_when,
                dayLabel,
                timeFormatter.format(started),
                minutes.toInt(),
            )
        val isExcellent = remember(training, language) { training.isExcellent(language) }
        val words =
            remember(training, unfinished) {
                val list =
                    if (unfinished) training.tasks
                    else training.tasks.filter { it.isTerminal() }
                list.sortedBy { it.position }
            }
        LazyColumn(
            Modifier.fillMaxSize().padding(insets).testTag("training-detail-list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TrainingMiniRing(counts, size = 56.dp, stroke = 7.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (unfinished) {
                            val (current, total) = training.wordProgress()
                            Text(
                                stringResource(R.string.training_unfinished),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                            Text(
                                stringResource(R.string.training_word_progress, current, total),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            whenLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(
                                R.string.history_summary,
                                counts.correct,
                                counts.assisted,
                                counts.missed,
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            stringResource(R.string.history_detail_total, training.tasks.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.history_words),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isExcellent) {
                        Image(
                            painter = painterResource(R.drawable.ic_paw_stamp),
                            contentDescription = stringResource(R.string.history_stamp_excellent),
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .rotate(-10f)
                                    .testTag("training-detail-stamp"),
                        )
                    }
                }
            }
            items(words, key = { it.id }) { task ->
                HistoryWordRow(task, language)
            }
        }
    }
}

@Composable
private fun HistoryWordRow(task: TrainingTask, language: String) {
    val gloss = task.translations.localizedText(language)
    val elapsed =
        if (task.elapsedMs > 0) {
            stringResource(R.string.history_word_seconds, ((task.elapsedMs + 999) / 1000).toInt())
        } else {
            "—"
        }
    Surface(
        Modifier.fillMaxWidth().testTag("training-detail-word-${task.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(task.writtenForm, style = MaterialTheme.typography.titleLarge)
                Text(
                    task.reading,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (gloss.isNotBlank()) {
                    Text(gloss, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    statusLabel(task.status),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor(task.status),
                )
                Text(
                    elapsed,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun statusLabel(status: TrainingTaskStatus): String =
    when (status) {
        TrainingTaskStatus.Correct -> stringResource(R.string.dashboard_learned)
        TrainingTaskStatus.Assisted -> stringResource(R.string.dashboard_assisted)
        TrainingTaskStatus.Skipped -> stringResource(R.string.history_status_skipped)
        TrainingTaskStatus.TimedOut -> stringResource(R.string.history_status_timed_out)
        TrainingTaskStatus.Pending -> stringResource(R.string.history_status_pending)
        TrainingTaskStatus.InProgress -> stringResource(R.string.history_status_in_progress)
    }

@Composable
private fun statusColor(status: TrainingTaskStatus) =
    when (status) {
        TrainingTaskStatus.Correct -> MaterialTheme.colorScheme.primary
        TrainingTaskStatus.Assisted -> ru.mobui.kanji.core.designsystem.KanjiColorAssisted
        TrainingTaskStatus.Skipped,
        TrainingTaskStatus.TimedOut -> MaterialTheme.colorScheme.error
        TrainingTaskStatus.Pending,
        TrainingTaskStatus.InProgress -> MaterialTheme.colorScheme.onSurfaceVariant
    }
