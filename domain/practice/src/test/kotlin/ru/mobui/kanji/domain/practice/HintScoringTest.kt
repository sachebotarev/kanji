package ru.mobui.kanji.domain.practice

import org.junit.Assert.assertEquals
import org.junit.Test

class HintScoringTest {
    @Test
    fun practiceSymbolPointsRetainTwentyPercentLessPerHint() {
        assertEquals(1.0, practiceSymbolPoints(TrainingSymbolStatus.Correct, 0, 3), 1e-9)
        assertEquals(0.8, practiceSymbolPoints(TrainingSymbolStatus.Correct, 1, 3), 1e-9)
        assertEquals(0.64, practiceSymbolPoints(TrainingSymbolStatus.Correct, 2, 3), 1e-9)
        assertEquals(0.0, practiceSymbolPoints(TrainingSymbolStatus.Incorrect, 3, 3), 1e-9)
    }

    @Test
    fun firstHintHasSameCostForEveryConfiguredLimit() {
        for (limit in 1..12) {
            assertEquals(0.8, practiceSymbolPoints(TrainingSymbolStatus.Correct, 1, limit), 1e-9)
        }
    }

    @Test
    fun usingAllHintsStillAwardsCreditForCorrectWriting() {
        assertEquals(0.8, practiceSymbolPoints(TrainingSymbolStatus.Correct, 1, 1), 1e-9)
        assertEquals(0.512, practiceSymbolPoints(TrainingSymbolStatus.Correct, 3, 3), 1e-9)
        assertEquals(0.32768, practiceSymbolPoints(TrainingSymbolStatus.Correct, 5, 5), 1e-9)
        org.junit.Assert.assertTrue(practiceSymbolPoints(TrainingSymbolStatus.Correct, 12, 12) > 0.0)
    }

    @Test
    fun unfinishedOrIncorrectWritingEarnsNoCreditEvenWithHints() {
        for (status in listOf(TrainingSymbolStatus.Empty, TrainingSymbolStatus.Editing, TrainingSymbolStatus.Incorrect)) {
            assertEquals(0.0, practiceSymbolPoints(status, 1, 1), 1e-9)
        }
    }

    @Test
    fun zeroMaxHintsGivesFullScoreWhenCorrectWithNoHints() {
        assertEquals(1.0, practiceSymbolPoints(TrainingSymbolStatus.Correct, 0, 0), 1e-9)
        assertEquals(0.0, practiceSymbolPoints(TrainingSymbolStatus.Incorrect, 0, 0), 1e-9)
        assertEquals(0.0, practiceSymbolPoints(TrainingSymbolStatus.Incorrect, 1, 0), 1e-9)
    }

    @Test
    fun wordPointsAveragesPracticeSymbolsAndAppliesReadingFactor() {
        val task =
            TrainingTask(
                id = "t",
                position = 0,
                writtenForm = "日本",
                reading = "にほん",
                translations = listOf(TrainingTaskTranslation("ru", "Япония")),
                status = TrainingTaskStatus.Assisted,
                readingHintUsed = true,
                symbols =
                    listOf(
                        symbol("0", TrainingSymbolRole.Practice, TrainingSymbolStatus.Correct, 1),
                        symbol("1", TrainingSymbolRole.Display, TrainingSymbolStatus.Prefilled, 0),
                        symbol("2", TrainingSymbolRole.Practice, TrainingSymbolStatus.Correct, 0),
                    ),
            )
        // avg(0.8, 1) × 0.85
        assertEquals((0.8 + 1.0) / 2.0 * 0.85, task.wordPoints(3), 1e-9)
    }

    @Test
    fun wordPointsAppliesTranslationFactor() {
        val task =
            TrainingTask(
                id = "t",
                position = 0,
                writtenForm = "日本",
                reading = "にほん",
                translations = listOf(TrainingTaskTranslation("ru", "Япония")),
                status = TrainingTaskStatus.Assisted,
                readingHintUsed = false,
                translationHintUsed = true,
                symbols =
                    listOf(
                        symbol("0", TrainingSymbolRole.Practice, TrainingSymbolStatus.Correct, 0),
                    ),
            )
        // 1.0 × 0.85
        assertEquals(0.85, task.wordPoints(3), 1e-9)
    }

    @Test
    fun wordPointsWithoutHintsHasNoPenaltyOnResult() {
        val task =
            TrainingTask(
                id = "t",
                position = 0,
                writtenForm = "日本",
                reading = "にほん",
                translations = listOf(TrainingTaskTranslation("ru", "Япония")),
                status = TrainingTaskStatus.Correct,
                readingHintUsed = false,
                translationHintUsed = false,
                symbols =
                    listOf(
                        symbol("0", TrainingSymbolRole.Practice, TrainingSymbolStatus.Correct, 0),
                        symbol("1", TrainingSymbolRole.Practice, TrainingSymbolStatus.Correct, 0),
                    ),
            )
        // avg(1, 1) × 1.0 × 1.0 = 1.0 (no penalty)
        assertEquals(1.0, task.wordPoints(3), 1e-9)
    }

    @Test
    fun wordPointsForTimedOutAwardsPartialCreditForCorrectKanji() {
        val task =
            TrainingTask(
                id = "t",
                position = 0,
                writtenForm = "日本",
                reading = "にほん",
                translations = listOf(TrainingTaskTranslation("ru", "Япония")),
                status = TrainingTaskStatus.TimedOut,
                readingHintUsed = false,
                translationHintUsed = false,
                symbols =
                    listOf(
                        symbol("0", TrainingSymbolRole.Practice, TrainingSymbolStatus.Correct, 0),
                        symbol("1", TrainingSymbolRole.Practice, TrainingSymbolStatus.Incorrect, 0),
                    ),
            )
        // (1.0 + 0.0) / 2 = 0.50
        assertEquals(0.50, task.wordPoints(3), 1e-9)
    }

    @Test
    fun wordPointsForSkippedAwardsPartialCreditForCorrectKanji() {
        val task =
            TrainingTask(
                id = "t",
                position = 0,
                writtenForm = "日本語",
                reading = "にほんご",
                translations = listOf(TrainingTaskTranslation("ru", "Японский язык")),
                status = TrainingTaskStatus.Skipped,
                readingHintUsed = true,
                translationHintUsed = false,
                symbols =
                    listOf(
                        symbol("0", TrainingSymbolRole.Practice, TrainingSymbolStatus.Correct, 0),
                        symbol("1", TrainingSymbolRole.Practice, TrainingSymbolStatus.Correct, 1),
                        symbol("2", TrainingSymbolRole.Practice, TrainingSymbolStatus.Empty, 0),
                    ),
            )
        // avg(1.0, 0.8, 0.0) × 0.85 = 0.51
        assertEquals((1.0 + 0.8 + 0.0) / 3.0 * 0.85, task.wordPoints(3), 1e-9)
    }

    private fun symbol(
        id: String,
        role: TrainingSymbolRole,
        status: TrainingSymbolStatus,
        hints: Int,
    ) =
        TrainingSymbol(
            id = id,
            position = id.toInt(),
            occurrenceId = id,
            role = role,
            script = TrainingSymbolScript.Kanji,
            glyph = "字",
            status = status,
            hintRequestCount = hints,
        )
}
