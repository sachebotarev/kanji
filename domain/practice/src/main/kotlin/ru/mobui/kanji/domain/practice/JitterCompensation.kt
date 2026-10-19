package ru.mobui.kanji.domain.practice

/** Bounded polyline simplification; the endpoints of every stroke are preserved. */
data class JitterCompensation(
    val removedPoints: Int = 0,
    val maxDeviationPercent: Float = 0f,
    val limitPercent: Float = 0f,
) {
    val isApplied: Boolean get() = removedPoints > 0
}
