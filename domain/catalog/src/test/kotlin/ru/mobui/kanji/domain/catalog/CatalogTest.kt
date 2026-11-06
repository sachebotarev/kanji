package ru.mobui.kanji.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    private val moon =
        CatalogKanji(
            26376,
            "月",
            4,
            74,
            true,
            true,
            false,
            mapOf("ru" to listOf("луна", "месяц")),
            listOf("ゲツ", "ガツ"),
            listOf("つき"),
        )
    private val one =
        CatalogKanji(
            19968,
            "一",
            1,
            1,
            true,
            true,
            false,
            mapOf("ru" to listOf("один")),
            listOf("イチ"),
            emptyList(),
        )
    private val january = CatalogWord(8, "一月", "いちがつ", mapOf("ru" to "январь", "en" to "January"))
    private val snapshot =
        CatalogSnapshot(
            1,
            content =
                CatalogContent(
                    listOf(CatalogSource(1, "Tobira", emptyMap())),
                    listOf(january),
                    listOf(moon, one),
                    listOf(
                        CatalogRadical(74, "月", 4, mapOf("ru" to "Луна")),
                        CatalogRadical(1, "一", 1, mapOf("ru" to "Единица")),
                    ),
                    listOf(CatalogPlacement(1, 19968, 1, 3), CatalogPlacement(1, 26376, 2, 4)),
                    listOf(CatalogLink(1, 8, 19968), CatalogLink(1, 8, 26376)),
                ),
        )

    @Test
    fun kanjiFacetsMatchOrWithinAndAcrossSearchScopeAndChapters() {
        val data = CatalogSnapshot(2, CatalogContent(snapshot.sources, snapshot.words,
            listOf(moon.copy(jlptLevel = JlptLevel.N5), one), snapshot.radicals, snapshot.placements, snapshot.links))
        val both = CatalogSelection(sourceId = 1, radicalIds = setOf(1, 74), strokeIds = setOf(1, 4),
            jlptFilters = setOf(JlptFilter.Exact(JlptLevel.N5), JlptFilter.Missing))
        assertEquals(setOf(one.id, moon.id), data.kanji(both, "", "ru").map { it.id }.toSet())
        assertEquals(listOf(moon.id), data.kanji(both, "ЛУНА", "ru").map { it.id })
        assertEquals(listOf(one.id), data.kanji(both.copy(chapterIds = setOf(3)), "", "ru").map { it.id })
        assertEquals(listOf(moon.id), data.kanji(both.copy(jlptFilters = setOf(JlptFilter.Exact(JlptLevel.N5))), "", "ru").map { it.id })
        assertTrue(data.kanji(both.copy(sourceId = 2), "", "ru").isEmpty())
        assertEquals(listOf(one.id), data.kanji(both.copy(strokeIds = setOf(1)), "", "ru").map { it.id })
        assertEquals(listOf(january.id), data.words(both, "", "ru").map { it.id })
    }

    @Test
    fun facetOptionsIgnoreOwnSetAndWordMatchesUseTheSameStudyLink() {
        val contradictory = CatalogSelection(sourceId = 1, radicalIds = setOf(74), strokeIds = setOf(1))
        assertTrue(snapshot.words(contradictory, "", "ru").isEmpty())
        assertTrue(snapshot.wordGroups(contradictory, "", "ru").isEmpty())
        assertEquals(mapOf(4 to 1), snapshot.strokeCounts(contradictory))
        assertEquals(listOf(1), snapshot.availableRadicals(contradictory, "", "ru").map { it.id })
        assertEquals(mapOf<JlptLevel?, Int>(null to 2), snapshot.jlptCounts(CatalogSelection(
            jlptFilters = setOf(JlptFilter.Exact(JlptLevel.N1), JlptFilter.Exact(JlptLevel.N2)))))
    }

    @Test
    fun radicalStrokeSetsAreOrMatchedAndIntersectSearch() {
        assertEquals(setOf(1, 74), snapshot.radicals("", "ru", strokeIds = setOf(1, 4)).map { it.id }.toSet())
        assertEquals(listOf(74), snapshot.radicals("луна", "ru", strokeIds = setOf(1, 4)).map { it.id })
        assertTrue(snapshot.radicals("луна", "ru", strokeIds = setOf(1, 2)).isEmpty())
        assertEquals(listOf(1), snapshot.radicals("", "ru", 1).map { it.id })
        assertEquals(2, snapshot.radicals("", "ru", strokeIds = emptySet()).size)
    }

    @Test
    fun chapterSetsAreOrMatchedAndIntersectKanjiOnTheSamePlacement() {
        val selection = CatalogSelection(1, chapterIds = setOf(3, 4))
        assertEquals(setOf(one.id, moon.id), snapshot.kanji(selection, "", "ru").map { it.id }.toSet())
        assertEquals(listOf(january), snapshot.words(selection, "", "ru"))
        assertEquals(listOf(3), snapshot.wordGroups(selection, "", "ru").map { it.chapter })
        assertEquals(listOf(3, 4), snapshot.availableChapters(selection.copy(chapterIds = setOf(3))).map { it.number })
        val incompatible = selection.copy(chapterIds = setOf(3), kanjiIds = setOf(moon.id))
        assertTrue(snapshot.words(incompatible, "", "ru").isEmpty())
        assertEquals(listOf(january), snapshot.words(incompatible.copy(chapterIds = setOf(3, 4)), "январь", "ru"))
        assertTrue(snapshot.words(selection.copy(sourceId = 2), "", "ru").isEmpty())
    }

    @Test
    fun multipleKanjiAreOrMatchedWithoutDuplicatingWords() {
        val selection = CatalogSelection(1, kanjiIds = setOf(one.id, moon.id))
        assertEquals(listOf(january), snapshot.words(selection, "", "ru"))
        assertEquals(listOf(3, 4), snapshot.availableChapters(selection).map { it.number })
        assertEquals(listOf(3), snapshot.wordGroups(selection, "", "ru").map { it.chapter })
        assertEquals(listOf(4), snapshot.wordGroups(selection.copy(chapter = 4), "", "ru").map { it.chapter })
    }

    @Test
    fun selectedKanjiIntersectChapterAndSearchOnTheSameStudyLink() {
        val selection = CatalogSelection(1, chapter = 3, kanjiIds = setOf(moon.id))
        assertTrue(snapshot.words(selection, "", "ru").isEmpty())
        assertEquals(listOf(january), snapshot.words(selection.copy(kanjiIds = setOf(one.id, moon.id)), "январь", "ru"))
        assertTrue(snapshot.words(selection.copy(chapter = null), "not present", "en").isEmpty())
    }

    @Test
    fun filterChoicesExcludeImpossibleCombinations() {
        assertEquals(
            listOf(74),
            snapshot.availableRadicals(CatalogSelection(1, 4), "", "ru").map { it.id },
        )
        assertEquals(
            listOf(1),
            snapshot.availableRadicals(CatalogSelection(1, 3), "", "ru").map { it.id },
        )
        assertEquals(
            listOf(4),
            snapshot.availableChapters(CatalogSelection(1, radicalId = 74)).map { it.number },
        )
        assertEquals(
            listOf(3),
            snapshot.availableChapters(CatalogSelection(1, kanjiId = 19968)).map { it.number },
        )
    }

    @Test
    fun chapterAndRadicalFiltersIntersect() {
        assertEquals(listOf(moon), snapshot.kanji(CatalogSelection(1, 4, 74), "", "ru"))
        assertTrue(snapshot.kanji(CatalogSelection(1, 3, 74), "", "ru").isEmpty())
    }

    @Test
    fun wordsAreDeduplicatedButAllStudyLinksRemain() {
        assertEquals(listOf(january), snapshot.words(CatalogSelection(1), "", "ru"))
        assertEquals(2, snapshot.links.size)
        assertEquals(1, snapshot.chapters(1).first().wordCount)
        assertEquals(1, snapshot.chapters(1).last().wordCount)
    }

    @Test
    fun localizedSearchUsesEnglishAndRussianFallbackAndKanaEquivalence() {
        assertEquals(listOf(january), snapshot.words(CatalogSelection(1), "JANUARY", "en-US"))
        assertEquals(listOf(january), snapshot.words(CatalogSelection(1), "январь", "de"))
        assertEquals(listOf(moon), snapshot.kanji(CatalogSelection(), "げつ", "ru"))
        assertEquals(listOf(moon), snapshot.kanji(CatalogSelection(), "луна", "en"))
    }

    @Test
    fun missingTranslationIsExplicitAndSourcesFallbackToOriginalTitle() {
        assertNull(emptyMap<String, String>().localized("en"))
        assertEquals("Tobira", snapshot.sources.single().title("en"))
        assertEquals("January", january.translation("en-GB"))
        assertEquals("январь", january.translation("ja"))
    }

    @Test
    fun literalKanjiAreDistinctUnicodeCodePointsNotStudyLinks() {
        val supplementary =
            CatalogKanji(
                0x20B9F,
                "𠮟",
                5,
                30,
                false,
                true,
                false,
                emptyMap(),
                emptyList(),
                emptyList(),
            )
        val catalog =
            CatalogSnapshot(
                1,
                content =
                    CatalogContent(
                        emptyList(),
                        emptyList(),
                        listOf(supplementary),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                    ),
            )
        assertEquals(
            listOf(supplementary),
            catalog.literalKanji(CatalogWord(1, "𠮟𠮟", "しかしか", emptyMap())),
        )
    }

    @Test
    fun exactCharacterSearchPrecedesMatchesInMeanings() {
        val first = one.copy(meanings = mapOf("ru" to listOf("пример 月")))
        val data =
            CatalogSnapshot(
                1,
                content =
                    CatalogContent(
                        emptyList(),
                        emptyList(),
                        listOf(first, moon),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                    ),
            )
        assertEquals(moon, data.kanji(CatalogSelection(), "月", "ru").first())
    }

    @Test
    fun iterationMarkIsSearchableAndAbsentFromTheKanjiIndex() {
        val mark =
            CatalogKanji(
                12293,
                "々",
                3,
                null,
                false,
                false,
                false,
                mapOf("ru" to listOf("знак повтора")),
                emptyList(),
                emptyList(),
            )
        val data =
            CatalogSnapshot(
                1,
                content =
                    CatalogContent(
                        emptyList(),
                        emptyList(),
                        listOf(mark, one),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                    ),
            )
        assertEquals(listOf(one), data.kanji(CatalogSelection(), "", "ru"))
        assertEquals(listOf(mark), data.kanji(CatalogSelection(), "々", "ru"))
        assertEquals(listOf(mark), data.kanji(CatalogSelection(), "знак повтора", "ru"))
        val placed =
            CatalogSnapshot(
                1,
                content =
                    CatalogContent(
                        emptyList(),
                        emptyList(),
                        listOf(mark, one),
                        emptyList(),
                        listOf(CatalogPlacement(1, 12293, 1, 1), CatalogPlacement(1, 19968, 2, 1)),
                        emptyList(),
                    ),
            )
        assertEquals(listOf(mark, one), placed.kanji(CatalogSelection(1), "", "ru"))
    }

    @Test
    fun chapterOrderUsesNumericValuesAndKanjiUseBookOrder() {
        assertEquals(listOf(one, moon), snapshot.kanji(CatalogSelection(1), "", "ru"))
        assertEquals(listOf(3, 4), snapshot.chapters(1).map { it.number })
    }

    @Test
    fun radicalIndexFiltersByItsOwnStrokeCount() {
        assertEquals(listOf(1), snapshot.radicals("", "ru", 1).map { it.id })
        assertEquals(listOf(74), snapshot.radicals("луна", "ru", 4).map { it.id })
        assertTrue(snapshot.radicals("луна", "ru", 1).isEmpty())
        assertEquals(mapOf(1 to 1, 4 to 1), snapshot.radicalStrokeCounts())
    }

    @Test
    fun strokeFacetsRespectOtherConstraintsButIgnoreTheirOwnValue() {
        assertEquals(listOf(1, 4), snapshot.availableStrokes(CatalogSelection(strokes = 99)))
        assertEquals(
            listOf(4),
            snapshot.availableStrokes(CatalogSelection(chapter = 4, sourceId = 1)),
        )
        assertEquals(
            listOf(moon),
            snapshot.kanji(CatalogSelection(strokes = 4, radicalId = 74), "", "ru"),
        )
        assertTrue(
            snapshot.kanji(CatalogSelection(strokes = 1, radicalId = 74), "", "ru").isEmpty(),
        )
    }

    @Test
    fun jlptIsExactAndMissingIsDistinctFromAnyLevel() {
        val classified = one.copy(jlptLevel = JlptLevel.N5)
        val data =
            CatalogSnapshot(
                1,
                content =
                    CatalogContent(
                        emptyList(),
                        emptyList(),
                        listOf(classified, moon),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                    ),
            )
        assertEquals(
            listOf(classified),
            data.kanji(CatalogSelection(jlpt = JlptFilter.Exact(JlptLevel.N5)), "", "ru"),
        )
        assertEquals(
            listOf(moon),
            data.kanji(CatalogSelection(jlpt = JlptFilter.Missing), "", "ru"),
        )
        assertEquals(
            listOf(JlptFilter.Exact(JlptLevel.N5), JlptFilter.Missing),
            data.availableJlpt(CatalogSelection(jlpt = JlptFilter.Missing)),
        )
    }

    @Test
    fun usageNotesFollowTheTranslationLanguageWithoutIndependentFallback() {
        val word = january.copy(usageNotes = mapOf("ru" to "Русское примечание"))
        assertNull(word.usageNote("en-US"))
        assertEquals("Русское примечание", word.usageNote("de"))
        assertNull(word.copy(translations = emptyMap()).usageNote("ru"))
    }

    @Test
    fun groupingUsesFirstEligiblePlacementAndPreservesUnfilteredCounts() {
        val groups = snapshot.wordGroups(CatalogSelection(1), "январь", "ru")
        assertEquals(listOf(3), groups.map { it.chapter })
        assertEquals(listOf(january), groups.single().words)
        assertEquals(1, groups.single().total)
        assertEquals(
            4,
            snapshot.wordGroups(CatalogSelection(1, kanjiId = moon.id), "", "ru").single().chapter,
        )
        assertTrue(snapshot.wordGroups(CatalogSelection(1), "нет совпадения", "ru").isEmpty())
    }

    @Test
    fun sourceCountsAreIndependentOfQueryAndDeduplicateWordsAcrossPlacements() {
        assertEquals(CatalogSourceCounts(2, 2, 1), snapshot.sourceCounts[1])
        snapshot.words(CatalogSelection(1), "нет совпадения", "ru")
        assertEquals(1, snapshot.sourceCounts[1]?.words)
    }

    @Test
    fun groupingUsesOnlyPlacementsInsideTheSelectedChapter() {
        val groups = snapshot.wordGroups(CatalogSelection(1, chapter = 4), "", "ru")
        assertEquals(listOf(4), groups.map { it.chapter })
        assertEquals(listOf(january), groups.single().words)
    }

    @Test
    fun groupingUsesOnlyPlacementsMatchingTheSelectedRadical() {
        val groups = snapshot.wordGroups(CatalogSelection(1, radicalId = moon.radicalId), "", "ru")
        assertEquals(listOf(4), groups.map { it.chapter })
        assertEquals(listOf(january), groups.single().words)
    }
}
