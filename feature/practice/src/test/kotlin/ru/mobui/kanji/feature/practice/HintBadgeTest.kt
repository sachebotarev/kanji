package ru.mobui.kanji.feature.practice

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.mobui.kanji.domain.practice.recordReadingHint
import ru.mobui.kanji.domain.practice.recordTranslationHint

class HintBadgeTest {
    @Test
    fun remainingHintsCalculatesCorrectly() {
        val max = 3
        assertEquals(3, (max - 0).coerceAtLeast(0))
        assertEquals(2, (max - 1).coerceAtLeast(0))
        assertEquals(1, (max - 2).coerceAtLeast(0))
        assertEquals(0, (max - 3).coerceAtLeast(0))
        assertEquals(0, (max - 4).coerceAtLeast(0))
    }

    @Test
    fun headerWordHintsIncreaseTotalHintCount() {
        val task = ru.mobui.kanji.domain.practice.PracticeTask(
            taskId = "test",
            russianWord = "день",
            kanji = listOf(
                ru.mobui.kanji.domain.practice.KanjiTask(
                    kanjiId = ru.mobui.kanji.domain.practice.KanjiId("日"),
                    mode = ru.mobui.kanji.domain.practice.EntryMode.User,
                    occurrenceId = "test-1",
                ),
            ),
        )
        val training = ru.mobui.kanji.domain.practice.HandwritingTraining(task)
        assertEquals(0, training.state.totalHintCount)

        training.recordReadingHint()
        assertEquals(1, training.state.totalHintCount)

        training.recordTranslationHint()
        assertEquals(2, training.state.totalHintCount)
    }

    @Test
    fun gesturesOnlyModeDefaultsToFalseInUiState() {
        val task = ru.mobui.kanji.domain.practice.PracticeTask(
            taskId = "test",
            russianWord = "день",
            kanji = listOf(
                ru.mobui.kanji.domain.practice.KanjiTask(
                    kanjiId = ru.mobui.kanji.domain.practice.KanjiId("日"),
                    mode = ru.mobui.kanji.domain.practice.EntryMode.User,
                    occurrenceId = "test-1",
                ),
            ),
        )
        val training = ru.mobui.kanji.domain.practice.HandwritingTraining(task)
        val state = PracticeUiState(practice = training.state)
        assertEquals(false, state.gesturesOnlyMode)
        assertEquals(true, state.showPadGestureHints)
    }

}
