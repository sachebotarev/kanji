package ru.mobui.kanji

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.util.Consumer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import ru.mobui.kanji.core.designsystem.preferredLanguageTag
import ru.mobui.kanji.data.kanjivg.BundledKanjiReferenceSource
import ru.mobui.kanji.domain.catalog.recipeWords
import ru.mobui.kanji.domain.practice.KanjiId
import ru.mobui.kanji.domain.practice.PracticeTask
import ru.mobui.kanji.domain.practice.TrainingStatus
import ru.mobui.kanji.domain.practice.summarize
import ru.mobui.kanji.feature.catalog.CatalogSection
import ru.mobui.kanji.feature.dashboard.DashboardDemo
import ru.mobui.kanji.feature.dashboard.DashboardScreen
import ru.mobui.kanji.feature.dashboard.history.findExcellentRecipeIds
import ru.mobui.kanji.feature.practice.CheckerSettingsScreen
import ru.mobui.kanji.feature.practice.PracticeRoute
import ru.mobui.kanji.feature.practice.PracticeViewModel
import ru.mobui.kanji.feature.practice.TrainingCompleteScreen
import ru.mobui.kanji.feature.settings.SettingsScreen

@Serializable private data object Dashboard

@Serializable private data object TrainingPick

@Serializable private data object Practice

@Serializable private data object Settings

@Serializable private data object CheckerSettings

@Serializable private data object Catalog

@Serializable private data object References

@Serializable private data object Dictionary

@Serializable
private data class RecipeBuilder(
    val openCreate: Boolean = false,
    /** After first save, start PRACTICE instead of returning to the recipe list. */
    val launchAfterCreate: Boolean = false,
)

@Serializable private data class TrainingHistory(val period: String = "all")

@Serializable private data object PracticeChecker

internal data class AppModels(
    val settingsModel: RecognitionSettingsViewModel,
    val dictionarySummary: DictionarySummaryViewModel,
)

