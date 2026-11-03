package ru.mobui.kanji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toString
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import ru.mobui.kanji.checker.shape.ShapeKanjiChecker
import ru.mobui.kanji.checker.writing.StrokeWritingChecker
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.data.kanjivg.BundledKanjiReferenceSource
import ru.mobui.kanji.domain.practice.AnswerStatus
import ru.mobui.kanji.domain.practice.CombinedKanjiChecker
import ru.mobui.kanji.domain.practice.EntryMode
import ru.mobui.kanji.domain.practice.EntryStatus
import ru.mobui.kanji.domain.practice.KanjiId
import ru.mobui.kanji.domain.practice.KanjiTask
import ru.mobui.kanji.domain.practice.PracticeResult
import ru.mobui.kanji.domain.practice.PracticeTask
import ru.mobui.kanji.domain.practice.TraceReferenceHint
import ru.mobui.kanji.feature.dashboard.DashboardDemo
import ru.mobui.kanji.feature.dashboard.DashboardScreen
import ru.mobui.kanji.feature.practice.CheckerSettingsScreen
import ru.mobui.kanji.feature.practice.PracticeRoute
import ru.mobui.kanji.feature.practice.PracticeViewModel
import ru.mobui.kanji.feature.settings.SettingsScreen

internal data class AppModels(
    val settingsModel: RecognitionSettingsViewModel,
    val trainingModel: DashboardTrainingViewModel,
    val dictionarySummary: DictionarySummaryViewModel,
)

internal data class DemoPracticeGraph(
    val task: PracticeTask,
    val source: BundledKanjiReferenceSource,
    val factory: ViewModelProvider.Factory,
)

internal class AppUiContext(
    val dependencies: KanjiApplication,
    val models: AppModels,
    val graph: DemoPracticeGraph,
    val modelStatus: JapaneseModelStatus,
    val settings: ru.mobui.kanji.domain.practice.RecognitionSettings,
) {
    val settingsModel
        get() = models.settingsModel

    val trainingModel
        get() = models.trainingModel

    val dictionarySummary
        get() = models.dictionarySummary

    val task
        get() = graph.task

    val source
        get() = graph.source

    val factory
        get() = graph.factory

    val modelReady
        get() = modelStatus == JapaneseModelStatus.Ready
}

