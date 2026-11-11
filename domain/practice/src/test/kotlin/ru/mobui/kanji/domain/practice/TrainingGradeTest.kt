package ru.mobui.kanji.domain.practice

import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingGradeTest {
    @Test
    fun excellentWhenMostlyCorrect() {
        val summary =
            training(
                task(TrainingTaskStatus.Correct),
                task(TrainingTaskStatus.Correct),
                task(TrainingTaskStatus.Correct),
                task(TrainingTaskStatus.Assisted, hintCount = 1),
            ).summarize()
        // (1+1+1+0.8)/4 = 95
        assertEquals(95, summary.percent)
        assertEquals(TrainingGradeLevel.Excellent, summary.grade)
        assertEquals(3, summary.correctCount)
        assertEquals(1, summary.assistedCount)
        assertEquals("Умный микс", summary.recipeName)
    }

    @Test
    fun steadyInMiddleBand() {
        val summary =
            training(
                task(TrainingTaskStatus.Correct),
                task(TrainingTaskStatus.Correct),
                task(TrainingTaskStatus.Assisted, hintCount = 1),
                task(TrainingTaskStatus.Assisted, hintCount = 1),
                task(TrainingTaskStatus.TimedOut),
            ).summarize()
        // (1+1+0.8+0.8+0)/5 = 72
        assertEquals(72, summary.percent)
        assertEquals(TrainingGradeLevel.Steady, summary.grade)
    }

    @Test
    fun keepGoingWhenMostlyMissed() {
        val summary =
            training(
                task(TrainingTaskStatus.Skipped),
                task(TrainingTaskStatus.Skipped),
                task(TrainingTaskStatus.TimedOut),
                task(TrainingTaskStatus.Assisted, hintCount = 1),
            ).summarize()
        // (0+0+0+0.8)/4 = 20
        assertEquals(20, summary.percent)
        assertEquals(TrainingGradeLevel.KeepGoing, summary.grade)
        assertEquals(2, summary.skippedCount)
        assertEquals(1, summary.timedOutCount)
    }

    @Test
    fun timedOutWithPartiallyCorrectKanjiScoresPartialCredit() {
        val task =
            TrainingTask(
                id = "task",
                position = 0,
                wordId = null,
                writtenForm = "日本",
                reading = "にほん",
                translations = listOf(TrainingTaskTranslation("ru", "Япония")),
                status = TrainingTaskStatus.TimedOut,
                startedAt = 1_000L,
                endedAt = 1_500L,
                elapsedMs = 1_000L,
                readingHintUsed = false,
                symbols =
                    listOf(
                        TrainingSymbol(
                            id = "s1",
                            position = 0,
                            occurrenceId = "occ1",
                            role = TrainingSymbolRole.Practice,
                            script = TrainingSymbolScript.Kanji,
                            glyph = "日",
                            kanjiId = null,
                            kanjiKey = "u65e5",
                            status = TrainingSymbolStatus.Correct,
                            hintRequestCount = 0,
                        ),
                        TrainingSymbol(
                            id = "s2",
                            position = 1,
                            occurrenceId = "occ2",
                            role = TrainingSymbolRole.Practice,
                            script = TrainingSymbolScript.Kanji,
                            glyph = "本",
                            kanjiId = null,
                            kanjiKey = "u672c",
                            status = TrainingSymbolStatus.Incorrect,
                            hintRequestCount = 0,
                        ),
                    ),
            )
        // (1.0 + 0.0) / 2 = 0.50
        assertEquals(0.50, task.wordPoints(3), 1e-9)
        val summary = training(task).summarize()
        assertEquals(50, summary.percent)
        assertEquals(1, summary.timedOutCount)
    }

    @Test
    fun readingHintScalesAssistedWord() {
        val summary =
            training(
                task(TrainingTaskStatus.Assisted, hintCount = 0, readingHint = true),
            ).summarize()
        // 1.0 × 0.85 → 85
        assertEquals(85, summary.percent)
        assertEquals(TrainingGradeLevel.Excellent, summary.grade)
    }

    @Test
    fun recipeNameFallsBackToRussian() {
        val training =
            training(task(TrainingTaskStatus.Correct))
                .copy(
                    recipeNames =
                        listOf(
                            TrainingRecipeName("en", "Smart mix"),
                            TrainingRecipeName("ru", "Умный микс"),
                        ),
                )
        assertEquals("Умный микс", training.summarize("de").recipeName)
        assertEquals("Smart mix", training.summarize("en").recipeName)
    }

    @Test
    fun summarizeKeepsHintCostIndependentOfSavedLimit() {
        val t = training(task(TrainingTaskStatus.Assisted, hintCount = 1)).copy(maxNextStrokeHints = 2)
        val summary = t.summarize()
        assertEquals(80, summary.percent)
        assertEquals(TrainingGradeLevel.Steady, summary.grade)
    }

    private fun task(
        status: TrainingTaskStatus,
        hintCount: Int = 0,
        readingHint: Boolean = false,
    ): TrainingTask =
        TrainingTask(
            id = "task",
            position = 0,
            wordId = null,
            writtenForm = "字",
            reading = "じ",
            translations = listOf(TrainingTaskTranslation("ru", "знак")),
            status = status,
            startedAt = 1_000L,
            endedAt = 1_500L,
            elapsedMs = 1_000L,
            readingHintUsed = readingHint,
            symbols =
                listOf(
                    TrainingSymbol(
                        id = "sym",
                        position = 0,
                        occurrenceId = "occ",
                        role = TrainingSymbolRole.Practice,
                        script = TrainingSymbolScript.Kanji,
                        glyph = "字",
                        kanjiId = null,
                        kanjiKey = "u5b57",
                        status =
                            when (status) {
                                TrainingTaskStatus.Skipped,
                                TrainingTaskStatus.TimedOut -> TrainingSymbolStatus.Incorrect
                                else -> TrainingSymbolStatus.Correct
                            },
                        hintRequestCount = hintCount,
                    ),
                ),
        )

    private fun training(vararg tasks: TrainingTask): Training =
        Training(
            id = "t1",
            recipeId = "r1",
            recipeRevision = 1,
            recipeNames = listOf(TrainingRecipeName("ru", "Умный микс")),
            taskCount = tasks.size,
            secondsPerSymbol = 60,
            status = TrainingStatus.Completed,
            startedAt = 1_000L,
            endedAt = 2_000L,
            tasks = tasks.mapIndexed { index, task -> task.copy(id = "task-$index", position = index) },
        )
}
