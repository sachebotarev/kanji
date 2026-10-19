package ru.mobui.kanji.domain.practice

enum class StrokeRelation { Separate, TouchFirst, TouchSecond, Crossing }
data class StrokeOrderMismatch(val actualPosition: Int, val expectedPosition: Int)

/** One-based reference stroke numbers, independent of the user's writing order. */
data class StrokeCheckResult(
    val ruleViolations: List<KanjiRuleViolation> = emptyList(),
    val orderMismatches: List<StrokeOrderMismatch> = emptyList(),
    val reversedStrokes: List<Int> = emptyList(),
    val missingHooks: List<Int> = emptyList(),
    val ambiguousReferenceHooks: List<Int> = emptyList(),
) {
    val failures: List<CheckFailure> get() = buildList {
        if (missingHooks.isNotEmpty()) add(CheckFailure.MissingHooks(missingHooks))
        if (ruleViolations.isNotEmpty()) add(CheckFailure.KanjiRules(ruleViolations))
        if (orderMismatches.isNotEmpty()) add(CheckFailure.StrokeOrder(orderMismatches))
        if (reversedStrokes.isNotEmpty()) add(CheckFailure.StrokeDirection(reversedStrokes))
    }
}
