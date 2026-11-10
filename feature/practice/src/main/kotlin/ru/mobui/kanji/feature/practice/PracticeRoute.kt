package ru.mobui.kanji.feature.practice

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.mobui.kanji.domain.practice.PracticeResult

/**
 * Navigation-independent entry point. The host owns the ViewModel and result destination.
 *
 * Checker settings are not opened from PRACTICE indicators; they live under SETTINGS.
 */
@Composable
fun PracticeRoute(
    viewModel: PracticeViewModel,
    onResult: (PracticeResult) -> Unit,
    onBack: () -> Unit,
    wordLabel: String? = null,
    progressLabel: String? = null,
) {
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    BackHandler {
        if (!viewModel.uiState.value.gestureActive) confirmLeave = true
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val preview = viewModel.preview.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(viewModel, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.setActive(true)
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.setActive(false)
        }
        lifecycle.addObserver(observer)
        viewModel.setActive(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.setActive(false)
        }
    }
    LaunchedEffect(state.practice.result) { viewModel.deliverResult(onResult) }
    PracticeScreen(
        state,
        viewModel::onAction,
        viewModel::onPadEvent,
        activeStroke = { preview.value?.stroke },
        wordLabel = wordLabel,
        progressLabel = progressLabel,
        onBack = { confirmLeave = true },
    )
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            modifier = Modifier.testTag("practice-interrupt-dialog"),
            title = { Text(stringResource(R.string.interrupt_title)) },
            text = { Text(stringResource(R.string.interrupt_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLeave = false
                        onBack()
                    },
                    modifier = Modifier.testTag("practice-interrupt-confirm"),
                ) {
                    Text(
                        stringResource(R.string.interrupt_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmLeave = false },
                    modifier = Modifier.testTag("practice-interrupt-stay"),
                ) {
                    Text(
                        stringResource(R.string.interrupt_stay),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )
    }
}
