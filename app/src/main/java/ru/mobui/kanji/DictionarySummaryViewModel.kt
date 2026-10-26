package ru.mobui.kanji

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import ru.mobui.kanji.domain.catalog.PersonalDictionaryRepository

/** A missing count is shown as a description, never as a fabricated zero. */
class DictionarySummaryViewModel(repository: PersonalDictionaryRepository) : ViewModel() {
    val count: StateFlow<Int?> = repository.observeWords().map { it.size as Int? }
        .catch { emit(null) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
