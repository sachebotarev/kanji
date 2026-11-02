package ru.mobui.kanji.feature.catalog

internal data class SearchUi(
    val state: SearchState,
    val label: Int,
    val numeric: Boolean = false,
    val query: (String) -> Unit,
    val toggle: () -> Unit,
)
