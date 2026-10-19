package ru.mobui.kanji.checker.shape

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import ru.mobui.kanji.domain.practice.*

/** Provider-independent adapter. Reference loading and geometry run away from the UI thread. */
class ShapeKanjiChecker(
    private val references: KanjiReferenceSource,
    private val dispatcher: CoroutineDispatcher,
    private val config: ShapeCheckConfig = ShapeCheckConfig(),
    private val diagnosticsEnabled: Boolean = false,
    private val configProvider: () -> ShapeCheckConfig = { config },
    private val ruleSource: KanjiRulesSource = KanjiRuleDictionary(),
) : KanjiChecker {

    override suspend fun check(request: CheckRequest): CheckVerdict = checkWithDetails(request).verdict
    override suspend fun checkWithDetails(request: CheckRequest): CheckReport = withContext(dispatcher) {
        val config = configProvider()
        val matcher = ShapeMatcher(config)
        val started = System.nanoTime()
        if (request.sample.strokeCount == 0) return@withContext CheckReport(CheckVerdict.Incomplete)
        val reference = references.load(request.kanjiId) ?: return@withContext CheckReport(CheckVerdict.Unavailable)
        val rules = ruleSource.rulesFor(request.kanjiId).takeIf { config.checkStrokeStructure }
        if (rules != null && rules.referenceStrokeCount != reference.strokes.size) return@withContext CheckReport(
            CheckVerdict.Unavailable, listOf(CheckFailure.RuleConfiguration(rules.referenceStrokeCount, reference.strokes.size)),
        )
        fun report(verdict: CheckVerdict, score: ShapeMatch? = null, failures: List<CheckFailure> = emptyList()): CheckReport {
            val metrics = if (!diagnosticsEnabled) emptyMap() else buildMap {
                put("algorithm", "StrokeMatcher / 49 arc samples; diagnostic grid 128×128")
                put("input.strokes", request.sample.strokeCount.toString())
                put("reference.strokes", reference.strokes.size.toString())
                put("input.points", request.sample.strokes.sumOf { it.points.size }.toString())
                put("reference.points", reference.strokes.sumOf { it.size }.toString())
                val points = request.sample.strokes.flatMap { it.points }
                if (points.isNotEmpty()) {
                    put("input.bounds", "x=${points.minOf { it.x }}…${points.maxOf { it.x }}, y=${points.minOf { it.y }}…${points.maxOf { it.y }}")
                    put("input.width", (points.maxOf { it.x } - points.minOf { it.x }).toString())
                    put("input.height", (points.maxOf { it.y } - points.minOf { it.y }).toString())
                    val height = points.maxOf { it.y } - points.minOf { it.y }
                    put("input.aspect", if (height > 0f) ((points.maxOf { it.x } - points.minOf { it.x }) / height).toString() else "undefined")
                    put("input.strokeDuration.ms", request.sample.strokes.sumOf { it.points.last().elapsedMs }.toString())
                    put("input.tools", request.sample.strokes.map { it.tool }.distinct().joinToString())
                }
                put("pipelineType", config.pipelineType.name)
                put("enabledCompensations", config.enabledCompensations.joinToString { it.name })
                put("checkStrokeStructure", config.checkStrokeStructure.toString())
                put("checkStrokeOrder", config.checkStrokeOrder.toString())
                put("checkStrokeDirection", config.checkStrokeDirection.toString())
                put("connectionTolerancePercent", config.connectionTolerancePercent.toString())
                put("protrusionTolerancePercent", config.protrusionTolerancePercent.toString())
                put("lengthDifferenceTolerancePercent", config.lengthDifferenceTolerancePercent.toString())
                put("dictionary.rules", rules?.rules?.joinToString { "${it.firstStroke} ${it.relation} ${it.secondStroke}" } ?: "none")
                put("maxCompensationBudgetPercent", config.maxCompensationBudgetPercent?.toString() ?: "unrestricted")
                put("jitterTolerancePercent", config.jitterTolerancePercent.toString())
                put("maxSizeCompensationPercent", config.maxSizeCompensationPercent?.toString() ?: "unrestricted")
                put("maxTranslationPercent", config.maxTranslationPercent.toString())
                put("requiredMatchPercent", config.requiredMatchPercent.toString())
                put("distanceTolerance", config.distanceTolerance.toString())
                put("maxHorizontalStretchPercent", config.maxHorizontalStretchPercent.toString())
                put("maxVerticalStretchPercent", config.maxVerticalStretchPercent.toString())
                put("maxShearDegrees", config.maxShearDegrees?.toString() ?: "disabled")
                put("maxRotationDegrees", config.maxRotationDegrees.toString())
                put("endpointArtifactTolerance", config.endpointArtifactTolerance.toString())
                if (score != null) {
                    put("dictionary.violations", score.strokeChecks.ruleViolations.size.toString())
                    put("strokeOrder.mismatches", score.strokeChecks.orderMismatches.size.toString())
                    put("hooks.required", reference.hookRequirements.count { it == ru.mobui.kanji.domain.handwriting.HookRequirement.Required }.toString())
                    put("hooks.referenceAmbiguous", score.strokeChecks.ambiguousReferenceHooks.joinToString())
                    put("hooks.missing", score.strokeChecks.missingHooks.joinToString())
                    put("strokeDirection.reversed", score.strokeChecks.reversedStrokes.joinToString())
                    put("coverage.baseline.percent", score.baselineMatchPercent.toString())
                    put("coverage.beforeEndpointCleanup.percent", score.preCleanupMatchPercent.toString())
                    put("coverage.global.percent", score.globalMatchPercent.toString())
                    put("coverage.strokes.percent", score.strokeMatchPercent.toString())
                    put("coverage.gain.percent", (score.matchPercent - score.baselineMatchPercent).toString())
                    put("coverage.final.percent", score.matchPercent.toString())
                    put("coverage.input.percent", score.inputCoveragePercent.toString())
                    put("coverage.reference.percent", score.referenceCoveragePercent.toString())
                    put("coverage.margin.percent", (score.matchPercent - config.requiredMatchPercent).toString())
                    val budget = score.compensation.budget
                    put("budget.shear.percent", budget.shearPercent.toString())
                    put("budget.total.percent", budget.totalPercent.toString())
                    put("budget.translation.percent", budget.translationPercent.toString())
                    put("budget.size.percent", budget.sizePercent.toString())
                    put("budget.stretch.percent", budget.stretchPercent.toString())
                    put("budget.rotation.percent", budget.rotationPercent.toString())
                    put("budget.jitter.percent", budget.jitterPercent.toString())
                    put("budget.endpoints.percent", budget.endpointPercent.toString())
                    put("budget.hasError", budget.hasError.toString())
                    put("shear.degrees", score.compensation.shear.degrees.toString())
                    put("shear.factor", score.compensation.shear.factor.toString())
                    put("shear.hasError", score.compensation.shear.hasError.toString())
                    put("shear.isEstimated", score.compensation.shear.isEstimated.toString())
                    put("rotation.pivot", score.compensation.rotationPivot.name)
                    put("rotation.hasError", score.compensation.rotation.hasError.toString())
                    put("rotation.degrees", score.compensation.rotationDegrees.toString())
                    put("jitter.removedPoints", score.compensation.jitter.removedPoints.toString())
                    put("jitter.maxDeviation.percent", score.compensation.jitter.maxDeviationPercent.toString())
                    put("stretch.x.percent", score.compensation.stretch.changeXPercent.toString())
                    put("stretch.y.percent", score.compensation.stretch.changeYPercent.toString())
                    put("stretch.hasError", score.compensation.stretch.hasError.toString())
                    put("size.factor", score.compensation.size.scaleFactor.toString())
                    put("size.change.percent", score.compensation.size.changePercent.toString())
                    put("size.hasError", score.compensation.size.hasError.toString())
                    put("size.isDefined", score.compensation.size.isDefined.toString())
                    put("translation.x.percent", score.compensation.translation.shiftXPercent.toString())
                    put("translation.y.percent", score.compensation.translation.shiftYPercent.toString())
                    put("translation.hasError", score.compensation.translation.hasError.toString())
                    put("strokeCountMatches", score.strokeCountMatches.toString())
                    put("evaluatedCandidates", score.evaluatedCandidates.toString())
                }
                put("check.duration.ms", ((System.nanoTime() - started) / 1_000_000.0).toString())
            }
            return CheckReport(verdict, failures, score?.compensation ?: CheckCompensation(), metrics, score?.compensatedDrawing?.takeIf { diagnosticsEnabled })
        }
        ensureActive()
        if (request.sample.strokeCount < reference.strokes.size && !diagnosticsEnabled) return@withContext report(CheckVerdict.Incomplete)
        val score = matcher.compare(request.sample, reference, rules)
        ensureActive()
        if (request.sample.strokeCount < reference.strokes.size) return@withContext report(CheckVerdict.Incomplete, score, score.compensation.failures)
        val failures = buildList {
            addAll(score.compensation.failures)
            addAll(score.strokeChecks.failures)
            if (!score.strokeCountMatches) add(CheckFailure.StrokeCount(request.sample.strokeCount, reference.strokes.size))
            if (score.matchPercent < config.requiredMatchPercent) add(CheckFailure.ShapeCoverage(score.matchPercent, config.requiredMatchPercent))
        }
        report(if (score.isAccepted(config)) CheckVerdict.Correct else CheckVerdict.Incorrect, score, failures)
    }
}
