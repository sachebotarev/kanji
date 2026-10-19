package ru.mobui.kanji

import android.os.Bundle
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
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.data.kanjivg.BundledKanjiReferenceSource
import ru.mobui.kanji.domain.practice.*
import ru.mobui.kanji.feature.practice.PracticeRoute
import ru.mobui.kanji.feature.practice.PracticeViewModel

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
        val settingsModel = ViewModelProvider(this, viewModelFactory {
            initializer { RecognitionSettingsViewModel(createSavedStateHandle()) }
        })[RecognitionSettingsViewModel::class.java]
        val checker = ShapeKanjiChecker(
            source, Dispatchers.Default, diagnosticsEnabled = BuildConfig.DEBUG,
            configProvider = { settingsModel.settings.value },
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
                val practice: PracticeViewModel = viewModel(key = task.taskId, factory = factory)
                val settings by settingsModel.settings.collectAsStateWithLifecycle()
                val state by practice.uiState.collectAsStateWithLifecycle()
                val result = state.practice.result
                var resultReceived by rememberSaveable { mutableStateOf(false) }
                if (result == null || !resultReceived) {
                    PracticeRoute(practice, onResult = { resultReceived = true }, onBack = ::finish,
                        settings = settings,
                        wordLabel = stringResource(R.string.demo_word),
                        onSettingsChanged = { settingsModel.update(it); practice.onCheckerSettingsChanged() },
                        onResetSettings = { settingsModel.reset(); practice.onCheckerSettingsChanged() },
                    )
                } else {
                    // Demo destination. A session coordinator can consume the same typed result.
                    PracticeSummary(result, onClose = ::finish)
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
