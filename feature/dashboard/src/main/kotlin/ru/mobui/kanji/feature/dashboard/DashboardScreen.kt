package ru.mobui.kanji.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.feature.dashboard.widgets.ActivityDetailsDialog
import ru.mobui.kanji.feature.dashboard.widgets.ActivityWidget
import ru.mobui.kanji.feature.dashboard.widgets.CurrentRecipeWidget
import ru.mobui.kanji.feature.dashboard.widgets.DashboardIcon
import ru.mobui.kanji.feature.dashboard.widgets.IllustratedIcon
import ru.mobui.kanji.feature.dashboard.widgets.VocabularyWidget
import ru.mobui.kanji.feature.dashboard.widgets.WordStatisticsDialog
import ru.mobui.kanji.feature.dashboard.widgets.WordStatisticsWidget

@Composable
fun DashboardScreen(
    state: DashboardState,
    onStartPractice: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    recipes: List<RecipeOverview>,
    onOpenCatalog: () -> Unit = {},
    onOpenDictionary: () -> Unit = {},
    selectedRecipeId: String = "default",
    onSelectRecipe: (String) -> Unit = {},
    onSaveRecipe: (String?, String, Int, Int) -> Unit = { _, _, _, _ -> },
    onOpenRecipes: (() -> Unit)? = null,
    onOpenReferences: () -> Unit = {},
) {
    val recipe =
        recipes.firstOrNull { it.id == selectedRecipeId }
            ?: recipes.firstOrNull()
            ?: RecipeOverview(selectedRecipeId, stringResource(R.string.dashboard_mix))
    var recipeDialog by rememberSaveable { mutableStateOf(RecipeDialogType.None) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    when (recipeDialog) {
        RecipeDialogType.None -> Unit
        RecipeDialogType.Choose ->
            RecipePicker(
                recipes,
                selectedRecipeId,
                RecipePickerActions(
                    onSelect = {
                        onSelectRecipe(it)
                        recipeDialog = RecipeDialogType.None
                    },
                    onEdit = {
                        editingId = it
                        recipeDialog = RecipeDialogType.Edit
                    },
                    onCreate = { recipeDialog = RecipeDialogType.Create },
                    onDismiss = { recipeDialog = RecipeDialogType.None },
                ),
            )
        RecipeDialogType.Create,
        RecipeDialogType.Edit ->
            LegacyRecipeEditor(
                if (recipeDialog == RecipeDialogType.Edit) {
                    recipes.firstOrNull { it.id == editingId }
                } else null,
                onSubmit = { name, words, seconds ->
                    onSaveRecipe(
                        if (recipeDialog == RecipeDialogType.Edit) editingId else null,
                        name,
                        words,
                        seconds,
                    )
                    recipeDialog =
                        if (recipeDialog == RecipeDialogType.Edit) RecipeDialogType.Choose
                        else RecipeDialogType.None
                },
                onDismiss = {
                    recipeDialog =
                        if (recipeDialog == RecipeDialogType.Edit) RecipeDialogType.Choose
                        else RecipeDialogType.None
                },
            )
        RecipeDialogType.History ->
            TrainingHistory(recipe, onDismiss = { recipeDialog = RecipeDialogType.None })
    }
    var activityOpen by rememberSaveable { mutableStateOf(false) }
    if (activityOpen) ActivityDetailsDialog(state.activity, onDismiss = { activityOpen = false })
    var statisticsOpen by rememberSaveable { mutableStateOf(false) }
    if (statisticsOpen)
        WordStatisticsDialog(state.statistics, onDismiss = { statisticsOpen = false })
    Scaffold(modifier.testTag("dashboard-screen")) { insets ->
        Column(
            Modifier.fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.dashboard_home),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onOpenSettings, Modifier.testTag("open-app-settings")) {
                    IllustratedIcon(
                        DashboardIcon.Settings,
                        Modifier.size(30.dp),
                        stringResource(R.string.dashboard_settings),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WordStatisticsWidget(
                    state.statistics,
                    { statisticsOpen = true },
                    Modifier.weight(1f),
                )
                ActivityWidget(state.activity, { activityOpen = true }, Modifier.weight(1f))
            }
            CurrentRecipeWidget(
                recipe,
                onSelect = { if (onOpenRecipes != null) onOpenRecipes() else recipeDialog = RecipeDialogType.Choose },
                onStart = onStartPractice,
                onHistory = { recipeDialog = RecipeDialogType.History },
                modifier = Modifier.fillMaxWidth(),
            )
            VocabularyWidget(
                state.vocabulary,
                Modifier.fillMaxWidth(),
                onOpenCatalog,
                onOpenDictionary,
                onOpenReferences,
            )
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
private fun DashboardLightPreview() {
    KanjiTheme(false) {
        DashboardScreen(
            DashboardDemo.state,
            {},
            {},
            recipes = listOf(RecipeOverview("default", stringResource(R.string.dashboard_mix))),
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
private fun DashboardDarkPreview() {
    KanjiTheme(true) {
        DashboardScreen(
            DashboardDemo.state,
            {},
            {},
            recipes = listOf(RecipeOverview("default", stringResource(R.string.dashboard_mix))),
        )
    }
}

private enum class RecipeDialogType {
    None,
    Choose,
    Create,
    Edit,
    History,
}
