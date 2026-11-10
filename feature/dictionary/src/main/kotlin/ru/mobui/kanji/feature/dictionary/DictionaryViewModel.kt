package ru.mobui.kanji.feature.dictionary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.DictionaryException
import ru.mobui.kanji.domain.catalog.DictionaryFailure
import ru.mobui.kanji.domain.catalog.PersonalDictionaryRepository
import ru.mobui.kanji.domain.catalog.WordDraft
import ru.mobui.kanji.domain.catalog.WordField
import ru.mobui.kanji.domain.catalog.WordTranslationDraft
import ru.mobui.kanji.domain.catalog.invalidFields
import ru.mobui.kanji.domain.catalog.matchesPersonalQuery
import ru.mobui.kanji.domain.catalog.requireManual

internal data class DictionaryState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val words: List<CatalogWord> = emptyList(),
    val snapshot: CatalogSnapshot? = null,
    val query: String = "",
    val busy: Boolean = false,
    val operationError: DictionaryFailure? = null,
    val storageError: Boolean = false,
    val deletedId: Int? = null,
    val tags: List<ru.mobui.kanji.domain.catalog.DictionaryTag> = emptyList(),
    val wordTags: Map<Int, Set<Long>> = emptyMap(),
    val tagIds: Set<Long> = emptySet(),
    val kanjiIds: Set<Int> = emptySet(),
)

