package ru.mobui.kanji.feature.catalog

internal data class FilterActions(
    val back: () -> Unit,
    val retry: () -> Unit,
    val query: (String) -> Unit,
    val toggleSearch: () -> Unit,
    val select: (FilterResult) -> Unit,
)
