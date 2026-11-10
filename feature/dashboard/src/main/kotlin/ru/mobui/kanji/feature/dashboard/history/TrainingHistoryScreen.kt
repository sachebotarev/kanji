package ru.mobui.kanji.feature.dashboard.history

import androidx.activity.compose.BackHandler
import ru.mobui.kanji.core.designsystem.KanjiSearchToggle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import ru.mobui.kanji.core.designsystem.inputShape
import ru.mobui.kanji.core.designsystem.kanjiOutlinedTextFieldColors
import ru.mobui.kanji.core.wordui.SelectionFilterChip
import ru.mobui.kanji.domain.practice.Training
import ru.mobui.kanji.domain.practice.TrainingGradeLevel
import ru.mobui.kanji.domain.practice.TrainingStatus
import ru.mobui.kanji.domain.practice.summarize
import ru.mobui.kanji.feature.dashboard.R

internal class TrainingHistoryActions(
    val onBack: () -> Unit,
    val onOpen: (String) -> Unit,
    val onDelete: (String) -> Unit,
    val onQuery: (String) -> Unit,
    val onPeriod: (TrainingHistoryPeriod) -> Unit,
    val onClearPeriod: () -> Unit,
    val onRetry: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrainingHistoryScreen(
    state: TrainingHistoryState,
    query: String,
    period: TrainingHistoryPeriod,
    language: String,
    actions: TrainingHistoryActions,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    var searching by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = searching) {
        searching = false
        actions.onQuery("")
    }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var periodPickerOpen by rememberSaveable { mutableStateOf(false) }
    val today = remember(zone) { LocalDate.now(zone) }
    val days =
        remember(state.trainings, period, query, language, zone, today) {
            groupHistoryByDay(filterHistory(state.trainings, period, query, language, zone, today), zone)
        }
    val locale = remember(language) { Locale.forLanguageTag(language) }
    val timeFormatter =
        remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    val dateFormatter =
        remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale) }

    Scaffold(
        modifier.testTag("training-history-screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_checked)) },
                navigationIcon = {
                    IconButton(actions.onBack, Modifier.testTag("training-history-back")) {
                        Icon(
                            painterResource(ru.mobui.kanji.core.designsystem.R.drawable.ic_arrow_back),
                            stringResource(R.string.recipe_back),
                        )
                    }
                },
                actions = {
                    KanjiSearchToggle(
                        open = searching,
                        onToggle = {
                            searching = !searching
                            if (!searching) actions.onQuery("")
                        },
                        testTag = "training-history-search-toggle",
                        contentDescription = stringResource(
                            if (searching) R.string.history_hide_search else R.string.history_find,
                        ),
                    )
                },
            )
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            if (searching) {
                ru.mobui.kanji.core.designsystem.KanjiSearchBar(
                    query = query,
                    onQuery = actions.onQuery,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = stringResource(R.string.history_search_hint),
                    autoFocus = true,
                    testTag = "training-history-search",
                )
            }
            SelectionFilterChip(
                label =
                    if (period == TrainingHistoryPeriod.All) stringResource(R.string.history_period)
                    else periodOptionLabel(period),
                onOpen = { periodPickerOpen = true },
                onClear = if (period != TrainingHistoryPeriod.All) actions.onClearPeriod else null,
                clearLabel = stringResource(R.string.library_clear),
                modifier =
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("training-history-period"),
            )
            when {
                state.error ->
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.history_load_failed),
                            Modifier.weight(1f),
                        )
                        TextButton(actions.onRetry) {
                            Text(stringResource(R.string.recipe_retry))
                        }
                    }
                state.loading -> Box(Modifier.fillMaxSize())
                state.trainings.isEmpty() ->
                    HistoryEmptyState(
                        title = stringResource(R.string.history_empty),
                        hint = stringResource(R.string.history_empty_hint),
                        Modifier.fillMaxSize().padding(24.dp),
                    )
                days.isEmpty() ->
                    HistoryEmptyState(
                        title = stringResource(R.string.history_no_results),
                        hint = stringResource(R.string.history_no_results_hint),
                        Modifier.fillMaxSize().padding(24.dp),
                    )
                else ->
                    LazyColumn(
                        Modifier.fillMaxSize().testTag("training-history-list"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        days.forEach { group ->
                            item(key = "day-${group.day}") {
                                Text(
                                    dayTitle(group.day, today, dateFormatter),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp),
                                )
                            }
                            items(group.items, key = { it.id }) { training ->
                                HistoryCard(
                                    training = training,
                                    language = language,
                                    timeFormatter = timeFormatter,
                                    zone = zone,
                                    busy = state.busy,
                                    pendingDeleteId = pendingDeleteId,
                                    onOpen = { actions.onOpen(training.id) },
                                    onDeleteRequest = { pendingDeleteId = training.id },
                                )
                            }
                        }
                    }
            }
        }
    }

    if (periodPickerOpen) {
        PeriodPickerDialog(
            selected = period,
            onSelect = {
                actions.onPeriod(it)
                periodPickerOpen = false
            },
            onDismiss = { periodPickerOpen = false },
        )
    }

    val pending = pendingDeleteId?.let { id -> state.trainings.firstOrNull { it.id == id } }
    if (pending != null) {
        val name =
            pending.recipeNames.localizedName(
                language,
                stringResource(R.string.dashboard_mix),
            )
        ru.mobui.kanji.core.designsystem.KanjiDeleteConfirmDialog(
            title = stringResource(R.string.history_delete_title),
            message = "$name\n${stringResource(R.string.history_delete_text)}",
            onConfirm = {
                val id = pending.id
                pendingDeleteId = null
                actions.onDelete(id)
            },
            onDismiss = { pendingDeleteId = null },
            confirmText = stringResource(R.string.dictionary_remove_recipe),
            dismissText = stringResource(R.string.library_cancel),
            confirmTag = "training-history-delete-confirm",
            dismissTag = "training-history-delete-cancel",
        )
    }
}

