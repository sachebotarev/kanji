package ru.mobui.kanji.checker.readability

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.mobui.kanji.domain.practice.CheckVerdict

class DigitalInkKanjiCheckerTest {
    @Test
    fun firstCandidateMatchesDirectly() {
        val (verdict, text) = evaluateCandidates(listOf("二", "ニ"), "二")
        assertEquals(CheckVerdict.Correct, verdict)
        assertEquals("二", text)
    }

    @Test
    fun secondCandidateMatchesWhenKatakanaIsFirst() {
        val (verdict, text) = evaluateCandidates(listOf("ニ", "二", "＝"), "二")
        assertEquals(CheckVerdict.Correct, verdict)
        assertEquals("二", text)
    }

    @Test
    fun thirdCandidateMatchesWhenHomoglyphsAreFirst() {
        val (verdict, text) = evaluateCandidates(listOf("ロ", "〇", "口"), "口")
        assertEquals(CheckVerdict.Correct, verdict)
        assertEquals("口", text)
    }

    @Test
    fun fourthCandidateBeyondTopThreeDoesNotMatch() {
        val (verdict, text) = evaluateCandidates(listOf("一", "二", "三", "口"), "口")
        assertEquals(CheckVerdict.Incorrect, verdict)
        assertEquals("一", text)
    }

    @Test
    fun emptyCandidatesReturnsIncorrectWithEmptyString() {
        val (verdict, text) = evaluateCandidates(emptyList(), "二")
        assertEquals(CheckVerdict.Incorrect, verdict)
        assertEquals("", text)
    }

    @Test
    fun unrelatedCandidatesReturnIncorrectWithTopCandidateText() {
        val (verdict, text) = evaluateCandidates(listOf("木", "本", "休"), "水")
        assertEquals(CheckVerdict.Incorrect, verdict)
        assertEquals("木", text)
    }
}
