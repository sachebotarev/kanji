package ru.mobui.kanji.feature.catalog.browse

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf

/**
 * Shared with the catalog scaffold so a list that already fits does not collapse the filter strip.
 */
internal val LocalCatalogScrollable = compositionLocalOf<MutableState<Boolean>?> { null }

@Composable
internal fun ReportCatalogScroll(scrollable: Boolean) {
    val slot = LocalCatalogScrollable.current ?: return
    SideEffect { if (slot.value != scrollable) slot.value = scrollable }
}

@Composable
internal fun rememberCatalogListState(): LazyListState {
    val state = rememberLazyListState()
    ReportCatalogScroll(state.canScrollForward || state.canScrollBackward)
    return state
}

@Composable
internal fun rememberCatalogGridState(): LazyGridState {
    val state = rememberLazyGridState()
    ReportCatalogScroll(state.canScrollForward || state.canScrollBackward)
    return state
}

@Composable
internal fun rememberCatalogColumnState(): ScrollState {
    val state = rememberScrollState()
    ReportCatalogScroll(state.maxValue > 0)
    return state
}
