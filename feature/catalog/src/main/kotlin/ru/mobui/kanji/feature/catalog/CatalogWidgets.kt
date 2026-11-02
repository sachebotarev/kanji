package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.text.style.TextOverflow
import ru.mobui.kanji.domain.catalog.*

@Composable
internal fun CatalogPanel(modifier: Modifier = Modifier, title: String? = null,
    spacing: androidx.compose.ui.unit.Dp = 8.dp, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(spacing)) {
            if (title != null) Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
internal fun CatalogSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, Modifier.padding(horizontal = 4.dp).semantics { heading() },
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp))
        content()
    }
}

@Composable
internal fun CatalogHint(text: String, modifier: Modifier = Modifier, inset: Boolean = true) {
    Text(text, modifier.fillMaxWidth().padding(start = if (inset) 20.dp else 0.dp,
        end = if (inset) 20.dp else 0.dp, top = 8.dp),
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun CatalogRow(title: String, subtitle: String?, onClick: () -> Unit, modifier: Modifier = Modifier,
    selected: Boolean = false, selection: Boolean = false, card: Boolean = true) {
    val body: @Composable RowScope.() -> Unit = {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp))
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected || !selection) Text(if (selected) "✓" else "›", Modifier.clearAndSetSemantics {},
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 22.sp)
    }
    if (card) Surface(onClick, modifier.fillMaxWidth().semantics { this.selected = selected }, shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.heightIn(min = 80.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp), content = { body() })
    } else Row(modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).semantics { this.selected = selected }
        .padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp), content = { body() })
}

@Composable
internal fun SourceCard(source: CatalogSource, language: String, summary: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CatalogIllustratedCard(source.title(language), summary,
        if (source.sourceType == "textbook") when (source.coverKey) {
            "sage" -> R.drawable.catalog_tobira; "blue" -> R.drawable.catalog_basic; else -> R.drawable.catalog_book
        } else R.drawable.catalog_book,
        onClick, modifier.testTag("source-${source.id}"), important = true)
}

