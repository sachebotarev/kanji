package ru.mobui.kanji.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.feature.dashboard.widgets.ActivityDetailsDialog
import ru.mobui.kanji.feature.dashboard.widgets.ActivityWidget
import ru.mobui.kanji.feature.dashboard.widgets.DashboardIcon
import ru.mobui.kanji.feature.dashboard.widgets.IllustratedIcon
import ru.mobui.kanji.feature.dashboard.widgets.LaunchTrainingWidget
import ru.mobui.kanji.feature.dashboard.widgets.RecipeBookEntry
import ru.mobui.kanji.feature.dashboard.widgets.UnfinishedTrainingDialog
import ru.mobui.kanji.feature.dashboard.widgets.VocabularyWidget
import ru.mobui.kanji.feature.dashboard.widgets.WordStatisticsDialog
import ru.mobui.kanji.feature.dashboard.widgets.WordStatisticsWidget

@Composable
fun DashboardScreen(
    state: DashboardState,
    onStartTraining: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    unfinished: UnfinishedTrainingSummary? = null,
    onContinueTraining: () -> Unit = {},
    onStartNewTraining: () -> Unit = {},
    onOpenRecipeBook: () -> Unit = {},
    onOpenCatalog: () -> Unit = {},
    onOpenDictionary: () -> Unit = {},
    onOpenReferences: () -> Unit = {},
) {
    var unfinishedOpen by rememberSaveable { mutableStateOf(false) }
    if (unfinishedOpen && unfinished != null) {
        UnfinishedTrainingDialog(
            unfinished,
            onContinue = {
                unfinishedOpen = false
                onContinueTraining()
            },
            onStartNew = {
                unfinishedOpen = false
                onStartNewTraining()
            },
            onDismiss = { unfinishedOpen = false },
        )
    }
    var activityOpen by rememberSaveable { mutableStateOf(false) }
    if (activityOpen) ActivityDetailsDialog(state.activity, onDismiss = { activityOpen = false })
    var statisticsOpen by rememberSaveable { mutableStateOf(false) }
    if (statisticsOpen)
        WordStatisticsDialog(state.statistics, onDismiss = { statisticsOpen = false })
    Scaffold(modifier.testTag("dashboard-screen")) { insets ->
        Column(
            Modifier.fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.dashboard_home),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onOpenSettings, Modifier.testTag("open-app-settings")) {
                    IllustratedIcon(
                        DashboardIcon.Settings,
                        Modifier.size(30.dp),
                        stringResource(R.string.dashboard_settings),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WordStatisticsWidget(
                    state.statistics,
                    { statisticsOpen = true },
                    Modifier.weight(1f),
                )
                ActivityWidget(state.activity, { activityOpen = true }, Modifier.weight(1f))
            }
            LaunchTrainingWidget(
                unfinished = unfinished != null,
                onClick = {
                    if (unfinished != null) unfinishedOpen = true else onStartTraining()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            RecipeBookEntry(onOpenRecipeBook, Modifier.fillMaxWidth())
            VocabularyWidget(
                state.vocabulary,
                Modifier.fillMaxWidth(),
                onOpenCatalog,
                onOpenDictionary,
                onOpenReferences,
            )
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
private fun DashboardLightPreview() {
    KanjiTheme(false) {
        DashboardScreen(DashboardDemo.state, {}, {})
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
private fun DashboardDarkPreview() {
    KanjiTheme(true) {
        DashboardScreen(DashboardDemo.state, {}, {})
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
private fun DashboardUnfinishedPreview() {
    KanjiTheme(false) {
        DashboardScreen(
            DashboardDemo.state,
            {},
            {},
            unfinished = UnfinishedTrainingSummary("Умный микс", "Слово 4 из 20"),
        )
    }
}
