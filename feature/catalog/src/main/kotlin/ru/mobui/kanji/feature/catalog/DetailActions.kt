package ru.mobui.kanji.feature.catalog

internal data class DetailActions(
    val back: () -> Unit,
    val retry: () -> Unit,
    val query: (String) -> Unit,
    val toggleSearch: () -> Unit,
    val navigate: (DetailNavigation) -> Unit,
)
