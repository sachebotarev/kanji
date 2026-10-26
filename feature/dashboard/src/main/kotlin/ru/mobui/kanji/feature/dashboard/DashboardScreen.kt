package ru.mobui.kanji.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.feature.dashboard.widgets.*

@Composable
fun DashboardScreen(
    state: DashboardState,
    onStartPractice: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    trainings: List<TrainingOverview>,
    onOpenCatalog: () -> Unit = {},
    onOpenDictionary: () -> Unit = {},
    selectedTrainingId: String = "default",
    onSelectTraining: (String) -> Unit = {},
    onSaveTraining: (String?, String, Int, Int) -> Unit = { _, _, _, _ -> },
) {
    val training = trainings.first { it.id == selectedTrainingId }
    var trainingDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    when (trainingDialog) {
        "choose" -> TrainingPicker(trainings, selectedTrainingId,
            onSelect = { onSelectTraining(it); trainingDialog = null },
            onEdit = { editingId = it; trainingDialog = "edit" },
            onCreate = { trainingDialog = "create" }, onDismiss = { trainingDialog = null })
        "create", "edit" -> TrainingEditor(if (trainingDialog == "edit") trainings.first { it.id == editingId } else null,
            onSubmit = { name, words, seconds ->
                onSaveTraining(if (trainingDialog == "edit") editingId else null, name, words, seconds)
                trainingDialog = if (trainingDialog == "edit") "choose" else null
            }, onDismiss = { trainingDialog = if (trainingDialog == "edit") "choose" else null })
        "history" -> TrainingHistory(training, onDismiss = { trainingDialog = null })
    }
    var activityOpen by rememberSaveable { mutableStateOf(false) }
    if (activityOpen) ActivityDetailsDialog(state.activity, onDismiss = { activityOpen = false })
    var statisticsOpen by rememberSaveable { mutableStateOf(false) }
    if (statisticsOpen) WordStatisticsDialog(state.statistics, onDismiss = { statisticsOpen = false })
    Scaffold(modifier.testTag("dashboard-screen")) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.dashboard_home), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                IconButton(onOpenSettings, Modifier.testTag("open-app-settings")) {
                    IllustratedIcon(DashboardIcon.Settings, Modifier.size(30.dp), stringResource(R.string.dashboard_settings))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WordStatisticsWidget(state.statistics, { statisticsOpen = true }, Modifier.weight(1f))
                ActivityWidget(state.activity, { activityOpen = true }, Modifier.weight(1f))
            }
            CurrentTrainingWidget(training,
                onSelect = { trainingDialog = "choose" },
                onStart = onStartPractice,
                onHistory = { trainingDialog = "history" }, modifier = Modifier.fillMaxWidth())
            VocabularyWidget(state.vocabulary, Modifier.fillMaxWidth(), onOpenCatalog, onOpenDictionary)

        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
private fun DashboardLightPreview() { KanjiTheme(false) { DashboardScreen(DashboardDemo.state, {}, {},
    trainings = listOf(TrainingOverview("default", stringResource(R.string.dashboard_mix)))) } }

@Preview(showBackground = true, locale = "ru")
@Composable
private fun DashboardDarkPreview() { KanjiTheme(true) { DashboardScreen(DashboardDemo.state, {}, {},
    trainings = listOf(TrainingOverview("default", stringResource(R.string.dashboard_mix)))) } }