internal data class DemoPracticeGraph(
    val source: BundledKanjiReferenceSource,
    val factory: (PracticeTask) -> ViewModelProvider.Factory,
)

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
        ru.mobui.kanji.core.designsystem.KanjiInfoDialog(
            title = stringResource(R.string.japanese_model_title),
            message = stringResource(R.string.japanese_model_failed),
            onDismiss = dependencies.japaneseModel::dismissError,
            buttonText = stringResource(R.string.close),
        )
    }
    val loaded by settingsModel.loaded.collectAsStateWithLifecycle()
    val storageError by settingsModel.storageError.collectAsStateWithLifecycle()
    if (!loaded || storageError) {
        SettingsStorageStatus(storageError, settingsModel::retryStorage)
        return
    }
    val nav = rememberNavController()
    var activeTraining by remember { mutableStateOf<ActiveTraining?>(null) }
    // Exit transition still composes Practice after pop; block resume of in_progress there.
    var suppressPracticeResume by remember { mutableStateOf(false) }
    var practicePreview by remember {
        mutableStateOf<ru.mobui.kanji.feature.practice.PracticeUiState?>(null)
    }
    val trainingPersistence =
        remember(dependencies) {
            TrainingPersistence(dependencies.trainingsRepository, dependencies.persistenceScope)
        }
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
    LaunchedEffect(activeTraining) {
        if (activeTraining == null) practicePreview = null
    }
    val context = AppUiContext(dependencies, models, graph, modelStatus, settings)
    HandleAppDeepLinks(nav)
    NavHost(nav, startDestination = Dashboard) {
        composable<Dashboard>(
            deepLinks = listOf(navDeepLink<Dashboard>(basePath = AppDeepLinks.HOME)),
        ) {
            DashboardDestination(
                context,
                nav,
                onActiveTrainingChange = {
                    suppressPracticeResume = false
                    activeTraining = it
                },
            )
        }
        composable<TrainingPick>(
            deepLinks = listOf(navDeepLink<TrainingPick>(basePath = AppDeepLinks.TRAINING_PICK)),
        ) {
            TrainingPickDestination(
                context,
                nav,
                onActiveTrainingChange = {
                    suppressPracticeResume = false
                    activeTraining = it
                },
                trainingPersistence = trainingPersistence,
            )
        }
        composable<Practice> {
            val training = activeTraining
            if (training == null) {
                // Route may survive recreate while in-memory ActiveTraining does not —
                // restore the sole in_progress run so practice survives rotation.
                LaunchedEffect(suppressPracticeResume) {
                    // Exit transition still composes this destination after leave();
                    // do not resume the in_progress row we intentionally kept for HOME.
                    if (suppressPracticeResume) return@LaunchedEffect
                    val resumed =
                        runCatching { context.dependencies.trainingsRepository.getInProgress() }
                            .getOrNull()
                            ?.let(::resumeActiveTraining)
                    if (resumed != null) activeTraining = resumed
                    else nav.popOrDashboard()
                }
            } else {
                PracticePadDestination(
                    context,
                    training,
                    onActiveTrainingChange = { activeTraining = it },
                    onPreview = { practicePreview = it },
                    trainingPersistence = trainingPersistence,
                    onLeave = {
                        suppressPracticeResume = true
                        activeTraining = null
                        nav.popOrDashboard()
                    },
                )
            }
        }
        composable<PracticeChecker> {
            if (activeTraining == null) {
                LaunchedEffect(Unit) { nav.popBackStack() }
            } else {
                PracticeCheckerDestination(context, nav, practicePreview)
            }
        }
        composable<Settings>(
            deepLinks = listOf(navDeepLink<Settings>(basePath = AppDeepLinks.SETTINGS)),
        ) {
            SettingsDestination(context, nav)
        }
        composable<CheckerSettings>(
            deepLinks =
                listOf(navDeepLink<CheckerSettings>(basePath = AppDeepLinks.CHECKER_SETTINGS)),
        ) {
            CheckerSettingsDestination(context, nav)
        }
        composable<Catalog>(
            deepLinks = listOf(navDeepLink<Catalog>(basePath = AppDeepLinks.CATALOG)),
        ) {
            CatalogDestination(context, onBack = { nav.popOrDashboard() }, CatalogSection.Textbooks)
        }
        composable<References>(
            deepLinks = listOf(navDeepLink<References>(basePath = AppDeepLinks.REFERENCES)),
        ) {
            CatalogDestination(context, onBack = { nav.popOrDashboard() }, CatalogSection.References)
        }
        composable<Dictionary>(
            deepLinks = listOf(navDeepLink<Dictionary>(basePath = AppDeepLinks.DICTIONARY)),
        ) {
            DictionaryDestination(context, onBack = { nav.popOrDashboard() })
        }
        composable<RecipeBuilder>(
            deepLinks = listOf(navDeepLink<RecipeBuilder>(basePath = AppDeepLinks.RECIPES)),
        ) { entry ->
            val route = entry.toRoute<RecipeBuilder>()
            RecipeBuilderDestination(
                context,
                nav,
                openCreate = route.openCreate,
                launchAfterCreate = route.launchAfterCreate,
                onBack = { nav.popOrDashboard() },
                onActiveTrainingChange = {
                    suppressPracticeResume = false
                    activeTraining = it
                },
                trainingPersistence = trainingPersistence,
            )
        }
        composable<TrainingHistory> { entry ->
            val period =
                ru.mobui.kanji.feature.dashboard.history.TrainingHistoryPeriod.fromKey(
                    entry.toRoute<TrainingHistory>().period,
                )
            ru.mobui.kanji.feature.dashboard.history.TrainingHistoryRoute(
                repository = context.dependencies.trainingsRepository,
                initialPeriod = period,
                onBack = { nav.popOrDashboard() },
            )
        }
    }
}

