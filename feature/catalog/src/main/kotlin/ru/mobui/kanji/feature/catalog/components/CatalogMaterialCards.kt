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
) {
    val gridState = rememberCatalogGridState()
    LazyVerticalGrid(
        columns =
            GridCells.Adaptive(
                with(androidx.compose.ui.platform.LocalDensity.current) {
                    88.dp * fontScale.coerceAtLeast(1f)
                },
            ),
        modifier = modifier.testTag("kanji-grid"),
        state = gridState,
        userScrollEnabled = gridState.canScrollForward || gridState.canScrollBackward,
        contentPadding =
            PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = if (header == null) 12.dp else 0.dp,
                bottom = 24.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(if (numbers.isEmpty()) 10.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (numbers.isEmpty()) 10.dp else 12.dp),
    ) {
        if (header != null) item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
        items(entries, key = { it.id }) { kanji ->
            KanjiTile(
                kanji,
                language,
                { onClick(kanji.id) },
                selected = kanji.id == selectedId,
                number = numbers[kanji.id],
            )
        }
    }
}

@Composable
internal fun RadicalGrid(
    entries: List<CatalogRadical>,
    language: String,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedId: Int? = null,
    fullNames: Boolean = false,
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
                    radical.name(language) ?: stringResource(R.string.catalog_missing),
                    radical.id.toString(),
                    null,
                ),
                radical.id == selectedId,
                { onClick(radical.id) },
                Modifier.testTag("radical-${radical.id}"),
                fullCaption = fullNames,
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
        SymbolTileContent(kanji.symbol, caption, number, strokeText),
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
) {
    with(content) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    Surface(
        onClick,
        modifier.semantics { this.selected = selected },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box {
            Column(
                Modifier.fillMaxWidth()
                    .padding(
                        start = 8.dp,
                        end = 8.dp,
                        top = if (ordinal == null) 18.dp else 24.dp,
                        bottom = 10.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    symbol,
                    style = CatalogJapaneseStyle.copy(fontSize = 36.sp, lineHeight = 44.sp),
                    color = CatalogJapaneseColor,
                )
                val captionHeight =
                    with(density) { (kotlin.math.ceil(20.sp.toPx()) * 2).toFloat().toDp() }
                Box(
                    Modifier.fillMaxWidth()
                        .then(
                            if (fullCaption) Modifier.heightIn(min = captionHeight)
                            else Modifier.height(captionHeight),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        caption,
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                lineHeightStyle =
                                    androidx.compose.ui.text.style.LineHeightStyle(
                                        androidx.compose.ui.text.style.LineHeightStyle.Alignment
                                            .Center,
                                        androidx.compose.ui.text.style.LineHeightStyle.Trim.None,
                                        androidx.compose.ui.text.style.LineHeightStyle.Mode.Tight,
                                    ),
                            ),
                        textAlign = TextAlign.Center,
                        maxLines = if (fullCaption) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (strokes != null)
                    Text(
                        strokes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
            }
            if (ordinal != null)
                Text(
                    ordinal,
                    Modifier.align(Alignment.TopEnd).padding(top = 7.dp, end = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            if (selected)
                Text(
                    "✓",
                    Modifier.align(Alignment.TopStart).padding(8.dp).clearAndSetSemantics {},
                    color = MaterialTheme.colorScheme.primary,
                )
        }
    }

    }
}


private data class SymbolTileContent(
    val symbol: String,
    val caption: String,
    val ordinal: String?,
    val strokes: String?,
)