internal class DictionaryViewModel(
    private val saved: SavedStateHandle,
    private val repository: PersonalDictionaryRepository,
    private val catalog: CatalogRepository,
    private val language: String,
    private val recipesRepository: ru.mobui.kanji.domain.catalog.RecipesRepository,
    private val compute: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val mutable =
        MutableStateFlow(
            DictionaryState(query = saved["query"] ?: "", deletedId = saved["deletedId"],
                tagIds = saved.get<LongArray>("tag-filter")?.toSet().orEmpty(),
                kanjiIds = saved.get<IntArray>("kanji-filter")?.toSet().orEmpty()),
        )
    val state = mutable.asStateFlow()
    val visible: StateFlow<List<CatalogWord>> =
        state
            .map { value ->
                withContext(compute) {
                    value.words.filter { it.matchesPersonalQuery(value.query, language) &&
                        (value.tagIds.isEmpty() || value.wordTags[it.id].orEmpty().any { id -> id in value.tagIds }) &&
                        (value.kanjiIds.isEmpty() || value.snapshot?.literalKanji(it).orEmpty().any { kanji -> kanji.id in value.kanjiIds }) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var observation: Job? = null

    init {
        retry()
    }

    fun query(value: String) {
        saved["query"] = value
        mutable.update { it.copy(query = value) }
    }

    @Suppress("TooGenericExceptionCaught")
    fun retry() {
        observation?.cancel()
        observation = viewModelScope.launch {
            mutable.update { it.copy(loading = true, error = false) }
            try {
                val snapshot = catalog.load()
                kotlinx.coroutines.flow.combine(repository.observeWords(), recipesRepository.observeTags(), recipesRepository.observeWordTags()) { words, tags,
                    assignments ->
                    Triple(words, tags, assignments.associate { it.wordId to it.tagIds })
                }.collect { (words, tags, assignments) ->
                    val ids = mutable.value.tagIds.intersect(tags.map { it.id }.toSet())
                    val kanji = mutable.value.kanjiIds.intersect(words.flatMap { snapshot.literalKanji(it) }.map { it.id }.toSet())
                    saved["tag-filter"] = ids.toLongArray()
                    saved["kanji-filter"] = kanji.toIntArray()
                    mutable.update { it.copy(loading = false, words = words, snapshot = snapshot,
                        tags = tags, wordTags = assignments, tagIds = ids, kanjiIds = kanji) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.log(Level.SEVERE, "Failed to load dictionary", error)
                mutable.update { it.copy(loading = false, error = true) }
            }
        }
    }

    fun tags(ids: Set<Long>) {
        saved["tag-filter"] = ids.toLongArray()
        mutable.update { it.copy(tagIds = ids) }
    }

    fun kanji(ids: Set<Int>) {
        saved["kanji-filter"] = ids.toIntArray()
        mutable.update { it.copy(kanjiIds = ids) }
    }

    fun clearOperation() {
        saved["deletedId"] = null
        mutable.update { it.copy(operationError = null, storageError = false, deletedId = null) }
    }

    @Suppress("TooGenericExceptionCaught")
    fun delete(id: Int) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, operationError = null, storageError = false) }
        viewModelScope.launch {
            try {
                repository.deleteWord(id)
                saved["deletedId"] = id
                mutable.update { it.copy(busy = false, deletedId = id) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: DictionaryException) {
                mutable.update { it.copy(busy = false, operationError = failure.failure) }
            } catch (error: Exception) {
                logger.log(Level.SEVERE, "Dictionary write failed", error)
                mutable.update { it.copy(busy = false, storageError = true) }
            }
        }
    }

    private companion object {
        val logger: Logger = Logger.getLogger("ru.mobui.kanji.Dictionary")
    }
}

internal data class EditorState(
    val draft: WordDraft,
    val original: WordDraft? = null,
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val busy: Boolean = false,
    val invalid: Set<WordField> = emptySet(),
    val failure: DictionaryFailure? = null,
    val storageError: Boolean = false,
    val savedId: Int? = null,
) {
    val dirty: Boolean
        get() = original != null && draft != original
}

internal class WordEditorViewModel(
    private val saved: SavedStateHandle,
    private val repository: PersonalDictionaryRepository,
    val id: Int?,
    private val language: String,
    private val recipesRepository: ru.mobui.kanji.domain.catalog.RecipesRepository,
) : ViewModel() {
    private val initial =
        WordDraft(
            "",
            "",
            listOf(WordTranslationDraft(if (language.startsWith("en")) "en" else "ru", "")),
        )
    private val mutable =
        MutableStateFlow(
            EditorState(
                saved.restoreWordDraft("draft") ?: initial,
                original = saved.restoreWordDraft("original"),
                savedId = saved["savedId"],
            ),
        )
    val state = mutable.asStateFlow()
    private var loadJob: Job? = null

    init {
        retry()
    }

    @Suppress("TooGenericExceptionCaught")
    fun retry() {
        if (mutable.value.busy) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutable.update { it.copy(loading = true, loadError = false) }
            try {
                if (id == null) {
                    if (mutable.value.original == null) {
                        saved.persistWordDraft("original", initial)
                        saved.persistWordDraft("draft", mutable.value.draft)
                    }
                    mutable.update { it.copy(loading = false, original = it.original ?: initial) }
                } else {
                    val word =
                        repository.observeWords().first().firstOrNull { it.id == id }
                            ?: throw DictionaryException(DictionaryFailure.Missing)
                    word.requireManual()
                    if (mutable.value.original == null) {
                        val sorted =
                            word.translations.entries.sortedWith(
                                compareBy<Map.Entry<String, String>> {
                                        when {
                                            it.key.equals(language, ignoreCase = true) -> 0
                                            it.key.equals(
                                                language.substringBefore('-'),
                                                ignoreCase = true,
                                            ) -> 1
                                            it.key.equals("ru", ignoreCase = true) -> 2
                                            else -> MISSING_TRANSLATION_PRIORITY
                                        }
                                    }
                                    .thenBy { it.key },
                            )
                        val draft =
                            WordDraft(
                                word.written,
                                word.reading,
                                sorted.map { WordTranslationDraft(it.key, it.value) },
                                tagIds = recipesRepository.observeWordTags().first().firstOrNull { it.wordId == id }?.tagIds.orEmpty(),
                            )
                        saved.persistWordDraft("original", draft)
                        saved.persistWordDraft("draft", draft)
                        mutable.update { it.copy(draft = draft, original = draft) }
                    }
                    mutable.update { it.copy(loading = false) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: DictionaryException) {
                mutable.update { it.copy(loading = false, failure = failure.failure) }
            } catch (error: Exception) {
                logger.log(Level.SEVERE, "Failed to load word", error)
                mutable.update { it.copy(loading = false, loadError = true) }
            }
        }
    }

    private fun change(transform: (WordDraft) -> WordDraft) {
        if (mutable.value.busy || mutable.value.loading) return
        val draft = transform(mutable.value.draft)
        saved.persistWordDraft("draft", draft)
        mutable.update {
            it.copy(draft = draft, invalid = emptySet(), failure = null, storageError = false)
        }
    }

    fun tags(ids: Set<Long>, pending: List<ru.mobui.kanji.domain.catalog.TagDraft>) = change {
        it.copy(tagIds = ids, pendingTags = pending)
    }

    fun written(value: String) = change { it.copy(written = value) }

    fun reading(value: String) = change { it.copy(reading = value) }

    fun translation(index: Int, value: String) = change {
        it.copy(
            translations =
                it.translations.mapIndexed { i, item ->
                    if (i == index) item.copy(text = value) else item
                },
        )
    }

    fun language(index: Int, value: String) = change {
        it.copy(
            translations =
                it.translations.mapIndexed { i, item ->
                    if (i == index) item.copy(languageTag = value) else item
                },
        )
    }

    fun removeTranslation(index: Int) = change {
        it.copy(translations = it.translations.filterIndexed { i, _ -> i != index })
    }

    fun putTranslation(languageTag: String, text: String) = change { draft ->
        val index = draft.translations.indexOfFirst { it.languageTag.equals(languageTag, true) }
        if (index < 0) draft.copy(translations = draft.translations + WordTranslationDraft(languageTag, text))
        else draft.copy(translations = draft.translations.mapIndexed { i, item -> if (i == index) item.copy(text = text) else item })
    }

    fun addTranslation() = change {
        val used = it.translations.map { item -> item.languageTag.lowercase() }
        it.copy(
            translations =
                it.translations +
                    WordTranslationDraft(
                        listOf("ru", "en").firstOrNull { tag -> tag !in used }.orEmpty(),
                        "",
                    ),
        )
    }

    @Suppress("TooGenericExceptionCaught")
    fun save() {
        if (mutable.value.busy || mutable.value.loading || mutable.value.original == null) return
        val draft = mutable.value.draft
        val invalid = draft.invalidFields()
        mutable.update { it.copy(invalid = invalid) }
        if (invalid.isNotEmpty()) return
        mutable.update { it.copy(busy = true, failure = null, storageError = false) }
        viewModelScope.launch {
            try {
                // Management may delete a tag while this private word draft remains open.
                val validIds = recipesRepository.observeTags().first().map { it.id }.toSet()
                val currentDraft = draft.copy(tagIds = draft.tagIds?.intersect(validIds))
                val result = repository.saveWord(id, currentDraft)
                saved["savedId"] = result
                mutable.update { it.copy(busy = false, savedId = result) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: DictionaryException) {
                mutable.update { it.copy(busy = false, failure = failure.failure) }
            } catch (error: Exception) {
                logger.log(Level.SEVERE, "Dictionary write failed", error)
                mutable.update { it.copy(busy = false, storageError = true) }
            }
        }
    }

    private companion object {
        val logger: Logger = Logger.getLogger("ru.mobui.kanji.Dictionary")
    }
}

private const val MISSING_TRANSLATION_PRIORITY = 3
