package ru.mobui.kanji.feature.catalog.components

import ru.mobui.kanji.feature.catalog.presentation.SearchState

internal data class SearchUi(
    val state: SearchState,
    val label: Int,
    val numeric: Boolean = false,
    val query: (String) -> Unit,
    val toggle: () -> Unit,
)
