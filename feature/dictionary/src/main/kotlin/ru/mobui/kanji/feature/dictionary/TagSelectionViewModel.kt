package ru.mobui.kanji.feature.dictionary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.RecipesRepository
import ru.mobui.kanji.domain.catalog.TagDraft
import ru.mobui.kanji.domain.catalog.WordDraft
import ru.mobui.kanji.domain.catalog.validated

internal data class TagSelectionState(
    val tags: List<DictionaryTag> = emptyList(), val selected: Set<Long> = emptySet(),
    val pending: List<TagDraft> = emptyList(), val query: String = "", val manage: Boolean = false,
    val busy: Boolean = false, val loading: Boolean = true, val error: Boolean = false,
    val usageCounts: Map<Long, Int> = emptyMap(),
)

/** Selection is a private transaction. Explicit management operates on the shared taxonomy. */
internal class TagSelectionViewModel(private val saved: SavedStateHandle,
    private val repository: RecipesRepository) : ViewModel() {
    private val mutable = MutableStateFlow(TagSelectionState())
    val state = mutable.asStateFlow()
    private var job: kotlinx.coroutines.Job? = null
    init { retry() }
    fun retry() {
        job?.cancel()
        job = viewModelScope.launch {
            mutable.update { it.copy(loading = true, error = false) }
            try { combine(repository.observeTags(), repository.observeWordTags()) { tags, assignments ->
                tags to assignments.flatMap { assignment -> assignment.tagIds.map { assignment.wordId to it } }
                    .distinct().groupingBy { it.second }.eachCount()
            }.collect { (tags, counts) ->
                val valid = tags.map { it.id }.toSet()
                val previous = if (saved.get<Boolean>("active") == true)
                    saved.get<LongArray>("selected")?.toSet().orEmpty() else mutable.value.selected
                val selected = previous.filter { it < 0 || it in valid }.toSet()
                saved["selected"] = selected.toLongArray()
                mutable.update { it.copy(tags = tags, selected = selected, usageCounts = counts, loading = false) }
            } } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.update { it.copy(loading = false, error = true) } }
        }
    }
    fun start(ids: Set<Long>, pending: List<TagDraft>) {
        if (saved.get<Boolean>("active") != true) {
            saved["active"] = true
            persist(ids + pending.indices.map { -it.toLong() - 1 }, pending)
        }
        mutable.update { it.copy(selected = saved.get<LongArray>("selected")?.toSet().orEmpty(),
            pending = saved.restoreWordDraft("session")?.pendingTags.orEmpty(),
            query = saved["query"] ?: "", manage = saved["manage"] ?: false) }
    }
    private fun persist(ids: Set<Long>, pending: List<TagDraft> = mutable.value.pending) {
        saved["selected"] = ids.toLongArray()
        saved.persistWordDraft("session", WordDraft("", "", emptyList(), pendingTags = pending))
        mutable.update { it.copy(selected = ids, pending = pending) }
    }
    fun toggle(id: Long) { if (!mutable.value.busy) persist(mutable.value.selected.let { if (id in it) it - id else it + id }) }
    fun clear() { if (!mutable.value.busy) persist(emptySet()) }
    fun query(text: String) { saved["query"] = text; mutable.update { it.copy(query = text) } }
    fun manage(value: Boolean) { saved["manage"] = value; mutable.update { it.copy(manage = value) } }
    fun finish() { saved["active"] = false; saved["query"] = ""; saved["manage"] = false }
    fun selectedIds() = mutable.value.selected.intersect(mutable.value.tags.map { it.id }.toSet())
    fun selectedPending() = mutable.value.pending.filterIndexed { index, _ -> -index.toLong() - 1 in mutable.value.selected }
    fun create(draft: TagDraft, privateDraft: Boolean, done: () -> Unit) {
        if (mutable.value.busy || mutable.value.loading) return
        mutable.update { it.copy(error = false) }
        if (privateDraft) {
            try {
                val value = draft.validated()
                val others = mutable.value.tags + mutable.value.pending.mapIndexed { index, item -> DictionaryTag(-index.toLong()-1, item.color,
                    item.translations) }
                require(others.none { existing -> value.translations.any { t -> existing.translations.any { it.languageTag.equals(t.languageTag,
                    true) && nameKey(it.name) == nameKey(t.name) } } })
                val next = mutable.value.pending + value
                persist(mutable.value.selected + -next.size.toLong(), next); done()
            } catch (_: IllegalArgumentException) { mutable.update { it.copy(error = true) } }
        } else operation { val id = repository.saveTag(null, draft); persist(mutable.value.selected + id); done() }
    }
    fun rename(id: Long, draft: TagDraft, done: () -> Unit) = operation { repository.saveTag(id, draft); done() }
    fun delete(id: Long, done: () -> Unit) = operation { repository.deleteTag(id); persist(mutable.value.selected - id); done() }
    private fun nameKey(value: String) = android.icu.lang.UCharacter.foldCase(
        java.text.Normalizer.normalize(value.trim(), java.text.Normalizer.Form.NFKC), true)

    private fun operation(action: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, error = false) }
        viewModelScope.launch {
            try { action(); mutable.update { it.copy(busy = false) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.update { it.copy(busy = false, error = true) } }
        }
    }
}
