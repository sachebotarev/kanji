package ru.mobui.kanji.checker.writing

import ru.mobui.kanji.checker.strokes.geometry.ShapePoint
import ru.mobui.kanji.checker.strokes.matching.StrokeCorrespondenceMatcher
import ru.mobui.kanji.checker.strokes.writing.StrokeWritingEvaluator

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.mobui.kanji.domain.practice.*

/** Order and direction only. Shape, hooks and dictionary cannot change this verdict. */
class StrokeWritingChecker(
    private val references: KanjiReferenceSource,
    private val dispatcher: CoroutineDispatcher,
) : KanjiChecker {
    override suspend fun check(request: CheckRequest) = checkWithDetails(request).verdict
    override suspend fun checkWithDetails(request: CheckRequest): CheckReport = withContext(dispatcher) {
        val reference = references.load(request.kanjiId) ?: return@withContext CheckReport(CheckVerdict.Unavailable)
        val count = request.sample.strokeCount
        if (count < reference.strokes.size) return@withContext CheckReport(CheckVerdict.Incomplete)
        if (count != reference.strokes.size) return@withContext CheckReport(CheckVerdict.Incorrect,
            listOf(CheckFailure.StrokeCount(count, reference.strokes.size)))
        val correspondence = StrokeCorrespondenceMatcher()
        val actual = correspondence.normalize(
            request.sample.strokes.map { stroke -> stroke.points.map { ShapePoint(it.x, it.y) } })
        val expected = correspondence.normalize(
            reference.strokes.map { stroke -> stroke.map { ShapePoint(it.x, it.y) } })
        val assignment = correspondence.assign(actual, expected) ?: return@withContext CheckReport(CheckVerdict.Correct)
        val checks = StrokeWritingEvaluator().check(actual, expected, assignment,
            checkOrder = true, checkDirection = true)
        CheckReport(if (checks.failures.isEmpty()) CheckVerdict.Correct else CheckVerdict.Incorrect, checks.failures)
    }
}
