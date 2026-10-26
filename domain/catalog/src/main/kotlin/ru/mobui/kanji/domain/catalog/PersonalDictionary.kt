package ru.mobui.kanji.domain.catalog

import java.util.Locale
import kotlinx.coroutines.flow.Flow

data class WordTranslationDraft(val languageTag: String, val text: String)
data class WordDraft(val written: String, val reading: String, val translations: List<WordTranslationDraft>)
enum class WordField { Written, Reading, Translations }
enum class DictionaryFailure { Invalid, Duplicate, ReadOnly, Missing }
class DictionaryException(val failure: DictionaryFailure) : IllegalArgumentException(failure.name)

/** Validation does not depend on the interface language; phrases are valid entries. */
fun WordDraft.invalidFields(): Set<WordField> = buildSet {
    if (written.isBlank()) add(WordField.Written)
    if (reading.isBlank()) add(WordField.Reading)
    val tags = translations.map { canonicalLanguageTag(it.languageTag.trim())?.lowercase(Locale.ROOT) }
    if (translations.isEmpty() || translations.any { it.text.isBlank() || canonicalLanguageTag(it.languageTag.trim()) == null } ||
        tags.distinct().size != tags.size) add(WordField.Translations)
}
private fun canonicalLanguageTag(tag: String): String? = try {
    Locale.Builder().setLanguageTag(tag).build().takeIf { it.language.isNotEmpty() }?.toLanguageTag()
} catch (_: java.util.IllformedLocaleException) { null }

fun WordDraft.normalized(): WordDraft {
    if (invalidFields().isNotEmpty()) throw DictionaryException(DictionaryFailure.Invalid)
    return copy(written = written.trim(), reading = reading.trim(), translations = translations.map {
        WordTranslationDraft(requireNotNull(canonicalLanguageTag(it.languageTag.trim())), it.text.trim())
    })
}
fun CatalogWord.requireManual() {
    if (!isUserCreated) throw DictionaryException(DictionaryFailure.ReadOnly)
}
fun CatalogWord.matchesPersonalQuery(query: String, language: String): Boolean {
    fun normalize(value: String) = buildString {
        value.lowercase(Locale.ROOT).forEach { append(if (it in '\u30a1'..'\u30f6') (it.code - 0x60).toChar() else it) }
    }
    val needle = normalize(query.trim())
    return listOfNotNull(written, reading, translation(language)).any { normalize(it).contains(needle) }
}

interface PersonalDictionaryRepository {
    /** Newest personal memberships first, including any catalog words already linked to personal sources. */
    fun observeWords(): Flow<List<CatalogWord>>
    /** Duplicate identity is reported; catalog content is never silently changed. */
    suspend fun saveWord(id: Int?, draft: WordDraft): Int
    /** Only manually created entries may be deleted. */
    suspend fun deleteWord(id: Int)
}