@Composable
internal fun CatalogIllustratedCard(title: String, subtitle: String, illustration: Int, onClick: () -> Unit,
    modifier: Modifier = Modifier, important: Boolean = false) {
    Surface(onClick, modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = if (important) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.heightIn(min = 80.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { CatalogArtwork(illustration, Modifier.size(if (important) 48.dp else 36.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun WordRow(word: CatalogWord, language: String, onClick: () -> Unit) =
    ru.mobui.kanji.core.wordui.WordEntryCard(word.written, word.reading,
        word.translation(language) ?: stringResource(R.string.catalog_missing), onClick,
        Modifier.testTag("word-${word.id}"), CatalogJapaneseStyle, CatalogJapaneseColor,
        readingStyle = CatalogJapaneseStyle.copy(fontSize = 16.sp, lineHeight = 24.sp))

@Composable
internal fun KanjiGrid(entries: List<CatalogKanji>, language: String, onClick: (Int) -> Unit,
    modifier: Modifier = Modifier, selectedId: Int? = null, numbers: Map<Int, String> = emptyMap(),
    header: (@Composable () -> Unit)? = null) {
    val gridState = rememberCatalogGridState()
    LazyVerticalGrid(columns = GridCells.Adaptive(with(androidx.compose.ui.platform.LocalDensity.current) { 88.dp * fontScale.coerceAtLeast(1f) }), modifier = modifier.testTag("kanji-grid"),
        state = gridState, userScrollEnabled = gridState.canScrollForward || gridState.canScrollBackward,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = if (header == null) 12.dp else 0.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(if (numbers.isEmpty()) 10.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (numbers.isEmpty()) 10.dp else 12.dp)) {
        if (header != null) item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
        items(entries, key = { it.id }) { kanji ->
            KanjiTile(kanji, language, { onClick(kanji.id) }, selected = kanji.id == selectedId,
                number = numbers[kanji.id])
        }
    }
}

@Composable
internal fun RadicalGrid(entries: List<CatalogRadical>, language: String, onClick: (Int) -> Unit,
    modifier: Modifier = Modifier, selectedId: Int? = null, fullNames: Boolean = false, header: (@Composable () -> Unit)? = null) {
    val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
    val gridState = rememberCatalogGridState()
    LazyVerticalGrid(GridCells.Adaptive(88.dp * scale), modifier.testTag("radical-grid"),
        state = gridState, userScrollEnabled = gridState.canScrollForward || gridState.canScrollBackward,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = if (header == null) 12.dp else 0.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (header != null) item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
        items(entries, key = { it.id }) { radical ->
            SymbolTile(radical.symbol, radical.name(language) ?: stringResource(R.string.catalog_missing), radical.id.toString(),
                null, radical.id == selectedId, { onClick(radical.id) }, Modifier.testTag("radical-${radical.id}"), fullCaption = fullNames)
        }
    }
}

@Composable
internal fun CatalogSearch(query: String, label: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier,
    inset: Boolean = true, numeric: Boolean = false) {
    val focus = androidx.compose.runtime.remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    androidx.compose.runtime.LaunchedEffect(Unit) { focus.requestFocus(); keyboard?.show() }
    OutlinedTextField(query, onQuery, modifier.then(if (inset) Modifier.padding(horizontal = 20.dp, vertical = 8.dp) else Modifier)
        .fillMaxWidth().testTag("catalog-search").focusRequester(focus), singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium, placeholder = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(painterResource(R.drawable.catalog_search), null, Modifier.size(24.dp)) },
        trailingIcon = { if (query.isNotEmpty()) TextButton({ onQuery("") }) { Text(stringResource(R.string.catalog_clear_search)) } },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = if (numeric) androidx.compose.ui.text.input.KeyboardType.Number else androidx.compose.ui.text.input.KeyboardType.Text,
            imeAction = androidx.compose.ui.text.input.ImeAction.Search),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { keyboard?.hide() }),
        shape = RoundedCornerShape(24.dp), colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh, focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent, focusedBorderColor = MaterialTheme.colorScheme.outline))
}

@Composable
internal fun KanjiTile(kanji: CatalogKanji, language: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, selected: Boolean = false, number: String? = null) {
    val fullMeaning = kanji.meaning(language) ?: stringResource(R.string.catalog_missing)
    SymbolTile(kanji.symbol, kanji.meanings.localized(language)?.firstOrNull() ?: fullMeaning, number,
        androidx.compose.ui.res.pluralStringResource(R.plurals.catalog_strokes, kanji.strokes, kanji.strokes),
        selected, onClick, modifier.testTag("kanji-${kanji.id}").semantics { contentDescription = kanji.symbol + ", " + fullMeaning })
}

@Composable
private fun SymbolTile(symbol: String, caption: String, ordinal: String?, strokes: String?, selected: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier, fullCaption: Boolean = false) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    Surface(onClick, modifier.semantics { this.selected = selected }, shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Box {
            Column(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = if (ordinal == null) 18.dp else 24.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(symbol, style = CatalogJapaneseStyle.copy(fontSize = 36.sp, lineHeight = 44.sp), color = CatalogJapaneseColor)
                val captionHeight = with(density) { (kotlin.math.ceil(20.sp.toPx()) * 2).toFloat().toDp() }
                Box(Modifier.fillMaxWidth().then(if (fullCaption) Modifier.heightIn(min = captionHeight) else Modifier.height(captionHeight)), contentAlignment = Alignment.Center) {
                    Text(caption, style = MaterialTheme.typography.bodyMedium.copy(lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                            androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.None, androidx.compose.ui.text.style.LineHeightStyle.Mode.Tight)), textAlign = TextAlign.Center,
                        maxLines = if (fullCaption) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
                }
                if (strokes != null) Text(strokes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (ordinal != null) Text(ordinal, Modifier.align(Alignment.TopEnd).padding(top = 7.dp, end = 10.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (selected) Text("✓", Modifier.align(Alignment.TopStart).padding(8.dp).clearAndSetSemantics {}, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
internal fun CatalogFilterChip(label: String, onOpen: () -> Unit, onClear: (() -> Unit)?, modifier: Modifier = Modifier) {
    val active = onClear != null
    Surface(modifier.semantics { selected = active }, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        androidx.compose.ui.layout.Layout(content = {
            TextButton(onOpen, Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
                Text(label, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                if (!active) Icon(painterResource(R.drawable.catalog_chevron_down), null, Modifier.padding(start = 6.dp).size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface)
            }
            if (onClear != null) IconButton(onClear, Modifier.size(48.dp)) {
                Icon(painterResource(R.drawable.catalog_close), stringResource(R.string.catalog_clear_filter, label),
                    Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }) { measurables, constraints ->
            val reset = measurables.getOrNull(1)?.measure(androidx.compose.ui.unit.Constraints.fixed(48.dp.roundToPx(), 48.dp.roundToPx()))
            val labelButton = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0,
                maxWidth = (constraints.maxWidth - (reset?.width ?: 0)).coerceAtLeast(0)))
            val width = constraints.constrainWidth(labelButton.width + (reset?.width ?: 0))
            val height = constraints.constrainHeight(maxOf(labelButton.height, reset?.height ?: 0))
            layout(width, height) {
                labelButton.placeRelative(0, (height - labelButton.height) / 2)
                reset?.placeRelative(labelButton.width, (height - reset.height) / 2)
            }
        }
    }
}

@Composable
internal fun CatalogClearFilterButton(label: String, onClear: () -> Unit, inset: Boolean = true) {
    FilledTonalButton(onClear, Modifier.padding(start = if (inset) 20.dp else 0.dp).heightIn(min = 48.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface)) { Text(label, style = MaterialTheme.typography.bodyMedium) }
}
