package ru.mobui.kanji.feature.dashboard.builder

import ru.mobui.kanji.core.designsystem.inputShape
import ru.mobui.kanji.core.designsystem.kanjiOutlinedTextFieldColors

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.UUID
import kotlinx.coroutines.launch
import ru.mobui.kanji.feature.dashboard.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.RecipeName
import ru.mobui.kanji.domain.catalog.WordTagAssignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import ru.mobui.kanji.domain.catalog.materialWords
import ru.mobui.kanji.domain.catalog.recipeWords
import ru.mobui.kanji.domain.catalog.validated

private const val DefaultTaskCount = 20
private const val DefaultSecondsPerSymbol = 10

/** Local recipe draft. The owner persists onSave and handles navigation only after success. */
@Composable
internal fun RecipeEditorScreen(initial: Recipe, snapshot: CatalogSnapshot, tags: List<DictionaryTag>,
    assignments: List<WordTagAssignment>, language: String, onSave: (Recipe) -> Unit,
    onBack: () -> Unit, onWord: (Int) -> Unit, onManageTags: () -> Unit,
    modifier: Modifier = Modifier, saving: Boolean = false, saveError: String? = null,
    creating: Boolean = initial.names.none { it.name.isNotBlank() }) {
    var draft by rememberSaveable(initial.id, stateSaver = RecipeSaver) { mutableStateOf(initial) }
    var countMode by rememberSaveable(initial.id) { mutableStateOf(initial.taskCount != null) }
    var timerMode by rememberSaveable(initial.id) { mutableStateOf(initial.secondsPerSymbol != null) }
    var count by rememberSaveable(initial.id) { mutableStateOf(initial.taskCount?.toString().orEmpty()) }
    var seconds by rememberSaveable(initial.id) { mutableStateOf(initial.secondsPerSymbol?.toString().orEmpty()) }
    var material by rememberSaveable(initial.id) { mutableStateOf<Bundle?>(null) }
    var undo by rememberSaveable(initial.id) { mutableStateOf<Bundle?>(null) }
    var preview by rememberSaveable(initial.id) { mutableStateOf(false) }
    var expanded by rememberSaveable(initial.id) { mutableStateOf(false) }
    var attempted by rememberSaveable(initial.id) { mutableStateOf(false) }
    var leaving by rememberSaveable(initial.id) { mutableStateOf(false) }
    var skipSuggested by rememberSaveable(initial.id) { mutableStateOf(false) }
    val lang = if (language.startsWith("en", true)) "en" else "ru"
    val name = draft.names.firstOrNull { it.languageTag.equals(lang, true) }?.name.orEmpty()
    val pools = remember(snapshot, draft.materials, assignments) { draft.materials.map { snapshot.materialWords(it, assignments) } }
    val words = remember(snapshot, draft.materials, assignments) { snapshot.recipeWords(draft.materials, assignments) }
    val typeLabels = mapOf(
        MaterialType.Book to stringResource(R.string.material_book_type),
        MaterialType.Dictionary to stringResource(R.string.material_dictionary_type),
        MaterialType.Kanji to stringResource(R.string.material_kanji_type),
        MaterialType.Words to stringResource(R.string.material_words_type),
    )
    val chaptersWord = stringResource(R.string.material_chapters)
    val chaptersMore = stringResource(R.string.material_chapters_and_more)
    val morePattern = stringResource(R.string.material_more)
    val kanjiPattern = stringResource(R.string.library_kanji)
    val andMoreMaterials = stringResource(R.string.recipe_name_and_more)
    val suggested = remember(draft.materials, snapshot, tags, language, typeLabels) {
        suggestedRecipeName(
            draft.materials, snapshot, tags, language, typeLabels,
            RecipeNameCopy(
                chaptersWord = chaptersWord,
                andMoreChapters = { chaptersMore.format(it) },
                moreTags = { morePattern.format(it) },
                kanjiLabel = { kanjiPattern.format(it) },
                andMoreMaterials = { andMoreMaterials.format(it) },
            ),
        )
    }
    fun setName(value: String) {
        val names = if (draft.names.any { it.languageTag.equals(lang, true) })
            draft.names.map { if (it.languageTag.equals(lang, true)) it.copy(name = value) else it }
        else draft.names + RecipeName(lang, value)
        draft = draft.copy(names = names)
    }
    LaunchedEffect(suggested, skipSuggested) {
        if (!skipSuggested && name.isBlank() && suggested.isNotBlank()) setName(suggested)
    }
    val effectiveName = name.trim().ifEmpty { if (skipSuggested) "" else suggested.trim() }
    val nameBad = effectiveName.length !in 1..120
    val poolBad = words.isEmpty() || pools.any { it.isEmpty() }
    val countBad = countMode && (count.toIntOrNull()?.let { it > 0 } != true)
    val timerBad = timerMode && (seconds.toIntOrNull()?.let { it > 0 } != true)
    val dirty = draft != initial || countMode != (initial.taskCount != null) || timerMode != (initial.secondsPerSymbol != null) ||
        (countMode && count != initial.taskCount?.toString()) || (timerMode && seconds != initial.secondsPerSymbol?.toString())
    val up = { if (!saving) { if (dirty) leaving = true else onBack() } }
    val list = rememberLazyListState()
    material?.let { bundle ->
        val current = bundle.toMaterial()
        MaterialEditorScreen(current, snapshot, tags, assignments, language, { updated ->
            val index = draft.materials.indexOfFirst { it.id == updated.id }
            draft = draft.copy(materials = if (index < 0) draft.materials + updated else draft.materials.toMutableList().apply { set(index, updated) })
            material = null
        }, { material = null }, onWord, onManageTags, modifier, editing = draft.materials.any { it.id == current.id })
        return
    }
    if (preview) { MaterialWordsPreview(words, language, onWord, { preview = false }, modifier); return }
    BackHandler(onBack = up)
    val scope = rememberCoroutineScope()
    val nameFocus = remember { FocusRequester() }
    val poolFocus = remember { FocusRequester() }
    val countFocus = remember { FocusRequester() }
    val timerFocus = remember { FocusRequester() }
    fun withResolvedName(recipe: Recipe): Recipe {
        if (name.isNotBlank()) return recipe
        if (skipSuggested || suggested.isBlank()) return recipe
        val names = if (recipe.names.any { it.languageTag.equals(lang, true) })
            recipe.names.map { if (it.languageTag.equals(lang, true)) it.copy(name = suggested) else it }
        else recipe.names + RecipeName(lang, suggested)
        return recipe.copy(names = names)
    }
    val showNameClear = name.isNotBlank() || (!skipSuggested && suggested.isNotBlank())
    val clearNameLabel = stringResource(R.string.recipe_name_clear)
    val save = {
        if (!saving) {
            attempted = true
            if (nameBad || poolBad || countBad || timerBad) {
                leaving = false
                scope.launch {
                    list.animateScrollToItem(if (nameBad) 0 else if (poolBad) 1 else 2)
                    when { nameBad -> nameFocus; poolBad -> poolFocus; countBad -> countFocus; else -> timerFocus }.requestFocus()
                }
            } else onSave(withResolvedName(draft.copy(taskCount = if (countMode) count.toInt() else null,
                secondsPerSymbol = if (timerMode) seconds.toInt() else null)).validated())
        }
    }
    val poolError = stringResource(R.string.recipe_pool_error)
    Scaffold(modifier.imePadding(), floatingActionButton = {
        if (!saving) ExtendedFloatingActionButton(save, Modifier.testTag("recipe-save"), shape = RoundedCornerShape(16.dp)) {
            Icon(painterResource(R.drawable.recipe_save), null, Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.recipe_save))
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MaterialHeader(stringResource(if (creating) R.string.recipe_new_title else R.string.recipe_edit_title), up)
            LazyColumn(Modifier.weight(1f).clipToBounds().testTag("recipe-list"), state = list,
                contentPadding = MaterialFormContentPadding, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecipeSectionTitle(stringResource(R.string.recipe_name))
                        OutlinedTextField(
                            name,
                            { value ->
                                skipSuggested = true
                                setName(value)
                            },
                            Modifier.fillMaxWidth().focusRequester(nameFocus).testTag("recipe-name"),
                            enabled = !saving,
                            shape = MaterialTheme.inputShape,
                            isError = attempted && nameBad,
                            singleLine = true,
                            placeholder = {
                                Text(
                                    if (!skipSuggested && suggested.isNotBlank()) suggested
                                    else stringResource(R.string.recipe_name_hint),
                                )
                            },
                            trailingIcon = {
                                if (showNameClear && !saving) {
                                    IconButton(
                                        {
                                            skipSuggested = true
                                            setName("")
                                        },
                                        Modifier.testTag("recipe-name-clear").semantics {
                                            contentDescription = clearNameLabel
                                        },
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.ic_recipe_clear),
                                            contentDescription = null,
                                            Modifier.size(20.dp),
                                        )
                                    }
                                }
                            },
                            supportingText = {
                                if (attempted && nameBad) MaterialFieldError(stringResource(R.string.recipe_name_error))
                            },
                            colors = kanjiOutlinedTextFieldColors(),
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        RecipeSectionTitle(
                            stringResource(R.string.recipe_materials),
                            Modifier.focusRequester(poolFocus).focusable()
                                .semantics { if (attempted && poolBad) error(poolError) },
                        )
                        draft.materials.take(if (expanded) Int.MAX_VALUE else 3).forEach { entry ->
                            RecipeMaterialRow(
                                entry, snapshot, tags, assignments, language,
                                onOpen = { if (!saving) material = entry.toBundle() },
                                onRemove = {
                                    if (!saving) {
                                        undo = entry.toBundle().apply { putInt("index", draft.materials.indexOf(entry)) }
                                        draft = draft.copy(materials = draft.materials.filterNot { it.id == entry.id })
                                    }
                                },
                            )
                        }
                        if (draft.materials.count() > 3) TextButton({ expanded = !expanded }, Modifier.testTag("recipe-more")) {
                            Text(if (expanded) stringResource(R.string.recipe_less) else stringResource(R.string.material_more, draft.materials.count() - 3))
                        }
                        undo?.let { removed -> TextButton({
                            val entry = removed.toMaterial()
                            if (draft.materials.none { it.id == entry.id }) draft = draft.copy(materials = draft.materials.toMutableList().apply {
                                add(removed.getInt("index").coerceAtMost(size), entry) })
                            undo = null
                        }, Modifier.testTag("recipe-undo"), enabled = !saving) { Text(stringResource(R.string.recipe_undo)) } }
                        FilledTonalButton({ material = RecipeMaterial(UUID.randomUUID().toString(), MaterialType.Book).toBundle() },
                            Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("recipe-add"), enabled = !saving) {
                            Icon(painterResource(R.drawable.library_add), null, Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.material_add_title))
                        }
                        if (attempted && poolBad) MaterialFieldError(poolError, Modifier.testTag("recipe-pool-error"))
                        if (words.isNotEmpty()) {
                            MaterialWordsPreviewLink(
                                words.count(),
                                { preview = true },
                                Modifier.padding(top = 4.dp).testTag("recipe-preview"),
                            )
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        RecipeSectionTitle(stringResource(R.string.recipe_training_settings))
                        RecipeLimit(stringResource(R.string.recipe_tasks), stringResource(R.string.recipe_count_input), countMode, {
                            countMode = it
                            if (it && count.isBlank()) count = DefaultTaskCount.toString()
                        }, count, { count = it },
                            stringResource(R.string.recipe_word_count), stringResource(R.string.recipe_custom_count), attempted && countBad, !saving,
                                "recipe-count", countFocus,
                            hint = if (!countMode && words.isNotEmpty())
                                pluralStringResource(R.plurals.recipe_tasks_from_pool, words.count(), words.count()) else null)
                        RecipeLimit(stringResource(R.string.recipe_seconds), stringResource(R.string.recipe_seconds_input), timerMode, {
                            timerMode = it
                            if (it && seconds.isBlank()) seconds = DefaultSecondsPerSymbol.toString()
                        }, seconds, { seconds = it },
                            stringResource(R.string.recipe_no_timer), stringResource(R.string.recipe_timer), attempted && timerBad, !saving, "recipe-timer",
                                timerFocus,
                            hint = if (timerMode) stringResource(R.string.recipe_timer_hint) else null)
                        if (saveError != null) Text(saveError, Modifier.testTag("recipe-save-error"), color = MaterialTheme.colorScheme.error)
                        if (saving) CircularProgressIndicator(Modifier.testTag("recipe-saving"))
                    }
                }
            }
        }
    }
    if (leaving) {
        AlertDialog(
            onDismissRequest = { if (!saving) leaving = false },
            title = { Text(stringResource(R.string.recipe_leave)) },
            text = { Text(stringResource(R.string.recipe_unsaved_message)) },
            confirmButton = {
                TextButton(onBack, Modifier.testTag("recipe-discard"), enabled = !saving) {
                    Text(stringResource(R.string.recipe_discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton({ leaving = false }, Modifier.testTag("recipe-stay"), enabled = !saving) {
                    Text(stringResource(R.string.recipe_stay), color = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }
}

@Composable
private fun RecipeLimit(title: String, inputLabel: String, active: Boolean, onMode: (Boolean) -> Unit, value: String, onValue: (String) -> Unit,
    defaultLabel: String, customLabel: String, invalid: Boolean, enabled: Boolean, tag: String, focus: FocusRequester,
    hint: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RecipeParamTitle(title)
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(4.dp), space = 0.dp) {
                listOf(false, true).forEach { choice ->
                    SegmentedButton(selected = active == choice, onClick = { onMode(choice) }, enabled = enabled,
                        shape = RoundedCornerShape(22.dp), modifier = Modifier.testTag("$tag-$choice"), icon = {},
                        border = BorderStroke(0.dp, Color.Transparent),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        Text(
                            if (choice) customLabel else defaultLabel,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
        if (hint != null && !active) {
            MaterialSupportingText(hint, Modifier.testTag("$tag-hint"))
        }
        if (active) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                inputLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedTextField(value, onValue, Modifier.fillMaxWidth().focusRequester(focus).testTag(tag), enabled = enabled,
                shape = MaterialTheme.inputShape, isError = invalid, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = {
                    when {
                        invalid -> MaterialFieldError(stringResource(R.string.recipe_number_error))
                        hint != null -> MaterialSupportingText(hint)
                    }
                },
                colors = kanjiOutlinedTextFieldColors())
        }
    }
}
