package ru.mobui.kanji.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingInputRulesTest {
    private val sun = '日'.code
    private val moon = '月'.code
    private val fire = '火'.code
    private val hiragana = 'あ'.code
    private val book = CatalogSource(1, "Tobira", emptyMap(), sourceType = "book")
    private val snapshot =
        CatalogSnapshot(
            1,
            CatalogContent(
                sources = listOf(book),
                words = emptyList(),
                kanji =
                    listOf(
                        CatalogKanji(
                            sun, "日", 4, null, false, true, false, emptyMap(), emptyList(), emptyList(),
                            jlptLevel = JlptLevel.N5,
                        ),
                        CatalogKanji(
                            moon, "月", 4, null, false, true, false, emptyMap(), emptyList(), emptyList(),
                            jlptLevel = JlptLevel.N3,
                        ),
                        CatalogKanji(
                            fire, "火", 4, null, false, true, false, emptyMap(), emptyList(), emptyList(),
                        ),
                    ),
                radicals = emptyList(),
                placements =
                    listOf(
                        CatalogPlacement(1, sun, 1, 1),
                        CatalogPlacement(1, moon, 2, 5),
                    ),
                links = emptyList(),
            ),
        )

    @Test
    fun capsUseMaxChapterPerBookSource() {
        val recipe =
            Recipe(
                id = "r",
                names = listOf(RecipeName("ru", "Тест")),
                materials =
                    listOf(
                        RecipeMaterial(
                            "m1",
                            MaterialType.Book,
                            sourceId = 1,
                            chapters = setOf(1, 3),
                        ),
                        RecipeMaterial(
                            "m2",
                            MaterialType.Book,
                            sourceId = 1,
                            chapters = setOf(2),
                        ),
                    ),
            )
        assertEquals(mapOf(1 to 3), TrainingInputRules.bookChapterCaps(recipe))
    }

    @Test
    fun bookWithoutChaptersYieldsNullMaxChapter() {
        val recipe =
            Recipe(
                id = "r",
                names = listOf(RecipeName("ru", "Тест")),
                materials =
                    listOf(
                        RecipeMaterial(
                            "m1",
                            MaterialType.Book,
                            sourceId = 1,
                            chapters = emptySet(),
                        ),
                    ),
            )
        assertEquals(mapOf(1 to null), TrainingInputRules.bookChapterCaps(recipe))
    }

    @Test
    fun jlptCeilingUsesHardestExactLevelAcrossKanjiMaterials() {
        val recipe =
            Recipe(
                id = "r",
                names = listOf(RecipeName("ru", "Тест")),
                materials =
                    listOf(
                        RecipeMaterial("a", MaterialType.Kanji, jlptLevels = setOf(5, 4, -1)),
                        RecipeMaterial("b", MaterialType.Kanji, jlptLevels = setOf(3)),
                    ),
            )
        assertEquals(JlptLevel.N3, TrainingInputRules.jlptDifficultyCeiling(recipe))
        assertNull(
            TrainingInputRules.jlptDifficultyCeiling(
                recipe.copy(materials = listOf(RecipeMaterial("c", MaterialType.Kanji, jlptLevels = setOf(-1)))),
            ),
        )
    }

    @Test
    fun kanaDoesNotRequireHandwriting() {
        assertFalse(
            TrainingInputRules.requiresHandwriting(hiragana, snapshot, mapOf(1 to 10)),
        )
    }

    @Test
    fun catalogKanjiWithinChaptersRequiresHandwriting() {
        val caps = mapOf(1 to 3)
        // sun is in Tobira ch 1 <= 3 -> User
        assertTrue(TrainingInputRules.requiresHandwriting(sun, snapshot, caps))
        // moon is in Tobira ch 5 > 3 -> Prefilled
        assertFalse(TrainingInputRules.requiresHandwriting(moon, snapshot, caps))
        // fire is NOT in Tobira -> Prefilled
        assertFalse(TrainingInputRules.requiresHandwriting(fire, snapshot, caps))
    }

    @Test
    fun kanjiNotInBookIsPrefilledEvenWithoutChapterLimit() {
        val caps = mapOf(1 to null)
        // sun and moon are in Tobira -> User
        assertTrue(TrainingInputRules.requiresHandwriting(sun, snapshot, caps))
        assertTrue(TrainingInputRules.requiresHandwriting(moon, snapshot, caps))
        // fire is NOT in Tobira -> Prefilled
        assertFalse(TrainingInputRules.requiresHandwriting(fire, snapshot, caps))
    }

    @Test
    fun emptyCapsDoNotGateLaterChapters() {
        // When no book materials are in recipe, all catalog kanji are eligible
        assertTrue(TrainingInputRules.requiresHandwriting(sun, snapshot, emptyMap()))
        assertTrue(TrainingInputRules.requiresHandwriting(moon, snapshot, emptyMap()))
        assertTrue(TrainingInputRules.requiresHandwriting(fire, snapshot, emptyMap()))
    }

    @Test
    fun harderJlptThanCeilingIsPrefilled() {
        val ceiling = JlptLevel.N5
        assertTrue(TrainingInputRules.requiresHandwriting(sun, snapshot, emptyMap(), ceiling))
        assertFalse(TrainingInputRules.requiresHandwriting(moon, snapshot, emptyMap(), ceiling))
        // Missing JLPT ≡ N1 → harder than N5 → Prefilled
        assertFalse(TrainingInputRules.requiresHandwriting(fire, snapshot, emptyMap(), ceiling))
        assertTrue(TrainingInputRules.requiresHandwriting(fire, snapshot, emptyMap(), JlptLevel.N1))
    }

    @Test
    fun chapterAndJlptGatesCombineWithAnd() {
        val caps = mapOf(1 to 3)
        val ceiling = JlptLevel.N5
        // Within chapters (sun ch1) but harder JLPT would still need both; sun is N5 → User
        assertTrue(TrainingInputRules.requiresHandwriting(sun, snapshot, caps, ceiling))
        // Within JLPT if ceiling were N3, but beyond chapters (moon ch5) → Prefilled
        assertFalse(TrainingInputRules.requiresHandwriting(moon, snapshot, caps, JlptLevel.N3))
        // Beyond JLPT (moon N3 vs N5) even within empty chapters → Prefilled
        assertFalse(TrainingInputRules.requiresHandwriting(moon, snapshot, emptyMap(), ceiling))
        // Beyond chapters even when JLPT gate is off → Prefilled
        assertFalse(TrainingInputRules.requiresHandwriting(moon, snapshot, caps, null))
        // Not in book (fire) even when JLPT is N1 (matches fire) → Prefilled
        assertFalse(TrainingInputRules.requiresHandwriting(fire, snapshot, caps, JlptLevel.N1))
    }

    @Test
    fun wordNeedsAtLeastOneWritableCell() {
        assertTrue(TrainingInputRules.hasWritableCell(listOf(false, true, false)))
        assertFalse(TrainingInputRules.hasWritableCell(listOf(false, false)))
    }
}
