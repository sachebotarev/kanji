package ru.mobui.kanji.checker.shape

import ru.mobui.kanji.domain.practice.*
import kotlin.math.*

/** Structural and writing checks use original ink, so cleanup cannot erase a distinguishing exit. */
internal class StrokeChecks(
    private val correspondence: StrokeCorrespondence,
    private val hooks: HookChecker = HookChecker(),
    private val dictionary: DictionaryRuleEvaluator = DictionaryRuleEvaluator(StrokeRelations()),
) {
    fun check(input: List<List<ShapePoint>>, reference: List<List<ShapePoint>>, settings: RecognitionSettings, rules: KanjiRuleSet? = null, requiredHooks: Set<Int> = emptySet(), knownAssignment: List<Int>? = null): StrokeCheckResult {
        if (requiredHooks.isEmpty() && !settings.checkStrokeStructure && !settings.checkStrokeOrder && !settings.checkStrokeDirection) return StrokeCheckResult()
        val actual = correspondence.normalize(input)
        val expected = correspondence.normalize(reference)
        val assignment = knownAssignment ?: correspondence.assign(actual, expected) ?: return StrokeCheckResult()
        val violations = if (settings.checkStrokeStructure && rules != null)
            dictionary.check(actual, expected, assignment, rules, settings) else emptyList()
        val order = if (!settings.checkStrokeOrder) emptyList() else assignment.mapIndexedNotNull { i, j ->
            if (i != j) StrokeOrderMismatch(i + 1, j + 1) else null
        }
        val reversed = if (!settings.checkStrokeDirection) emptyList() else assignment.mapIndexedNotNull { i, j ->
            val fractions = listOf(0f, .25f, .5f, .75f, 1f)
            val forward = fractions.sumOf { distanceSquared(at(actual[i], it), at(expected[j], it)).toDouble() }
            val backward = fractions.sumOf { distanceSquared(at(actual[i], it), at(expected[j], 1 - it)).toDouble() }
            if (backward + .000001 < forward) j + 1 else null
        }
        val missingHooks = assignment.mapIndexedNotNull { i, j ->
            if (j in requiredHooks && !hooks.hasHook(actual[i], expected[j])) j + 1 else null
        }
        val ambiguousHooks = requiredHooks.filter { !hooks.hasDistinctHook(expected[it]) }.map { it + 1 }.sorted()
        return StrokeCheckResult(violations, order, reversed, missingHooks.sorted(), ambiguousHooks)
    }

    private fun length(stroke: List<ShapePoint>): Float = stroke.zipWithNext().sumOf { (a, b) -> hypot(b.x - a.x, b.y - a.y).toDouble() }.toFloat()
    private fun distanceSquared(a: ShapePoint, b: ShapePoint): Float = (a.x - b.x).pow(2) + (a.y - b.y).pow(2)
    private fun at(stroke: List<ShapePoint>, fraction: Float): ShapePoint {
        val target = length(stroke) * fraction
        var walked = 0f
        for ((a, b) in stroke.zipWithNext()) {
            val segment = hypot(b.x - a.x, b.y - a.y)
            if (segment > 0f && walked + segment >= target) {
                val t = (target - walked) / segment
                return ShapePoint(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
            }
            walked += segment
        }
        return stroke.last()
    }
}
