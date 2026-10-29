package ru.mobui.kanji.checker.strokes.geometry

/** Working coordinates may leave the unit square during compensation and rasterization. */
data class ShapePoint(val x: Float, val y: Float)
