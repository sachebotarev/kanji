package ru.mobui.kanji

import ru.mobui.kanji.checker.shape.matching.ShapeMatcher

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.checker.shape.ShapeKanjiChecker
import ru.mobui.kanji.data.kanjivg.BundledKanjiReferenceSource
import ru.mobui.kanji.domain.handwriting.*
import ru.mobui.kanji.domain.practice.*

class TracingCompensationTest {
    @Test fun elevenRandomLinesDoNotReceiveHighStrokeSimilarity() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = BundledKanjiReferenceSource(context.assets, Dispatchers.IO)
        val reference = requireNotNull(source.load(KanjiId("u8133")))
        assertEquals(11, reference.strokes.size)
        val settings = RecognitionSettings(requiredMatchPercent = 95f, distanceTolerance = .035f,
            maxTranslationPercent = 10f, maxSizeCompensationPercent = 30f, maxShearDegrees = 5f,
            maxCompensationBudgetPercent = 15f, jitterTolerancePercent = .5f)
        val matcher = ShapeMatcher(settings)
        val random = kotlin.random.Random(8133)
        var highest = 0f
        for (attempt in 0 until 20) {
            val sample = WritingSample(List(11) { InkStroke(List(2) { i ->
                InkPoint(.1f + random.nextFloat() * .8f, .1f + random.nextFloat() * .8f, i.toLong())
            }, InkTool.Finger) })
            val result = matcher.compare(sample, reference)
            highest = maxOf(highest, result.matchPercent)
            assertFalse(result.isAccepted(settings))
            assertTrue("attempt=$attempt score=${result.matchPercent}", result.matchPercent < 60f)
        }
        android.util.Log.i("TracingCalibration", "20 random sets of 11 lines: maximum=$highest")
    }

    @Test fun closeTracingOfRealTemplatesPasses95PercentWithoutMovingInk() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = BundledKanjiReferenceSource(context.assets, Dispatchers.IO)
        val settings = RecognitionSettings(requiredMatchPercent = 95f, distanceTolerance = .035f,
            maxTranslationPercent = 10f, maxSizeCompensationPercent = 30f,
            maxShearDegrees = 5f, maxCompensationBudgetPercent = 15f, jitterTolerancePercent = .5f)
        val checker = ShapeKanjiChecker(source, Dispatchers.Default, settings, diagnosticsEnabled = true)
        for (code in listOf("8133", "6c34", "6728", "65e5", "5c0f")) for (offset in listOf(.002f, .01f, .018f)) {
            val id = KanjiId("u$code")
            val reference = requireNotNull(source.load(id))
            val sample = WritingSample(reference.strokes.mapIndexed { strokeIndex, stroke ->
                InkStroke(stroke.mapIndexed { pointIndex, point ->
                    // Small coherent slant and per-stroke hand offset, not an exact template copy.
                    val x = point.x + .005f * (point.y - .5f) + (strokeIndex % 3 - 1) * offset
                    InkPoint(x.coerceIn(0f, 1f), (point.y + .003f).coerceIn(0f, 1f), pointIndex.toLong())
                }, InkTool.Finger)
            })
            val report = checker.checkWithDetails(CheckRequest("trace", code, id, 0, sample))
            assertEquals("$code offset=$offset: ${report.failures}", CheckVerdict.Correct, report.verdict)
            assertEquals(0f, report.compensation.budget.totalPercent, 0f)
            assertEquals(sample.strokes.map { s -> s.points.map { it.x to it.y } },
                report.compensatedDrawing!!.strokes.map { s -> s.map { it.x to it.y } })
            assertEquals("0.0", report.metrics["coverage.gain.percent"])
            assertEquals(report.metrics["coverage.baseline.percent"], report.metrics["coverage.final.percent"])
        }
    }
}
