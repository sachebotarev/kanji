package ru.mobui.kanji.feature.practice

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import ru.mobui.kanji.domain.practice.RecognitionSettings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.mobui.kanji.domain.practice.PracticeResult

/** Navigation-independent entry point. The host owns the ViewModel and result destination. */
@Composable
fun PracticeRoute(
    viewModel: PracticeViewModel,
    onResult: (PracticeResult) -> Unit,
    onBack: () -> Unit,
    settings: RecognitionSettings? = null,
    onSettingsChanged: (RecognitionSettings) -> Unit = {},
    onResetSettings: () -> Unit = {},
    wordLabel: String? = null,
    readabilityAvailable: Boolean = true,
) {
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(settingsOpen) { settingsOpen = false }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val preview = viewModel.preview.collectAsStateWithLifecycle()
    BackHandler(!settingsOpen) { if (!state.gestureActive) onBack() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(viewModel, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.setActive(true)
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.setActive(false)
        }
        lifecycle.addObserver(observer)
        viewModel.setActive(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose { lifecycle.removeObserver(observer); viewModel.setActive(false) }
    }
    LaunchedEffect(state.practice.result) { viewModel.deliverResult(onResult) }
    if (settingsOpen && settings != null) {
        CheckerSettingsScreen(settings, onSettingsChanged, { settingsOpen = false }, onResetSettings, preview = state, readabilityAvailable = readabilityAvailable)
    } else PracticeScreen(
        state, viewModel::onAction, viewModel::onPadEvent,
        activeStroke = { preview.value?.stroke }, onBack = onBack,
        onOpenSettings = if (settings != null) ({ settingsOpen = true }) else null,
        wordLabel = wordLabel,
    )
}
