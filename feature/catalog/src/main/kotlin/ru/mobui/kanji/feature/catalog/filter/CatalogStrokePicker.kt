package ru.mobui.kanji.feature.catalog.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ru.mobui.kanji.core.wordui.FacetPickerScreen
import ru.mobui.kanji.core.wordui.SymbolFacet
import ru.mobui.kanji.core.wordui.SymbolFacetOption
import ru.mobui.kanji.feature.catalog.R

@Composable
internal fun CatalogStrokePicker(content: FilterContent.Strokes, actions: FilterActions) {
    val options = (content.counts.keys + content.selectedIds).sorted().map {
        SymbolFacetOption(it, pluralStringResource(R.plurals.catalog_strokes, it, it))
    }
    FacetPickerScreen(
        SymbolFacet("strokes", stringResource(R.string.catalog_choose_strokes), options, value = { null }),
        content.counts, content.selectedIds,
        onApply = { actions.select(FilterResult.Strokes(it)) }, onBack = actions.back,
        backTag = "catalog-back", searchTag = "catalog-search-toggle",
    )
}
