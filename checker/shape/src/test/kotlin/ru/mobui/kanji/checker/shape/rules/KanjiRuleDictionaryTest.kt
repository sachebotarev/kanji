package ru.mobui.kanji.checker.shape.rules

import ru.mobui.kanji.checker.shape.ShapeKanjiChecker
import ru.mobui.kanji.checker.strokes.geometry.ShapePoint
import ru.mobui.kanji.checker.strokes.matching.StrokeCorrespondenceMatcher
import ru.mobui.kanji.checker.shape.stroke.StrokeRuleChecker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.mobui.kanji.domain.handwriting.*
import ru.mobui.kanji.domain.practice.*

class KanjiRuleDictionaryTest {
    private val dictionary = KanjiRuleDictionary()
    @Test fun onlyConfirmedKanjiHaveOneReadableRuleEach() {
        for (character in "牛午未末土士") {
            val rules = requireNotNull(dictionary.rulesFor(KanjiId("u" + character.code.toString(16))))
            assertEquals(1, rules.rules.size)
        }
        assertNull(dictionary.rulesFor(KanjiId("u6728")))
        assertEquals(dictionary.rulesFor(KanjiId("u725b")), dictionary.rulesFor(KanjiId("u0725B")))
        assertThrows(IllegalArgumentException::class.java) { KanjiRuleSet(3, listOf(KanjiRule(4, StrokeRuleRelation.Crosses, 2))) }
    }

    @Test fun genericLengthRuleRejectsReversalAndAlmostEqualLengthsDespiteCorrectOrder() {
        val checker = StrokeRuleChecker(StrokeCorrespondenceMatcher())
        fun lines(top: Float, bottom: Float) = listOf(
            listOf(ShapePoint(.5f - top / 2, .3f), ShapePoint(.5f + top / 2, .3f)),
            listOf(ShapePoint(.5f - bottom / 2, .7f), ShapePoint(.5f + bottom / 2, .7f)),
        )
        val reference = lines(.5f, .8f)
        val rules = KanjiRuleSet(2, listOf(KanjiRule(1, StrokeRuleRelation.ShorterThan, 2)))
        val settings = RecognitionSettings()
        assertTrue(checker.check(reference, reference, settings, rules).failures.isEmpty())
        assertTrue(checker.check(reference.reversed().map { it.reversed() }, reference, settings, rules).failures.isEmpty())
        assertEquals(1, checker.check(lines(.8f, .5f), reference, settings, rules).ruleViolations.size)
        assertEquals(1, checker.check(lines(.79f, .8f), reference, settings, rules).ruleViolations.size)
        assertTrue(checker.check(lines(.79f, .8f), reference, settings.copy(lengthDifferenceTolerancePercent = 1f), rules).failures.isEmpty())
    }

    @Test fun staleReferenceBindingIsUnavailableRatherThanBlamingTheUser() = runBlocking {
        val reference = ReferenceDrawing(listOf(listOf(ReferencePoint(.2f, .2f), ReferencePoint(.8f, .8f))))
        val checker = ShapeKanjiChecker(KanjiReferenceSource { reference }, Dispatchers.Unconfined)
        val sample = WritingSample(listOf(InkStroke(listOf(InkPoint(.2f, .2f, 0), InkPoint(.8f, .8f, 1)), InkTool.Finger)))
        val report = checker.checkWithDetails(CheckRequest("t", "c", KanjiId("u725b"), 0, sample))
        assertEquals(CheckVerdict.Unavailable, report.verdict)
        assertEquals(listOf(CheckFailure.RuleConfiguration(4, 1)), report.failures)
    }

    @Test fun curvedStrokeLengthIsMeasuredAlongPathNotChord() {
        val checker = StrokeRuleChecker(StrokeCorrespondenceMatcher())
        // Top stroke: straight line of length 0.5 (chord = 0.5, path = 0.5).
        val topStroke = listOf(ShapePoint(.25f, .3f), ShapePoint(.75f, .3f))
        // Bottom stroke: curved V-shape stroke with chord 0.4 (from x=0.3 to x=0.7), but path length 0.3f + 0.3f = 0.6f.
        val curvedBottomStroke = listOf(ShapePoint(.3f, .7f), ShapePoint(.5f, .9207f), ShapePoint(.7f, .7f))
        val input = listOf(topStroke, curvedBottomStroke)
        val reference = listOf(
            listOf(ShapePoint(.25f, .3f), ShapePoint(.75f, .3f)),
            listOf(ShapePoint(.2f, .7f), ShapePoint(.8f, .7f)),
        )
        val rules = KanjiRuleSet(2, listOf(KanjiRule(1, StrokeRuleRelation.ShorterThan, 2)))
        val settings = RecognitionSettings(lengthDifferenceTolerancePercent = 5f)
        
        // Path length of bottom stroke is ~0.6, so top stroke (0.5) is SHORTER than bottom stroke (0.6).
        // (If chord length was used, bottom chord = 0.4 < top chord = 0.5, causing a false violation).
        val result = checker.check(input, reference, settings, rules)
        assertTrue("Expected no rule violations because curved stroke path length is longer than top stroke", result.ruleViolations.isEmpty())
    }
}
