package ru.mobui.kanji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

internal enum class AppDestination {
    Dashboard,
    Practice,
    Settings,
    CheckerSettings,
    Catalog,
    References,
    Dictionary,
    RecipeBuilder,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dependencies = application as KanjiApplication
        val models = createModels(dependencies)
        val graph = createPracticeGraph(dependencies, models.settingsModel)
        setContent { KanjiTheme { MainContent(dependencies, models, graph) } }
    }

    private fun createTask(): PracticeTask {
        return PracticeTask(
            taskId = "demo-eeg-timed",
            russianWord = getString(R.string.demo_word),
            kanji =
                listOf(
                    KanjiTask("eeg-0", KanjiId("u8133"), EntryMode.User),
                    KanjiTask("eeg-1", KanjiId("u6ce2"), EntryMode.User),
                    KanjiTask("eeg-2", KanjiId("u691c"), EntryMode.User),
                    KanjiTask("eeg-3", KanjiId("u67fb"), EntryMode.Prefilled),
                ),
        )
    }

    private fun createModels(dependencies: KanjiApplication): AppModels {
        val settingsModel =
            ViewModelProvider(
                this,
                viewModelFactory {
                    initializer {
                        RecognitionSettingsViewModel(
                            createSavedStateHandle(),
                            dependencies.recognitionSettings,
                            dependencies.persistenceScope,
                        )
                    }
                },
            )[RecognitionSettingsViewModel::class.java]
        val dictionarySummary =
            ViewModelProvider(
                this,
                viewModelFactory {
                    initializer { DictionarySummaryViewModel(dependencies.dictionary) }
                },
            )[DictionarySummaryViewModel::class.java]
        return AppModels(settingsModel, dictionarySummary)
    }

    private fun createPracticeGraph(
        dependencies: KanjiApplication,
        settingsModel: RecognitionSettingsViewModel,
    ): DemoPracticeGraph {
        val task = createTask()
        val source = BundledKanjiReferenceSource(assets, Dispatchers.IO)
        val checker = createChecker(dependencies, settingsModel, source)
        val factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(PracticeViewModel::class.java))
                    @Suppress("UNCHECKED_CAST")
                    return (PracticeViewModel(
                        task,
                        source,
                        checker,
                        hintAlgorithm = TraceReferenceHint(),
                        showCheckDiagnostics = BuildConfig.DEBUG,
                    ) as T)
                }
            }
        return DemoPracticeGraph(task, source, factory)
    }

    private fun createChecker(
        dependencies: KanjiApplication,
        settingsModel: RecognitionSettingsViewModel,
        source: BundledKanjiReferenceSource,
    ): CombinedKanjiChecker {
        val readability = dependencies.readability
        return CombinedKanjiChecker(
            readability,
            shape = { config ->
                ShapeKanjiChecker(
                    source,
                    Dispatchers.Default,
                    config = config.copy(checkStrokeOrder = false, checkStrokeDirection = false),
                    diagnosticsEnabled = BuildConfig.DEBUG,
                )
            },
            writing = StrokeWritingChecker(source, Dispatchers.Default),
            settings = {
                settingsModel.settings.value.let {
                    it.copy(
                        readabilityEnabled =
                            it.readabilityEnabled &&
                                dependencies.japaneseModel.status.value ==
                                    JapaneseModelStatus.Ready,
                    )
                }
            },
        )
    }
}

@Composable
internal fun PracticeSummary(
    result: PracticeResult,
    onRestart: () -> Unit = {},
    onClose: () -> Unit,
) {
    Scaffold { safePadding ->
        Column(
            Modifier.fillMaxSize().padding(safePadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.demo_word), style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(
                    when (result.status) {
                        AnswerStatus.Correct -> R.string.result_correct
                        AnswerStatus.Assisted -> R.string.result_assisted
                        AnswerStatus.Skipped -> R.string.result_skipped
                        AnswerStatus.TimedOut -> R.string.result_timed_out
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            result.kanji.forEachIndexed { index, cell ->
                Text(
                    stringResource(
                        R.string.result_cell,
                        index + 1,
                        stringResource(
                            when (cell.status) {
                                EntryStatus.Empty -> R.string.entry_empty
                                EntryStatus.Editing -> R.string.entry_editing
                                EntryStatus.Correct -> R.string.result_correct
                                EntryStatus.Incorrect -> R.string.entry_incorrect
                                EntryStatus.Prefilled -> R.string.entry_prefilled
                            },
                        ),
                        cell.elapsedMs / MILLIS_PER_SECOND,
                    ),
                )
            }
            Text(stringResource(R.string.result_time, result.elapsedMs / MILLIS_PER_SECOND))
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.reference_attribution),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onRestart, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_storage_retry))
                }
                Button(onClick = onClose, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

internal const val APP_UNICODE_HEX_RADIX = 16
private const val MILLIS_PER_SECOND = 1000
