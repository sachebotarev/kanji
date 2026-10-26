package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged

/** Enter-always behavior using stable nested scroll APIs; content keeps the full released height. */
internal class CatalogHeaderState(private val hidden: MutableFloatState) {
    private val heights = mutableStateMapOf<Int, Int>()
    fun height(slot: Int, height: Int) { heights[slot] = height }
    fun remove(slot: Int) { heights.remove(slot) }
    fun hiddenIn(slot: Int): Float = (hidden.floatValue - heights.filterKeys { it < slot }.values.sum())
        .coerceIn(0f, (heights[slot] ?: 0).toFloat())
    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val previous = hidden.floatValue
            hidden.floatValue = (previous - available.y).coerceIn(0f, heights.values.sum().toFloat())
            return Offset(0f, previous - hidden.floatValue)
        }
    }
}

@Composable
internal fun rememberCatalogHeaderState(): CatalogHeaderState {
    val hidden = rememberSaveable { mutableFloatStateOf(0f) }
    return remember { CatalogHeaderState(hidden) }
}

@Composable
internal fun CatalogHeader(state: CatalogHeaderState, slot: Int, content: @Composable ColumnScope.() -> Unit) {
    DisposableEffect(state, slot) { onDispose { state.remove(slot) } }
    Column(Modifier.clipToBounds().layout { measurable, constraints ->
        val measured = measurable.measure(constraints.copy(minHeight = 0))
        val offset = state.hiddenIn(slot).toInt().coerceIn(0, measured.height)
        layout(measured.width, measured.height - offset) { measured.placeRelative(0, -offset) }
    }.onSizeChanged { state.height(slot, it.height) }, content = content)
}