@Composable
internal fun MainContent(
    dependencies: KanjiApplication,
    models: AppModels,
    graph: DemoPracticeGraph,
) {
    val settingsModel = models.settingsModel
    val modelStatus by dependencies.japaneseModel.status.collectAsStateWithLifecycle()
    val modelError by dependencies.japaneseModel.error.collectAsStateWithLifecycle()
    if (modelError) {
        AlertDialog(
            onDismissRequest = dependencies.japaneseModel::dismissError,
            title = { Text(stringResource(R.string.japanese_model_title)) },
            text = { Text(stringResource(R.string.japanese_model_failed)) },
            confirmButton = {
                TextButton(onClick = dependencies.japaneseModel::dismissError) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
    val loaded by settingsModel.loaded.collectAsStateWithLifecycle()
    val storageError by settingsModel.storageError.collectAsStateWithLifecycle()
    if (!loaded || storageError) {
        SettingsStorageStatus(storageError, settingsModel::retryStorage)
        return
    }
    var destination by rememberSaveable { mutableStateOf(AppDestination.Dashboard) }
    val savedSettings by settingsModel.settings.collectAsStateWithLifecycle()
    val modelReady = modelStatus == JapaneseModelStatus.Ready
    val settings =
        savedSettings.copy(
            readabilityEnabled = savedSettings.readabilityEnabled && modelReady,
        )
    LaunchedEffect(modelStatus, savedSettings) {
        if (modelStatus == JapaneseModelStatus.Missing && savedSettings.readabilityEnabled) {
            settingsModel.update(savedSettings.copy(readabilityEnabled = false))
        }
    }
    val context = AppUiContext(dependencies, models, graph, modelStatus, settings)
    AppDestinationContent(destination, context) { destination = it }
}

@Composable
private fun AppDestinationContent(
    destination: AppDestination,
    context: AppUiContext,
    navigate: (AppDestination) -> Unit,
) {
    when (destination) {
        AppDestination.Dictionary -> DictionaryDestination(context, navigate)
        AppDestination.Catalog -> CatalogDestination(context, navigate)
        AppDestination.Settings -> SettingsDestination(context, navigate)
        AppDestination.CheckerSettings -> CheckerSettingsDestination(context, navigate)
        AppDestination.Dashboard -> DashboardDestination(context, navigate)
        AppDestination.Practice -> PracticeDestination(context, navigate)
    }
}

@Composable
private fun DictionaryDestination(context: AppUiContext, navigate: (AppDestination) -> Unit) {
    with(context) {
        val revision by dependencies.catalogRevision.collectAsStateWithLifecycle()
        ru.mobui.kanji.feature.dictionary.DictionaryRoute(
            dependencies.dictionary,
            dependencies.catalog,
            onBack = { navigate(AppDestination.Dashboard) },
        ) { kanjiId, sourceId, back ->
            ru.mobui.kanji.feature.catalog.CatalogRoute(
                dependencies.catalog,
                back,
                kanjiId,
                sourceId,
                loadDrawing = { source.load(KanjiId("u${it.toString(APP_UNICODE_HEX_RADIX)}")) },
                revision = revision,
            )
        }
    }
}

@Composable
private fun CatalogDestination(context: AppUiContext, navigate: (AppDestination) -> Unit) {
    with(context) {
        val revision by dependencies.catalogRevision.collectAsStateWithLifecycle()
        ru.mobui.kanji.feature.catalog.CatalogRoute(
            dependencies.catalog,
            onBack = { navigate(AppDestination.Dashboard) },
            loadDrawing = { source.load(KanjiId("u${it.toString(APP_UNICODE_HEX_RADIX)}")) },
            revision = revision,
        )
    }
}

@Composable
private fun SettingsDestination(context: AppUiContext, navigate: (AppDestination) -> Unit) {
    with(context) {
        BackHandler { navigate(AppDestination.Dashboard) }
        SettingsScreen(
            onBack = { navigate(AppDestination.Dashboard) },
            onOpenChecker = { navigate(AppDestination.CheckerSettings) },
            modelLoaded = modelStatus == JapaneseModelStatus.Ready,
            modelLoading = modelStatus == JapaneseModelStatus.Loading,
            onDownloadModel = dependencies.japaneseModel::load,
        )
    }
}

@Composable
private fun CheckerSettingsDestination(context: AppUiContext, navigate: (AppDestination) -> Unit) {
    with(context) {
        BackHandler { navigate(AppDestination.Settings) }
        CheckerSettingsScreen(
            settings,
            settingsModel::update,
            onBack = { navigate(AppDestination.Settings) },
            onReset = settingsModel::reset,
            readabilityAvailable = modelReady,
        )
    }
}

@Composable
private fun DashboardDestination(context: AppUiContext, navigate: (AppDestination) -> Unit) {
    with(context) {
        val trainings by trainingModel.trainings.collectAsStateWithLifecycle()
        val selectedTraining by trainingModel.selected.collectAsStateWithLifecycle()
        val personalCount by dictionarySummary.count.collectAsStateWithLifecycle()
        DashboardScreen(
            DashboardDemo.state.copy(
                vocabulary = DashboardDemo.state.vocabulary.copy(personalTotal = personalCount),
            ),
            onStartPractice = { navigate(AppDestination.Practice) },
            onOpenSettings = { navigate(AppDestination.Settings) },
            onOpenCatalog = { navigate(AppDestination.Catalog) },
            onOpenDictionary = { navigate(AppDestination.Dictionary) },
            trainings = trainings,
            selectedTrainingId = selectedTraining,
            onSelectTraining = trainingModel::select,
            onSaveTraining = trainingModel::submit,
        )
    }
}

@Composable
private fun PracticeDestination(context: AppUiContext, navigate: (AppDestination) -> Unit) {
    with(context) {
        val practice: PracticeViewModel = viewModel(key = task.taskId, factory = factory)
        val closeAndReset: () -> Unit = {
            practice.restart()
            navigate(AppDestination.Dashboard)
        }
        BackHandler { closeAndReset() }
        LaunchedEffect(modelStatus) { practice.onCheckerSettingsChanged() }
        val state by practice.uiState.collectAsStateWithLifecycle()
        val result = state.practice.result
        var resultReceived by rememberSaveable { mutableStateOf(false) }
        if (result == null || !resultReceived) {
            PracticeRoute(
                practice,
                onResult = { resultReceived = true },
                onBack = { navigate(AppDestination.Dashboard) },
                settings = settings,
                wordLabel = stringResource(R.string.demo_word),
                readabilityAvailable = modelReady,
                onSettingsChanged = {
                    settingsModel.update(it)
                    practice.onCheckerSettingsChanged()
                },
                onResetSettings = {
                    settingsModel.reset()
                    practice.onCheckerSettingsChanged()
                },
            )
        } else {
            // Demo destination. A session coordinator can consume the same typed
            // result.
            PracticeSummary(
                result,
                onRestart = {
                    practice.restart()
                    resultReceived = false
                },
                onClose = {
                    practice.restart()
                    resultReceived = false
                    navigate(AppDestination.Dashboard)
                },
            )
        }
    }
}

