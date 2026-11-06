package ru.mobui.kanji.feature.catalog.filter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import ru.mobui.kanji.core.wordui.SymbolGridItem
import ru.mobui.kanji.core.wordui.SymbolPickerHeader
import ru.mobui.kanji.core.wordui.SymbolPickerScreen
import ru.mobui.kanji.core.wordui.kanjiSymbolFacets
import ru.mobui.kanji.domain.catalog.localized
import ru.mobui.kanji.domain.catalog.meaning
import ru.mobui.kanji.feature.catalog.R

@Composable
internal fun CatalogKanjiPicker(content: FilterContent.Kanji, language: String, onApply: (FilterResult) -> Unit, onBack: () -> Unit) {
    val pool = content.rows.entries
    val byId = pool.associateBy { it.id }
    val missing = stringResource(ru.mobui.kanji.core.wordui.R.string.word_missing)
    SymbolPickerScreen(pool.map {
        val meaning = it.meaning(language) ?: missing
        SymbolGridItem(it.id, it.symbol, it.meanings.localized(language)?.firstOrNull() ?: meaning, it.symbol + ", " + meaning,
            ordinal = content.rows.numbers[it.id])
    }, content.selectedIds, { onApply(FilterResult.Kanji(it)) },
        header = { search -> SymbolPickerHeader(stringResource(R.string.catalog_choose_kanji), onBack, search,
            backTag = "catalog-back", searchTag = "catalog-search-toggle") },
        modifier = Modifier.safeDrawingPadding().testTag("catalog-screen"),
        facets = kanjiSymbolFacets(pool, content.radicals, language), onBack = onBack,
        backHandler = { back -> BackHandler(onBack = back) },
        facetHeader = { title, back, search -> SymbolPickerHeader(title, back, search, backTag = "catalog-back", searchTag = "catalog-search-toggle") },
        searchText = { item ->
            val kanji = byId.getValue(item.id)
            item.description + " " + kanji.on.joinToString(" ") + " " + kanji.kun.joinToString(" ")
        })
}
