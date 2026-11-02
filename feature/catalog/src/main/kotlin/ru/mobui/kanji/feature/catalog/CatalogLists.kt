package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.mobui.kanji.domain.catalog.CatalogChapter
import ru.mobui.kanji.domain.catalog.CatalogWord

@Composable
internal fun ChapterList(
    chapters: List<CatalogChapter>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selected: Int? = null,
    showWordCount: Boolean = true,
    header: (@Composable () -> Unit)? = null,
) {
    val listState = rememberCatalogListState()
    LazyColumn(
        modifier.testTag("chapter-list"),
        state = listState,
        userScrollEnabled = listState.canScrollForward || listState.canScrollBackward,
        contentPadding =
            PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = if (header == null) 20.dp else 0.dp,
                bottom = 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (header != null)
            item(key = "header") { Column(Modifier.padding(top = 12.dp)) { header() } }
        if (chapters.isEmpty()) item { Text(stringResource(R.string.catalog_empty)) }
        items(chapters, key = { it.number }) { chapter ->
            Surface(
                onClick = { onClick(chapter.number) },
                Modifier.fillMaxWidth().testTag("chapter-${chapter.number}").semantics {
                    this.selected = chapter.number == selected
                },
                shape = RoundedCornerShape(24.dp),
                color =
                    if (chapter.number == selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.catalog_chapter, chapter.number),
                            Modifier.weight(1f),
                            style =
                                MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 24.sp,
                                ),
                        )
                        if (chapter.number == selected)
                            Text(
                                "✓",
                                Modifier.clearAndSetSemantics {},
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 22.sp,
                            )
                    }
                    Text(
                        if (showWordCount)
                            catalogMaterialSummary(chapter.kanjiCount, chapter.wordCount)
                        else
                            pluralStringResource(
                                R.plurals.catalog_kanji_count,
                                chapter.kanjiCount,
                                chapter.kanjiCount,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(0.dp))
                    ChapterKanjiPreview(chapter.preview)
                }
            }
        }
    }
}

@Composable
private fun ChapterKanjiPreview(preview: String) {
    val symbols = preview.split(" ").filter { it.isNotEmpty() }
    val density = LocalDensity.current
    val symbolWidth = with(density) { 26.sp.toDp() }
    val rowHeight = with(density) { 36.sp.toDp() }
    BoxWithConstraints(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
        // Extra rows are allowed for narrow windows or accessibility font scaling.
        val capacity = (maxWidth / (symbolWidth + 8.dp)).toInt().coerceAtLeast(1)
        val rowCount = maxOf(1, (symbols.size + capacity - 1) / capacity)
        val perRow = symbols.size / rowCount
        val extra = symbols.size % rowCount
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            var start = 0
            repeat(rowCount) { row ->
                val count = perRow + if (row < extra) 1 else 0
                Row(
                    Modifier.fillMaxWidth().heightIn(min = rowHeight),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    symbols.subList(start, start + count).forEach { symbol ->
                        Text(
                            symbol,
                            style =
                                CatalogJapaneseStyle.copy(
                                    fontSize = 26.sp,
                                    lineHeight = 36.sp,
                                ),
                            color = CatalogJapaneseColor,
                        )
                    }
                }
                start += count
            }
        }
    }
}

@Composable
internal fun WordList(
    words: List<CatalogWord>,
    language: String,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    val listState = rememberCatalogListState()
    LazyColumn(
        modifier.testTag("word-list"),
        state = listState,
        userScrollEnabled = listState.canScrollForward || listState.canScrollBackward,
        contentPadding =
            PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = if (header == null) 12.dp else 0.dp,
                bottom = 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (header != null) item(key = "header") { header() }
        items(words, key = { it.id }) { word -> WordRow(word, language, { onClick(word.id) }) }
    }
}

@Composable
internal fun DetailColumn(content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberCatalogColumnState()
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(scroll, enabled = scroll.maxValue > 0)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content,
    )
}
