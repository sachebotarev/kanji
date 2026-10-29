package ru.mobui.kanji.checker.shape.stroke

import ru.mobui.kanji.checker.strokes.geometry.ShapePoint
import ru.mobui.kanji.checker.strokes.matching.StrokeCorrespondenceMatcher

import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.domain.practice.*

class StrokeRuleCheckerTest {
    private val rules = KanjiRuleSet(2, listOf(KanjiRule(2, StrokeRuleRelation.EndsAt, 1)))
    private val checker = StrokeRuleChecker(StrokeCorrespondenceMatcher())
    private val horizontal = listOf(ShapePoint(.1f, .5f), ShapePoint(.9f, .5f))
    private val crossing = listOf(horizontal, listOf(ShapePoint(.5f, .2f), ShapePoint(.5f, .9f)))
    private val ending = listOf(horizontal, listOf(ShapePoint(.5f, .5f), ShapePoint(.5f, .9f)))

    @Test fun differentiatesCrossingFromEndingUsingOriginalInk() {
        assertTrue(checker.check(crossing, ending, RecognitionSettings(), rules).ruleViolations.isNotEmpty())
        assertTrue(checker.check(ending, crossing, RecognitionSettings(), KanjiRuleSet(2, listOf(KanjiRule(2, StrokeRuleRelation.Crosses, 1)))).ruleViolations.isNotEmpty())
        assertTrue(checker.check(crossing, crossing, RecognitionSettings()).failures.isEmpty())
        val reversed = crossing.reversed().map { it.reversed() }
        assertTrue(checker.check(reversed, crossing, RecognitionSettings()).failures.isEmpty())
        assertTrue(checker.check(crossing, ending, RecognitionSettings(checkStrokeStructure = false), rules).failures.isEmpty())
        val smallExit = listOf(horizontal, listOf(ShapePoint(.5f, .49f), ShapePoint(.5f, .9f)))
        assertTrue(checker.check(smallExit, ending, RecognitionSettings(), rules).ruleViolations.isEmpty())
    }

    @Test fun directionAndOrderAreIndependentAndDefaultOff() {
        val permuted = crossing.reversed()
        val backwards = crossing.map { it.reversed() }
        assertTrue(checker.check(permuted, crossing, RecognitionSettings()).failures.isEmpty())
        assertTrue(checker.check(backwards, crossing, RecognitionSettings()).failures.isEmpty())
        val order = checker.check(permuted, crossing, RecognitionSettings(checkStrokeOrder = true))
        assertEquals(2, order.orderMismatches.size)
        assertTrue(order.reversedStrokes.isEmpty())
        val direction = checker.check(permuted.map { it.reversed() }, crossing, RecognitionSettings(checkStrokeDirection = true))
        assertEquals(setOf(1, 2), direction.reversedStrokes.toSet())
        assertTrue(direction.orderMismatches.isEmpty())
        val orderOnly = checker.check(backwards, crossing, RecognitionSettings(checkStrokeOrder = true))
        assertTrue(orderOnly.failures.isEmpty())
    }
}
