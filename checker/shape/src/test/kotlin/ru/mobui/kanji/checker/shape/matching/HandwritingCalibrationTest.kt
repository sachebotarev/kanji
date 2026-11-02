package ru.mobui.kanji.checker.shape.matching

import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Test
import ru.mobui.kanji.checker.shape.rules.KanjiRuleDictionary
import ru.mobui.kanji.domain.handwriting.HookRequirement
import ru.mobui.kanji.domain.handwriting.InkPoint
import ru.mobui.kanji.domain.handwriting.InkStroke
import ru.mobui.kanji.domain.handwriting.InkTool
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.ReferencePoint
import ru.mobui.kanji.domain.handwriting.WritingSample
import ru.mobui.kanji.domain.practice.KanjiId
import ru.mobui.kanji.domain.practice.RecognitionSettings

/** Opt-in corpus probe: KANJI_CALIBRATION=1; fixtures and provenance live in doc/calibration. */
class HandwritingCalibrationTest {
    @Test
    fun evaluateCorpus() {
        assumeTrue(System.getenv("KANJI_CALIBRATION") == "1")
        val root = File("../../doc/calibration").canonicalFile
        val references =
            root.resolve("external/references.tsv").readLines().associate { line ->
                val parts = line.split('\t')
                parts[0] to
                    ReferenceDrawing(
                        parts[2].split(';').map { s ->
                            s.split(' ').map {
                                val p = it.split(',')
                                ReferencePoint(p[0].toFloat(), p[1].toFloat())
                            }
                        },
                        hookRequirements = parts[1].split(',').map { HookRequirement.valueOf(it) },
                    )
            }
        val records =
            File(System.getenv("KANJI_INPUTS") ?: root.resolve("external/inputs.tsv").path)
                .readLines()
        val tolerances =
            (System.getenv("KANJI_TOLERANCES")
                    ?: RecognitionSettings.touchInput().distanceTolerance.toString())
                .split(',')
                .map(String::toFloat)
        File(System.getenv("KANJI_RESULTS") ?: root.resolve("external/results.tsv").path)
            .bufferedWriter()
            .use { out ->
                out.appendLine(
                    "id\ttarget\tlabel\tsplit\ttolerance\tscore\traw\tbudget\t" +
                        "compensationErrors\thooks\trules\taccepted\tcompensationFailures\t" +
                        "inputStrokes\treferenceStrokes",
                )
                for (line in records) {
                    val p = line.split('\t')
                    val reference = references.getValue(p[1])
                    val sample =
                        WritingSample(
                            p[4].split(';').map { s ->
                                InkStroke(
                                    s.split(' ').mapIndexed { i, point ->
                                        val xy = point.split(',')
                                        InkPoint(xy[0].toFloat(), xy[1].toFloat(), i.toLong())
                                    },
                                    InkTool.Finger,
                                )
                            },
                        )
                    for (tol in tolerances) {
                        val profile = RecognitionSettings.touchInput()
                        val config =
                            profile.copy(
                                distanceTolerance = tol,
                                requiredMatchPercent =
                                    System.getenv("KANJI_MATCH_PERCENT")?.toFloat()
                                        ?: profile.requiredMatchPercent,
                                maxCompensationBudgetPercent =
                                    System.getenv("KANJI_BUDGET_PERCENT")?.toFloat()
                                        ?: profile.maxCompensationBudgetPercent,
                            )
                        val match =
                            ShapeMatcher(config)
                                .compare(
                                    sample,
                                    reference,
                                    KanjiRuleDictionary().rulesFor(KanjiId("u${p[1]}")),
                                )
                        out.appendLine(
                            listOf(
                                    p[0],
                                    p[1],
                                    p[2],
                                    p[3],
                                    tol,
                                    match.matchPercent,
                                    match.baselineMatchPercent,
                                    match.compensation.budget.totalPercent,
                                    match.compensation.failures.size,
                                    match.strokeChecks.missingHooks.joinToString(","),
                                    match.strokeChecks.ruleViolations.size,
                                    match.isAccepted(config),
                                    match.compensation.failures.joinToString(",") {
                                        it.javaClass.simpleName
                                    },
                                    sample.strokeCount,
                                    reference.strokes.size,
                                )
                                .joinToString("\t"),
                        )
                        out.flush()
                    }
                }
            }
    }
}
