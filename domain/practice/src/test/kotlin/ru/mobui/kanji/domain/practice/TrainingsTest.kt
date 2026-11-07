package ru.mobui.kanji.domain.practice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TrainingsTest {
    private fun sample(
        status: TrainingStatus = TrainingStatus.InProgress,
        endedAt: Long? = null,
        role: TrainingSymbolRole = TrainingSymbolRole.Practice,
        symbolStatus: TrainingSymbolStatus = TrainingSymbolStatus.Empty,
    ) =
        Training(
            id = "t1",
            recipeNames = listOf(TrainingRecipeName("ru", "Рецепт")),
            status = status,
            startedAt = 10,
            endedAt = endedAt,
            tasks =
                listOf(
                    TrainingTask(
                        id = "task",
                        position = 0,
                        writtenForm = "あ",
                        reading = "あ",
                        translations = listOf(TrainingTaskTranslation("ru", "а")),
                        symbols =
                            listOf(
                                TrainingSymbol(
                                    id = "s",
                                    position = 0,
                                    occurrenceId = "0",
                                    role = role,
                                    script = TrainingSymbolScript.Hiragana,
                                    glyph = "あ",
                                    status = symbolStatus,
                                ),
                            ),
                    ),
                ),
        )

    @Test
    fun startRequiresInProgressWithoutEnd() {
        assertEquals(TrainingStatus.InProgress, sample().validatedForStart().status)
        assertThrows(IllegalArgumentException::class.java) {
            sample(TrainingStatus.Completed, endedAt = 20).validatedForStart()
        }
    }

    @Test
    fun displaySymbolMustBePrefilled() {
        assertThrows(IllegalArgumentException::class.java) {
            sample(role = TrainingSymbolRole.Display, symbolStatus = TrainingSymbolStatus.Empty)
                .validatedForSave()
        }
        sample(role = TrainingSymbolRole.Display, symbolStatus = TrainingSymbolStatus.Prefilled)
            .validatedForSave()
    }
}
