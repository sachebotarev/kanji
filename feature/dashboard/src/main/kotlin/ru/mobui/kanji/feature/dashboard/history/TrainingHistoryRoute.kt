package ru.mobui.kanji.feature.dashboard.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import ru.mobui.kanji.core.designsystem.preferredLanguageTag
import ru.mobui.kanji.domain.practice.Training
import ru.mobui.kanji.domain.practice.TrainingsRepository

@Serializable private data object HistoryList

@Serializable private data class HistoryDetail(val id: String)

@Composable
fun TrainingHistoryRoute(
    repository: TrainingsRepository,
    initialPeriod: TrainingHistoryPeriod,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = rememberNavController()
    val language = LocalConfiguration.current.preferredLanguageTag()
    val model: TrainingHistoryViewModel =
        viewModel(
            key = "training-history-${initialPeriod.key}",
            factory =
                viewModelFactory {
                    initializer {
                        TrainingHistoryViewModel(
                            createSavedStateHandle(),
                            repository,
                            initialPeriod,
                        )
                    }
                },
        )
    NavHost(nav, startDestination = HistoryList, modifier = modifier) {
        composable<HistoryList> {
            val state by model.state.collectAsStateWithLifecycle()
            val query by model.query.collectAsStateWithLifecycle()
            val period by model.periodState.collectAsStateWithLifecycle()
            TrainingHistoryScreen(
                state = state,
                query = query,
                period = period,
                language = language,
                actions =
                    TrainingHistoryActions(
                        onBack = onBack,
                        onOpen = { nav.navigate(HistoryDetail(it)) },
                        onDelete = model::delete,
                        onQuery = model::query,
                        onPeriod = model::setPeriod,
                        onClearPeriod = model::clearPeriod,
                        onRetry = model::retry,
                    ),
            )
        }
        composable<HistoryDetail> { entry ->
            val id = entry.toRoute<HistoryDetail>().id
            var training by remember(id) { mutableStateOf<Training?>(null) }
            val state by model.state.collectAsStateWithLifecycle()
            LaunchedEffect(id, state.trainings) {
                training =
                    state.trainings.firstOrNull { it.id == id }
                        ?: withContext(Dispatchers.IO) { model.training(id) }
            }
            TrainingDetailScreen(
                training = training ?: state.trainings.firstOrNull { it.id == id },
                language = language,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
