package ru.mobui.kanji.feature.catalog

internal data class BrowseActions(
    val back: () -> Unit,
    val retry: () -> Unit,
    val query: (String) -> Unit,
    val toggleSearch: () -> Unit,
    val tab: (SourceTab) -> Unit,
    val filter: (FilterResult) -> Unit,
    val navigate: (BrowseNavigation) -> Unit,
)
