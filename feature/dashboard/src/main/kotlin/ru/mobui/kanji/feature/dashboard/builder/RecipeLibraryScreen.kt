package ru.mobui.kanji.feature.dashboard.builder

import ru.mobui.kanji.core.designsystem.inputShape
import ru.mobui.kanji.core.designsystem.kanjiOutlinedTextFieldColors

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import java.text.Normalizer
import java.util.Locale
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.name
import ru.mobui.kanji.feature.dashboard.R
import ru.mobui.kanji.feature.dashboard.recipeTaskMetricLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.DictionaryTag

/** UI alias for [Recipe.name]; keep call sites readable in Compose screens. */
internal fun recipeName(recipe: Recipe, language: String): String = recipe.name(language)

private fun searchKey(text: String) = Normalizer.normalize(text, Normalizer.Form.NFKC)
    .lowercase(Locale.ROOT).replace('ё', 'е').trim().split(Regex("\\s+")).joinToString(" ")

private fun recipeSearchText(
    recipe: Recipe,
    snapshot: CatalogSnapshot?,
    tagsById: Map<Long, DictionaryTag>,
    typeLabels: Map<MaterialType, String>,
): String {
    val materials = recipe.materials.joinToString { material ->
        buildString {
            append(typeLabels[material.type].orEmpty())
            append(' ')
            material.sourceId?.let { id ->
                snapshot?.sourceById?.get(id)?.let { source ->
                    append(source.originalTitle)
                    append(' ')
                    append(source.titles.values.joinToString())
                }
            }
            append(' ')
            append(material.chapters.joinToString())
            append(' ')
            append(material.kanjiIds.joinToString { id -> snapshot?.kanjiById?.get(id)?.symbol.orEmpty() })
            append(' ')
            append(material.wordIds.joinToString { snapshot?.wordById?.get(it)?.written.orEmpty() })
            append(' ')
            append(
                material.tagIds.mapNotNull(tagsById::get).joinToString { tag ->
                    tag.translations.joinToString { translation -> translation.name }
                },
            )
        }
    }
    return searchKey(recipe.names.joinToString { it.name } + " " + materials)
}

internal data class RecipeLibraryActions(
    val back: () -> Unit, val create: () -> Unit, val open: (String) -> Unit,
    val reorder: (List<String>) -> Unit,
    val delete: (String) -> Unit,
    val query: (String) -> Unit, val retry: () -> Unit,
)

