package ru.mobui.kanji.feature.dashboard

import ru.mobui.kanji.core.designsystem.inputShape
import ru.mobui.kanji.core.designsystem.kanjiOutlinedTextFieldColors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toString
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun RecipePicker(recipes: List<RecipeOverview>, selectedId: String, actions: RecipePickerActions) {
    with(actions) {
    var query by rememberSaveable { mutableStateOf("") }
    val matches = recipes.filter { it.name.contains(query.trim(), ignoreCase = true) }
    Dialog(
        onDismiss,
        properties =
            DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Scaffold(Modifier.fillMaxSize().testTag("recipe-picker")) { insets ->
            Column(
                Modifier.fillMaxSize().padding(insets).padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onDismiss,
                        Modifier.testTag("recipe-picker-back"),
                        contentPadding = PaddingValues(start = 0.dp, end = 12.dp),
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_recipe_back),
                            null,
                            Modifier.size(20.dp),
                        )
                        Text(stringResource(R.string.recipe_back))
                    }
                    Text(
                        stringResource(R.string.recipe_picker),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                OutlinedTextField(
                    query,
                    { query = it },
                    Modifier.fillMaxWidth().testTag("recipe-search"),
                    label = { Text(stringResource(R.string.recipe_search)) },
                    shape = MaterialTheme.inputShape,
                    singleLine = true,
                    colors = kanjiOutlinedTextFieldColors(),
                )
                OutlinedButton(
                    onCreate,
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("create-recipe"),
                ) {
                    Text(stringResource(R.string.recipe_create_action))
                }
                HorizontalDivider()
                if (matches.isEmpty()) {
                    Text(
                        stringResource(R.string.recipe_search_empty),
                        Modifier.testTag("recipe-search-empty"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f).testTag("recipe-list"),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(matches, key = { it.id }) { recipe ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                { onSelect(recipe.id) },
                                Modifier.weight(1f)
                                    .heightIn(min = 64.dp)
                                    .testTag("choose-recipe-${recipe.id}")
                                    .semantics { selected = recipe.id == selectedId },
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(recipe.name, color = MaterialTheme.colorScheme.onSurface)
                                    if (recipe.id == selectedId)
                                        Text(
                                            stringResource(R.string.recipe_current),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                }
                                if (recipe.id == selectedId) {
                                    Icon(
                                        painterResource(R.drawable.ic_check_mark),
                                        null,
                                        Modifier.size(18.dp),
                                    )
                                }
                            }
                            IconButton(
                                { onEdit(recipe.id) },
                                Modifier.testTag("edit-recipe-${recipe.id}"),
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_recipe_edit),
                                    stringResource(R.string.recipe_edit_named, recipe.name),
                                    Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    }
}

@Composable
internal fun LegacyRecipeEditor(
    existing: RecipeOverview?,
    onSubmit: (String, Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var words by
        rememberSaveable(existing?.id) {
            mutableStateOf((existing?.wordsPerTraining ?: DEFAULT_WORDS_PER_TRAINING).toString())
        }
    var seconds by
        rememberSaveable(existing?.id) {
            mutableStateOf((existing?.secondsPerWord ?: DEFAULT_SECONDS_PER_WORD).toString())
        }
    val wordCount = words.toIntOrNull()
    val time = seconds.toIntOrNull()
    val valid =
        name.isNotBlank() &&
            wordCount != null &&
            wordCount in 1..MAX_WORDS_PER_TRAINING &&
            time != null &&
            time in DEFAULT_WORDS_PER_TRAINING..MAX_SECONDS_PER_WORD
    Dialog(onDismiss) {
        val focusManager = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        Surface(shape = RoundedCornerShape(24.dp), modifier = Modifier.testTag("recipe-editor")) {
            Column(
                Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(
                        if (existing == null) R.string.recipe_create else R.string.recipe_edit,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
                OutlinedTextField(
                    name,
                    { name = it.take(DEFAULT_SECONDS_PER_WORD) },
                    Modifier.fillMaxWidth().testTag("recipe-name"),
                    label = { Text(stringResource(R.string.legacy_recipe_name)) },
                    shape = MaterialTheme.inputShape,
                    singleLine = true,
                    colors = kanjiOutlinedTextFieldColors(),
                )
                OutlinedTextField(
                    words,
                    { words = it },
                    Modifier.fillMaxWidth().testTag("recipe-words"),
                    label = { Text(stringResource(R.string.recipe_words)) },
                    shape = MaterialTheme.inputShape,
                    supportingText = { Text(stringResource(R.string.recipe_words_range)) },
                    isError = wordCount == null || wordCount !in 1..100,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = kanjiOutlinedTextFieldColors(),
                )
                OutlinedTextField(
                    seconds,
                    { seconds = it },
                    Modifier.fillMaxWidth().testTag("recipe-seconds"),
                    label = { Text(stringResource(R.string.legacy_recipe_seconds)) },
                    shape = MaterialTheme.inputShape,
                    supportingText = { Text(stringResource(R.string.recipe_seconds_range)) },
                    isError = time == null || time !in 10..600,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = kanjiOutlinedTextFieldColors(),
                )
                Button(
                    onClick = {
                        if (valid) {
                            focusManager.clearFocus()
                            keyboard?.hide()
                            onSubmit(name, requireNotNull(wordCount), requireNotNull(time))
                        }
                    },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth().testTag("save-recipe"),
                ) {
                    Text(
                        stringResource(
                            if (existing == null) R.string.recipe_create_select
                            else R.string.legacy_recipe_save,
                        ),
                    )
                }
                TextButton(onDismiss, Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.recipe_cancel))
                }
            }
        }
    }
}

@Composable
internal fun TrainingHistory(recipe: RecipeOverview, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("training-history-dialog"),
        title = { Text(stringResource(R.string.training_history)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(recipe.name, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.training_history_empty))
            }
        },
        confirmButton = {
            TextButton(onDismiss) { Text(stringResource(R.string.dashboard_close)) }
        },
    )
}

private const val DEFAULT_WORDS_PER_TRAINING = 10
private const val DEFAULT_SECONDS_PER_WORD = 60
private const val MAX_WORDS_PER_TRAINING = 100
private const val MAX_SECONDS_PER_WORD = 600
