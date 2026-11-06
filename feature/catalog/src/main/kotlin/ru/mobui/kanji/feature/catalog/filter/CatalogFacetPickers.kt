package ru.mobui.kanji.feature.catalog.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.mobui.kanji.core.wordui.FacetPickerScreen
import ru.mobui.kanji.core.wordui.SymbolFacet
import ru.mobui.kanji.core.wordui.SymbolFacetOption
import ru.mobui.kanji.core.wordui.R as WordUiR
import ru.mobui.kanji.domain.catalog.localized
import ru.mobui.kanji.feature.catalog.components.decodeJlpt
import ru.mobui.kanji.feature.catalog.components.savedValue

@Composable
internal fun CatalogRadicalPicker(content: FilterContent.Radical, language: String, actions: FilterActions) {
    val options = content.entries.map {
        SymbolFacetOption(it.id, it.names.localized(language) ?: it.symbol, it.symbol, it.symbol)
    }
    FacetPickerScreen(
        SymbolFacet("radical", stringResource(WordUiR.string.selection_radical), options) { null },
        content.counts, content.selectedIds,
        { actions.select(FilterResult.Radical(it)) }, actions.back,
        backTag = "catalog-back", searchTag = "catalog-search-toggle",
    )
}

@Composable
internal fun CatalogJlptPicker(content: FilterContent.Jlpt, actions: FilterActions) {
    val options = (5 downTo 1).map { SymbolFacetOption(it, "N$it") } +
        SymbolFacetOption(-1, stringResource(WordUiR.string.selection_no_jlpt))
    val facet = SymbolFacet("jlpt", stringResource(WordUiR.string.selection_jlpt), options,
        showZeroCounts = true, helpLabel = stringResource(WordUiR.string.selection_jlpt_help),
        helpText = stringResource(WordUiR.string.selection_jlpt_explanation)) { null }
    FacetPickerScreen(facet, content.counts.mapKeys { it.key.savedValue() },
        content.selectedIds.map { it.savedValue() }.toSet(),
        { ids -> actions.select(FilterResult.Jlpt(ids.map(::decodeJlpt).toSet())) }, actions.back,
        backTag = "catalog-back", searchTag = "catalog-search-toggle",
    )
}
