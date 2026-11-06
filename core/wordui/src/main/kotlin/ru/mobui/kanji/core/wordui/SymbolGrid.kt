package ru.mobui.kanji.core.wordui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState

data class SymbolGridItem(val id: Int, val symbol: String, val caption: String,
    val description: String = symbol + ", " + caption, val ordinal: String? = null, val strokes: String? = null)

@Composable
fun SymbolGrid(entries: List<SymbolGridItem>, selectedIds: Set<Int>, onClick: (Int) -> Unit,
    modifier: Modifier = Modifier, state: LazyGridState = rememberLazyGridState(),
    numbered: Boolean = false, bottomPadding: Dp = 24.dp, header: (@Composable () -> Unit)? = null) {
    LazyVerticalGrid(GridCells.Adaptive(88.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)),
        modifier.testTag("kanji-grid"), state = state,
        userScrollEnabled = state.canScrollForward || state.canScrollBackward,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = if (header == null) 12.dp else 0.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(if (numbered) 12.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (numbered) 12.dp else 10.dp)) {
        if (header != null) item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
        items(entries, key = { it.id }) { item ->
            SymbolTile(item.symbol, item.caption, item.id in selectedIds, { onClick(item.id) },
                Modifier.testTag("kanji-${item.id}").semantics { contentDescription = item.description }, item.ordinal, item.strokes)
        }
    }
}
