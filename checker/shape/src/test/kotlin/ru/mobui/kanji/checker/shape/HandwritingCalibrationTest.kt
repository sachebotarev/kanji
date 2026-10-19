package ru.mobui.kanji.checker.shape

import org.junit.Assume.assumeTrue
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.*
import ru.mobui.kanji.domain.practice.*
import java.io.File

/** Opt-in corpus probe: KANJI_CALIBRATION=1; fixtures and provenance live in doc/calibration. */
class HandwritingCalibrationTest {
    @Test fun evaluateCorpus() {
        assumeTrue(System.getenv("KANJI_CALIBRATION") == "1")
        val root = File("../../doc/calibration").canonicalFile
        val references = root.resolve("external/references.tsv").readLines().associate { line ->
            val parts = line.split('\t')
            parts[0] to ReferenceDrawing(parts[2].split(';').map { s -> s.split(' ').map {
                val p = it.split(','); ReferencePoint(p[0].toFloat(), p[1].toFloat())
            } }, hookRequirements = parts[1].split(',').map { HookRequirement.valueOf(it) })
        }
        val records = root.resolve("external/inputs.tsv").readLines()
        val tolerances = (System.getenv("KANJI_TOLERANCES") ?: "0.048").split(',').map(String::toFloat)
        File(System.getenv("KANJI_RESULTS") ?: root.resolve("external/results.tsv").path).bufferedWriter().use { out ->
            out.appendLine("id\ttarget\tlabel\tsplit\ttolerance\tscore\traw\tbudget\tcompensationErrors\thooks\trules\taccepted")
            for (line in records) {
                val p = line.split('\t'); val reference = references.getValue(p[1])
                val sample = WritingSample(p[4].split(';').map { s -> InkStroke(s.split(' ').mapIndexed { i, point ->
                    val xy = point.split(','); InkPoint(xy[0].toFloat(), xy[1].toFloat(), i.toLong())
                }, InkTool.Finger) })
                for (tol in tolerances) {
                    val config = RecognitionSettings.touchInput().copy(distanceTolerance = tol)
                    val match = ShapeMatcher(config).compare(sample, reference, KanjiRuleDictionary().rulesFor(KanjiId("u${p[1]}")))
                    out.appendLine(listOf(p[0], p[1], p[2], p[3], tol, match.matchPercent, match.baselineMatchPercent,
                        match.compensation.budget.totalPercent, match.compensation.failures.size,
                        match.strokeChecks.missingHooks.joinToString(","), match.strokeChecks.ruleViolations.size,
                        match.isAccepted(config)).joinToString("\t"))
                    out.flush()
                }
            }
        }
    }
}
