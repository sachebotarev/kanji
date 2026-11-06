package ru.mobui.kanji.feature.dashboard.builder

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.mobui.kanji.domain.catalog.MaterialType

class RecipeMaterialTagsTest {
    @Test fun chapterTagKeepsFirstThreeAndAddsMoreSuffix() {
        assertEquals("Главы 3, 4, 5", chapterTagLabel(listOf(5, 3, 4), "Главы") { "и ещё $it" })
        assertEquals(
            "Главы 3, 4, 5 и ещё 3",
            chapterTagLabel(listOf(8, 3, 4, 5, 6, 7), "Главы") { "и ещё $it" },
        )
        assertEquals(
            "Chapters 1, 2, 3 and 2 more",
            chapterTagLabel(listOf(1, 2, 3, 4, 5), "Chapters") { "and $it more" },
        )
    }

    @Test fun dictionaryTagsKeepFirstTwoNames() {
        assertEquals("A, B", dictionaryTagsLabel(listOf("A", "B")) { "Ещё $it" })
        assertEquals("A, B · Ещё 2", dictionaryTagsLabel(listOf("A", "B", "C", "D")) { "Ещё $it" })
    }

    @Test fun compactMaterialTagJoinsFiltersAndKeepsType() {
        assertEquals(
            "Tobira · Главы 3" to MaterialType.Book,
            compactMaterialTag(
                listOf(
                    "Tobira" to MaterialType.Book,
                    "Главы 3" to null,
                ),
            ),
        )
        assertEquals(
            "Кандзи: 1" to MaterialType.Kanji,
            compactMaterialTag(listOf("Кандзи: 1" to MaterialType.Kanji)),
        )
    }
}
