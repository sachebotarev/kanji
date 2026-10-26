package ru.mobui.kanji.domain.catalog

import org.junit.Assert.*
import org.junit.Test

class CatalogTest {
    private val moon = CatalogKanji(26376, "月", 4, 74, true, true, false,
        mapOf("ru" to listOf("луна", "месяц")), listOf("ゲツ", "ガツ"), listOf("つき"))
    private val one = CatalogKanji(19968, "一", 1, 1, true, true, false,
        mapOf("ru" to listOf("один")), listOf("イチ"), emptyList())
    private val january = CatalogWord(8, "一月", "いちがつ", mapOf("ru" to "январь", "en" to "January"))
    private val snapshot = CatalogSnapshot(1, listOf(CatalogSource(1, "Tobira", emptyMap())),
        listOf(january), listOf(moon, one), listOf(CatalogRadical(74, "月", 4, mapOf("ru" to "Луна")), CatalogRadical(1, "一", 1, mapOf("ru" to "Единица"))),
        listOf(CatalogPlacement(1, 19968, 1, 3), CatalogPlacement(1, 26376, 2, 4)),
        listOf(CatalogLink(1, 8, 19968), CatalogLink(1, 8, 26376)))

    @Test fun filterChoicesExcludeImpossibleCombinations() {
        assertEquals(listOf(74), snapshot.availableRadicals(CatalogSelection(1, 4), "", "ru").map { it.id })
        assertEquals(listOf(1), snapshot.availableRadicals(CatalogSelection(1, 3), "", "ru").map { it.id })
        assertEquals(listOf(4), snapshot.availableChapters(CatalogSelection(1, radicalId = 74)).map { it.number })
        assertEquals(listOf(3), snapshot.availableChapters(CatalogSelection(1, kanjiId = 19968)).map { it.number })
    }
    @Test fun chapterAndRadicalFiltersIntersect() {
        assertEquals(listOf(moon), snapshot.kanji(CatalogSelection(1, 4, 74), "", "ru"))
        assertTrue(snapshot.kanji(CatalogSelection(1, 3, 74), "", "ru").isEmpty())
    }
    @Test fun wordsAreDeduplicatedButAllStudyLinksRemain() {
        assertEquals(listOf(january), snapshot.words(CatalogSelection(1), "", "ru"))
        assertEquals(2, snapshot.links.size)
        assertEquals(1, snapshot.chapters(1).first().wordCount)
        assertEquals(1, snapshot.chapters(1).last().wordCount)
    }
    @Test fun localizedSearchUsesEnglishAndRussianFallbackAndKanaEquivalence() {
        assertEquals(listOf(january), snapshot.words(CatalogSelection(1), "JANUARY", "en-US"))
        assertEquals(listOf(january), snapshot.words(CatalogSelection(1), "январь", "de"))
        assertEquals(listOf(moon), snapshot.kanji(CatalogSelection(), "げつ", "ru"))
        assertEquals(listOf(moon), snapshot.kanji(CatalogSelection(), "луна", "en"))
    }
    @Test fun missingTranslationIsExplicitAndSourcesFallbackToOriginalTitle() {
        assertNull(emptyMap<String, String>().localized("en"))
        assertEquals("Tobira", snapshot.sources.single().title("en"))
        assertEquals("January", january.translation("en-GB"))
        assertEquals("январь", january.translation("ja"))
    }
    @Test fun literalKanjiAreDistinctUnicodeCodePointsNotStudyLinks() {
        val supplementary = CatalogKanji(0x20B9F, "𠮟", 5, 30, false, true, false, emptyMap(), emptyList(), emptyList())
        val catalog = CatalogSnapshot(1, emptyList(), emptyList(), listOf(supplementary), emptyList(), emptyList(), emptyList())
        assertEquals(listOf(supplementary), catalog.literalKanji(CatalogWord(1, "𠮟𠮟", "しかしか", emptyMap())))
    }
    @Test fun exactCharacterSearchPrecedesMatchesInMeanings() {
        val first = one.copy(meanings = mapOf("ru" to listOf("пример 月")))
        val data = CatalogSnapshot(1, emptyList(), emptyList(), listOf(first, moon), emptyList(), emptyList(), emptyList())
        assertEquals(moon, data.kanji(CatalogSelection(), "月", "ru").first())
    }
    @Test fun chapterOrderUsesNumericValuesAndKanjiUseBookOrder() {
        assertEquals(listOf(one, moon), snapshot.kanji(CatalogSelection(1), "", "ru"))
        assertEquals(listOf(3, 4), snapshot.chapters(1).map { it.number })
    }
}
