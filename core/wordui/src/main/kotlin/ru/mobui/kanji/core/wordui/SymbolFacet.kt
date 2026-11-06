package ru.mobui.kanji.core.wordui

/** Stable identifiers and translated display data are supplied by the feature adapter. */
data class SymbolFacetOption(val id: Int, val label: String, val chipLabel: String = label, val symbol: String? = null)

data class SymbolFacet(
    val key: String,
    val title: String,
    val options: List<SymbolFacetOption>,
    val showZeroCounts: Boolean = false,
    val helpLabel: String? = null,
    val helpText: String? = null,
    val emptyLabel: String = title,
    val value: (SymbolGridItem) -> Int?,
)

/** OR within one facet, AND between facets; an empty set is unrestricted. */
fun matchesSymbolFacets(item: SymbolGridItem, facets: List<SymbolFacet>, selected: Map<String, Set<Int>>, except: String? = null): Boolean =
    facets.all { facet -> facet.key == except || selected[facet.key].isNullOrEmpty() || facet.value(item) in selected.getValue(facet.key) }
