package ru.mobui.kanji.feature.dictionary

import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import ru.mobui.kanji.core.wordui.WordEditorActions
import ru.mobui.kanji.domain.catalog.*

@Serializable private data object PersonalList
@Serializable private data class PersonalWord(val id: Int)
@Serializable private data class PersonalEditor(val id: Int = 0)
@Serializable private data class DictionaryReference(val kanjiId: Int = 0, val sourceId: Int = 0)

/** The app supplies catalog navigation; this feature never depends on feature:catalog. */
@Composable
fun DictionaryRoute(repository: PersonalDictionaryRepository, catalog: CatalogRepository, onBack: () -> Unit,
    referenceContent: @Composable (kanjiId: Int, sourceId: Int, onBack: () -> Unit) -> Unit) {
    val nav = rememberNavController()
    val language = LocalConfiguration.current.locales[0].toLanguageTag()
    val model: DictionaryViewModel = viewModel(key = "personal-dictionary-$language", factory = viewModelFactory {
        initializer { DictionaryViewModel(createSavedStateHandle(), repository, catalog, language) }
    })
    val state by model.state.collectAsStateWithLifecycle()
    val visible by model.visible.collectAsStateWithLifecycle()
    fun back() { if (!state.busy && !nav.popBackStack()) onBack() }
    BackHandler { back() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        NavHost(nav, startDestination = PersonalList) {
            composable<PersonalList> {
                DictionaryListScreen(state, visible, language, ::back, model::query,
                    { id -> model.clearOperation(); nav.navigate(PersonalWord(id)) },
                    { nav.navigate(PersonalEditor()) }, model::retry)
            }
            composable<PersonalWord> { entry ->
                val id = entry.toRoute<PersonalWord>().id
                LaunchedEffect(state.deletedId) {
                    if (state.deletedId == id) { nav.popBackStack(); model.clearOperation() }
                }
                DictionaryWordScreen(state.words.firstOrNull { it.id == id }, state, language, ::back,
                    { nav.navigate(PersonalEditor(id)) }, { model.delete(id) },
                    { nav.navigate(DictionaryReference(kanjiId = it)) },
                    { nav.navigate(DictionaryReference(it.kanjiId ?: 0, it.sourceId)) }, model::retry)
            }
            composable<PersonalEditor> { entry ->
                val id = entry.toRoute<PersonalEditor>().id.takeIf { it != 0 }
                val editor: WordEditorViewModel = viewModel(entry, factory = viewModelFactory {
                    initializer { WordEditorViewModel(createSavedStateHandle(), repository, id, language) }
                })
                val editState by editor.state.collectAsStateWithLifecycle()
                var leaving by rememberSaveable { mutableStateOf(false) }
                fun editorBack() {
                    if (!editState.busy) {
                        if (editState.dirty) leaving = true else nav.popBackStack()
                    }
                }
                BackHandler { editorBack() }
                LaunchedEffect(editState.savedId) {
                    editState.savedId?.let { savedId ->
                        nav.popBackStack()
                        if (id == null) nav.navigate(PersonalWord(savedId))
                    }
                }
                WordEditorScreen(editState, id == null,
                    WordEditorActions(editor::written, editor::reading, editor::translation, editor::language,
                        editor::addTranslation, editor::removeTranslation, editor::save), ::editorBack, editor::retry)
                if (leaving) AlertDialog(onDismissRequest = { leaving = false },
                    title = { Text(stringResource(R.string.dictionary_unsaved)) },
                    text = { androidx.compose.foundation.layout.Column {
                        Text(stringResource(R.string.dictionary_unsaved_text))
                        TextButton({ leaving = false }) { Text(stringResource(R.string.dictionary_continue)) }
                    } },
                    confirmButton = { TextButton({ leaving = false; editor.save() }) { Text(stringResource(R.string.dictionary_save)) } },
                    dismissButton = { TextButton({ leaving = false; nav.popBackStack() }) { Text(stringResource(R.string.dictionary_discard)) } })
            }
            composable<DictionaryReference> { entry ->
                val reference = entry.toRoute<DictionaryReference>()
                referenceContent(reference.kanjiId, reference.sourceId) { nav.popBackStack() }
            }
        }
    }
}
