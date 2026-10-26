package ru.mobui.kanji.core.wordui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.*

@Composable
fun WordInfoPanel(modifier: Modifier = Modifier, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) Text(title, Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
fun WordSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
fun WordNavigationRow(title: String, subtitle: String?, onClick: () -> Unit, modifier: Modifier = Modifier,
    selected: Boolean = false, selectedLabel: String? = null) {
    Surface(onClick, modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { this.selected = selected }
            .heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected && selectedLabel != null) Text(selectedLabel, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun WordRow(word: CatalogWord, language: String, onClick: () -> Unit, modifier: Modifier = Modifier, showOrigin: Boolean = false) {
    Surface(onClick, modifier.fillMaxWidth().testTag("word-${word.id}"),
        color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(24.dp)) {
        val origin = stringResource(if (word.isUserCreated) R.string.word_manual else R.string.word_catalog)
        Row(Modifier.padding(12.dp).semantics(mergeDescendants = true) {
            if (showOrigin) stateDescription = origin
        }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(word.written, Modifier.alignByBaseline(), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp))
                    Text(word.reading, Modifier.alignByBaseline(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(word.translation(language) ?: stringResource(R.string.word_missing), style = MaterialTheme.typography.bodyMedium)
            }
            if (showOrigin) Icon(painterResource(if (word.isUserCreated) R.drawable.word_origin_manual else R.drawable.word_origin_catalog),
                null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WordList(words: List<CatalogWord>, language: String, onClick: (Int) -> Unit, modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(), showOrigin: Boolean = false, bottomPadding: Int = 20) {
    LazyColumn(modifier.testTag("word-list"), state = listState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomPadding.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(words, key = { it.id }) { word -> WordRow(word, language, { onClick(word.id) }, showOrigin = showOrigin) }
    }
}

@Composable
fun WordSearch(query: String, label: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(query, onQuery, modifier.fillMaxWidth().semantics { contentDescription = label },
        placeholder = { Text(label, style = MaterialTheme.typography.bodyMedium) }, singleLine = true,
        leadingIcon = { Text("⌕", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics {}) },
        trailingIcon = { if (query.isNotEmpty()) TextButton({ onQuery("") }) { Text(stringResource(R.string.word_clear)) } },
        shape = RoundedCornerShape(24.dp), colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary))
}

data class WordStudyItem(val sourceId: Int, val kanjiId: Int?, val title: String, val subtitle: String? = null)

/** Mapping is outside the widget; callers supply navigation callbacks, never repositories. */
fun wordStudyItems(snapshot: CatalogSnapshot, wordId: Int, language: String, chapterLabel: (Int) -> String): List<WordStudyItem> = buildList {
    snapshot.links.filter { it.wordId == wordId }.groupBy { it.sourceId }.forEach { (sourceId, links) ->
        val source = snapshot.sourceById[sourceId] ?: return@forEach
        add(WordStudyItem(sourceId, null, source.title(language)))
        links.mapNotNull { it.kanjiId }.distinct().forEach { id ->
            val placement = snapshot.placements.firstOrNull { it.sourceId == sourceId && it.kanjiId == id }
            add(WordStudyItem(sourceId, id, snapshot.kanjiById[id]?.symbol.orEmpty(), placement?.let { chapterLabel(it.chapter) }))
        }
    }
}

@Composable
fun WordDetailsContent(word: CatalogWord, language: String, kanji: List<CatalogKanji>, studyItems: List<WordStudyItem>,
    onKanji: (Int) -> Unit, onStudy: (WordStudyItem) -> Unit, modifier: Modifier = Modifier, showOrigin: Boolean = false) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (showOrigin) Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(if (word.isUserCreated) R.string.word_manual else R.string.word_catalog),
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        WordInfoPanel {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(word.written, Modifier.fillMaxWidth().testTag("word-written"), textAlign = TextAlign.Center,
                    style = if (word.written.codePointCount(0, word.written.length) > 8)
                        MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayMedium)
                Text(stringResource(R.string.word_reading), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(word.reading, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(stringResource(R.string.word_translation), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(word.translation(language) ?: stringResource(R.string.word_missing), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            }
        }
        if (word.parts.isNotEmpty()) WordInfoPanel(title = stringResource(R.string.word_parts)) {
            word.parts.forEach { Text(it.written + " · " + it.reading) }
        }
        if (kanji.isNotEmpty()) WordSection(stringResource(R.string.word_kanji)) {
            kanji.forEach { item -> WordNavigationRow(item.symbol, item.meaning(language), { onKanji(item.id) }) }
        }
        if (studyItems.isNotEmpty()) WordSection(stringResource(R.string.word_links)) {
            studyItems.forEach { item -> WordNavigationRow(item.title, item.subtitle, { onStudy(item) }) }
        }
    }
}

@Preview(locale = "ru")
@Composable
private fun WordPreview() = KanjiTheme(true) {
    WordDetailsContent(CatalogWord(-1, "木漏れ日", "こもれび", mapOf("ru" to "солнечный свет сквозь листву"), isUserCreated = true),
        "ru", emptyList(), emptyList(), {}, {}, showOrigin = true)
}
