@file:Suppress("MatchingDeclarationName")

package ru.mobui.kanji.core.writingpad

import android.os.SystemClock
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
) = coroutineScope {
    val inset = 8.dp.toPx()
    val touchSlop = viewConfiguration.touchSlop
    val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
    val taps = TwoFingerTaps(this, doubleTapTimeout, publish)

    try {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = true)
            val mouseAllowed = down.type != PointerType.Mouse || currentEvent.buttons.isPrimaryPressed
            val side = minOf(size.width, size.height).toFloat() - 2 * inset
            val bounds = Rect(inset, inset, inset + side, inset + side)
            if (!mouseAllowed || side <= 0 || !bounds.contains(down.position)) {
                return@awaitEachGesture
            }

            val initialPressed = currentEvent.changes.filter { it.pressed }
            if (initialPressed.size >= 2) {
                initialPressed.forEach { it.consume() }
                val isTap = trackTwoFingerTap(bounds, touchSlop)
                if (isTap) {
                    taps.accept()
                }
                return@awaitEachGesture
            }

            down.consume()
            val stroke = PadStroke(version, active.nextId(), down, bounds)
            var longPressTriggered = false
            val longPressJob = launch {
                delay(longPressTimeout)
                longPressTriggered = true
                active.clear()
                publish(PadEvent.GesturePreview(version, stroke.id, null))
                publish(PadEvent.HintRequested)
            }
            try {
                stroke.append(down, active)
                publish(stroke.preview())
                val result =
                    trackPadStroke(
                        stroke = stroke,
                        active = active,
                        bounds = bounds,
                        touchSlop = touchSlop,
                        publish = publish,
                        flushPendingUndo = taps::flush,
                        cancelLongPress = { longPressJob.cancel() },
                        isLongPressTriggered = { longPressTriggered },
                    )
                if (result == StrokeResult.TwoFingerTap) {
                    taps.accept()
                }
            } finally {
                longPressJob.cancel()
                active.clear()
                publish(PadEvent.GesturePreview(version, stroke.id, null))
            }
        }
    } finally {
        taps.flush()
    }
}

private class TwoFingerTaps(
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val timeout: Long,
    private val publish: (PadEvent) -> Unit,
) {
    private var lastTap = 0L
    private var pending: Job? = null

    fun flush() {
        if (pending?.isActive == true) {
            pending?.cancel()
            pending = null
            lastTap = 0L
            publish(PadEvent.UndoRequested)
        }
    }

    fun accept() {
        val now = SystemClock.uptimeMillis()
        pending?.cancel()
        if (lastTap > 0L && now - lastTap <= timeout) {
            pending = null
            lastTap = 0L
            publish(PadEvent.ClearRequested)
        } else {
            lastTap = now
            pending = scope.launch {
                delay(timeout)
                if (lastTap == now) {
                    lastTap = 0L
                    publish(PadEvent.UndoRequested)
                }
            }
        }
    }
}

private enum class StrokeResult {
    Finished,
    Cancelled,
    TwoFingerTap,
    LongPress,
}

private class PadStroke(
    val version: ContentVersion,
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
    bounds: Rect,
    touchSlop: Float,
    publish: (PadEvent) -> Unit,
    flushPendingUndo: () -> Unit,
    cancelLongPress: () -> Unit,
    isLongPressTriggered: () -> Boolean,
): StrokeResult {
    val startSize = size
    val downPos = stroke.down.position
    var movedPastSlop = false

    while (true) {
        val event = awaitPointerEvent()
        val pressedPointers = event.changes.filter { it.pressed }

        if (pressedPointers.size >= 2) {
            cancelLongPress()
            return if (!movedPastSlop && !isLongPressTriggered()) {
                active.clear()
                publish(PadEvent.GesturePreview(stroke.version, stroke.id, null))
                event.changes.forEach { it.consume() }
                val isTap = trackTwoFingerTap(bounds, touchSlop)
                if (isTap) StrokeResult.TwoFingerTap else StrokeResult.Cancelled
            } else {
                StrokeResult.Cancelled
            }
        }

        val change = event.changes.firstOrNull { it.id == stroke.down.id }
        val valid =
            change != null &&
                size == startSize &&
                !change.isConsumed
        if (!valid) {
            cancelLongPress()
            return StrokeResult.Cancelled
        }

        val currentChange = requireNotNull(change)
        currentChange.consume()

        if (isLongPressTriggered()) {
            if (!currentChange.pressed) {
                return StrokeResult.LongPress
            }
            continue
        }

        val dx = currentChange.position.x - downPos.x
        val dy = currentChange.position.y - downPos.y
        if (dx * dx + dy * dy > touchSlop * touchSlop) {
            if (!movedPastSlop) {
                movedPastSlop = true
                cancelLongPress()
                flushPendingUndo()
            }
        }
        stroke.append(currentChange, active)
        if (currentChange.pressed) {
            publish(stroke.preview())
        } else {
            cancelLongPress()
            flushPendingUndo()
            publish(stroke.finished())
            return StrokeResult.Finished
        }
    }
}

private suspend fun AwaitPointerEventScope.trackTwoFingerTap(
    bounds: Rect,
    touchSlop: Float,
    maxDurationMs: Long = 300L,
): Boolean {
    val initialPointers = currentEvent.changes.filter { it.pressed }
    if (initialPointers.size != 2) return false
    val p0 = initialPointers[0]
    val p1 = initialPointers[1]
    if (!bounds.contains(p0.position) || !bounds.contains(p1.position)) return false
    val p0Id = p0.id
    val p1Id = p1.id
    val p0Start = p0.position
    val p1Start = p1.position
    val startTime = SystemClock.uptimeMillis()
    val maxSlopSq = (touchSlop * 2) * (touchSlop * 2)

    while (true) {
        val event = awaitPointerEvent()
        val changes = event.changes
        if (changes.count { it.pressed } > 2) {
            changes.forEach { it.consume() }
            return false
        }
        val currentTime = SystemClock.uptimeMillis()
        if (currentTime - startTime > maxDurationMs) {
            changes.forEach { it.consume() }
            return false
        }
        for (change in changes) {
            val start = when (change.id) {
                p0Id -> p0Start
                p1Id -> p1Start
                else -> null
            }
            if (start != null && (change.position - start).getDistanceSquared() > maxSlopSq) return false
            change.consume()
        }
        if (changes.none { it.pressed }) {
            return true
        }
    }
}
