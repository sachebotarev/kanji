package ru.mobui.kanji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.mobui.kanji.data.kanjivg.BundledKanjiReferenceSource
import ru.mobui.kanji.domain.practice.KanjiId
import ru.mobui.kanji.domain.practice.PracticeTask
import ru.mobui.kanji.feature.dashboard.DashboardDemo
import ru.mobui.kanji.feature.dashboard.DashboardScreen
import ru.mobui.kanji.feature.practice.CheckerSettingsScreen
import ru.mobui.kanji.feature.practice.PracticeRoute
import ru.mobui.kanji.feature.practice.PracticeViewModel
import ru.mobui.kanji.feature.settings.SettingsScreen

internal data class AppModels(
    val settingsModel: RecognitionSettingsViewModel,
    val dictionarySummary: DictionarySummaryViewModel,
)

internal data class DemoPracticeGraph(
    val fallbackTask: PracticeTask,
    val source: BundledKanjiReferenceSource,
    val factory: (PracticeTask) -> ViewModelProvider.Factory,
)

/**
 * In-memory queue: one [PracticeTask] per recipe word, in catalogue order.
 * Length follows recipe `taskCount`, or the full convertible pool when count is unset.
 */
internal data class RecipePracticeSession(
    val tasks: List<PracticeTask>,
    val index: Int = 0,
) {
    init {
        require(tasks.isNotEmpty())
        require(index in tasks.indices)
    }

    val task: PracticeTask
        get() = tasks[index]

    val hasNext: Boolean
        get() = index < tasks.lastIndex

    val ordinal: Int
        get() = index + 1

    val total: Int
        get() = tasks.size

    /** Next word in order, or null after the last task. */
    fun advance(): RecipePracticeSession? =
        if (hasNext) copy(index = index + 1) else null
}

internal class AppUiContext(
    val dependencies: KanjiApplication,
    val models: AppModels,
    val graph: DemoPracticeGraph,
    val modelStatus: JapaneseModelStatus,
    val settings: ru.mobui.kanji.domain.practice.RecognitionSettings,
) {
    val settingsModel
        get() = models.settingsModel

    val dictionarySummary
        get() = models.dictionarySummary

    val source
        get() = graph.source

    val modelReady
        get() = modelStatus == JapaneseModelStatus.Ready
}

