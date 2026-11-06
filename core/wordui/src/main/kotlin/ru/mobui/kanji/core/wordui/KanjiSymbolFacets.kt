package ru.mobui.kanji.core.wordui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogRadical
import ru.mobui.kanji.domain.catalog.localized

/** Shared facet metadata for catalogue, personal dictionary and material adapters. */
@Composable
fun kanjiSymbolFacets(pool: List<CatalogKanji>, radicals: List<CatalogRadical>, language: String): List<SymbolFacet> {
    val byId = pool.associateBy { it.id }
    val radicalIds = pool.mapNotNull { it.radicalId }.toSet()
    val missing = stringResource(R.string.word_missing)
    return listOf(
        SymbolFacet("strokes", stringResource(R.string.selection_strokes),
            pool.map { it.strokes }.distinct().sorted().map { SymbolFacetOption(it, it.toString()) },
            emptyLabel = stringResource(R.string.selection_all_strokes)) { byId[it.id]?.strokes },
        SymbolFacet("radical", stringResource(R.string.selection_radical), radicals.filter { it.id in radicalIds }.map {
            SymbolFacetOption(it.id, it.names.localized(language) ?: missing, it.symbol, it.symbol)
        }, emptyLabel = stringResource(R.string.selection_all_radicals)) { byId[it.id]?.radicalId },
        SymbolFacet("jlpt", stringResource(R.string.selection_jlpt),
            (5 downTo 1).map { SymbolFacetOption(it, "N$it") } + SymbolFacetOption(-1, stringResource(R.string.selection_no_jlpt)),
            showZeroCounts = true, helpLabel = stringResource(R.string.selection_jlpt_help), helpText = stringResource(R.string.selection_jlpt_explanation)) {
            byId[it.id]?.jlptLevel?.number ?: -1
        },
    )
}
