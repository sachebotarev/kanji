package ru.mobui.kanji.checker.shape.rules

import ru.mobui.kanji.checker.strokes.geometry.ShapePoint
import ru.mobui.kanji.checker.shape.geometry.StrokeRelationClassifier

import kotlin.math.*
import ru.mobui.kanji.domain.practice.*

/** Executes the same small vocabulary for every kanji; no kanji-specific branches. */
internal class DictionaryRuleEvaluator(private val relations: StrokeRelationClassifier) {
    fun check(input: List<List<ShapePoint>>, reference: List<List<ShapePoint>>, assignment: List<Int>, rules: KanjiRuleSet, settings: RecognitionSettings): List<KanjiRuleViolation> {
        val inverse = IntArray(assignment.size)
        assignment.forEachIndexed { i, j -> inverse[j] = i }
        return rules.rules.mapNotNull { rule ->
            val first = input[inverse[rule.firstStroke - 1]]
            val second = input[inverse[rule.secondStroke - 1]]
            when (rule.relation) {
                StrokeRuleRelation.Crosses, StrokeRuleRelation.EndsAt -> {
                    val connection = if (rule.relation == StrokeRuleRelation.EndsAt) rule.tolerancePercent ?: settings.connectionTolerancePercent else settings.connectionTolerancePercent
                    val protrusion = if (rule.relation == StrokeRuleRelation.Crosses) rule.tolerancePercent ?: settings.protrusionTolerancePercent else settings.protrusionTolerancePercent
                    val actual = relations.relation(first, second, connection / 100, protrusion / 100)
                    val expected = if (rule.relation == StrokeRuleRelation.Crosses) StrokeRelation.Crossing else StrokeRelation.TouchFirst
                    if (actual != expected) KanjiRuleViolation(rule, actualRelation = actual) else null
                }
                StrokeRuleRelation.ShorterThan -> {
                    fun strokeLength(s: List<ShapePoint>): Float {
                        if (s.size < 2) return 0f
                        var total = 0f
                        for (i in 1 until s.size) {
                            total += hypot(s[i].x - s[i - 1].x, s[i].y - s[i - 1].y)
                        }
                        return total
                    }
                    fun center(s: List<ShapePoint>) = ShapePoint((s.first().x + s.last().x) / 2, (s.first().y + s.last().y) / 2)
                    val shorter = strokeLength(first); val longer = strokeLength(second)
                    val difference = if (longer > 0f) (longer - shorter) / longer * 100 else -100f
                    val expectedA = center(reference[rule.firstStroke - 1]); val expectedB = center(reference[rule.secondStroke - 1])
                    val actualA = center(first); val actualB = center(second)
                    // Reject swapped geometric assignments rather than satisfying a rule by exchanging lines.
                    val samePlacement = (expectedB.x - expectedA.x) * (actualB.x - actualA.x) +
                        (expectedB.y - expectedA.y) * (actualB.y - actualA.y) > 0f
                    val required = rule.tolerancePercent ?: settings.lengthDifferenceTolerancePercent
                    if (!samePlacement || shorter >= longer || difference + .0001f < required) KanjiRuleViolation(rule, difference) else null
                }
            }
        }
    }
}
