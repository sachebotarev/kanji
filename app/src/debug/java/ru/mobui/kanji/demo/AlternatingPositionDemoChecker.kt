package ru.mobui.kanji.demo

import ru.mobui.kanji.domain.practice.CheckRequest
import ru.mobui.kanji.domain.practice.CheckVerdict
import ru.mobui.kanji.domain.practice.EntryMode
import ru.mobui.kanji.domain.practice.KanjiChecker
import ru.mobui.kanji.domain.practice.PracticeTask

/** Visual fixture only: even 1-based positions pass, odd positions fail, regardless of shape. */
internal class AlternatingPositionDemoChecker(task: PracticeTask) : KanjiChecker {
    private val taskId = task.taskId
    private val positions = task.kanji.withIndex().associate { it.value.occurrenceId to it }

    override suspend fun check(request: CheckRequest): CheckVerdict {
        val position = positions[request.occurrenceId] ?: return CheckVerdict.Unavailable
        if (request.taskId != taskId || request.kanjiId != position.value.kanjiId ||
            position.value.mode != EntryMode.User) return CheckVerdict.Unavailable
        if (request.sample.strokeCount == 0) return CheckVerdict.Incomplete
        return if ((position.index + 1) % 2 == 0) CheckVerdict.Correct else CheckVerdict.Incorrect
    }
}
