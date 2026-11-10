package ru.mobui.kanji.core.wordui

import ru.mobui.kanji.core.designsystem.inputShape
import ru.mobui.kanji.core.designsystem.kanjiOutlinedTextFieldColors

import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.translation
import java.text.Normalizer
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** A private word-set draft. Search and kanji filter never change the chosen word IDs.
 * The caller supplies the candidate scope and handles Back and committing the returned set.
 */
@Composable
fun WordPickerScreen(
    words: List<CatalogWord>,
    kanjiEntries: List<SymbolGridItem>,
    kanjiByWord: Map<Int, Set<Int>>,
    language: String,
    initialIds: Set<Int>,
    onApply: (Set<Int>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    kanjiFacets: List<SymbolFacet> = emptyList(),
    backHandler: @Composable (() -> Unit) -> Unit = {},
) {
    var ids by rememberSaveable { mutableStateOf(initialIds.toIntArray()) }
    var kanjiIds by rememberSaveable { mutableStateOf(intArrayOf()) }
    var choosingKanji by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var selectedOnly by rememberSaveable { mutableStateOf(false) }
    val eligible = words.map { it.id }.toSet()
    val selected = ids.toSet().intersect(eligible)
    val usedKanji = words.flatMap { kanjiByWord[it.id].orEmpty() }.toSet()
    val pool = kanjiEntries.filter { it.id in usedKanji }
    val filter = kanjiIds.toSet().intersect(pool.map { it.id }.toSet())
    if (choosingKanji) {
        val back = { choosingKanji = false }
        SymbolPickerScreen(pool, filter, { kanjiIds = it.toIntArray(); choosingKanji = false },
            header = { search -> SymbolPickerHeader(stringResource(R.string.word_picker_kanji), back, search) },
            modifier = modifier, facets = kanjiFacets, onBack = back, backHandler = backHandler,
            facetHeader = { title, up, search -> SymbolPickerHeader(title, up, search) })
        return
    }
    val back = {
        if (searchOpen) {
            searchOpen = false
            query = ""
        } else {
            onBack()
        }
    }
    backHandler(back)
    // Normalizing the whole catalogue is independent of toggling a choice or changing the query.
    val searchIndex = remember(words, language) {
        words.associate { word -> word.id to listOfNotNull(word.written, word.reading, word.translation(language)).map(::normalizeWordSearch) }
    }
    val needle = normalizeWordSearch(query.trim())
    val shown = words.filter { word ->
        (!selectedOnly || word.id in selected) &&
            (filter.isEmpty() || kanjiByWord[word.id].orEmpty().any { it in filter }) &&
            searchIndex.getValue(word.id).any { it.contains(needle) }
    }
    Scaffold(modifier, floatingActionButton = {
        SelectionApplyButton(if (selected.isEmpty()) stringResource(R.string.selection_apply)
            else stringResource(R.string.selection_choose, selected.count()), { onApply(selected) }, Modifier.testTag("word-picker-apply"))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SymbolPickerHeader(
                stringResource(R.string.word_picker_title),
                back,
                {
                    searchOpen = !searchOpen
                    if (!searchOpen) query = ""
                },
                searchOpen = searchOpen,
                backTag = "word-picker-back",
                searchTag = "word-picker-search-toggle",
            )
            if (searchOpen) ru.mobui.kanji.core.designsystem.KanjiSearchBar(
                query = query,
                onQuery = { query = it },
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                placeholder = stringResource(R.string.word_picker_search),
                autoFocus = true,
                testTag = "word-picker-search",
            )
            val chosenKanji = pool.filter { it.id in filter }
            val label = stringResource(R.string.word_picker_kanji) + if (chosenKanji.isEmpty()) "" else
                " · " + chosenKanji.take(2).joinToString("、") { it.symbol } +
                    if (chosenKanji.size > 2) " +${chosenKanji.size - 2}" else ""
            SelectionFilterChip(label, { choosingKanji = true },
                if (filter.isEmpty()) null else ({ kanjiIds = intArrayOf() }),
                stringResource(R.string.selection_clear),
                Modifier.padding(horizontal = 38.dp, vertical = 16.dp).testTag("word-picker-kanji"))
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton({ selectedOnly = !selectedOnly }, Modifier.weight(1f).heightIn(min = 48.dp)
                    .wrapContentWidth(Alignment.Start)
                    .testTag(if (selectedOnly) "word-picker-all" else "word-picker-selected")
                    .semantics { this.selected = selectedOnly }) {
                    Text(stringResource(if (selectedOnly) R.string.selection_all else R.string.selection_count,
                        selected.count()))
                }
                TextButton({ ids = intArrayOf() }, Modifier.weight(1f).heightIn(min = 48.dp)
                    .wrapContentWidth(Alignment.End).testTag("word-picker-clear"), enabled = selected.isNotEmpty()) {
                    Text(stringResource(R.string.selection_clear))
                }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 38.dp, end = 38.dp, top = 28.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (shown.isEmpty()) item {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painterResource(ru.mobui.kanji.core.designsystem.R.drawable.mascot_sticker_magnify),
                            null, Modifier.size(96.dp))
                        Text(stringResource(R.string.selection_empty))
                    }
                }
                items(shown, key = { it.id }) { word ->
                    WordRow(word, language, {
                        ids = (if (word.id in selected) selected - word.id else selected + word.id).toIntArray()
                    }, chosen = word.id in selected)
                }
            }
        }
    }
}

private fun normalizeWordSearch(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase(Locale.ROOT).map {
        if (it in '\u30a1'..'\u30f6') (it.code - 0x60).toChar() else it
    }.joinToString("")

@androidx.compose.ui.tooling.preview.Preview(locale = "ru", widthDp = 360, fontScale = 1.6f)
@androidx.compose.ui.tooling.preview.Preview(locale = "en", widthDp = 412, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WordPickerPreview() {
    ru.mobui.kanji.core.designsystem.KanjiTheme {
        WordPickerScreen(listOf(CatalogWord(1, "月がきれいですね。", "つきがきれいですね。", mapOf("ru" to "Какая красивая луна!"))),
            listOf(SymbolGridItem('月'.code, "月", "")), mapOf(1 to setOf('月'.code)), "ru", setOf(1), {}, {})
    }
}
