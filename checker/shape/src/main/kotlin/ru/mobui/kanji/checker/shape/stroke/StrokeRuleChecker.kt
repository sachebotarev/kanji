package ru.mobui.kanji.checker.shape.stroke

import ru.mobui.kanji.checker.strokes.geometry.ShapePoint
import ru.mobui.kanji.checker.shape.geometry.StrokeRelationClassifier
import ru.mobui.kanji.checker.strokes.matching.StrokeCorrespondenceMatcher
import ru.mobui.kanji.checker.shape.rules.DictionaryRuleEvaluator

import ru.mobui.kanji.domain.practice.*
import ru.mobui.kanji.checker.strokes.writing.StrokeWritingEvaluator

/** Structural and writing checks use original ink, so cleanup cannot erase a distinguishing exit. */
internal class StrokeRuleChecker(
    private val correspondence: StrokeCorrespondenceMatcher,
    private val hooks: HookChecker = HookChecker(),
    private val dictionary: DictionaryRuleEvaluator = DictionaryRuleEvaluator(StrokeRelationClassifier()),
) {
    fun check(input: List<List<ShapePoint>>, reference: List<List<ShapePoint>>, settings: RecognitionSettings, rules: KanjiRuleSet? = null, requiredHooks: Set<Int> = emptySet(), knownAssignment: List<Int>? = null): StrokeCheckResult {
        if (requiredHooks.isEmpty() && !settings.checkStrokeStructure && !settings.checkStrokeOrder && !settings.checkStrokeDirection) return StrokeCheckResult()
        val actual = correspondence.normalize(input)
        val expected = correspondence.normalize(reference)
        val assignment = knownAssignment ?: correspondence.assign(actual, expected) ?: return StrokeCheckResult()
        val violations = if (settings.checkStrokeStructure && rules != null)
            dictionary.check(actual, expected, assignment, rules, settings) else emptyList()
        val writing = StrokeWritingEvaluator().check(actual, expected, assignment,
            checkOrder = settings.checkStrokeOrder, checkDirection = settings.checkStrokeDirection)
        val missingHooks = assignment.mapIndexedNotNull { i, j ->
            if (j in requiredHooks && !hooks.hasHook(actual[i], expected[j])) j + 1 else null
        }
        val ambiguousHooks = requiredHooks.filter { !hooks.hasDistinctHook(expected[it]) }.map { it + 1 }.sorted()
        return writing.copy(ruleViolations = violations, missingHooks = missingHooks.sorted(), ambiguousReferenceHooks = ambiguousHooks)
    }
}
