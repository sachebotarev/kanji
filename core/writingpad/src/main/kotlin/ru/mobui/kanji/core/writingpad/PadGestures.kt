@file:Suppress("MatchingDeclarationName")

package ru.mobui.kanji.core.writingpad

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.domain.handwriting.InkPoint
import ru.mobui.kanji.domain.handwriting.InkStroke
import ru.mobui.kanji.domain.handwriting.InkTool

/** Only the draw phase observes mutable paths and revisions. */
internal class ActivePadGesture {
    var path by mutableStateOf<Path?>(null)
    var dot by mutableStateOf<Offset?>(null)
    private var revision by mutableLongStateOf(0L)
    private var counter = 0L

    fun nextId(): Long = ++counter

    fun update(path: Path, start: Offset) {
        this.path = path
        dot = start
        revision++
    }

    fun clear() {
        path = null
        dot = null
    }

    val renderedPath: Path?
        get() = revision.let { path }
}

internal suspend fun PointerInputScope.capturePadGestures(
    version: ContentVersion,
    active: ActivePadGesture,
    publish: (PadEvent) -> Unit,
) {
    val inset = 8.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = true)
        val mouseAllowed = down.type != PointerType.Mouse || currentEvent.buttons.isPrimaryPressed
        val side = minOf(size.width, size.height).toFloat() - 2 * inset
        val bounds = Rect(inset, inset, inset + side, inset + side)
        if (mouseAllowed && side > 0 && bounds.contains(down.position)) {
            down.consume()
            val stroke = PadStroke(version, active.nextId(), down, bounds)
            try {
                stroke.append(down, active)
                publish(stroke.preview())
                trackPadStroke(stroke, active, publish)
            } finally {
                active.clear()
                publish(PadEvent.GesturePreview(version, stroke.id, null))
            }
        }
    }
}

private class PadStroke(
    private val version: ContentVersion,
    val id: Long,
    val down: PointerInputChange,
    val bounds: Rect,
) {
    private val points = mutableListOf<InkPoint>()
    private val path = Path()
    private val tool =
        when (down.type) {
            PointerType.Touch -> InkTool.Finger
            PointerType.Stylus -> InkTool.Stylus
            PointerType.Mouse -> InkTool.Mouse
            else -> InkTool.Unknown
        }

    fun append(change: PointerInputChange, active: ActivePadGesture) {
        val position = change.position
        points.add(
            InkPoint(
                ((position.x - bounds.left) / bounds.width).coerceIn(0f, 1f),
                ((position.y - bounds.top) / bounds.height).coerceIn(0f, 1f),
                (change.uptimeMillis - down.uptimeMillis).coerceAtLeast(0),
            ),
        )
        if (points.size == 1) path.moveTo(position.x, position.y)
        else path.lineTo(position.x, position.y)
        active.update(path, down.position)
    }

    fun preview() = PadEvent.GesturePreview(version, id, InkStroke(points, tool))

    fun finished() = PadEvent.StrokeFinished(version, id, InkStroke(points, tool))
}

private suspend fun AwaitPointerEventScope.trackPadStroke(
    stroke: PadStroke,
    active: ActivePadGesture,
    publish: (PadEvent) -> Unit,
) {
    val startSize = size
    var tracking = true
    while (tracking) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == stroke.down.id }
        val valid =
            change != null &&
                size == startSize &&
                !change.isConsumed
        if (valid) {
            requireNotNull(change).consume()
            stroke.append(change, active)
            if (change.pressed) publish(stroke.preview()) else publish(stroke.finished())
            tracking = change.pressed
        } else tracking = false
    }
}
