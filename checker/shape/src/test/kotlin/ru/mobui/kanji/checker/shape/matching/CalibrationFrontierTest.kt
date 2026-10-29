package ru.mobui.kanji.checker.shape.matching

import ru.mobui.kanji.checker.shape.compensation.CompensatedShape
import ru.mobui.kanji.checker.shape.compensation.compensationPipeline
import ru.mobui.kanji.checker.shape.coverage.StrokeCoverageCalculator
import ru.mobui.kanji.checker.strokes.geometry.ShapePoint
import ru.mobui.kanji.checker.shape.rules.KanjiRuleDictionary
import ru.mobui.kanji.checker.shape.stroke.StrokeRuleChecker

import org.junit.Assume.assumeTrue
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.HookRequirement
import ru.mobui.kanji.domain.practice.*
import java.io.File
import ru.mobui.kanji.checker.strokes.matching.StrokeCorrespondenceMatcher

/** Exports all nondominated valid candidates, so threshold searches do not repeatedly fit the corpus. */
class CalibrationFrontierTest {
    @Test fun exportOperatingPoints() {
        assumeTrue(System.getenv("KANJI_FRONTIER") == "1")
        val root = File("../../doc/calibration").canonicalFile
        val references = root.resolve("external/references.tsv").readLines().associate { line ->
            val p = line.split('\t')
            p[0] to (p[2].split(';').map { s -> s.split(' ').map {
                val xy = it.split(','); ShapePoint(xy[0].toFloat(), xy[1].toFloat())
            } } to p[1].split(',').map { HookRequirement.valueOf(it) })
        }
        val correspondence = StrokeCorrespondenceMatcher()
        val geometry = StrokeCoverageCalculator()
        val settings = RecognitionSettings.touchInput().copy(maxCompensationBudgetPercent = null)
        val pipeline = compensationPipeline(settings)
        root.resolve("tomoe-frontier.tsv").bufferedWriter().use { out ->
            out.appendLine("id\ttarget\tbudget\tscore\thooks\trules")
            for (line in root.resolve("tomoe-full-inputs.tsv").readLines()) {
                val p = line.split('\t')
                if (p[1].toInt(16) !in 0x3400..0x9fff) continue
                val (reference, requirements) = references.getValue(p[1])
                val raw = p[4].split(';').map { s -> s.split(' ').map {
                    val xy = it.split(','); ShapePoint(xy[0].toFloat(), xy[1].toFloat())
                } }
                if (raw.size != reference.size) continue
                val assignment = correspondence.assign(correspondence.normalize(raw), correspondence.normalize(reference)) ?: continue
                val required = requirements.mapIndexedNotNull { i, h -> i.takeIf { h == HookRequirement.Required } }.toSet()
                val protected = assignment.mapIndexedNotNull { i, j -> i.takeIf { j in required } }.toSet()
                val rules = KanjiRuleDictionary().rulesFor(KanjiId("u${p[1]}"))
                val independent = StrokeRuleChecker(correspondence).check(raw, reference, settings, rules, required, assignment)
                val points = reference.flatten()
                val side = maxOf(points.maxOf { it.x } - points.minOf { it.x }, points.maxOf { it.y } - points.minOf { it.y })
                val candidates = listOf(CompensatedShape(raw, CheckCompensation())) +
                    pipeline.prepare(raw, reference, protected, assignment).candidates
                var best = -1f
                for (candidate in candidates.filter { it.compensation.failures.isEmpty() }.sortedBy { it.compensation.budget.totalPercent }) {
                    val paired = geometry.compare(candidate.strokes, reference, assignment, settings.distanceTolerance * side)
                    val score = minOf(paired.inputPercent, paired.referencePercent)
                    if (score <= best) continue
                    best = score
                    out.appendLine(listOf(p[0], p[1], candidate.compensation.budget.totalPercent, score,
                        independent.missingHooks.joinToString(","), independent.ruleViolations.size).joinToString("\t"))
                }
                out.flush()
            }
        }
    }
}
