package ru.mobui.kanji

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import ru.mobui.kanji.checker.shape.ShapeKanjiChecker
import ru.mobui.kanji.checker.writing.StrokeWritingChecker
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.data.kanjivg.BundledKanjiReferenceSource
import ru.mobui.kanji.domain.practice.CombinedKanjiChecker
import ru.mobui.kanji.domain.practice.PracticeTask
import ru.mobui.kanji.feature.practice.PracticeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dependencies = application as KanjiApplication
        val models = createModels(dependencies)
        val graph = createPracticeGraph(dependencies, models.settingsModel)
        setContent { KanjiTheme { MainContent(dependencies, models, graph) } }
    }

    public override fun onNewIntent(intent: Intent) {
        // setIntent before listeners so handleDeepLink sees the VIEW URI.
        setIntent(intent)
        super.onNewIntent(intent)
    }

    private fun createModels(dependencies: KanjiApplication): AppModels {
        val settingsModel =
            ViewModelProvider(
                this,
                viewModelFactory {
                    initializer {
                        RecognitionSettingsViewModel(
                            createSavedStateHandle(),
                            dependencies.recognitionSettings,
                            dependencies.persistenceScope,
                        )
                    }
                },
            )[RecognitionSettingsViewModel::class.java]
        val dictionarySummary =
            ViewModelProvider(
                this,
                viewModelFactory {
                    initializer { DictionarySummaryViewModel(dependencies.dictionary) }
                },
            )[DictionarySummaryViewModel::class.java]
        return AppModels(settingsModel, dictionarySummary)
    }

    private fun createPracticeGraph(
        dependencies: KanjiApplication,
        settingsModel: RecognitionSettingsViewModel,
    ): DemoPracticeGraph {
        val source = BundledKanjiReferenceSource(assets, Dispatchers.IO)
        val checker = createChecker(dependencies, settingsModel, source)
        return DemoPracticeGraph(
            source = source,
            factory = { task ->
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        require(modelClass.isAssignableFrom(PracticeViewModel::class.java))
                        @Suppress("UNCHECKED_CAST")
                        return PracticeViewModel(
                            task,
                            source,
                            checker,
                            hintMode = { task.strokeHintMode },
                            maxNextStrokeHints = {
                                settingsModel.settings.value.maxNextStrokeHints
                            },
                            fullHintDisplayMillis = {
                                settingsModel.settings.value.fullHintDisplayMillis
                            },
                            autoAdvanceKanjiOnSuccess = {
                                settingsModel.settings.value.autoAdvanceKanjiOnSuccess
                            },
                            autoAdvanceOnSuccess = {
                                settingsModel.settings.value.autoAdvanceOnSuccess
                            },
                            gesturesOnlyMode = {
                                settingsModel.settings.value.gesturesOnlyMode
                            },
                            showPadGestureHints = {
                                settingsModel.settings.value.showPadGestureHints
                            },
                            showCheckDiagnostics = BuildConfig.DEBUG,
                        ) as T
                    }
                }
            },
        )
    }

    private fun createChecker(
        dependencies: KanjiApplication,
        settingsModel: RecognitionSettingsViewModel,
        source: BundledKanjiReferenceSource,
    ): CombinedKanjiChecker {
        val readability = dependencies.readability
        val shape =
            ShapeKanjiChecker(
                source,
                Dispatchers.Default,
                diagnosticsEnabled = BuildConfig.DEBUG,
                configProvider = {
                    settingsModel.settings.value.copy(
                        checkStrokeOrder = false,
                        checkStrokeDirection = false,
                    )
                },
            )
        val writing = StrokeWritingChecker(source, Dispatchers.Default)
        return CombinedKanjiChecker(
            readability,
            shape = { shape },
            writing = writing,
            settings = {
                settingsModel.settings.value.let {
                    it.copy(
                        readabilityEnabled =
                            it.readabilityEnabled &&
                                dependencies.japaneseModel.status.value ==
                                    JapaneseModelStatus.Ready,
                    )
                }
            },
        )
    }
}

internal const val APP_UNICODE_HEX_RADIX = 16
