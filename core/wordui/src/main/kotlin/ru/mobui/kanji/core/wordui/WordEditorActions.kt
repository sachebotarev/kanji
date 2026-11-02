package ru.mobui.kanji.core.wordui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class WordEditorActions(
    val written: (String) -> Unit,
    val reading: (String) -> Unit,
    val translation: (Int, String) -> Unit,
    val language: (Int, String) -> Unit,
    val addTranslation: () -> Unit,
    val removeTranslation: (Int) -> Unit,
    val save: () -> Unit,
)
