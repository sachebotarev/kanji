package ru.mobui.kanji.domain.practice

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.WritingSample

class ShapeAcceptanceTest {
    private fun shape(actual: Float, required: Float = 80f) =
        CheckReport(CheckVerdict.Incorrect, listOf(CheckFailure.ShapeCoverage(actual, required)), matchPercent = actual)

    @Test
    fun configuredMarginControlsAcceptanceAndZeroDisablesIt() {
        assertTrue(shape(75f).withShapeTolerance(6f).acceptedWithPenalty)
        assertFalse(shape(75f).withShapeTolerance(5f).acceptedWithPenalty)
        assertFalse(shape(75f).withShapeTolerance(0f).acceptedWithPenalty)
        assertTrue(shape(66f).withShapeTolerance(15f).acceptedWithPenalty)
        assertEquals(10f, RecognitionSettings.touchInput().shapeSoftMarginPoints, 0f)
        assertEquals(6f, RecognitionSettings(shapeSoftMarginPoints = 6.4f).roundedParameters().shapeSoftMarginPoints, 0f)
    }

    @Test
    fun combinedCheckerReadsCurrentMargin() = runBlocking {
        val request = CheckRequest("task", "cell", KanjiId("日"), 0, WritingSample())
        val provider = object : KanjiChecker {
            override suspend fun check(request: CheckRequest) = CheckVerdict.Incorrect
            override suspend fun checkWithDetails(request: CheckRequest) = shape(75f)
        }
        var config = RecognitionSettings(shapeSoftMarginPoints = 6f)
        val correct = KanjiChecker { CheckVerdict.Correct }
        val checker = CombinedKanjiChecker(correct, { provider }, correct) { config }
        assertTrue(checker.checkWithDetails(request).acceptedWithPenalty)
        config = config.copy(shapeSoftMarginPoints = 5f)
        assertEquals(CheckVerdict.Incorrect, checker.check(request))
        config = config.copy(shapeSoftMarginPoints = 0f)
        assertEquals(CheckVerdict.Incorrect, checker.check(request))
    }

    @Test
    fun onlyDeficitsStrictlyBelowTenPointsAreAccepted() {
        assertTrue(shape(75f).withShapeTolerance().acceptedWithPenalty)
        assertEquals(CheckVerdict.Correct, shape(70.01f).withShapeTolerance().verdict)
        assertEquals(CheckVerdict.Incorrect, shape(70f).withShapeTolerance().verdict)
        assertEquals(CheckVerdict.Incorrect, shape(69f).withShapeTolerance().verdict)
        assertTrue(shape(55f, 60f).withShapeTolerance().acceptedWithPenalty)
        assertEquals(CheckVerdict.Incorrect, shape(Float.NaN).withShapeTolerance().verdict)
    }

    @Test
    fun otherFailuresAndUnfinishedInputCannotBeSoftAccepted() {
        assertFalse(shape(75f).copy(
            failures = listOf(CheckFailure.ShapeCoverage(75f, 80f), CheckFailure.StrokeCount(1, 2)),
        ).withShapeTolerance().acceptedWithPenalty)
        assertEquals(CheckVerdict.Incomplete, shape(75f).copy(verdict = CheckVerdict.Incomplete).withShapeTolerance().verdict)
        assertFalse(CheckReport(CheckVerdict.Correct).withShapeTolerance().acceptedWithPenalty)
        assertFalse(CheckReport(CheckVerdict.Incorrect).withShapeTolerance().acceptedWithPenalty)
    }

    @Test
    fun strictReadabilityAndWritingStillBlockSoftShape() = runBlocking {
        val request = CheckRequest("task", "cell", KanjiId("日"), 0, WritingSample())
        val softShape = object : KanjiChecker {
            override suspend fun check(request: CheckRequest) = CheckVerdict.Incorrect
            override suspend fun checkWithDetails(request: CheckRequest) = shape(75f)
        }
        val correct = KanjiChecker { CheckVerdict.Correct }
        val incorrect = KanjiChecker { CheckVerdict.Incorrect }
        for ((readability, writing, expected) in listOf(
            Triple(correct, correct, CheckVerdict.Correct),
            Triple(incorrect, correct, CheckVerdict.Incorrect),
            Triple(correct, incorrect, CheckVerdict.Incorrect),
        )) {
            val report = CombinedKanjiChecker(readability, { softShape }, writing) { RecognitionSettings() }.checkWithDetails(request)
            assertEquals(expected, report.verdict)
            assertTrue(report.checks.single { it.kind == CheckKind.Shape }.acceptedWithPenalty)
            assertEquals(expected == CheckVerdict.Correct, report.acceptedWithPenalty)
        }
    }

    @Test
    fun shapePenaltyMultipliesHintScoreRatherThanCountingAsAHint() {
        val symbol = TrainingSymbol("s", 0, "0", TrainingSymbolRole.Practice, TrainingSymbolScript.Kanji, "日",
            status = TrainingSymbolStatus.Correct, hintRequestCount = 1, shapeVerdict = StoredCheckVerdict.AcceptedWithPenalty)
        val task = TrainingTask("t", 0, writtenForm = "日", reading = "ひ", translations = listOf(TrainingTaskTranslation("ru", "день")),
            status = TrainingTaskStatus.Assisted, symbols = listOf(symbol))
        assertEquals(0.64, task.wordPoints(3), 1e-9)
        assertEquals(0.8, task.copy(symbols = listOf(symbol.copy(hintRequestCount = 0))).wordPoints(3), 1e-9)
        assertEquals(0.0, task.copy(symbols = listOf(symbol.copy(status = TrainingSymbolStatus.Incorrect))).wordPoints(3), 1e-9)
    }
}
