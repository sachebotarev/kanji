package ru.mobui.kanji

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.domain.practice.CompletedTrainingTask
import ru.mobui.kanji.domain.practice.PracticeResult
import ru.mobui.kanji.domain.practice.AnswerStatus
import ru.mobui.kanji.domain.practice.TrainingsRepository
import ru.mobui.kanji.domain.practice.HintMode
import ru.mobui.kanji.domain.practice.TaskClueMode
import ru.mobui.kanji.domain.practice.Training
import ru.mobui.kanji.domain.practice.TrainingRecipeName
import ru.mobui.kanji.domain.practice.TrainingStatus
import ru.mobui.kanji.domain.practice.TrainingSymbol
import ru.mobui.kanji.domain.practice.TrainingSymbolRole
import ru.mobui.kanji.domain.practice.TrainingSymbolScript
import ru.mobui.kanji.domain.practice.TrainingSymbolStatus
import ru.mobui.kanji.domain.practice.TrainingTask
import ru.mobui.kanji.domain.practice.TrainingTaskStatus
import ru.mobui.kanji.domain.practice.TrainingTaskTranslation

class TrainingSessionRecreationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun activityRecreationKeepsTheProductionTrainingOwnerAndActiveWord() {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("dashboard-screen").fetchSemanticsNodes().isNotEmpty()
        }
        lateinit var original: TrainingSessionViewModel
        val active = requireNotNull(resumeActiveTraining(fixture()))
        compose.runOnIdle {
            original = ViewModelProvider(compose.activity)[TrainingSessionViewModel::class.java]
            original.activate(active)
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("dashboard-screen").fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle {
            val recreated = ViewModelProvider(compose.activity)[TrainingSessionViewModel::class.java]
            assertSame(original, recreated)
            assertEquals(active, recreated.state.value.active)
            recreated.leave()
        }
    }

    private fun fixture(): Training =
Training(
                id = "run",
                recipeNames = listOf(TrainingRecipeName("ru", "Луна")),
                clueMode = TaskClueMode.None,
                strokeHintMode = HintMode.FullCharacter,
                maxNextStrokeHints = 5,
                status = TrainingStatus.InProgress,
                startedAt = 1L,
                tasks =
                    listOf(
                        TrainingTask(
                            id = "word",
                            position = 0,
                            writtenForm = "日",
                            reading = "ひ",
                            translations = listOf(TrainingTaskTranslation("ru", "солнце")),
                            status = TrainingTaskStatus.Pending,
                            symbols =
                                listOf(
                                    TrainingSymbol(
                                        id = "s",
                                        position = 0,
                                        occurrenceId = "cell",
                                        role = TrainingSymbolRole.Practice,
                                        script = TrainingSymbolScript.Kanji,
                                        glyph = "日",
                                        kanjiKey = "u65e5",
                                        status = TrainingSymbolStatus.Empty,
                                    ),
                                ),
                        ),
                    ),
            )
}
