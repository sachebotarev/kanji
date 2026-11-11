package ru.mobui.kanji

import org.junit.Assert.assertEquals
import org.junit.Test
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

class ResumeActiveTrainingTest {
    @Test
    fun resumeKeepsClueAndStrokeHintFromTheStoredTraining() {
        val training =
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
        val resumed = requireNotNull(resumeActiveTraining(training))
        assertEquals(TaskClueMode.None, resumed.task.clueMode)
        assertEquals(HintMode.FullCharacter, resumed.task.strokeHintMode)
        assertEquals(5, resumed.training.maxNextStrokeHints)
        val completed = training.copy(status = TrainingStatus.Completed,
            tasks = training.tasks.map { it.copy(status = TrainingTaskStatus.Correct) })
        assertEquals(TrainingStatus.Completed, requireNotNull(resumeActiveTraining(completed)).training.status)
        val legacy = training.copy(tasks = training.tasks.map { task ->
            task.copy(symbols = task.symbols.map { it.copy(kanjiKey = null) })
        })
        assertEquals("u65e5", requireNotNull(resumeActiveTraining(legacy)).task.kanji.single().kanjiId.value)
    }
}