@Composable
internal fun MainContent(
    dependencies: KanjiApplication,
    models: AppModels,
    graph: DemoPracticeGraph,
) {
    val settingsModel = models.settingsModel
    val modelStatus by dependencies.japaneseModel.status.collectAsStateWithLifecycle()
    val modelError by dependencies.japaneseModel.error.collectAsStateWithLifecycle()
    if (modelError) {
        AlertDialog(
            onDismissRequest = dependencies.japaneseModel::dismissError,
            title = { Text(stringResource(R.string.japanese_model_title)) },
            text = { Text(stringResource(R.string.japanese_model_failed)) },
            confirmButton = {
                TextButton(onClick = dependencies.japaneseModel::dismissError) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
    val loaded by settingsModel.loaded.collectAsStateWithLifecycle()
    val storageError by settingsModel.storageError.collectAsStateWithLifecycle()
    if (!loaded || storageError) {
        SettingsStorageStatus(storageError, settingsModel::retryStorage)
        return
    }
    var destination by rememberSaveable { mutableStateOf(AppDestination.Dashboard) }
    var practiceSession by remember { mutableStateOf<RecipePracticeSession?>(null) }
    val savedSettings by settingsModel.settings.collectAsStateWithLifecycle()
    val modelReady = modelStatus == JapaneseModelStatus.Ready
    val settings =
        savedSettings.copy(
            readabilityEnabled = savedSettings.readabilityEnabled && modelReady,
        )
    LaunchedEffect(modelStatus, savedSettings) {
        if (modelStatus == JapaneseModelStatus.Missing && savedSettings.readabilityEnabled) {
            settingsModel.update(savedSettings.copy(readabilityEnabled = false))
        }
    }
    val context = AppUiContext(dependencies, models, graph, modelStatus, settings)
    AppDestinationContent(
        destination = destination,
        context = context,
        practiceSession = practiceSession,
        onPracticeSessionChange = { practiceSession = it },
        navigate = { destination = it },
    )
}

@Composable
private fun AppDestinationContent(
    destination: AppDestination,
    context: AppUiContext,
    practiceSession: RecipePracticeSession?,
    onPracticeSessionChange: (RecipePracticeSession?) -> Unit,
    navigate: (AppDestination) -> Unit,
) {
    when (destination) {
        AppDestination.RecipeBuilder -> RecipeBuilderDestination(context, navigate)
        AppDestination.Dictionary -> DictionaryDestination(context, navigate)
        AppDestination.Catalog -> CatalogDestination(context, navigate)
        AppDestination.References -> CatalogDestination(context, navigate, ru.mobui.kanji.feature.catalog.CatalogSection.References)
        AppDestination.Settings -> SettingsDestination(context, navigate)
        AppDestination.CheckerSettings -> CheckerSettingsDestination(context, navigate)
        AppDestination.Dashboard -> DashboardDestination(context, navigate, onPracticeSessionChange)
        AppDestination.Practice -> {
            val session = practiceSession
            if (session == null) {
                LaunchedEffect(Unit) { navigate(AppDestination.Dashboard) }
            } else {
                PracticeDestination(context, session, onPracticeSessionChange, navigate)
            }
        }
    }
}

@Composable
private fun DictionaryDestination(context: AppUiContext, navigate: (AppDestination) -> Unit) {
    with(context) {
        val revision by dependencies.catalogRevision.collectAsStateWithLifecycle()
        ru.mobui.kanji.feature.dictionary.DictionaryRoute(
            dependencies.dictionary,
            dependencies.catalog,
            dependencies.recipesRepository,
            onBack = { navigate(AppDestination.Dashboard) },
        ) { kanjiId, sourceId, back ->
            ru.mobui.kanji.feature.catalog.CatalogRoute(
                dependencies.catalog,
                back,
                kanjiId,
                sourceId,
                loadDrawing = { source.load(KanjiId("u${it.toString(APP_UNICODE_HEX_RADIX)}")) },
                revision = revision,
            )
        }
    }
}

@Composable
private fun CatalogDestination(
    context: AppUiContext,
    navigate: (AppDestination) -> Unit,
    section: ru.mobui.kanji.feature.catalog.CatalogSection = ru.mobui.kanji.feature.catalog.CatalogSection.Textbooks,
) {
    with(context) {
        val revision by dependencies.catalogRevision.collectAsStateWithLifecycle()
        ru.mobui.kanji.feature.catalog.CatalogRoute(
            dependencies.catalog,
            onBack = { navigate(AppDestination.Dashboard) },
            section = section,
            loadDrawing = { source.load(KanjiId("u${it.toString(APP_UNICODE_HEX_RADIX)}")) },
            revision = revision,
        )
    }
}

@Composable
private fun SettingsDestination(context: AppUiContext, navigate: (AppDestination) -> Unit) {
    with(context) {
        BackHandler { navigate(AppDestination.Dashboard) }
        SettingsScreen(
            onBack = { navigate(AppDestination.Dashboard) },
            onOpenChecker = { navigate(AppDestination.CheckerSettings) },
            modelLoaded = modelStatus == JapaneseModelStatus.Ready,
            modelLoading = modelStatus == JapaneseModelStatus.Loading,
            onDownloadModel = dependencies.japaneseModel::load,
        )
    }
}

@Composable
private fun CheckerSettingsDestination(context: AppUiContext, navigate: (AppDestination) -> Unit) {
    with(context) {
        BackHandler { navigate(AppDestination.Settings) }
        CheckerSettingsScreen(
            settings,
            settingsModel::update,
            onBack = { navigate(AppDestination.Settings) },
            onReset = settingsModel::reset,
            readabilityAvailable = modelReady,
        )
    }
}

@Composable
private fun DashboardDestination(
    context: AppUiContext,
    navigate: (AppDestination) -> Unit,
    onPracticeSessionChange: (RecipePracticeSession?) -> Unit,
) {
    with(context) {
        val personalCount by dictionarySummary.count.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        ru.mobui.kanji.feature.dashboard.builder.RecipeHomeSummary(dependencies.catalog, dependencies.dictionary, dependencies.recipesRepository) { recipe ->
        DashboardScreen(
            DashboardDemo.state.copy(
                vocabulary = DashboardDemo.state.vocabulary.copy(personalTotal = personalCount),
            ),
            onStartPractice = {
                scope.launch {
                    val tasks = loadPracticeTasks(dependencies) ?: listOf(graph.fallbackTask)
                    onPracticeSessionChange(RecipePracticeSession(tasks))
                    navigate(AppDestination.Practice)
                }
            },
            onOpenSettings = { navigate(AppDestination.Settings) },
            onOpenCatalog = { navigate(AppDestination.Catalog) },
            onOpenReferences = { navigate(AppDestination.References) },
            onOpenDictionary = { navigate(AppDestination.Dictionary) },
            recipes = listOf(recipe),
            selectedRecipeId = recipe.id,
            onOpenRecipes = { navigate(AppDestination.RecipeBuilder) },
        )
        }
    }
}

private suspend fun loadPracticeTasks(dependencies: KanjiApplication): List<PracticeTask>? {
    val recipes = dependencies.recipesRepository.observeRecipes().first()
    val recipe = recipes.firstOrNull { it.isActive } ?: return null
    val assignments = dependencies.recipesRepository.observeWordTags().first()
    val snapshot = dependencies.catalog.load()
    return recipePracticeTasks(snapshot, recipe, assignments).takeIf { it.isNotEmpty() }
}

@Composable
private fun RecipeBuilderDestination(context: AppUiContext, navigate: (AppDestination) -> Unit) {
    with(context) {
        val revision by dependencies.catalogRevision.collectAsStateWithLifecycle()
        ru.mobui.kanji.feature.dashboard.builder.RecipeBuilderRoute(dependencies.catalog, dependencies.dictionary, dependencies.recipesRepository,
            ru.mobui.kanji.feature.dashboard.builder.RecipeBuilderActions(
                onBack = { navigate(AppDestination.Dashboard) },
                wordContent = { id, back ->
                    ru.mobui.kanji.feature.catalog.CatalogRoute(dependencies.catalog, back, initialWordId = id,
                        loadDrawing = { source.load(KanjiId("u${it.toString(APP_UNICODE_HEX_RADIX)}")) }, revision = revision)
                },
                tagContent = { back -> ru.mobui.kanji.feature.dictionary.DictionaryTagManagementRoute(dependencies.recipesRepository, back) }))
    }
}

@Composable
private fun PracticeDestination(
    context: AppUiContext,
    session: RecipePracticeSession,
    onPracticeSessionChange: (RecipePracticeSession?) -> Unit,
    navigate: (AppDestination) -> Unit,
) {
    with(context) {
        val task = session.task
        val practice: PracticeViewModel =
            viewModel(key = "${task.taskId}-${session.index}", factory = graph.factory(task))
        val leave: () -> Unit = {
            practice.restart()
            onPracticeSessionChange(null)
            navigate(AppDestination.Dashboard)
        }
        BackHandler { leave() }
        LaunchedEffect(modelStatus, task.taskId, session.index) { practice.onCheckerSettingsChanged() }
        val state by practice.uiState.collectAsStateWithLifecycle()
        val result = state.practice.result
        var resultReceived by rememberSaveable(task.taskId, session.index) { mutableStateOf(false) }
        if (result == null || !resultReceived) {
            PracticeRoute(
                practice,
                onResult = {
                    val next = session.advance()
                    if (next != null) {
                        // Sequential pass: open the next recipe word without wrapping.
                        onPracticeSessionChange(next)
                    } else {
                        resultReceived = true
                    }
                },
                onBack = leave,
                settings = settings,
                wordLabel = task.russianWord,
                progressLabel =
                    stringResource(R.string.practice_word_progress, session.ordinal, session.total),
                readabilityAvailable = modelReady,
                onSettingsChanged = {
                    settingsModel.update(it)
                    practice.onCheckerSettingsChanged()
                },
                onResetSettings = {
                    settingsModel.reset()
                    practice.onCheckerSettingsChanged()
                },
            )
        } else {
            PracticeSummary(
                result,
                progressLabel =
                    stringResource(R.string.practice_word_progress, session.ordinal, session.total),
                onClose = leave,
            )
        }
    }
}

