package ru.mobui.kanji.feature.dictionary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.mobui.kanji.core.wordui.SymbolGridItem
import ru.mobui.kanji.core.wordui.SymbolPickerHeader
import ru.mobui.kanji.core.wordui.SymbolPickerScreen
import ru.mobui.kanji.core.wordui.kanjiSymbolFacets
import ru.mobui.kanji.domain.catalog.localized
import ru.mobui.kanji.domain.catalog.meaning

@Composable
internal fun DictionaryKanjiPicker(state: DictionaryState, language: String,
    onApply: (Set<Int>) -> Unit, onBack: () -> Unit) {
    val pool = state.words.flatMap { state.snapshot?.literalKanji(it).orEmpty() }.distinctBy { it.id }
    val byId = pool.associateBy { it.id }
    val missing = stringResource(ru.mobui.kanji.core.wordui.R.string.word_missing)
    SymbolPickerScreen(pool.map {
        val meaning = it.meaning(language) ?: missing
        SymbolGridItem(it.id, it.symbol, it.meanings.localized(language)?.firstOrNull() ?: meaning, it.symbol + ", " + meaning)
    }, state.kanjiIds, onApply, header = { search ->
        SymbolPickerHeader(stringResource(ru.mobui.kanji.core.wordui.R.string.word_kanji), onBack, search,
            backTag = "dictionary-back", searchTag = "kanji-search-toggle")
    }, modifier = Modifier.safeDrawingPadding(), facets = kanjiSymbolFacets(pool, state.snapshot?.radicals.orEmpty(), language),
        onBack = onBack, backHandler = { back -> BackHandler(onBack = back) }, facetHeader = { title, back, search ->
            SymbolPickerHeader(title, back, search, backTag = "dictionary-back")
        }, searchText = { item ->
            val kanji = byId.getValue(item.id)
            item.description + " " + kanji.on.joinToString(" ") + " " + kanji.kun.joinToString(" ")
        })
}
