package ru.mobui.kanji.feature.dashboard.builder

import android.content.ClipData
import android.graphics.Canvas
import android.graphics.Point
import android.view.View
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Platform requires a shadow; the list card already shows the drag affordance. */
private object RecipeDragShadow : View.DragShadowBuilder() {
    override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
        outShadowSize.set(1, 1)
        outShadowTouchPoint.set(0, 0)
    }

    override fun onDrawShadow(canvas: Canvas) = Unit
}

private const val EDGE_DP = 64
private const val SPEED_DP_PER_SECOND = 640
private const val NANOS_PER_SECOND = 1_000_000_000f
private const val MAX_FRAME_SECONDS = 0.05f

/** Android owns the gesture, shadow and session. This adapter owns only the local order. */
internal class RecipeReorderState(private val list: LazyListState) {
    private data class Payload(val owner: Any, val id: String)
    private val owner = Any()
    var ids: List<String> = emptyList()
    var enabled = false
    var commit: (List<String>) -> Unit = {}
    var bounds = Rect.Zero
    var draggedId by mutableStateOf<String?>(null)
        private set
    var order by mutableStateOf<List<String>>(emptyList())
        private set
    /** Keeps the drop order on screen until Room/`ids` catch up — avoids a snap-back jerk. */
    var heldOrder by mutableStateOf<List<String>?>(null)
        private set
    var pointer by mutableStateOf<Offset?>(null)
        private set
    private var original = emptyList<String>()
    private var lastMovedLayout = emptyList<Pair<Any, Int>>()
    private var dropped = false

    fun accepts(event: DragAndDropEvent): Boolean {
        val data = event.toAndroidDragEvent().localState as? Payload ?: return false
        return enabled && data.owner === owner && data.id in ids
    }

    /** Order to render: live drag, pending commit, or repository order. */
    fun visibleIds(): List<String> {
        if (draggedId != null && order.isNotEmpty()) return order
        val held = heldOrder
        if (held != null && held.toSet() == ids.toSet()) return held
        return ids
    }

    fun syncIds(current: List<String>) {
        ids = current
        val held = heldOrder ?: return
        if (held == current || held.toSet() != current.toSet()) heldOrder = null
    }

    /**
     * Card body: short tap opens, long-press starts platform drag. One detector so open
     * does not swallow the hold that begins reorder.
     */
    fun body(id: String, view: View, onOpen: () -> Unit): Modifier = Modifier.pointerInput(id, enabled) {
        detectTapGestures(
            onLongPress = { _ ->
                if (!enabled || id !in ids) return@detectTapGestures
                view.startDragAndDrop(
                    ClipData.newPlainText("", id),
                    RecipeDragShadow,
                    Payload(owner, id),
                    0,
                )
            },
            onTap = { onOpen() },
        )
    }

    private fun locate(event: DragAndDropEvent) {
        val android = event.toAndroidDragEvent()
        pointer = Offset(android.x, android.y) - bounds.topLeft
        updateOrder()
    }

    fun updateOrder() {
        if (!enabled || original != ids) return
        val id = draggedId ?: return
        val position = pointer ?: return
        val visible = list.layoutInfo.visibleItemsInfo.filter { it.key in original }
        val layout = visible.map { it.key to it.offset }
        if (layout == lastMovedLayout) return
        val nearest = visible.minByOrNull { abs(it.offset + it.size / 2f - position.y) } ?: return
        val from = order.indexOf(id)
        val to = order.indexOf(nearest.key)
        if (from < 0 || to < 0 || from == to) return
        order = order.toMutableList().apply { add(to, removeAt(from)) }
        lastMovedLayout = layout
    }

    fun scrollSpeed(edge: Float, maximum: Float): Float {
        val y = pointer?.y ?: return 0f
        if (!enabled || draggedId == null || original != ids) return 0f
        val end = bounds.height
        if (end <= 0f) return 0f
        val top = ((edge - y) / edge).coerceIn(0f, 1f)
        val bottom = ((y - (end - edge)) / edge).coerceIn(0f, 1f)
        return (bottom - top) * maximum
    }

    val target: DragAndDropTarget = object : DragAndDropTarget {
        override fun onStarted(event: DragAndDropEvent) {
            if (!accepts(event)) return
            original = ids.toList()
            order = original
            draggedId = (event.toAndroidDragEvent().localState as Payload).id
            dropped = false
            lastMovedLayout = emptyList()
        }
        override fun onEntered(event: DragAndDropEvent) { if (accepts(event)) locate(event) }
        override fun onMoved(event: DragAndDropEvent) { if (accepts(event)) locate(event) }
        override fun onExited(event: DragAndDropEvent) { pointer = null }
        override fun onDrop(event: DragAndDropEvent): Boolean {
            if (!accepts(event) || original != ids || dropped) return false
            locate(event)
            dropped = true
            if (order != original) {
                val next = order.toList()
                heldOrder = next
                commit(next)
            }
            return true
        }
        override fun onEnded(event: DragAndDropEvent) {
            draggedId = null
            pointer = null
            order = emptyList()
            original = emptyList()
        }
    }
}

@Composable
internal fun rememberRecipeReorderState(
    list: LazyListState,
    ids: List<String>,
    enabled: Boolean,
    onCommit: (List<String>) -> Unit,
): RecipeReorderState {
    val state = remember(list) { RecipeReorderState(list) }
    state.syncIds(ids)
    state.enabled = enabled
    state.commit = onCommit
    val density = LocalDensity.current
    val edge = with(density) { EDGE_DP.dp.toPx() }
    val maximum = with(density) { SPEED_DP_PER_SECOND.dp.toPx() }
    LaunchedEffect(state.draggedId, enabled) {
        if (!enabled || state.draggedId == null) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (state.draggedId != null) {
            val now = withFrameNanos { it }
            val seconds = ((now - previous) / NANOS_PER_SECOND).coerceAtMost(MAX_FRAME_SECONDS)
            previous = now
            val speed = state.scrollSpeed(edge, maximum)
            if (speed != 0f) list.scrollBy(speed * seconds)
            state.updateOrder()
        }
    }
    return state
}

/** Non-composable: caller supplies [view] from LocalView so this stays a plain Modifier factory. */
internal fun Modifier.recipeLibraryBody(
    reorder: RecipeReorderState,
    id: String,
    enabled: Boolean,
    busy: Boolean,
    view: View,
    onOpen: () -> Unit,
): Modifier = then(
    when {
        busy -> Modifier
        enabled -> reorder.body(id, view, onOpen)
        else -> Modifier.pointerInput(id) { detectTapGestures(onTap = { onOpen() }) }
    },
)
