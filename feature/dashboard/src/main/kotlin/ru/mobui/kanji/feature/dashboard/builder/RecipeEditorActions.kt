package ru.mobui.kanji.feature.dashboard.builder

/** Navigation belongs to the entry owner; the editor never opens another feature directly. */
class RecipeEditorActions(
    val onSaved: (String) -> Unit,
    val onBack: () -> Unit,
    val onWord: (Int) -> Unit,
    val onManageTags: () -> Unit,
)
