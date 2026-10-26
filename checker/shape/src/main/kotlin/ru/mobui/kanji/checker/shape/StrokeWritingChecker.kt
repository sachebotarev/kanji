package ru.mobui.kanji.checker.shape

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
        val checks = StrokeChecks(StrokeCorrespondence()).check(
            request.sample.strokes.map { stroke -> stroke.points.map { ShapePoint(it.x, it.y) } },
            reference.strokes.map { stroke -> stroke.map { ShapePoint(it.x, it.y) } },
            RecognitionSettings(checkStrokeStructure = false, checkStrokeOrder = true, checkStrokeDirection = true),
        )
        CheckReport(if (checks.failures.isEmpty()) CheckVerdict.Correct else CheckVerdict.Incorrect, checks.failures)
    }
}
