package ru.mobui.kanji.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
internal fun TrainingPicker(
    trainings: List<TrainingOverview>, selectedId: String,
    onSelect: (String) -> Unit, onEdit: (String) -> Unit, onCreate: () -> Unit, onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val matches = trainings.filter { it.name.contains(query.trim(), ignoreCase = true) }
    Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(Modifier.fillMaxSize().testTag("training-picker")) { insets ->
            Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onDismiss, Modifier.testTag("training-picker-back"),
                        contentPadding = PaddingValues(start = 0.dp, end = 12.dp)) {
                        Icon(painterResource(R.drawable.ic_training_back), null, Modifier.size(20.dp))
                        Text(stringResource(R.string.training_back))
                    }
                    Text(stringResource(R.string.training_picker), Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge)
                }
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().testTag("training-search"),
                    label = { Text(stringResource(R.string.training_search)) }, singleLine = true)
                OutlinedButton(onCreate, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("create-training")) {
                    Text(stringResource(R.string.training_create_action))
                }
                HorizontalDivider()
                if (matches.isEmpty()) {
                    Text(stringResource(R.string.training_search_empty), Modifier.testTag("training-search-empty"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("training-list"),
                    contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(matches, key = { it.id }) { training ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton({ onSelect(training.id) }, Modifier.weight(1f).heightIn(min = 64.dp)
                                .testTag("choose-training-${training.id}")
                                .semantics { selected = training.id == selectedId }) {
                                Column(Modifier.weight(1f)) {
                                    Text(training.name, color = MaterialTheme.colorScheme.onSurface)
                                    if (training.id == selectedId) Text(stringResource(R.string.training_current),
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                if (training.id == selectedId) {
                                    Icon(painterResource(R.drawable.ic_check_mark), null, Modifier.size(18.dp))
                                }
                            }
                            IconButton({ onEdit(training.id) }, Modifier.testTag("edit-training-${training.id}")) {
                                Icon(painterResource(R.drawable.ic_training_edit),
                                    stringResource(R.string.training_edit_named, training.name), Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TrainingEditor(
    existing: TrainingOverview?,
    onSubmit: (String, Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var words by rememberSaveable(existing?.id) { mutableStateOf((existing?.wordsPerSession ?: 10).toString()) }
    var seconds by rememberSaveable(existing?.id) { mutableStateOf((existing?.secondsPerWord ?: 60).toString()) }
    val wordCount = words.toIntOrNull()
    val time = seconds.toIntOrNull()
    val valid = name.isNotBlank() && wordCount != null && wordCount in 1..100 && time != null && time in 10..600
    Dialog(onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), modifier = Modifier.testTag("training-editor")) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(if (existing == null) R.string.training_create else R.string.training_edit),
                    style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(name, { name = it.take(60) }, Modifier.fillMaxWidth().testTag("training-name"),
                    label = { Text(stringResource(R.string.training_name)) }, singleLine = true)
                OutlinedTextField(words, { words = it }, Modifier.fillMaxWidth().testTag("training-words"),
                    label = { Text(stringResource(R.string.training_words)) },
                    supportingText = { Text(stringResource(R.string.training_words_range)) },
                    isError = wordCount == null || wordCount !in 1..100,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(seconds, { seconds = it }, Modifier.fillMaxWidth().testTag("training-seconds"),
                    label = { Text(stringResource(R.string.training_seconds)) },
                    supportingText = { Text(stringResource(R.string.training_seconds_range)) },
                    isError = time == null || time !in 10..600,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                Button(onClick = { if (valid) onSubmit(name, requireNotNull(wordCount), requireNotNull(time)) },
                    enabled = valid, modifier = Modifier.fillMaxWidth().testTag("save-training")) {
                    Text(stringResource(if (existing == null) R.string.training_create_select else R.string.training_save))
                }
                TextButton(onDismiss, Modifier.fillMaxWidth()) { Text(stringResource(R.string.training_cancel)) }
            }
        }
    }
}

@Composable
internal fun TrainingHistory(training: TrainingOverview, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("training-history-dialog"),
        title = { Text(stringResource(R.string.training_history)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(training.name, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.training_history_empty))
        } },
        confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.dashboard_close)) } })
}
