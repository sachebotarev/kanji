package ru.mobui.kanji.domain.practice

import kotlin.math.abs

enum class RotationPivot { Center, TopLeft, TopRight, BottomLeft, BottomRight, Centroid }

data class RotationCompensation(
    val degrees: Float = 0f,
    val pivot: RotationPivot = RotationPivot.Center,
    val limitDegrees: Float = 7f,
) {
    val hasError: Boolean get() = abs(degrees) > limitDegrees + .0001f
    val isApplied: Boolean get() = degrees != 0f
}
