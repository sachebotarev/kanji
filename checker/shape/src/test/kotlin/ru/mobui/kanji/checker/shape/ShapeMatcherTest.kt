package ru.mobui.kanji.checker.shape

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import ru.mobui.kanji.domain.practice.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
import ru.mobui.kanji.domain.handwriting.*

class ShapeMatcherTest {
    private val cross = listOf(listOf(0.1f to 0.5f, 0.9f to 0.5f), listOf(0.5f to 0.1f, 0.5f to 0.9f))
    private fun reference(lines: List<List<Pair<Float, Float>>>) = ReferenceDrawing(lines.map { s -> s.map { ReferencePoint(it.first, it.second) } })
    private fun sample(lines: List<List<Pair<Float, Float>>>) = WritingSample(lines.map { s -> InkStroke(s.mapIndexed { i, p -> InkPoint(p.first, p.second, i.toLong()) }, InkTool.Finger) })

    @Test fun ignoresScaleTranslationOrderAndDirection() {
        val moved = cross.reversed().map { s -> s.reversed().map { (x, y) -> x * 0.3f + 0.4f to y * 0.3f + 0.2f } }
        assertTrue(ShapeMatcher().compare(sample(moved), reference(cross)).matchPercent > 98f)
    }
    @Test fun penalizesMissingAndExtraLines() {
        assertTrue(ShapeMatcher().compare(sample(cross.take(1)), reference(cross)).matchPercent < 80f)
        assertTrue(ShapeMatcher().compare(sample(cross), reference(cross.take(1))).matchPercent < 80f)
        assertEquals(0f, ShapeMatcher().compare(WritingSample(), reference(cross)).matchPercent)
    }
    @Test fun strokeCountCannotBeOverriddenByThreshold() {
        // Identical ink geometry, but one stroke was split into two.
        val split = listOf(cross[0], listOf(0.5f to 0.1f, 0.5f to 0.5f), listOf(0.5f to 0.5f, 0.5f to 0.9f))
        val config = ShapeCheckConfig(requiredMatchPercent = 1f)
        val result = ShapeMatcher(config).compare(sample(split), reference(cross))
        assertTrue(result.globalMatchPercent > 98f)
        assertEquals(0f, result.strokeMatchPercent, 0f)
        assertFalse(result.strokeCountMatches)
        assertFalse(result.isAccepted(config))
        assertTrue(ShapeMatcher(config).compare(sample(cross), reference(cross)).isAccepted(config))
    }
    @Test fun checkerReportsExactCountAndCoverageFailures() = runBlocking {
        val checker = ShapeKanjiChecker(KanjiReferenceSource { reference(cross) }, Dispatchers.Unconfined)
        val request = CheckRequest("task", "cell", KanjiId("any"), 0, sample(List(3) { cross.first() }))
        val report = checker.checkWithDetails(request)
        assertEquals(CheckVerdict.Incorrect, report.verdict)
        assertTrue(report.failures.contains(CheckFailure.StrokeCount(3, 2)))
        assertTrue(report.failures.any { it is CheckFailure.ShapeCoverage })
        assertEquals(CheckReport(CheckVerdict.Correct), checker.checkWithDetails(request.copy(sample = sample(cross))))
        assertEquals(CheckVerdict.Incomplete, checker.check(request.copy(sample = sample(cross.take(1)))))
    }
    @Test fun tuningMetricsAreOptionalAndIncludeScoresAndConfiguration() = runBlocking {
        val references = KanjiReferenceSource { reference(cross) }
        val request = CheckRequest("task", "cell", KanjiId("any"), 0, sample(cross))
        assertTrue(ShapeKanjiChecker(references, Dispatchers.Unconfined).checkWithDetails(request).metrics.isEmpty())
        val config = ShapeCheckConfig(requiredMatchPercent = 93f)
        val report = ShapeKanjiChecker(references, Dispatchers.Unconfined, config, diagnosticsEnabled = true).checkWithDetails(request)
        assertEquals("93.0", report.metrics["requiredMatchPercent"])
        assertEquals("100.0", report.metrics["coverage.baseline.percent"])
        assertEquals("100.0", report.metrics["coverage.final.percent"])
        assertEquals("2", report.metrics["input.strokes"])
        assertTrue(report.metrics.getValue("check.duration.ms").toDouble() >= 0)
    }
    @Test fun compensatesEachAxisWithinConfiguredLimits() {
        val target = reference(cross)
        val disabled = ShapeCheckConfig(requiredMatchPercent = 99f, distanceTolerance = .01f, maxHorizontalStretchPercent = 0f, maxVerticalStretchPercent = 0f)
        for (axis in 0..1) for (factor in listOf(.88f, 1.12f)) {
            val changed = cross.map { line -> line.map { (x, y) ->
                (.5f + (x - .5f) * if (axis == 0) factor else 1f) to
                    (.5f + (y - .5f) * if (axis == 1) factor else 1f)
            } }
            val enabled = disabled.copy(maxHorizontalStretchPercent = 15f,
                maxVerticalStretchPercent = 15f)
            val before = ShapeMatcher(disabled).compare(sample(changed), target)
            val after = ShapeMatcher(enabled).compare(sample(changed), target)
            assertTrue(after.matchPercent > 98f)
            assertTrue(after.compensation.isApplied)
            assertFalse(after.compensation.stretch.hasError)
            assertTrue(after.compensation.scaleX in (1f - enabled.maxHorizontalStretchPercent / 100f)..(1f + enabled.maxHorizontalStretchPercent / 100f))
            assertTrue(after.compensation.scaleY in (1f - enabled.maxVerticalStretchPercent / 100f)..(1f + enabled.maxVerticalStretchPercent / 100f))
            assertFalse(before.isAccepted(disabled))
            assertTrue(after.isAccepted(enabled))
        }
        val excessive = cross.map { line -> line.map { (x, y) -> x * .2f + .4f to y } }
        assertFalse(ShapeMatcher().compare(sample(excessive), target).isAccepted(ShapeCheckConfig()))
        val wrongCount = sample(cross + cross.take(1))
        assertFalse(ShapeMatcher().compare(wrongCount, target).isAccepted(ShapeCheckConfig(requiredMatchPercent = 1f)))
    }
    @Test fun rotationCompensationIsBoundedAndCanBeDisabled() {
        val strict = ShapeCheckConfig(requiredMatchPercent = 95f, distanceTolerance = .01f,
            maxHorizontalStretchPercent = 0f, maxVerticalStretchPercent = 0f, maxRotationDegrees = 0f)
        fun rotated(degrees: Float): WritingSample {
            val a = degrees * PI.toFloat() / 180f
            return sample(cross.map { line -> line.map { (x, y) ->
                val dx = x - .5f; val dy = y - .5f
                (.5f + dx * cos(a) - dy * sin(a)) to (.5f + dx * sin(a) + dy * cos(a))
            } })
        }
        for (angle in listOf(-7f, 7f)) {
            assertFalse(ShapeMatcher(strict).compare(rotated(angle), reference(cross)).isAccepted(strict))
            val enabled = strict.copy(maxRotationDegrees = 7f)
            val result = ShapeMatcher(enabled).compare(rotated(angle), reference(cross))
            assertTrue(result.isAccepted(enabled))
            assertTrue(result.compensation.rotationDegrees * angle < 0f)
            assertTrue(abs(result.compensation.rotationDegrees) <= enabled.maxRotationDegrees)
            assertTrue(result.matchPercent >= enabled.requiredMatchPercent)
        }
        val enabled = strict.copy(maxRotationDegrees = 7f)
        assertFalse(ShapeMatcher(enabled).compare(rotated(25f), reference(cross)).isAccepted(enabled))
    }
    @Test fun toleranceIsConfigurableAndAspectRatioIsPreserved() {
        val narrow = cross.map { s -> s.map { (x, y) -> x * 0.4f + 0.3f to y } }
        val strict = ShapeMatcher(ShapeCheckConfig(requiredMatchPercent = 95f, distanceTolerance = 0.01f)).compare(sample(narrow), reference(cross))
        val lenient = ShapeMatcher(ShapeCheckConfig(requiredMatchPercent = 95f, distanceTolerance = 0.15f)).compare(sample(narrow), reference(cross))
        assertFalse(strict.isAccepted(ShapeCheckConfig(requiredMatchPercent = 95f, distanceTolerance = 0.01f)))
        assertFalse(lenient.isAccepted(ShapeCheckConfig(requiredMatchPercent = 95f, distanceTolerance = 0.15f)))
    }
}
