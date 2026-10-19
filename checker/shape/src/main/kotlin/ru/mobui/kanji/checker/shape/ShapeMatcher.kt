package ru.mobui.kanji.checker.shape

import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.practice.CheckCompensation
import ru.mobui.kanji.domain.handwriting.ComparisonDrawing
import ru.mobui.kanji.domain.handwriting.ComparisonPoint
import ru.mobui.kanji.domain.handwriting.WritingSample

/** Matched stroke coverage; raster union is diagnostic. Stroke count is a mandatory gate. */
class ShapeMatcher internal constructor(
    private val config: ShapeCheckConfig,
    private val geometry: RasterShapeChecker,
    private val compensations: CompensationPipeline,
    private val strokeChecks: StrokeChecks,
    private val strokeGeometry: StrokeShapeChecker = StrokeShapeChecker(),
) {
    constructor(config: ShapeCheckConfig = ShapeCheckConfig()) : this(
        config,
        RasterShapeChecker(config.distanceTolerance),
        compensationPipeline(config),
        StrokeChecks(StrokeCorrespondence()),
    )

    internal constructor(
        config: ShapeCheckConfig,
        pipeline: CompensationPipeline,
    ) : this(
        config,
        RasterShapeChecker(config.distanceTolerance),
        pipeline,
        StrokeChecks(StrokeCorrespondence()),
    )

    fun compare(sample: WritingSample, reference: ReferenceDrawing, rules: ru.mobui.kanji.domain.practice.KanjiRuleSet? = null): ShapeMatch {
        val raw = sample.strokes.map { stroke -> stroke.points.map { ShapePoint(it.x, it.y) } }
        val referenceStrokes = reference.strokes.map { stroke -> stroke.map { ShapePoint(it.x, it.y) } }
        val requiredHooks = reference.hookRequirements.mapIndexedNotNull { index, requirement ->
            index.takeIf { requirement == ru.mobui.kanji.domain.handwriting.HookRequirement.Required }
        }.toSet()
        val assignment = StrokeCorrespondence().let {
            it.assign(it.normalize(raw), it.normalize(referenceStrokes))
        }
        val checkedStrokes = strokeChecks.check(raw, referenceStrokes, config, rules, requiredHooks, assignment)
        // Without correspondence, preserve every stroke: cleanup must not guess the hook location.
        val protectedStrokes = if (requiredHooks.isEmpty()) emptySet() else assignment?.mapIndexedNotNull { i, j ->
            i.takeIf { j in requiredHooks }
        }?.toSet() ?: raw.indices.toSet()
        val countMatches = sample.strokeCount == referenceStrokes.size
        val target = geometry.prepare(referenceStrokes)
        val noCorrection = CheckCompensation(budget = ru.mobui.kanji.domain.practice.CompensationBudget(
            limitPercent = config.maxCompensationBudgetPercent,
        ))
        val unmodified = checkCandidates(listOf(CompensatedShape(raw, noCorrection)), target, countMatches, noCorrection, assignment)
            .copy(strokeChecks = checkedStrokes)
        // Incomplete/extra ink must not be expanded to the full reference bounding box.
        // Geometry already good enough needs no correction, even if an independent stroke check fails.
        if (!countMatches || unmodified.matchPercent >= config.requiredMatchPercent) return unmodified
        val prepared = compensations.prepare(raw, referenceStrokes, protectedStrokes, assignment)
        val candidate = checkCandidates(prepared.candidates, target, countMatches, prepared.compensation, assignment)
        val evaluated = unmodified.evaluatedCandidates + candidate.evaluatedCandidates
        val chosen = if (isPreferred(candidate, unmodified)) candidate else unmodified
        return chosen.copy(
            baselineMatchPercent = unmodified.matchPercent,
            evaluatedCandidates = evaluated,
            strokeChecks = checkedStrokes,
        )
    }

    private fun isPreferred(candidate: ShapeMatch, current: ShapeMatch): Boolean {
        val candidateAccepted = candidate.isAccepted(config)
        val currentAccepted = current.isAccepted(config)
        if (candidateAccepted != currentAccepted) return candidateAccepted
        if (candidateAccepted && candidate.compensation.budget.totalPercent != current.compensation.budget.totalPercent)
            return candidate.compensation.budget.totalPercent < current.compensation.budget.totalPercent
        if (candidate.matchPercent != current.matchPercent) return candidate.matchPercent > current.matchPercent
        return candidate.compensation.budget.totalPercent < current.compensation.budget.totalPercent
    }

    private fun checkCandidates(
        candidates: List<CompensatedShape>,
        target: RasterShapeChecker.Target,
        countMatches: Boolean,
        compensation: CheckCompensation,
        assignment: List<Int>?,
    ): ShapeMatch {
        if (target.isEmpty) return ShapeMatch(0f, 0f, countMatches, compensation)
        var best = ShapeMatch(0f, 0f, countMatches, compensation)
        var bestStrokes: List<List<ShapePoint>>? = null
        var baseline: Float? = null
        var evaluated = 0
        fun finish(): ShapeMatch {
            val global = bestStrokes?.let { geometry.compare(it, target) }
            return best.copy(
                baselineMatchPercent = baseline ?: 0f,
                evaluatedCandidates = evaluated,
                globalInputCoveragePercent = global?.inputPercent ?: 0f,
                globalReferenceCoveragePercent = global?.referencePercent ?: 0f,
                compensatedDrawing = bestStrokes?.let { strokes ->
                    ComparisonDrawing(strokes.map { stroke -> stroke.map { ComparisonPoint(it.x, it.y) } })
                },
            )
        }
        val referencePoints = target.reference.flatten()
        val referenceSide = if (referencePoints.isEmpty()) 0f else maxOf(
            referencePoints.maxOf { it.x } - referencePoints.minOf { it.x },
            referencePoints.maxOf { it.y } - referencePoints.minOf { it.y },
        )
        for (candidate in candidates.sortedBy { it.compensation.budget.totalPercent }) {
            val first = candidate.strokes.firstOrNull()?.firstOrNull() ?: continue
            if (candidate.strokes.all { stroke -> stroke.all { it == first } }) continue
            val paired = strokeGeometry.compare(candidate.strokes, target.reference, assignment, config.distanceTolerance * referenceSide)
            val result = ShapeMatch(
                paired.inputPercent, paired.referencePercent,
                countMatches, candidate.compensation,
                strokeInputCoveragePercent = paired.inputPercent, strokeReferenceCoveragePercent = paired.referencePercent,
            )
            evaluated++
            if (baseline == null) baseline = result.matchPercent
            if (isPreferred(result, best) || bestStrokes == null) {
                best = result
                bestStrokes = candidate.strokes
            }
            if (best.isAccepted(config)) return finish()
        }
        return finish()
    }
}
