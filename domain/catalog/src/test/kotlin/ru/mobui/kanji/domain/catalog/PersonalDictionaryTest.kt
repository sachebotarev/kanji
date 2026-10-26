package ru.mobui.kanji.domain.catalog

import org.junit.Assert.*
import org.junit.Test

class PersonalDictionaryTest {
    @Test fun preservesPhrasesAndNormalizesLanguageTagsWithoutRewritingJapanese() {
        val draft = WordDraft(" お元気ですか。 ", " おげんきですか。 ", listOf(
            WordTranslationDraft(" RU ", " Как дела? "), WordTranslationDraft("pt-br", " Como vai? "))).normalized()
        assertEquals("お元気ですか。", draft.written)
        assertEquals("おげんきですか。", draft.reading)
        assertEquals(listOf("ru", "pt-BR"), draft.translations.map { it.languageTag })
        assertEquals("Как дела?", draft.translations.first().text)
    }
    @Test fun rejectsEmptyTranslationsDuplicateLanguagesAndInvalidTags() {
        val valid = WordDraft("猫", "ねこ", listOf(WordTranslationDraft("ru", "кошка")))
        assertTrue(valid.invalidFields().isEmpty())
        assertEquals(setOf(WordField.Translations), valid.copy(translations = emptyList()).invalidFields())
        assertTrue(valid.copy(translations = listOf(WordTranslationDraft("ru", "кошка"), WordTranslationDraft("RU", "кот")))
            .invalidFields().contains(WordField.Translations))
        assertTrue(valid.copy(translations = listOf(WordTranslationDraft("???", "кошка"))).invalidFields().isNotEmpty())
        assertTrue(valid.copy(translations = listOf(WordTranslationDraft("iw", "חתול"), WordTranslationDraft("he", "חתול")))
            .invalidFields().contains(WordField.Translations))
        assertTrue(valid.copy(translations = listOf(WordTranslationDraft("ru-???", "кошка"))).invalidFields().isNotEmpty())
        assertEquals(setOf(WordField.Written, WordField.Reading), valid.copy(written = " ", reading = "").invalidFields())
    }
    @Test fun catalogWordIsReadOnlyRegardlessOfIdOrLocale() {
        try { CatalogWord(-1, "猫", "ねこ", mapOf("ru" to "кошка")).requireManual(); fail("Missing manual origin") }
        catch (failure: DictionaryException) { assertEquals(DictionaryFailure.ReadOnly, failure.failure) }
        CatalogWord(1, "猫", "ねこ", emptyMap(), isUserCreated = true).requireManual()
    }
    @Test fun searchUsesKanaEquivalenceAndRussianFallbackWithExplicitAbsence() {
        val word = CatalogWord(-1, "猫", "ねこ", mapOf("ru" to "кошка"), isUserCreated = true)
        assertTrue(word.matchesPersonalQuery("ネコ", "en"))
        assertTrue(word.matchesPersonalQuery("КОШ", "en"))
        assertFalse(word.matchesPersonalQuery("cat", "en"))
        assertNull(word.copy(translations = emptyMap()).translation("en"))
    }
}
