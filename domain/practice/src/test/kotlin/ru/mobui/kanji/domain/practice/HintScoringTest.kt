package ru.mobui.kanji.domain.practice

import org.junit.Assert.assertEquals
import org.junit.Test

class HintScoringTest {
    @Test
    fun practiceSymbolPointsLinearInHintBudget() {
        assertEquals(1.0, practiceSymbolPoints(TrainingSymbolStatus.Correct, 0, 3), 1e-9)
        assertEquals(2.0 / 3.0, practiceSymbolPoints(TrainingSymbolStatus.Correct, 1, 3), 1e-9)
        assertEquals(1.0 / 3.0, practiceSymbolPoints(TrainingSymbolStatus.Correct, 2, 3), 1e-9)
        assertEquals(0.0, practiceSymbolPoints(TrainingSymbolStatus.Incorrect, 3, 3), 1e-9)
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
        // avg(2/3, 1) × 0.85
        assertEquals((2.0 / 3.0 + 1.0) / 2.0 * 0.85, task.wordPoints(3), 1e-9)
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