/** Cold-start and warm VIEW intents for [AppDeepLinks]; skips re-entry after rotation. */
@Composable
private fun HandleAppDeepLinks(nav: NavHostController) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    var coldStartHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(nav, activity) {
        if (!coldStartHandled) {
            coldStartHandled = true
            nav.handleDeepLink(activity.intent)
        }
    }
    DisposableEffect(nav, activity) {
        val listener = Consumer<Intent> { intent -> nav.handleDeepLink(intent) }
        activity.addOnNewIntentListener(listener)
        onDispose { activity.removeOnNewIntentListener(listener) }
    }
}

private fun NavHostController.popOrDashboard() {
    if (!popBackStack()) {
        navigate(Dashboard) {
            popUpTo(Dashboard) { inclusive = true }
            launchSingleTop = true
        }
    }
}

@Composable
private fun DictionaryDestination(context: AppUiContext, onBack: () -> Unit) {
    with(context) {
        val revision by dependencies.catalogRevision.collectAsStateWithLifecycle()
        ru.mobui.kanji.feature.dictionary.DictionaryRoute(
            dependencies.dictionary,
            dependencies.catalog,
            dependencies.recipesRepository,
            onBack = onBack,
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
    onBack: () -> Unit,
    section: CatalogSection,
) {
    with(context) {
        val revision by dependencies.catalogRevision.collectAsStateWithLifecycle()
        ru.mobui.kanji.feature.catalog.CatalogRoute(
            dependencies.catalog,
            onBack = onBack,
            section = section,
            loadDrawing = { source.load(KanjiId("u${it.toString(APP_UNICODE_HEX_RADIX)}")) },
            revision = revision,
        )
    }
}

@Composable
private fun SettingsDestination(context: AppUiContext, nav: NavHostController) {
    with(context) {
        SettingsScreen(
            onBack = { nav.popOrDashboard() },
            onOpenChecker = { nav.navigate(CheckerSettings) },
            modelLoaded = modelStatus == JapaneseModelStatus.Ready,
            modelLoading = modelStatus == JapaneseModelStatus.Loading,
            onDownloadModel = dependencies.japaneseModel::load,
            onClearTrainingHistory = {
                dependencies.persistenceScope.launch(Dispatchers.IO) {
                    runCatching { dependencies.trainingsRepository.clearHistory() }
                }
            },
        )
    }
}

@Composable
private fun CheckerSettingsDestination(context: AppUiContext, nav: NavHostController) {
    with(context) {
        CheckerSettingsScreen(
            settings,
            settingsModel::update,
            onBack = { nav.popOrDashboard() },
            onReset = settingsModel::reset,
            readabilityAvailable = modelReady,
        )
    }
}

@Composable
private fun DashboardDestination(
    context: AppUiContext,
    nav: NavHostController,
    onActiveTrainingChange: (ActiveTraining?) -> Unit,
) {
    with(context) {
        val personalCount by dictionarySummary.count.collectAsStateWithLifecycle()
        val unfinished by
            dependencies.trainingsRepository.observeInProgress().collectAsStateWithLifecycle(null)
        val completedTasks by
            dependencies.trainingsRepository
                .observeCompletedTasks()
                .collectAsStateWithLifecycle(emptyList())
        val zone = java.time.ZoneId.systemDefault()
        val activity =
            remember(completedTasks) {
                ru.mobui.kanji.feature.dashboard.activityOverviewFromEndedAt(
                    completedTasks.map { it.endedAt },
                    zone,
                )
            }
        val statistics =
            remember(completedTasks) {
                ru.mobui.kanji.feature.dashboard.wordStatisticsForDay(
                    completedTasks.map { it.endedAt to it.statusKey },
                    zone,
                )
            }
        val scope = rememberCoroutineScope()
        val language = LocalConfiguration.current.preferredLanguageTag()
        val unfinishedSummary =
            unfinished?.let { training ->
                val done =
                    training.tasks.count {
                        it.status != ru.mobui.kanji.domain.practice.TrainingTaskStatus.Pending &&
                            it.status !=
                                ru.mobui.kanji.domain.practice.TrainingTaskStatus.InProgress
                    }
                val current = (done + 1).coerceAtMost(training.tasks.size)
                ru.mobui.kanji.feature.dashboard.UnfinishedTrainingSummary(
                    recipeName =
                        training.recipeNames
                            .firstOrNull { it.languageTag.equals(language, ignoreCase = true) }
                            ?.name
                            ?: training.recipeNames
                                .firstOrNull { it.languageTag.equals("ru", ignoreCase = true) }
                                ?.name
                            ?: training.recipeNames.firstOrNull()?.name
                            ?: stringResource(ru.mobui.kanji.feature.dashboard.R.string.dashboard_mix),
                    progressLabel =
                        stringResource(
                            ru.mobui.kanji.feature.dashboard.R.string.training_word_progress,
                            current,
                            training.tasks.size,
                        ),
                )
            }
        DashboardScreen(
            DashboardDemo.state.copy(
                statistics = statistics,
                activity = activity,
                vocabulary = DashboardDemo.state.vocabulary.copy(personalTotal = personalCount),
            ),
            onStartTraining = { nav.navigate(TrainingPick) },
            onOpenSettings = { nav.navigate(Settings) },
            unfinished = unfinishedSummary,
            onContinueTraining = {
                scope.launch {
                    val training =
                        dependencies.trainingsRepository.getInProgress() ?: return@launch
                    val active = resumeActiveTraining(training) ?: return@launch
                    withContext(Dispatchers.Main.immediate) {
                        onActiveTrainingChange(active)
                        nav.navigate(Practice) {
                            popUpTo(Dashboard) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            },
            onStartNewTraining = {
                scope.launch {
                    interruptInProgress(dependencies)
                    withContext(Dispatchers.Main.immediate) { nav.navigate(TrainingPick) }
                }
            },
            onOpenRecipeBook = { nav.navigate(RecipeBuilder()) },
            onOpenCatalog = { nav.navigate(Catalog) },
            onOpenReferences = { nav.navigate(References) },
            onOpenDictionary = { nav.navigate(Dictionary) },
            onOpenWordStatistics = {
                nav.navigate(TrainingHistory(period = "today")) { launchSingleTop = true }
            },
            onOpenActivity = {
                nav.navigate(TrainingHistory(period = "all")) { launchSingleTop = true }
            },
        )
    }
}

@Composable
private fun TrainingPickDestination(
    context: AppUiContext,
    nav: NavHostController,
    onActiveTrainingChange: (ActiveTraining?) -> Unit,
    trainingPersistence: TrainingPersistence,
) {
    with(context) {
        val recipes by
            dependencies.recipesRepository.observeRecipes().collectAsStateWithLifecycle(emptyList())
        val assignments by
            dependencies.recipesRepository
                .observeWordTags()
                .collectAsStateWithLifecycle(emptyList())
        val history by
            dependencies.trainingsRepository
                .observeHistory()
                .collectAsStateWithLifecycle(emptyList())
        val excellentRecipeIds = remember(recipes, history) {
            findExcellentRecipeIds(recipes, history)
        }
        var poolCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
        LaunchedEffect(recipes, assignments) {
            val snapshot = withContext(Dispatchers.IO) { dependencies.catalog.load() }
            poolCounts =
                withContext(Dispatchers.Default) {
                    recipes.associate { recipe ->
                        recipe.id to
                            snapshot
                                .recipeWords(recipe.materials, assignments)
                                .size
                    }
                }
        }
        var query by rememberSaveable { mutableStateOf("") }
        var emptyLaunch by rememberSaveable { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val language = LocalConfiguration.current.preferredLanguageTag()
        if (emptyLaunch) {
            ru.mobui.kanji.core.designsystem.KanjiInfoDialog(
                title = stringResource(ru.mobui.kanji.feature.dashboard.R.string.training_pick_no_tasks_title),
                message = stringResource(ru.mobui.kanji.feature.dashboard.R.string.training_pick_no_tasks_body),
                onDismiss = { emptyLaunch = false },
                buttonText = stringResource(R.string.close),
                testTag = "training-pick-no-tasks",
            )
        }
        ru.mobui.kanji.feature.dashboard.TrainingPickScreen(
            recipes = recipes,
            loading = false,
            query = query,
            language = language,
            onBack = { nav.popOrDashboard() },
            onQuery = { query = it },
            poolCounts = poolCounts,
            excellentRecipeIds = excellentRecipeIds,
            onPick = { recipeId ->
                scope.launch {
                    val launch = loadTrainingLaunch(dependencies, recipeId)
                    if (launch == null) {
                        withContext(Dispatchers.Main.immediate) { emptyLaunch = true }
                        return@launch
                    }
                    trainingPersistence.start(launch.active.training)
                    withContext(Dispatchers.Main.immediate) {
                        onActiveTrainingChange(launch.active)
                        nav.navigate(Practice) {
                            popUpTo(TrainingPick) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            },
            onCreateFirstRecipe = {
                nav.navigate(RecipeBuilder(openCreate = true, launchAfterCreate = true))
            },
        )
    }
}

private suspend fun interruptInProgress(dependencies: KanjiApplication) {
    val training = dependencies.trainingsRepository.getInProgress() ?: return
    val now = System.currentTimeMillis()
    dependencies.trainingsRepository.saveTraining(
        training.copy(
            status = TrainingStatus.Interrupted,
            endedAt = now,
        ),
    )
}

private suspend fun loadTrainingLaunch(
    dependencies: KanjiApplication,
    recipeId: String? = null,
): TrainingLaunch? {
    val recipes = dependencies.recipesRepository.observeRecipes().first()
    val recipe =
        recipeId?.let { id -> recipes.firstOrNull { it.id == id } }
            ?: recipes.firstOrNull { it.isActive }
            ?: return null
    val assignments = dependencies.recipesRepository.observeWordTags().first()
    val snapshot = dependencies.catalog.load()
    return launchTrainingFromRecipe(snapshot, recipe, assignments)
}

@Composable
private fun RecipeBuilderDestination(
    context: AppUiContext,
    nav: NavHostController,
    openCreate: Boolean,
    launchAfterCreate: Boolean,
    onBack: () -> Unit,
    onActiveTrainingChange: (ActiveTraining?) -> Unit,
    trainingPersistence: TrainingPersistence,
) {
    with(context) {
        val revision by dependencies.catalogRevision.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        var emptyLaunch by rememberSaveable { mutableStateOf(false) }
        if (emptyLaunch) {
            ru.mobui.kanji.core.designsystem.KanjiInfoDialog(
                title = stringResource(ru.mobui.kanji.feature.dashboard.R.string.training_pick_no_tasks_title),
                message = stringResource(ru.mobui.kanji.feature.dashboard.R.string.training_pick_no_tasks_body),
                onDismiss = { emptyLaunch = false },
                buttonText = stringResource(R.string.close),
                testTag = "recipe-create-no-tasks",
            )
        }
        ru.mobui.kanji.feature.dashboard.builder.RecipeBuilderRoute(
            dependencies.catalog,
            dependencies.dictionary,
            dependencies.recipesRepository,
            ru.mobui.kanji.feature.dashboard.builder.RecipeBuilderActions(
                onBack = onBack,
                wordContent = { id, back ->
                    ru.mobui.kanji.feature.catalog.CatalogRoute(
                        dependencies.catalog,
                        back,
                        initialWordId = id,
                        loadDrawing = { source.load(KanjiId("u${it.toString(APP_UNICODE_HEX_RADIX)}")) },
                        revision = revision,
                    )
                },
                tagContent = { back ->
                    ru.mobui.kanji.feature.dictionary.DictionaryTagManagementRoute(
                        dependencies.recipesRepository,
                        back,
                    )
                },
                onCreatedForLaunch = { recipeId ->
                    scope.launch {
                        val launch = loadTrainingLaunch(dependencies, recipeId)
                        if (launch == null) {
                            withContext(Dispatchers.Main.immediate) {
                                emptyLaunch = true
                                // Recipe exists: return to pick so the user can edit materials.
                                nav.popBackStack()
                            }
                            return@launch
                        }
                        trainingPersistence.start(launch.active.training)
                        withContext(Dispatchers.Main.immediate) {
                            onActiveTrainingChange(launch.active)
                            nav.navigate(Practice) {
                                popUpTo(TrainingPick) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                },
            ),
            openCreate = openCreate,
            launchAfterCreate = launchAfterCreate,
            trainingsRepository = dependencies.trainingsRepository,
        )
    }
}

@Composable
private fun PracticePadDestination(
    context: AppUiContext,
    active: ActiveTraining,
    onActiveTrainingChange: (ActiveTraining?) -> Unit,
    onPreview: (ru.mobui.kanji.feature.practice.PracticeUiState) -> Unit,
    trainingPersistence: TrainingPersistence,
    onLeave: () -> Unit,
) {
    with(context) {
        val task = active.task
        // Application scope: leave/pop must not cancel the in-progress save.
        val persistence = dependencies.persistenceScope
        val practice: PracticeViewModel =
            viewModel(key = "${task.taskId}-${active.index}", factory = graph.factory(task))
        var leaving by remember(task.taskId, active.index) { mutableStateOf(false) }
        val leave: () -> Unit = {
            if (!leaving) {
                leaving = true
                if (active.training.status == TrainingStatus.InProgress) {
                    trainingPersistence.saveAsync(active.training)
                }
                onLeave()
            }
        }
        LaunchedEffect(settings, modelStatus, task.taskId, active.index) {
            practice.onCheckerSettingsChanged()
        }
        val state by practice.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(state) { onPreview(state) }
        val result = state.practice.result
        var resultReceived by rememberSaveable(task.taskId, active.index) { mutableStateOf(false) }
        var advancing by remember(task.taskId, active.index) { mutableStateOf(false) }
        if (result == null || !resultReceived) {
            PracticeRoute(
                practice,
                onResult = { practiceResult ->
                    if (!advancing && !leaving) {
                        advancing = true
                        persistence.launch {
                            val updated =
                                active.withTaskResult(
                                    practiceResult,
                                    System.currentTimeMillis(),
                                )
                            // Await boundary write so resume after kill matches the demo screen.
                            runCatching { trainingPersistence.save(updated.training) }
                            val next = updated.advance()
                            if (next != null) {
                                onActiveTrainingChange(next)
                            } else {
                                onActiveTrainingChange(updated)
                                resultReceived = true
                                advancing = false
                            }
                        }
                    }
                },
                onBack = leave,
                wordLabel = task.russianWord,
                progressLabel =
                    stringResource(
                        R.string.practice_word_progress,
                        active.ordinal,
                        active.total,
                    ),
            )
        } else {
            BackHandler(enabled = !leaving) { leave() }
            TrainingCompleteScreen(
                summary = active.training.summarize(),
                onDone = leave,
            )
        }
    }
}

@Composable
private fun PracticeCheckerDestination(
    context: AppUiContext,
    nav: NavHostController,
    preview: ru.mobui.kanji.feature.practice.PracticeUiState?,
) {
    with(context) {
        CheckerSettingsScreen(
            settings,
            onChange = { settingsModel.update(it) },
            onBack = { nav.popBackStack() },
            onReset = { settingsModel.reset() },
            preview = preview,
            readabilityAvailable = modelReady,
        )
    }
}
