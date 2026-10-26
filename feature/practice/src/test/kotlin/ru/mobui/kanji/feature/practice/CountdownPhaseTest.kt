package ru.mobui.kanji.feature.practice

import org.junit.Assert.*
import org.junit.Test

class CountdownPhaseTest {
    @Test fun thresholdsUseElapsedTimeAtEightyAndNinetyPercent() {
        assertEquals(CountdownPhase.Normal, countdownPhase(79_999, 100_000))
        assertEquals(CountdownPhase.Warning, countdownPhase(80_000, 100_000))
        assertEquals(CountdownPhase.Warning, countdownPhase(89_999, 100_000))
        assertEquals(CountdownPhase.Critical, countdownPhase(90_000, 100_000))
        assertEquals(CountdownPhase.Critical, countdownPhase(120_000, 100_000))
    }
    @Test fun remainingTimeClampsNegativeAndExpiredValues() {
        assertEquals(100_000L, remainingCountdownMs(-1, 100_000))
        assertEquals(20_000L, remainingCountdownMs(80_000, 100_000))
        assertEquals(0L, remainingCountdownMs(120_000, 100_000))
        assertEquals(CountdownPhase.Normal, countdownPhase(-1, 100_000))
    }
}