@Composable
internal fun RecipeLibraryScreen(state: RecipeLibraryState, query: String, language: String,
    actions: RecipeLibraryActions, modifier: Modifier = Modifier) {
    var searching by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val scroll = rememberLazyListState()
    val snapshot = state.snapshot
    val canDrag = query.isBlank() && !state.busy && !state.loading
    val reorder = rememberRecipeReorderState(scroll, state.recipes.map { it.id }, canDrag, actions.reorder)
    val dragId = reorder.draggedId
    val platformView = LocalView.current
    val back = { if (dragId != null) platformView.cancelDragAndDrop() else if (!state.busy) actions.back() }
    BackHandler { back() }
    val typeLabels = mapOf(MaterialType.Book to stringResource(R.string.material_book_type),
        MaterialType.Dictionary to stringResource(R.string.material_dictionary_type),
        MaterialType.Kanji to stringResource(R.string.material_kanji_type), MaterialType.Words to stringResource(R.string.material_words_type))
    val visibleIds = reorder.visibleIds()
    val ordered = remember(visibleIds, state.recipes) {
        val byId = state.recipes.associateBy { it.id }
        visibleIds.mapNotNull(byId::get)
    }
    val rows = remember(query, ordered, snapshot, state.tags, typeLabels) {
        if (query.isBlank()) ordered
        else {
            val typed = searchKey(query).split(' ').filter { it.isNotBlank() }
            val tagsById = state.tags.associateBy { it.id }
            ordered.filter { recipe ->
                val text = recipeSearchText(recipe, snapshot, tagsById, typeLabels)
                typed.all { it in text }
            }
        }
    }
    val currentReorder by rememberUpdatedState(actions.reorder)
    fun move(id: String, index: Int) {
        val ids = state.recipes.map { it.id }.toMutableList()
        if (!canDrag || !ids.remove(id)) return
        ids.add(index.coerceIn(0, ids.count()), id)
        currentReorder(ids)
    }
    Scaffold(modifier, floatingActionButton = {
        if (!state.busy) ExtendedFloatingActionButton(actions.create, Modifier.testTag("library-create"),
            containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(16.dp)) {
            Icon(painterResource(R.drawable.library_add), null, Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.library_create), style = MaterialTheme.typography.labelLarge)
        }
    }) { padding ->
        Column(Modifier.padding(padding)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(back, Modifier.size(48.dp)) {
                    Icon(painterResource(R.drawable.ic_recipe_back), stringResource(R.string.material_back))
                }
                Text(
                    stringResource(R.string.library_title),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val searchLabel = stringResource(R.string.library_search)
                IconButton({ searching = !searching; if (!searching) actions.query("") },
                    Modifier.testTag("library-search-toggle").semantics { contentDescription = searchLabel }) {
                    Icon(androidx.compose.ui.res.painterResource(R.drawable.library_search), null)
                }
            }
            if (searching) OutlinedTextField(query, actions.query, Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("library-search"),
                label = { Text(stringResource(R.string.library_search)) }, shape = MaterialTheme.inputShape, singleLine = true,
                trailingIcon = { TextButton({ actions.query("") }) { Text(stringResource(R.string.library_clear)) } },
                colors = kanjiOutlinedTextFieldColors())
            if (state.error) Row {
                Text(stringResource(R.string.recipe_load_failed), Modifier.weight(1f).padding(16.dp))
                TextButton(actions.retry) { Text(stringResource(R.string.recipe_retry)) }
            }
            if (state.loading) CircularProgressIndicator(Modifier.padding(24.dp))
            LazyColumn(state = scroll, contentPadding = MaterialFormContentPadding,
                verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f).testTag("recipe-library")
                    .onGloballyPositioned { reorder.bounds = it.boundsInRoot() }
                    .dragAndDropTarget(reorder::accepts, reorder.target)) {
                if (!state.loading && state.recipes.isNotEmpty()) {
                    item(key = "intro") {
                        Row(Modifier.padding(bottom = 8.dp).heightIn(min = 112.dp).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                MaterialSectionTitle(stringResource(R.string.library_intro))
                                MaterialSupportingText(
                                    if (query.isBlank()) pluralStringResource(R.plurals.library_total, rows.count(), rows.count())
                                    else stringResource(R.string.library_count, rows.count(), state.recipes.count()),
                                )
                            }
                            androidx.compose.foundation.Image(
                                androidx.compose.ui.res.painterResource(
                                    ru.mobui.kanji.core.designsystem.R.drawable.mascot_recipe_builder,
                                ),
                                null,
                                Modifier.size(96.dp),
                            )
                        }
                    }
                }
                if (!state.loading && rows.isEmpty()) {
                    item(key = "empty") {
                        if (state.recipes.isEmpty()) {
                            LibraryEmptyState(Modifier.fillParentMaxHeight())
                        } else {
                            LibrarySearchEmpty(Modifier.fillParentMaxHeight(0.5f))
                        }
                    }
                }
                items(rows, key = { it.id }) { recipe ->
                    val title = recipeName(recipe, language)
                    val active = dragId == recipe.id
                    // Snap back on release — a spring/tween pop stacks with list settle and feels like a jerk.
                    val scale by animateFloatAsState(
                        if (active) 1.025f else 1f,
                        animationSpec = if (active) tween(120) else snap(),
                        label = "recipe-drag-scale",
                    )
                    val alpha by animateFloatAsState(
                        if (active) 0.35f else 1f,
                        animationSpec = if (active) tween(120) else snap(),
                        label = "recipe-drag-alpha",
                    )
                    val above = stringResource(R.string.library_above)
                    val below = stringResource(R.string.library_below)
                    val open = stringResource(R.string.library_open, title)
                    val remove = stringResource(R.string.dictionary_remove_recipe)
                    val index = ordered.indexOfFirst { it.id == recipe.id }
                    val dismissState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(pendingDeleteId, recipe.id) {
                        if (pendingDeleteId != recipe.id &&
                            dismissState.settledValue != SwipeToDismissBoxValue.Settled
                        ) {
                            dismissState.reset()
                        }
                    }
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    painterResource(R.drawable.recipe_remove),
                                    contentDescription = null,
                                    Modifier.padding(end = 24.dp).size(24.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        },
                        modifier = Modifier
                            .animateItem()
                            .zIndex(if (active) 1f else 0f)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            },
                        enableDismissFromStartToEnd = false,
                        gesturesEnabled = !state.busy && dragId == null,
                        onDismiss = {
                            if (it == SwipeToDismissBoxValue.EndToStart) pendingDeleteId = recipe.id
                        },
                    ) {
                        Surface(
                            Modifier
                                .fillMaxWidth()
                                .testTag("library-${recipe.id}")
                                .recipeLibraryBody(
                                    reorder, recipe.id, canDrag, state.busy, platformView,
                                ) { actions.open(recipe.id) }
                                .semantics(mergeDescendants = true) {
                                    contentDescription = open
                                    onClick(label = open) {
                                        if (!state.busy) actions.open(recipe.id)
                                        true
                                    }
                                    customActions = buildList {
                                        if (canDrag) {
                                            if (index > 0) {
                                                add(CustomAccessibilityAction(above) {
                                                    move(recipe.id, index - 1); true
                                                })
                                            }
                                            if (index < ordered.lastIndex) {
                                                add(CustomAccessibilityAction(below) {
                                                    move(recipe.id, index + 1); true
                                                })
                                            }
                                        }
                                        if (!state.busy) {
                                            add(CustomAccessibilityAction(remove) {
                                                pendingDeleteId = recipe.id
                                                true
                                            })
                                        }
                                    }
                                },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shadowElevation = if (active) 8.dp else 0.dp,
                        ) {
                            Box(
                                Modifier.padding(12.dp).fillMaxWidth(),
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(end = 26.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        title,
                                        Modifier.fillMaxWidth().testTag("library-title-${recipe.id}"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    val chips = recipeMaterialTags(
                                        recipe, snapshot, state.tags, language, typeLabels,
                                    )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    chips.take(1).forEach { (label, type) ->
                                        val colors = MaterialTheme.colorScheme
                                        val pair = when (type) {
                                            MaterialType.Book -> colors.secondaryContainer to colors.onSecondaryContainer
                                            MaterialType.Dictionary -> colors.tertiaryContainer to colors.onTertiaryContainer
                                            MaterialType.Kanji -> colors.primaryContainer to colors.onPrimaryContainer
                                            else -> colors.surfaceContainerHighest to colors.onSurface
                                        }
                                        Surface(color = pair.first, contentColor = pair.second, shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                label,
                                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                    }
                                    if (chips.count() > 1) {
                                        Text(
                                            stringResource(R.string.material_more, chips.count() - 1),
                                            Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                HorizontalDivider(Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                val tasks =
                                    recipeTaskMetricLabel(
                                        recipe.taskCount,
                                        state.counts[recipe.id] ?: 0,
                                    )
                                val time = recipe.secondsPerSymbol?.let { stringResource(R.string.library_seconds, it) }
                                    ?: stringResource(R.string.library_no_timer)
                                if (LocalDensity.current.fontScale >= 1.3f) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        LibraryMetric(tasks, R.drawable.library_words)
                                        LibraryMetric(time, if (recipe.secondsPerSymbol == null) R.drawable.library_no_timer else R.drawable.library_timer)
                                    }
                                } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    LibraryMetric(tasks, R.drawable.library_words, Modifier.weight(1f))
                                    LibraryMetric(time, if (recipe.secondsPerSymbol == null) R.drawable.library_no_timer else R.drawable.library_timer,
                                        Modifier.weight(1f))
                                }
                            }
                            Column(
                                modifier = Modifier.align(Alignment.TopEnd).width(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    painterResource(ru.mobui.kanji.core.wordui.R.drawable.selection_chevron_down),
                                    null,
                                    Modifier.size(18.dp).rotate(-90f),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (state.excellentRecipeIds.contains(recipe.id)) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_paw_stamp),
                                        contentDescription = stringResource(R.string.history_stamp_excellent),
                                        modifier =
                                            Modifier
                                                .size(22.dp)
                                                .rotate(-10f)
                                                .testTag("recipe-stamp-${recipe.id}"),
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
    val pending = pendingDeleteId?.let { id -> state.recipes.firstOrNull { it.id == id } }
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.library_delete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(recipeName(pending, language))
                    Text(stringResource(R.string.library_delete_message))
                }
            },
            confirmButton = {
                TextButton(
                    {
                        val id = pending.id
                        pendingDeleteId = null
                        actions.delete(id)
                    },
                    Modifier.testTag("library-delete-confirm"),
                ) { Text(stringResource(R.string.dictionary_remove_recipe)) }
            },
            dismissButton = {
                TextButton(
                    { pendingDeleteId = null },
                    Modifier.testTag("library-delete-cancel"),
                ) { Text(stringResource(R.string.library_cancel)) }
            },
        )
    }
}
