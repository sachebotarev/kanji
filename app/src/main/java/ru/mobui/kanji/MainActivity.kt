package ru.mobui.kanji

import android.os.Bundle
import androidx.activity.compose.BackHandler
import ru.mobui.kanji.feature.settings.SettingsScreen
import ru.mobui.kanji.feature.practice.CheckerSettingsScreen
import ru.mobui.kanji.feature.dashboard.DashboardScreen
import ru.mobui.kanji.feature.dashboard.DashboardDemo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import ru.mobui.kanji.checker.shape.ShapeKanjiChecker
import ru.mobui.kanji.checker.shape.StrokeWritingChecker
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.data.kanjivg.BundledKanjiReferenceSource
import ru.mobui.kanji.domain.practice.*
import ru.mobui.kanji.feature.practice.PracticeRoute
import ru.mobui.kanji.feature.practice.PracticeViewModel

private enum class AppDestination { Dashboard, Practice, Settings, CheckerSettings, Catalog, Dictionary }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val task = PracticeTask(
            taskId = "demo-eeg-timed",
            russianWord = getString(R.string.demo_word),
            kanji = listOf(
                KanjiTask("eeg-0", KanjiId("u8133"), EntryMode.User),
                KanjiTask("eeg-1", KanjiId("u6ce2"), EntryMode.User),
                KanjiTask("eeg-2", KanjiId("u691c"), EntryMode.User),
                KanjiTask("eeg-3", KanjiId("u67fb"), EntryMode.Prefilled),
            ),
        )
        val source = BundledKanjiReferenceSource(assets, Dispatchers.IO)
        val dependencies = application as KanjiApplication
        val settingsModel = ViewModelProvider(this, viewModelFactory {
            initializer { RecognitionSettingsViewModel(createSavedStateHandle(), dependencies.recognitionSettings, dependencies.persistenceScope) }
        })[RecognitionSettingsViewModel::class.java]
        val trainingModel = ViewModelProvider(this, viewModelFactory {
            initializer { DashboardTrainingViewModel(createSavedStateHandle(),
                getString(ru.mobui.kanji.feature.dashboard.R.string.dashboard_mix)) }
        })[DashboardTrainingViewModel::class.java]
        val dictionarySummary = ViewModelProvider(this, viewModelFactory {
            initializer { DictionarySummaryViewModel(dependencies.dictionary) }
        })[DictionarySummaryViewModel::class.java]
        val readability = dependencies.readability
        val checker = CombinedKanjiChecker(
            readability,
            shape = { config -> ShapeKanjiChecker(source, Dispatchers.Default,
                config = config.copy(checkStrokeOrder = false, checkStrokeDirection = false),
                diagnosticsEnabled = BuildConfig.DEBUG) },
            writing = StrokeWritingChecker(source, Dispatchers.Default),
            settings = { settingsModel.settings.value.let {
                it.copy(readabilityEnabled = it.readabilityEnabled && dependencies.japaneseModel.status.value == JapaneseModelStatus.Ready)
            } },
        )
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(PracticeViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return PracticeViewModel(task, source, checker, hintAlgorithm = TraceReferenceHint(), showCheckDiagnostics = BuildConfig.DEBUG) as T
            }
        }
        setContent {
            KanjiTheme {
                val modelStatus by dependencies.japaneseModel.status.collectAsStateWithLifecycle()
                val modelError by dependencies.japaneseModel.error.collectAsStateWithLifecycle()
                if (modelError) {
                    AlertDialog(
                        onDismissRequest = dependencies.japaneseModel::dismissError,
                        title = { Text(stringResource(R.string.japanese_model_title)) },
                        text = { Text(stringResource(R.string.japanese_model_failed)) },
                        confirmButton = { TextButton(onClick = dependencies.japaneseModel::dismissError) {
                            Text(stringResource(R.string.close))
                        } },
                    )
                }
                val loaded by settingsModel.loaded.collectAsStateWithLifecycle()
                val storageError by settingsModel.storageError.collectAsStateWithLifecycle()
                if (!loaded || storageError) {
                    SettingsStorageStatus(storageError, settingsModel::retryStorage)
                    return@KanjiTheme
                }
                var destination by rememberSaveable { mutableStateOf(AppDestination.Dashboard) }
                val savedSettings by settingsModel.settings.collectAsStateWithLifecycle()
                val modelReady = modelStatus == JapaneseModelStatus.Ready
                val settings = savedSettings.copy(readabilityEnabled = savedSettings.readabilityEnabled && modelReady)
                LaunchedEffect(modelStatus, savedSettings) {
                    if (modelStatus == JapaneseModelStatus.Missing && savedSettings.readabilityEnabled) {
                        settingsModel.update(savedSettings.copy(readabilityEnabled = false))
                    }
                }
                if (destination == AppDestination.Dictionary) {
                    ru.mobui.kanji.feature.dictionary.DictionaryRoute(dependencies.dictionary, dependencies.catalog,
                        onBack = { destination = AppDestination.Dashboard }) { kanjiId, sourceId, back ->
                        ru.mobui.kanji.feature.catalog.CatalogRoute(dependencies.catalog, back, kanjiId, sourceId)
                    }
                } else if (destination == AppDestination.Catalog) {
                    ru.mobui.kanji.feature.catalog.CatalogRoute(dependencies.catalog, onBack = { destination = AppDestination.Dashboard })
                } else if (destination == AppDestination.Settings) {
                    BackHandler { destination = AppDestination.Dashboard }
                    SettingsScreen(onBack = { destination = AppDestination.Dashboard },
                        onOpenChecker = { destination = AppDestination.CheckerSettings },
                        modelLoaded = modelStatus == JapaneseModelStatus.Ready,
                        modelLoading = modelStatus == JapaneseModelStatus.Loading,
                        onDownloadModel = dependencies.japaneseModel::load)
                } else if (destination == AppDestination.CheckerSettings) {
                    BackHandler { destination = AppDestination.Settings }
                    CheckerSettingsScreen(settings, settingsModel::update,
                        onBack = { destination = AppDestination.Settings }, onReset = settingsModel::reset,
                        readabilityAvailable = modelReady)
                } else if (destination == AppDestination.Dashboard) {
                    val trainings by trainingModel.trainings.collectAsStateWithLifecycle()
                    val selectedTraining by trainingModel.selected.collectAsStateWithLifecycle()
                    val personalCount by dictionarySummary.count.collectAsStateWithLifecycle()
                    DashboardScreen(DashboardDemo.state.copy(vocabulary = DashboardDemo.state.vocabulary.copy(personalTotal = personalCount)),
                        onStartPractice = { destination = AppDestination.Practice },
                        onOpenSettings = { destination = AppDestination.Settings },
                        onOpenCatalog = { destination = AppDestination.Catalog },
                        onOpenDictionary = { destination = AppDestination.Dictionary },
                        trainings = trainings, selectedTrainingId = selectedTraining,
                        onSelectTraining = trainingModel::select, onSaveTraining = trainingModel::submit)
                } else {
                    BackHandler { destination = AppDestination.Dashboard }
                    val practice: PracticeViewModel = viewModel(key = task.taskId, factory = factory)
                    LaunchedEffect(modelStatus) { practice.onCheckerSettingsChanged() }
                    val state by practice.uiState.collectAsStateWithLifecycle()
                    val result = state.practice.result
                    var resultReceived by rememberSaveable { mutableStateOf(false) }
                    if (result == null || !resultReceived) {
                        PracticeRoute(practice, onResult = { resultReceived = true }, onBack = { destination = AppDestination.Dashboard },
                            settings = settings,
                            wordLabel = stringResource(R.string.demo_word),
                            readabilityAvailable = modelReady,
                            onSettingsChanged = { settingsModel.update(it); practice.onCheckerSettingsChanged() },
                            onResetSettings = { settingsModel.reset(); practice.onCheckerSettingsChanged() },
                        )
                    } else {
                        // Demo destination. A session coordinator can consume the same typed result.
                        PracticeSummary(result, onClose = { destination = AppDestination.Dashboard })
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeSummary(result: PracticeResult, onClose: () -> Unit) {
    Scaffold { safePadding ->
        Column(Modifier.fillMaxSize().padding(safePadding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.demo_word), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(when (result.status) {
                AnswerStatus.Correct -> R.string.result_correct
                AnswerStatus.Assisted -> R.string.result_assisted
                AnswerStatus.Skipped -> R.string.result_skipped
                AnswerStatus.TimedOut -> R.string.result_timed_out
            }), style = MaterialTheme.typography.titleLarge)
            result.kanji.forEachIndexed { index, cell ->
                Text(stringResource(R.string.result_cell, index + 1, stringResource(when (cell.status) {
                    EntryStatus.Empty -> R.string.entry_empty
                    EntryStatus.Editing -> R.string.entry_editing
                    EntryStatus.Correct -> R.string.result_correct
                    EntryStatus.Incorrect -> R.string.entry_incorrect
                    EntryStatus.Prefilled -> R.string.entry_prefilled
                }), cell.elapsedMs / 1000))
            }
            Text(stringResource(R.string.result_time, result.elapsedMs / 1000))
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.reference_attribution), style = MaterialTheme.typography.bodySmall)
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.close)) }
        }
    }
}
