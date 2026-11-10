package ru.mobui.kanji.feature.dashboard.builder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.recipeWords
import ru.mobui.kanji.feature.dashboard.R
import ru.mobui.kanji.feature.dashboard.recipeTaskMetricLabel

internal data class RecipeDetailActions(
    val back: () -> Unit,
    val edit: () -> Unit,
    val delete: () -> Unit,
    val onWord: (Int) -> Unit,
)

@Composable
internal fun RecipeDetailScreen(
    recipe: Recipe?,
    state: RecipeLibraryState,
    language: String,
    actions: RecipeDetailActions,
    modifier: Modifier = Modifier,
) {
    var menu by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var preview by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val snapshot = state.snapshot
    BackHandler(onBack = actions.back)
    if (preview && recipe != null && snapshot != null) {
        val words = remember(snapshot, recipe.materials, state.assignments) {
            snapshot.recipeWords(recipe.materials, state.assignments)
        }
        MaterialWordsPreview(words, language, actions.onWord, { preview = false }, modifier)
        return
    }
    Scaffold(
        modifier = modifier.fillMaxSize().testTag("recipe-detail"),
        floatingActionButton = {
            if (recipe != null && !state.busy) {
                ExtendedFloatingActionButton(
                    onClick = actions.edit,
                    Modifier.testTag("recipe-detail-edit"),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_recipe_edit), null, Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.library_edit), style = MaterialTheme.typography.labelLarge)
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(actions.back, Modifier.size(48.dp).testTag("recipe-detail-back")) {
                    Icon(
                        painterResource(ru.mobui.kanji.core.designsystem.R.drawable.ic_arrow_back),
                        stringResource(R.string.material_back),
                        Modifier.size(24.dp),
                    )
                }
                Text(
                    stringResource(R.string.recipe_title),
                    Modifier.weight(1f).padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (recipe != null) {
                    Box {
                        val menuLabel = stringResource(R.string.library_menu, recipeName(recipe, language))
                        IconButton(
                            { menu = true },
                            enabled = !state.busy,
                            modifier = Modifier.size(48.dp).testTag("recipe-detail-menu").semantics {
                                contentDescription = menuLabel
                            },
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_recipe_more),
                                contentDescription = null,
                                Modifier.size(24.dp),
                            )
                        }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(
                                { Text(stringResource(R.string.dictionary_remove_recipe)) },
                                {
                                    menu = false
                                    confirmDelete = true
                                },
                                Modifier.testTag("recipe-detail-delete"),
                            )
                        }
                    }
                }
            }
            when {
                recipe == null -> Text(stringResource(R.string.recipe_missing), Modifier.padding(20.dp))
                snapshot == null -> Text(stringResource(R.string.recipe_load_failed), Modifier.padding(20.dp))
                else -> RecipeDetailBody(
                    recipe, snapshot, state, language, expanded,
                    { expanded = it },
                    { preview = true },
                    Modifier.weight(1f),
                )
            }
        }
    }
    if (confirmDelete && recipe != null) {
        ru.mobui.kanji.core.designsystem.KanjiDeleteConfirmDialog(
            title = stringResource(R.string.library_delete_title),
            message = "${recipeName(recipe, language)}\n${stringResource(R.string.library_delete_message)}",
            onConfirm = {
                confirmDelete = false
                actions.delete()
            },
            onDismiss = { confirmDelete = false },
            confirmText = stringResource(R.string.dictionary_remove_recipe),
            dismissText = stringResource(R.string.library_cancel),
        )
    }
}

@Composable
private fun RecipeDetailBody(
    recipe: Recipe,
    snapshot: CatalogSnapshot,
    state: RecipeLibraryState,
    language: String,
    expanded: Boolean,
    onExpanded: (Boolean) -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = recipeName(recipe, language)
    val words = remember(snapshot, recipe.materials, state.assignments) {
        snapshot.recipeWords(recipe.materials, state.assignments)
    }
    LazyColumn(
        modifier.clipToBounds().testTag("recipe-detail-list"),
        contentPadding = MaterialFormContentPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecipeSectionTitle(stringResource(R.string.recipe_name))
                Text(
                    title,
                    Modifier.testTag("recipe-detail-name"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                RecipeSectionTitle(stringResource(R.string.recipe_materials))
                // Quiet panel (surfaceContainerLow): groups materials without looking like tappable cards.
                val visible = recipe.materials.take(if (expanded) Int.MAX_VALUE else 3)
                Surface(
                    Modifier.fillMaxWidth().testTag("recipe-detail-materials"),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Column {
                        visible.forEachIndexed { index, entry ->
                            if (index > 0) {
                                HorizontalDivider(
                                    Modifier.padding(horizontal = 14.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                            RecipeMaterialRow(
                                entry, snapshot, state.tags, state.assignments, language,
                            )
                        }
                    }
                }
                if (recipe.materials.count() > 3) {
                    TextButton({ onExpanded(!expanded) }, Modifier.testTag("recipe-detail-more")) {
                        Text(
                            if (expanded) stringResource(R.string.recipe_less)
                            else stringResource(R.string.material_more, recipe.materials.count() - 3),
                        )
                    }
                }
                if (words.isNotEmpty()) {
                    MaterialWordsPreviewLink(
                        words.count(),
                        onPreview,
                        Modifier.padding(top = 4.dp).testTag("recipe-detail-preview"),
                    )
                }
            }
        }
        item {
            val tasks =
                if (recipe.taskCount == null && words.isEmpty()) {
                    stringResource(R.string.recipe_word_count)
                } else {
                    recipeTaskMetricLabel(recipe.taskCount, words.count())
                }
            val time = recipe.secondsPerSymbol?.let { stringResource(R.string.library_seconds, it) }
                ?: stringResource(R.string.library_no_timer)
            val timeHint = if (recipe.secondsPerSymbol != null) stringResource(R.string.recipe_timer_hint) else null
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("recipe-detail-limits")) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                RecipeSectionTitle(stringResource(R.string.recipe_training_settings))
                RecipeLimitSummary(stringResource(R.string.recipe_tasks), tasks, "recipe-detail-count")
                RecipeLimitSummary(stringResource(R.string.recipe_seconds), time, "recipe-detail-timer", timeHint)
            }
        }
    }
}

/** Section heading shared with RECIPE.01.02 — titleMedium / SemiBold / onSurface. */
@Composable
internal fun RecipeSectionTitle(text: String, modifier: Modifier = Modifier) {
    MaterialSectionTitle(text, modifier.semantics { heading() })
}

@Composable
internal fun RecipeParamTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RecipeLimitSummary(title: String, value: String, tag: String, hint: String? = null) {
    var helpOpen by rememberSaveable(tag) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecipeParamTitle(title, Modifier.weight(1f))
            if (hint != null) {
                IconButton(
                    onClick = { helpOpen = !helpOpen },
                    modifier = Modifier.size(48.dp).testTag("$tag-hint-toggle"),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_recipe_info),
                        contentDescription =
                            stringResource(
                                if (helpOpen) R.string.recipe_param_hide_help
                                else R.string.recipe_param_show_help,
                                title,
                            ),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Text(
            value,
            Modifier.testTag(tag),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (hint != null && helpOpen) {
            Surface(
                modifier =
                    Modifier.fillMaxWidth().padding(top = 4.dp).testTag("$tag-hint"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Text(
                    hint,
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
