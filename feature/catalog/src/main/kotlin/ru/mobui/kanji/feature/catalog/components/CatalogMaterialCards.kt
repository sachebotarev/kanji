package ru.mobui.kanji.feature.catalog.components

import ru.mobui.kanji.feature.catalog.R
import ru.mobui.kanji.feature.catalog.browse.rememberCatalogGridState
import ru.mobui.kanji.feature.catalog.theme.CatalogJapaneseColor
import ru.mobui.kanji.feature.catalog.theme.CatalogJapaneseStyle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.imeAction
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.shape
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogRadical
import ru.mobui.kanji.domain.catalog.CatalogSource
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.localized
import ru.mobui.kanji.domain.catalog.meaning
import ru.mobui.kanji.domain.catalog.name
import ru.mobui.kanji.domain.catalog.title
import ru.mobui.kanji.domain.catalog.translation

@Composable
internal fun KanjiGrid(
    entries: List<CatalogKanji>,
    language: String,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedId: Int? = null,
    numbers: Map<Int, String> = emptyMap(),
    header: (@Composable () -> Unit)? = null,
    selectionMode: Boolean = false,
) {
    val tiles = entries.map { kanji ->
        val meaning = kanji.meaning(language) ?: stringResource(R.string.catalog_missing)
        ru.mobui.kanji.core.wordui.SymbolGridItem(kanji.id, kanji.symbol,
            kanji.meanings.localized(language)?.firstOrNull() ?: meaning, kanji.symbol + ", " + meaning,
            if (selectionMode) null else numbers[kanji.id],
            if (selectionMode) null else androidx.compose.ui.res.pluralStringResource(R.plurals.catalog_strokes, kanji.strokes, kanji.strokes))
    }
    ru.mobui.kanji.core.wordui.SymbolGrid(tiles, setOfNotNull(selectedId), onClick, modifier,
        state = rememberCatalogGridState(), numbered = numbers.isNotEmpty(), header = header)
}

@Composable
internal fun RadicalGrid(
    entries: List<CatalogRadical>,
    language: String,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedId: Int? = null,
    fullNames: Boolean = true,
    header: (@Composable () -> Unit)? = null,
) {
    val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
    val gridState = rememberCatalogGridState()
    LazyVerticalGrid(
        GridCells.Adaptive(88.dp * scale),
        modifier.testTag("radical-grid"),
        state = gridState,
        userScrollEnabled = gridState.canScrollForward || gridState.canScrollBackward,
        contentPadding =
            PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = if (header == null) 12.dp else 0.dp,
                bottom = 24.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (header != null) item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
        items(entries, key = { it.id }) { radical ->
            SymbolTile(
                SymbolTileContent(
                    radical.symbol,
                    radicalDisplayName(radical, language),
                    radical.id.toString(),
                    null,
                ),
                radical.id == selectedId,
                { onClick(radical.id) },
                Modifier.testTag("radical-${radical.id}"),
                fullCaption = fullNames,
                containerColor = radicalContainerColor(radical.strokes),
            )
        }
    }
}

@Composable
internal fun KanjiTile(
    kanji: CatalogKanji,
    language: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    number: String? = null,
    showStrokes: Boolean = true,
) {
    val fullMeaning = kanji.meaning(language) ?: stringResource(R.string.catalog_missing)
    val strokeText =
        androidx.compose.ui.res.pluralStringResource(
            R.plurals.catalog_strokes,
            kanji.strokes,
            kanji.strokes,
        )
    val caption = kanji.meanings.localized(language)?.firstOrNull() ?: fullMeaning
    SymbolTile(
        SymbolTileContent(kanji.symbol, caption, number, if (showStrokes) strokeText else null),
        selected,
        onClick,
        modifier.testTag("kanji-${kanji.id}").semantics {
            contentDescription = kanji.symbol + ", " + fullMeaning
        },
    )
}

@Composable
private fun SymbolTile(
    content: SymbolTileContent,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fullCaption: Boolean = false,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    ru.mobui.kanji.core.wordui.SymbolTile(content.symbol, content.caption, selected, onClick, modifier,
        content.ordinal, content.strokes, fullCaption, containerColor)
}


private data class SymbolTileContent(
    val symbol: String,
    val caption: String,
    val ordinal: String?,
    val strokes: String?,
)
