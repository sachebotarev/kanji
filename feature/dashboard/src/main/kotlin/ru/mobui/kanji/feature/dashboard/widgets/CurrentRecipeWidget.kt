package ru.mobui.kanji.feature.dashboard.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.feature.dashboard.R
import ru.mobui.kanji.feature.dashboard.RecipeOverview

/** Selection, configuration and launch refer to the same recipe. */
@Composable
fun CurrentRecipeWidget(
    recipe: RecipeOverview,
    onSelect: () -> Unit,
    onStart: () -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.testTag("selected-recipe")) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.recipe_current),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onSelect,
                Modifier.testTag("select-training"),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Text(
                    stringResource(R.string.recipe_choose_action),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Box {
                Column(Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                    Column(Modifier.fillMaxWidth().padding(end = 112.dp).heightIn(min = 80.dp)) {
                        Text(
                            recipe.name,
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("current-recipe-name"),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            ru.mobui.kanji.feature.dashboard.recipeTrainingSummaryLine(
                                recipe.taskCount,
                                recipe.poolWordCount,
                                recipe.secondsPerSymbol,
                            ),
                            Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (recipe.completedTrainingCount == 0) {
                            Text(
                                stringResource(R.string.training_not_practiced),
                                Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                                    .testTag("training-status"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            TextButton(
                                onHistory,
                                Modifier.padding(start = 8.dp)
                                    .heightIn(min = 48.dp)
                                    .testTag("training-history"),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) {
                                Text(
                                    pluralStringResource(
                                        R.plurals.trainings_action,
                                        recipe.completedTrainingCount,
                                        recipe.completedTrainingCount,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    StartPracticeWidget(
                        onStart,
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        subtitle = stringResource(R.string.training_new),
                    )
                }
                MascotArtwork(
                    Modifier.align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 52.dp)
                        .size(112.dp),
                )
            }
        }
    }
}
