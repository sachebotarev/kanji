package ru.mobui.kanji.domain.practice

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.WritingSample

class CombinedKanjiCheckerTest {
    private val request = CheckRequest("task", "cell", KanjiId("u65e5"), 1, WritingSample())
    @Test fun disabledProvidersAreNotCalledAndNoChecksCannotPass() = runBlocking {
        val forbidden = KanjiChecker { error("Disabled provider called") }
        val report = CombinedKanjiChecker(forbidden, { forbidden }, forbidden) {
            RecognitionSettings(readabilityEnabled = false, shapeEnabled = false, strokeWritingEnabled = false)
        }.checkWithDetails(request)
        assertEquals(CheckVerdict.Incomplete, report.verdict)
        assertTrue(report.checks.all { it.verdict == null })
    }
    @Test fun providerFailurePreservesOtherResults() = runBlocking {
        val correct = KanjiChecker { CheckVerdict.Correct }
        val report = CombinedKanjiChecker(KanjiChecker { error("Model missing") }, { correct }, correct) {
            RecognitionSettings()
        }.checkWithDetails(request)
        assertEquals(CheckVerdict.Unavailable, report.verdict)
        assertEquals(listOf(CheckVerdict.Unavailable, CheckVerdict.Correct, CheckVerdict.Correct), report.checks.map { it.verdict })
    }
    @Test fun shapePercentageSurvivesWithoutDebugMetrics() = runBlocking {
        val shape = object : KanjiChecker {
            override suspend fun check(request: CheckRequest) = CheckVerdict.Incorrect
            override suspend fun checkWithDetails(request: CheckRequest) =
                CheckReport(CheckVerdict.Incorrect, matchPercent = 68.4f, failures = listOf(CheckFailure.ShapeCoverage(68.4f, 73f)))
        }
        val correct = KanjiChecker { CheckVerdict.Correct }
        val report = CombinedKanjiChecker(correct, { shape }, correct) { RecognitionSettings() }.checkWithDetails(request)
        assertEquals(68.4f, report.checks.first { it.kind == CheckKind.Shape }.matchPercent!!, 0f)
        assertTrue(report.metrics.isEmpty())
        assertEquals(listOf(CheckFailure.ShapeCoverage(68.4f, 73f)), report.checks.first { it.kind == CheckKind.Shape }.failures)
        assertTrue(report.checks.first { it.kind == CheckKind.StrokeWriting }.failures.isEmpty())
    }
    @Test fun everyEnabledCheckMustPass() = runBlocking {
        val correct = KanjiChecker { CheckVerdict.Correct }
        val incorrect = KanjiChecker { CheckVerdict.Incorrect }
        var config = RecognitionSettings()
        val checker = CombinedKanjiChecker(correct, { correct }, incorrect) { config }
        assertEquals(CheckVerdict.Incorrect, checker.check(request))
        config = config.copy(strokeWritingEnabled = false)
        assertEquals(CheckVerdict.Correct, checker.check(request))
    }
}