@Composable
private fun HistoryCard(
    training: Training,
    language: String,
    timeFormatter: DateTimeFormatter,
    zone: ZoneId,
    busy: Boolean,
    pendingDeleteId: String?,
    onOpen: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val counts = remember(training) { training.taskCounts() }
    val unfinished = training.status == TrainingStatus.InProgress
    val isExcellent = remember(training, language) { training.isExcellent(language) }
    val name =
        training.recipeNames.localizedName(language, stringResource(R.string.dashboard_mix))
    val started = Instant.ofEpochMilli(training.startedAt).atZone(zone)
    val minutes = ((training.durationMs() + 59_999L) / 60_000L).coerceAtLeast(1L)
    val meta =
        if (unfinished) {
            val (current, total) = training.wordProgress()
            stringResource(
                R.string.history_card_meta_unfinished,
                stringResource(R.string.training_unfinished),
                current,
                total,
            )
        } else {
            stringResource(
                R.string.history_card_meta,
                timeFormatter.format(started),
                minutes.toInt(),
            )
        }
    val openLabel = stringResource(R.string.history_open, name)
    val removeLabel = stringResource(R.string.dictionary_remove_recipe)
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(pendingDeleteId, training.id) {
        if (pendingDeleteId != training.id &&
            dismissState.settledValue != SwipeToDismissBoxValue.Settled
        ) {
            dismissState.reset()
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    painterResource(R.drawable.recipe_remove),
                    contentDescription = null,
                    Modifier.padding(end = 24.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        enableDismissFromStartToEnd = false,
        gesturesEnabled = !busy,
        onDismiss = {
            if (it == SwipeToDismissBoxValue.EndToStart) onDeleteRequest()
        },
    ) {
        Surface(
            Modifier.fillMaxWidth()
                .testTag("training-history-${training.id}")
                .clickable(enabled = !busy, role = Role.Button, onClick = onOpen)
                .semantics(mergeDescendants = true) {
                    contentDescription = openLabel
                    role = Role.Button
                    onClick(label = openLabel) {
                        if (!busy) onOpen()
                        true
                    }
                    customActions =
                        listOf(
                            CustomAccessibilityAction(removeLabel) {
                                onDeleteRequest()
                                true
                            },
                        )
                },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TrainingMiniRing(counts)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (unfinished) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (isExcellent) {
                    Image(
                        painter = painterResource(R.drawable.ic_paw_stamp),
                        contentDescription = stringResource(R.string.history_stamp_excellent),
                        modifier =
                            Modifier
                                .size(24.dp)
                                .rotate(-10f)
                                .testTag("training-history-stamp-${training.id}"),
                    )
                }
                Icon(
                    painter = painterResource(ru.mobui.kanji.core.designsystem.R.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HistoryEmptyState(title: String, hint: String, modifier: Modifier = Modifier) {
    ru.mobui.kanji.core.designsystem.KanjiEmptyState(
        title = title,
        hint = hint,
        modifier = modifier,
        testTag = "training-history-empty",
    )
}

@Composable
private fun PeriodPickerDialog(
    selected: TrainingHistoryPeriod,
    onSelect: (TrainingHistoryPeriod) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_choose_period)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TrainingHistoryPeriod.entries.forEach { option ->
                    val label = periodOptionLabel(option)
                    Surface(
                        onClick = { onSelect(option) },
                        shape = MaterialTheme.shapes.medium,
                        color =
                            if (option == selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier =
                            Modifier.fillMaxWidth()
                                .testTag("training-history-period-${option.key}"),
                    ) {
                        Text(label, Modifier.padding(horizontal = 16.dp, vertical = 14.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onDismiss) { Text(stringResource(R.string.library_cancel)) }
        },
    )
}

@Composable
private fun periodOptionLabel(period: TrainingHistoryPeriod): String =
    when (period) {
        TrainingHistoryPeriod.Today -> stringResource(R.string.history_period_today)
        TrainingHistoryPeriod.Days7 -> stringResource(R.string.history_period_7)
        TrainingHistoryPeriod.Days30 -> stringResource(R.string.history_period_30)
        TrainingHistoryPeriod.All -> stringResource(R.string.history_period_all)
    }

@Composable
private fun dayTitle(
    day: LocalDate,
    today: LocalDate,
    formatter: DateTimeFormatter,
): String =
    when (dayRelation(day, today)) {
        0L -> stringResource(R.string.history_today)
        1L -> stringResource(R.string.history_yesterday)
        else -> formatter.format(day)
    }
