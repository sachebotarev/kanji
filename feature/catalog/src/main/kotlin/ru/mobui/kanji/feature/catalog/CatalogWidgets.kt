package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import ru.mobui.kanji.domain.catalog.*

@Composable
internal fun CatalogPanel(modifier: Modifier = Modifier, title: String? = null,
    content: @Composable ColumnScope.() -> Unit) = ru.mobui.kanji.core.wordui.WordInfoPanel(modifier, title, content)

@Composable
internal fun CatalogSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    ru.mobui.kanji.core.wordui.WordSection(title, modifier, content)

@Composable
internal fun CatalogFilterIcon() {
    Icon(painterResource(R.drawable.catalog_filter), null, Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
}

@Composable
internal fun CatalogRow(title: String, subtitle: String?, onClick: () -> Unit, modifier: Modifier = Modifier,
    selected: Boolean = false) = ru.mobui.kanji.core.wordui.WordNavigationRow(title, subtitle, onClick, modifier,
        selected, stringResource(R.string.catalog_selected))

@Composable
internal fun SourceCard(source: CatalogSource, language: String, summary: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag("source-${source.id}"),
        shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .08f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.catalog_book), null, Modifier.size(32.dp))
                }
                Text(source.title(language), Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge)
            }
            Text(summary, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun WordRow(word: CatalogWord, language: String, onClick: () -> Unit) =
    ru.mobui.kanji.core.wordui.WordRow(word, language, onClick)

@Composable
internal fun KanjiGrid(entries: List<CatalogKanji>, language: String, onClick: (Int) -> Unit,
    modifier: Modifier = Modifier, selectedId: Int = 0) {
    LazyVerticalGrid(columns = GridCells.Adaptive(96.dp), modifier = modifier.testTag("kanji-grid"),
        contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(entries, key = { it.id }) { kanji ->
            val fullMeaning = kanji.meaning(language) ?: stringResource(R.string.catalog_missing)
            Surface(onClick = { onClick(kanji.id) }, modifier = Modifier.testTag("kanji-${kanji.id}").semantics { selected = kanji.id == selectedId },
                shape = RoundedCornerShape(24.dp),
                color = if (kanji.id == selectedId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 16.dp).heightIn(min = 104.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (kanji.id == selectedId) Text(stringResource(R.string.catalog_selected), style = MaterialTheme.typography.labelSmall)
                    Text(kanji.symbol, style = MaterialTheme.typography.displayMedium.copy(fontSize = 44.sp))
                    Text(fullMeaning,
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
internal fun RadicalGrid(entries: List<CatalogRadical>, language: String, onClick: (Int) -> Unit,
    modifier: Modifier = Modifier, selectedId: Int = 0) {
    LazyVerticalGrid(GridCells.Adaptive(88.dp), modifier.testTag("radical-grid"), contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(entries, key = { it.id }) { radical ->
            val selected = radical.id == selectedId
            Surface(onClick = { onClick(radical.id) }, modifier = Modifier.testTag("radical-${radical.id}").semantics { this.selected = selected },
                shape = RoundedCornerShape(24.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(10.dp).heightIn(min = 100.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(radical.symbol, style = MaterialTheme.typography.headlineLarge)
                    Text(radical.name(language) ?: stringResource(R.string.catalog_missing), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.catalog_radical_number, radical.id), style = MaterialTheme.typography.labelSmall)
                    if (selected) Text(stringResource(R.string.catalog_selected), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
internal fun CatalogSearch(query: String, label: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier, inset: Boolean = true) =
    ru.mobui.kanji.core.wordui.WordSearch(query, label, onQuery,
        modifier.then(if (inset) Modifier.padding(horizontal = 20.dp, vertical = 8.dp) else Modifier).testTag("catalog-search"))

