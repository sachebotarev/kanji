package ru.mobui.kanji.domain.handwriting

/** Compact JSON for [WritingSample] storage (coordinates 0…1). Not a general JSON library. */
fun WritingSample.toStorageJson(): String {
    if (strokeCount == 0) return "[]"
    return buildString {
        append('[')
        strokes.forEachIndexed { strokeIndex, stroke ->
            if (strokeIndex > 0) append(',')
            append("{\"tool\":\"")
            append(stroke.tool.storageKey())
            append("\",\"points\":[")
            stroke.points.forEachIndexed { pointIndex, point ->
                if (pointIndex > 0) append(',')
                append("{\"x\":")
                append(point.x)
                append(",\"y\":")
                append(point.y)
                append(",\"elapsed_ms\":")
                append(point.elapsedMs)
                append('}')
            }
            append("]}")
        }
        append(']')
    }
}

private fun InkTool.storageKey(): String =
    when (this) {
        InkTool.Finger -> "finger"
        InkTool.Stylus -> "stylus"
        InkTool.Mouse -> "mouse"
        InkTool.Unknown -> "unknown"
    }
