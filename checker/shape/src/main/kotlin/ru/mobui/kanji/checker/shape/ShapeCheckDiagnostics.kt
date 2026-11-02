package ru.mobui.kanji.checker.shape

import ru.mobui.kanji.checker.shape.matching.ShapeMatch
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.practice.CheckRequest
import ru.mobui.kanji.domain.practice.KanjiRuleSet

/** Diagnostic serialization is independent of the acceptance decision. */
internal class ShapeCheckDiagnostics(
    private val request: CheckRequest,
    private val reference: ReferenceDrawing,
    private val config: ShapeCheckConfig,
    private val rules: KanjiRuleSet?,
    private val started: Long,
) {
    fun collect(score: ShapeMatch?): Map<String, String> = buildMap {
        inputMetrics()
        settingsMetrics()
        if (score != null) {
            strokeMetrics(score)
            coverageMetrics(score)
            budgetMetrics(score)
            transformMetrics(score)
            sizeMetrics(score)
        }
        put("check.duration.ms", ((System.nanoTime() - started) / NANOS_PER_MILLISECOND).toString())
    }

    private fun MutableMap<String, String>.inputMetrics() {
        put(
            "algorithm",
            "StrokeMatcher / 49 arc samples; diagnostic grid 128×128",
        )
        put("input.strokes", request.sample.strokeCount.toString())
        put("reference.strokes", reference.strokes.size.toString())
        put(
            "input.points",
            request.sample.strokes.sumOf { it.points.size }.toString(),
        )
        put("reference.points", reference.strokes.sumOf { it.size }.toString())
        val points = request.sample.strokes.flatMap { it.points }
        if (points.isNotEmpty()) {
            put(
                "input.bounds",
                "x=${points.minOf { it.x }}…${points.maxOf { it.x }}, " +
                    "y=${points.minOf { it.y }}…${points.maxOf { it.y }}",
            )
            put(
                "input.width",
                (points.maxOf { it.x } - points.minOf { it.x }).toString(),
            )
            put(
                "input.height",
                (points.maxOf { it.y } - points.minOf { it.y }).toString(),
            )
            val height = points.maxOf { it.y } - points.minOf { it.y }
            put(
                "input.aspect",
                if (height > 0f)
                    ((points.maxOf { it.x } - points.minOf { it.x }) / height).toString()
                else "undefined",
            )
            put(
                "input.strokeDuration.ms",
                request.sample.strokes.sumOf { it.points.last().elapsedMs }.toString(),
            )
            put(
                "input.tools",
                request.sample.strokes.map { it.tool }.distinct().joinToString(),
            )
        }
    }

    private fun MutableMap<String, String>.settingsMetrics() {
        put(
            "enabledCompensations",
            config.effectiveCompensations.joinToString { it.name },
        )
        put("checkStrokeStructure", config.checkStrokeStructure.toString())
        put("checkStrokeOrder", config.checkStrokeOrder.toString())
        put("checkStrokeDirection", config.checkStrokeDirection.toString())
        put(
            "connectionTolerancePercent",
            config.connectionTolerancePercent.toString(),
        )
        put(
            "protrusionTolerancePercent",
            config.protrusionTolerancePercent.toString(),
        )
        put(
            "lengthDifferenceTolerancePercent",
            config.lengthDifferenceTolerancePercent.toString(),
        )
        put(
            "dictionary.rules",
            rules?.rules?.joinToString {
                "${it.firstStroke} ${it.relation} ${it.secondStroke}"
            } ?: "none",
        )
        put(
            "maxCompensationBudgetPercent",
            config.maxCompensationBudgetPercent?.toString() ?: "unrestricted",
        )
        put("jitterTolerancePercent", config.jitterTolerancePercent.toString())
        put(
            "maxSizeCompensationPercent",
            config.maxSizeCompensationPercent?.toString() ?: "unrestricted",
        )
        put("maxTranslationPercent", config.maxTranslationPercent.toString())
        put("requiredMatchPercent", config.requiredMatchPercent.toString())
        put("distanceTolerance", config.distanceTolerance.toString())
        put(
            "maxHorizontalStretchPercent",
            config.maxHorizontalStretchPercent.toString(),
        )
        put(
            "maxVerticalStretchPercent",
            config.maxVerticalStretchPercent.toString(),
        )
        put("maxShearDegrees", config.maxShearDegrees?.toString() ?: "disabled")
        put("maxRotationDegrees", config.maxRotationDegrees.toString())
    }

    private fun MutableMap<String, String>.strokeMetrics(score: ShapeMatch) {
        put(
            "dictionary.violations",
            score.strokeChecks.ruleViolations.size.toString(),
        )
        put(
            "strokeOrder.mismatches",
            score.strokeChecks.orderMismatches.size.toString(),
        )
        put(
            "hooks.required",
            reference.hookRequirements
                .count {
                    it == ru.mobui.kanji.domain.handwriting.HookRequirement.Required
                }
                .toString(),
        )
        put(
            "hooks.referenceAmbiguous",
            score.strokeChecks.ambiguousReferenceHooks.joinToString(),
        )
        put("hooks.missing", score.strokeChecks.missingHooks.joinToString())
        put(
            "strokeDirection.reversed",
            score.strokeChecks.reversedStrokes.joinToString(),
        )
    }

    private fun MutableMap<String, String>.coverageMetrics(score: ShapeMatch) {
        put(
            "coverage.baseline.percent",
            score.baselineMatchPercent.toString(),
        )
        put("coverage.global.percent", score.globalMatchPercent.toString())
        put("coverage.strokes.percent", score.strokeMatchPercent.toString())
        put(
            "coverage.gain.percent",
            (score.matchPercent - score.baselineMatchPercent).toString(),
        )
        put("coverage.final.percent", score.matchPercent.toString())
        put("coverage.input.percent", score.inputCoveragePercent.toString())
        put(
            "coverage.reference.percent",
            score.referenceCoveragePercent.toString(),
        )
        put(
            "coverage.margin.percent",
            (score.matchPercent - config.requiredMatchPercent).toString(),
        )
    }

    private fun MutableMap<String, String>.budgetMetrics(score: ShapeMatch) {
        val budget = score.compensation.budget
        put("budget.shear.percent", budget.shearPercent.toString())
        put("budget.total.percent", budget.totalPercent.toString())
        put(
            "budget.translation.percent",
            budget.translationPercent.toString(),
        )
        put("budget.size.percent", budget.sizePercent.toString())
        put("budget.stretch.percent", budget.stretchPercent.toString())
        put("budget.rotation.percent", budget.rotationPercent.toString())
        put("budget.jitter.percent", budget.jitterPercent.toString())
        put("budget.hasError", budget.hasError.toString())
    }

    private fun MutableMap<String, String>.transformMetrics(score: ShapeMatch) {
        put("shear.degrees", score.compensation.shear.degrees.toString())
        put("shear.factor", score.compensation.shear.factor.toString())
        put("shear.hasError", score.compensation.shear.hasError.toString())
        put(
            "shear.isEstimated",
            score.compensation.shear.isEstimated.toString(),
        )
        put("rotation.pivot", "Centroid")
        put(
            "rotation.hasError",
            score.compensation.rotation.hasError.toString(),
        )
        put(
            "rotation.degrees",
            score.compensation.rotationDegrees.toString(),
        )
        put(
            "jitter.removedPoints",
            score.compensation.jitter.removedPoints.toString(),
        )
        put(
            "jitter.maxDeviation.percent",
            score.compensation.jitter.maxDeviationPercent.toString(),
        )
        put(
            "stretch.x.percent",
            score.compensation.stretch.changeXPercent.toString(),
        )
        put(
            "stretch.y.percent",
            score.compensation.stretch.changeYPercent.toString(),
        )
        put(
            "stretch.hasError",
            score.compensation.stretch.hasError.toString(),
        )
    }

    private fun MutableMap<String, String>.sizeMetrics(score: ShapeMatch) {
        put("size.factor", score.compensation.size.scaleFactor.toString())
        put(
            "size.change.percent",
            score.compensation.size.changePercent.toString(),
        )
        put("size.hasError", score.compensation.size.hasError.toString())
        put("size.isDefined", score.compensation.size.isDefined.toString())
        put(
            "translation.x.percent",
            score.compensation.translation.shiftXPercent.toString(),
        )
        put(
            "translation.y.percent",
            score.compensation.translation.shiftYPercent.toString(),
        )
        put(
            "translation.hasError",
            score.compensation.translation.hasError.toString(),
        )
        put("strokeCountMatches", score.strokeCountMatches.toString())
        put("evaluatedCandidates", score.evaluatedCandidates.toString())
    }
}

private const val NANOS_PER_MILLISECOND = 1_000_000.0
