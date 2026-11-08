package ru.mobui.kanji.domain.practice

/**
 * Deadline for one PRACTICE word from the recipe budget.
 *
 * [secondsPerSymbol] is stored on the recipe/training; the word limit is
 * `seconds × practiceSymbolCount` (writable symbols only). Null budget → no timer.
 */
fun wordTimeLimitMs(secondsPerSymbol: Int?, practiceSymbolCount: Int): Long? {
    if (secondsPerSymbol == null) return null
    require(secondsPerSymbol > 0)
    require(practiceSymbolCount >= 0)
    val n = practiceSymbolCount.coerceAtLeast(1)
    return secondsPerSymbol.toLong() * n * MILLIS_PER_SECOND
}

private const val MILLIS_PER_SECOND = 1_000L
