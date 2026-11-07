package ru.mobui.kanji.domain.practice

import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingGradeTest {
    @Test
    fun excellentWhenMostlyCorrect() {
        val summary =
            training(
                TrainingTaskStatus.Correct,
                TrainingTaskStatus.Correct,
                TrainingTaskStatus.Correct,
                TrainingTaskStatus.Assisted,
            ).summarize()
        assertEquals(90, summary.percent)
        assertEquals(TrainingGradeLevel.Excellent, summary.grade)
        assertEquals(3, summary.correctCount)
        assertEquals(1, summary.assistedCount)
        assertEquals("Умный микс", summary.recipeName)
    }

    @Test
    fun steadyInMiddleBand() {
        val summary =
            training(
                TrainingTaskStatus.Correct,
                TrainingTaskStatus.Correct,
                TrainingTaskStatus.Assisted,
                TrainingTaskStatus.Skipped,
                TrainingTaskStatus.TimedOut,
            ).summarize()
        // (1+1+0.6+0+0.25)/5 = 0.57 → 57%
        assertEquals(57, summary.percent)
        assertEquals(TrainingGradeLevel.Steady, summary.grade)
    }

    @Test
    fun keepGoingWhenMostlyMissed() {
        val summary =
            training(
                TrainingTaskStatus.Skipped,
                TrainingTaskStatus.Skipped,
                TrainingTaskStatus.TimedOut,
                TrainingTaskStatus.Assisted,
            ).summarize()
        // (0+0+0.25+0.6)/4 = 0.2125 → 21%
        assertEquals(21, summary.percent)
        assertEquals(TrainingGradeLevel.KeepGoing, summary.grade)
        assertEquals(2, summary.skippedCount)
        assertEquals(1, summary.timedOutCount)
    }

    @Test
    fun recipeNameFallsBackToRussian() {
        val training =
            training(TrainingTaskStatus.Correct)
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

    private fun training(vararg statuses: TrainingTaskStatus): Training =
        Training(
            id = "t1",
            recipeId = "r1",
            recipeRevision = 1,
            recipeNames = listOf(TrainingRecipeName("ru", "Умный микс")),
            taskCount = statuses.size,
            secondsPerWord = 60,
            status = TrainingStatus.Completed,
            startedAt = 1_000L,
            endedAt = 2_000L,
            tasks =
                statuses.mapIndexed { index, status ->
                    TrainingTask(
                        id = "task-$index",
                        position = index,
                        wordId = null,
                        writtenForm = "字",
                        reading = "じ",
                        translations = listOf(TrainingTaskTranslation("ru", "знак")),
                        status = status,
                        startedAt = 1_000L,
                        endedAt = 1_500L,
                        elapsedMs = 1_000L * (index + 1),
                        symbols =
                            listOf(
                                TrainingSymbol(
                                    id = "sym-$index",
                                    position = 0,
                                    occurrenceId = "occ-$index",
                                    role = TrainingSymbolRole.Practice,
                                    script = TrainingSymbolScript.Kanji,
                                    glyph = "字",
                                    kanjiId = null,
                                    kanjiKey = "u5b57",
                                    status = TrainingSymbolStatus.Correct,
                                ),
                            ),
                    )
                },
        )
}
