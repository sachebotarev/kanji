package ru.mobui.kanji.feature.practice

internal enum class CountdownPhase { Normal, Warning, Critical }

/** Thresholds use elapsed time, including time spent in the background. */
internal fun countdownPhase(elapsedMs: Long, limitMs: Long): CountdownPhase {
    require(limitMs > 0)
    val fraction = elapsedMs.coerceAtLeast(0).toDouble() / limitMs
    return when {
        fraction >= .9 -> CountdownPhase.Critical
        fraction >= .8 -> CountdownPhase.Warning
        else -> CountdownPhase.Normal
    }
}

internal fun remainingCountdownMs(elapsedMs: Long, limitMs: Long): Long {
    require(limitMs > 0)
    return limitMs - elapsedMs.coerceIn(0, limitMs)
}
