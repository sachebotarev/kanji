package ru.mobui.kanji.feature.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.core.designsystem.inputShape
import ru.mobui.kanji.core.designsystem.kanjiOutlinedTextFieldColors
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.name

/** HOME.01 — choose a recipe to start a training. */
@Composable
fun TrainingPickScreen(
    recipes: List<Recipe>,
    loading: Boolean,
    query: String,
    language: String,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onPick: (String) -> Unit,
    onCreateFirstRecipe: () -> Unit,
    modifier: Modifier = Modifier,
    /** Unique words in each recipe pool; used when [Recipe.taskCount] is null. */
    poolCounts: Map<String, Int> = emptyMap(),
    /** Recipe IDs whose current revision has been completed with an excellent score. */
    excellentRecipeIds: Set<String> = emptySet(),
) {
    BackHandler { onBack() }
    var searching by rememberSaveable { mutableStateOf(false) }
    val empty = !loading && recipes.isEmpty()
    val rows =
        if (query.isBlank()) recipes
        else {
            val needle = query.trim().lowercase()
            recipes.filter { recipe ->
                recipe.names.any { it.name.lowercase().contains(needle) }
            }
        }
    Scaffold(modifier.testTag("training-pick")) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onBack, Modifier.size(48.dp).testTag("training-pick-back")) {
                    Icon(
                        painterResource(R.drawable.ic_recipe_back),
                        stringResource(R.string.recipe_back),
                    )
                }
                Text(
                    stringResource(R.string.training_pick_title),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (!empty) {
                    IconButton(
                        { searching = !searching; if (!searching) onQuery("") },
                        Modifier.testTag("training-pick-search-toggle"),
                    ) {
                        Icon(
                            painterResource(R.drawable.library_search),
                            stringResource(R.string.recipe_search),
                        )
                    }
                }
            }
            if (searching && !empty) {
                OutlinedTextField(
                    query,
                    onQuery,
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("training-pick-search"),
                    label = { Text(stringResource(R.string.recipe_search)) },
                    shape = MaterialTheme.inputShape,
                    singleLine = true,
                    trailingIcon = {
                        TextButton({ onQuery("") }) { Text(stringResource(R.string.recipe_cancel)) }
                    },
                    colors = kanjiOutlinedTextFieldColors(),
                )
            }
            when {
                loading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.testTag("training-pick-loading"))
                    }
                empty ->
                    TrainingPickEmptyState(
                        onCreateFirstRecipe = onCreateFirstRecipe,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f).testTag("training-pick-list"),
                    ) {
                        items(rows, key = { it.id }) { recipe ->
                            TrainingPickRow(
                                recipe = recipe,
                                language = language,
                                poolWordCount = poolCounts[recipe.id] ?: 0,
                                isMaster = excellentRecipeIds.contains(recipe.id),
                                onPick = onPick,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** HOME.01.W04 — centered empty data state with mascot and create-first CTA. */
@Composable
private fun TrainingPickEmptyState(
    onCreateFirstRecipe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .padding(horizontal = 32.dp)
            .padding(bottom = 48.dp)
            .testTag("training-pick-empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Image(
            painterResource(ru.mobui.kanji.core.designsystem.R.drawable.mascot_recipe_builder),
            contentDescription = null,
            Modifier.size(112.dp).testTag("training-pick-empty-mascot"),
        )
        Text(
            stringResource(R.string.training_pick_empty),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.training_pick_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(
            onCreateFirstRecipe,
            Modifier.padding(top = 4.dp).testTag("training-pick-create-first"),
        ) {
            Text(stringResource(R.string.training_pick_create_first))
        }
    }
}

@Composable
private fun TrainingPickRow(
    recipe: Recipe,
    language: String,
    poolWordCount: Int,
    isMaster: Boolean = false,
    onPick: (String) -> Unit,
) {
    val title = recipe.name(language)
    Surface(
        Modifier.fillMaxWidth().testTag("training-pick-${recipe.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier
                .clickable { onPick(recipe.id) }
                .padding(16.dp)
                .testTag("training-pick-open-${recipe.id}"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    recipeTrainingSummaryLine(
                        recipe.taskCount,
                        poolWordCount,
                        recipe.secondsPerSymbol,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isMaster) {
                Image(
                    painter = painterResource(R.drawable.ic_paw_stamp),
                    contentDescription = stringResource(R.string.history_stamp_excellent),
                    modifier =
                        Modifier
                            .padding(start = 12.dp)
                            .size(24.dp)
                            .rotate(-10f)
                            .testTag("training-pick-stamp-${recipe.id}"),
                )
            }
        }
    }
}
