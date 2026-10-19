package ru.mobui.kanji.domain.practice

import kotlin.math.abs

data class RotationCompensation(
    val degrees: Float = 0f,
    val limitDegrees: Float = 7f,
) {
    val hasError: Boolean get() = abs(degrees) > limitDegrees + .0001f
    val isApplied: Boolean get() = degrees != 0f
}
