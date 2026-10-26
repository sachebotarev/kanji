package ru.mobui.kanji.feature.dictionary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.mobui.kanji.domain.catalog.*

internal data class DictionaryState(
    val loading: Boolean = true, val error: Boolean = false, val words: List<CatalogWord> = emptyList(),
    val snapshot: CatalogSnapshot? = null, val query: String = "", val busy: Boolean = false,
    val operationError: DictionaryFailure? = null, val storageError: Boolean = false, val deletedId: Int? = null
)

internal class DictionaryViewModel(private val saved: SavedStateHandle,
    private val repository: PersonalDictionaryRepository, private val catalog: CatalogRepository,
    private val language: String, private val compute: CoroutineDispatcher = Dispatchers.Default) : ViewModel() {
    private val mutable = MutableStateFlow(DictionaryState(query = saved["query"] ?: "", deletedId = saved["deletedId"]))
    val state = mutable.asStateFlow()
    val visible: StateFlow<List<CatalogWord>> = state.map { value ->
        withContext(compute) { value.words.filter { it.matchesPersonalQuery(value.query, language) } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var observation: Job? = null
    init { retry() }
    fun query(value: String) { saved["query"] = value; mutable.update { it.copy(query = value) } }
    fun retry() {
        observation?.cancel()
        observation = viewModelScope.launch {
            mutable.update { it.copy(loading = true, error = false) }
            try {
                val snapshot = catalog.load()
                repository.observeWords().collect { words ->
                    mutable.update { it.copy(loading = false, words = words, snapshot = snapshot) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.update { it.copy(loading = false, error = true) } }
        }
    }
    fun clearOperation() { saved["deletedId"] = null; mutable.update { it.copy(operationError = null, storageError = false, deletedId = null) } }
    fun delete(id: Int) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, operationError = null, storageError = false) }
        viewModelScope.launch {
            try {
                repository.deleteWord(id)
                saved["deletedId"] = id
                mutable.update { it.copy(busy = false, deletedId = id) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: DictionaryException) { mutable.update { it.copy(busy = false, operationError = failure.failure) } }
            catch (_: Exception) { mutable.update { it.copy(busy = false, storageError = true) } }
        }
    }
}

internal data class EditorState(
    val draft: WordDraft, val original: WordDraft? = null, val loading: Boolean = true,
    val loadError: Boolean = false, val busy: Boolean = false, val invalid: Set<WordField> = emptySet(),
    val failure: DictionaryFailure? = null, val storageError: Boolean = false, val savedId: Int? = null
) { val dirty: Boolean get() = original != null && draft != original }

internal class WordEditorViewModel(private val saved: SavedStateHandle, private val repository: PersonalDictionaryRepository,
    val id: Int?, private val language: String) : ViewModel() {
    private fun restored(prefix: String): WordDraft? {
        val written = saved.get<String>("$prefix-written") ?: return null
        val tags = saved.get<ArrayList<String>>("$prefix-tags") ?: return null
        val texts = saved.get<ArrayList<String>>("$prefix-texts") ?: return null
        return WordDraft(written, saved.get<String>("$prefix-reading").orEmpty(), tags.zip(texts).map { WordTranslationDraft(it.first, it.second) })
    }
    private fun persist(prefix: String, draft: WordDraft) {
        saved["$prefix-written"] = draft.written; saved["$prefix-reading"] = draft.reading
        saved["$prefix-tags"] = ArrayList(draft.translations.map { it.languageTag })
        saved["$prefix-texts"] = ArrayList(draft.translations.map { it.text })
    }
    private val initial = WordDraft("", "", listOf(WordTranslationDraft(if (language.startsWith("en")) "en" else "ru", "")))
    private val mutable = MutableStateFlow(EditorState(restored("draft") ?: initial,
        original = restored("original"), savedId = saved["savedId"]))
    val state = mutable.asStateFlow()
    private var loadJob: Job? = null
    init { retry() }
    fun retry() {
        if (mutable.value.busy) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutable.update { it.copy(loading = true, loadError = false) }
            try {
                if (id == null) {
                    if (mutable.value.original == null) { persist("original", initial); persist("draft", mutable.value.draft) }
                    mutable.update { it.copy(loading = false, original = it.original ?: initial) }
                } else {
                    val word = repository.observeWords().first().firstOrNull { it.id == id }
                        ?: throw DictionaryException(DictionaryFailure.Missing)
                    word.requireManual()
                    if (mutable.value.original == null) {
                        val sorted = word.translations.entries.sortedWith(compareBy<Map.Entry<String, String>> {
                            when {
                                it.key.equals(language, ignoreCase = true) -> 0
                                it.key.equals(language.substringBefore('-'), ignoreCase = true) -> 1
                                it.key.equals("ru", ignoreCase = true) -> 2
                                else -> 3
                            }
                        }.thenBy { it.key })
                        val draft = WordDraft(word.written, word.reading, sorted.map { WordTranslationDraft(it.key, it.value) })
                        persist("original", draft); persist("draft", draft)
                        mutable.update { it.copy(draft = draft, original = draft) }
                    }
                    mutable.update { it.copy(loading = false) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: DictionaryException) { mutable.update { it.copy(loading = false, failure = failure.failure) } }
            catch (_: Exception) { mutable.update { it.copy(loading = false, loadError = true) } }
        }
    }
    private fun change(transform: (WordDraft) -> WordDraft) {
        if (mutable.value.busy || mutable.value.loading) return
        val draft = transform(mutable.value.draft)
        persist("draft", draft)
        mutable.update { it.copy(draft = draft, invalid = emptySet(), failure = null, storageError = false) }
    }
    fun written(value: String) = change { it.copy(written = value) }
    fun reading(value: String) = change { it.copy(reading = value) }
    fun translation(index: Int, value: String) = change { it.copy(translations = it.translations.mapIndexed { i, item -> if (i == index) item.copy(text = value) else item }) }
    fun language(index: Int, value: String) = change { it.copy(translations = it.translations.mapIndexed { i, item -> if (i == index) item.copy(languageTag = value) else item }) }
    fun removeTranslation(index: Int) = change { it.copy(translations = it.translations.filterIndexed { i, _ -> i != index }) }
    fun addTranslation() = change {
        val used = it.translations.map { item -> item.languageTag.lowercase() }
        it.copy(translations = it.translations + WordTranslationDraft(listOf("ru", "en").firstOrNull { tag -> tag !in used }.orEmpty(), ""))
    }
    fun save() {
        if (mutable.value.busy || mutable.value.loading || mutable.value.original == null) return
        val draft = mutable.value.draft
        val invalid = draft.invalidFields()
        mutable.update { it.copy(invalid = invalid) }
        if (invalid.isNotEmpty()) return
        mutable.update { it.copy(busy = true, failure = null, storageError = false) }
        viewModelScope.launch {
            try {
                val result = repository.saveWord(id, draft)
                saved["savedId"] = result
                mutable.update { it.copy(busy = false, savedId = result) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: DictionaryException) { mutable.update { it.copy(busy = false, failure = failure.failure) } }
            catch (_: Exception) { mutable.update { it.copy(busy = false, storageError = true) } }
        }
    }
}
