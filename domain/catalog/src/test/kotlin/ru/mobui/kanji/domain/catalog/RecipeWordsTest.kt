package ru.mobui.kanji.domain.catalog

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class RecipeWordsTest {
    private val sun = '日'.code
    private val moon = '月'.code
    private val snapshot = CatalogSnapshot(1, CatalogContent(
        listOf(CatalogSource(1, "Book", emptyMap(), sourceType = "book"), CatalogSource(2, "Other", emptyMap(), sourceType = "book"),
            CatalogSource(-1, "Personal", emptyMap(), sourceType = "personal")),
        listOf(CatalogWord(1, "月日", "", emptyMap()), CatalogWord(-2, "月", "", emptyMap()), CatalogWord(-3, "かな", "", emptyMap())),
        listOf(sun, moon).map { CatalogKanji(it, String(Character.toChars(it)), 4, null, false, true, false, emptyMap(), emptyList(), emptyList()) },
        emptyList(), listOf(CatalogPlacement(1, sun, 1, 1), CatalogPlacement(1, moon, 2, 1), CatalogPlacement(2, moon, 1, 1)),
        listOf(CatalogLink(1, 1, sun), CatalogLink(2, 1, moon), CatalogLink(-1, -2, null), CatalogLink(-1, -3, null))))
    private fun ids(material: RecipeMaterial, tags: List<WordTagAssignment> = emptyList()) = snapshot.materialWords(material, tags).map { it.id }.toSet()

    @Test fun bookUsesSameRealStudyLinkNotWrittenSymbolsOrOtherSource() {
        val book = RecipeMaterial("b", MaterialType.Book, sourceId = 1)
        assertEquals(setOf(1), ids(book))
        assertTrue(ids(book.copy(chapters = setOf(2))).isEmpty())
        assertTrue(ids(book.copy(kanjiIds = setOf(moon))).isEmpty())
        assertEquals(setOf(1), ids(book.copy(chapters = setOf(1, 2), kanjiIds = setOf(sun, moon))))
        assertTrue(ids(book.copy(sourceId = -1)).isEmpty())
    }
    @Test fun personalTagOrAndLiteralKanjiOrNoBorrowedBookWords() {
        val personal = RecipeMaterial("p", MaterialType.Dictionary, sourceId = -1)
        val tags = listOf(WordTagAssignment(1, setOf(1)), WordTagAssignment(-2, setOf(2)), WordTagAssignment(-3, setOf(3)))
        assertEquals(setOf(-2, -3), ids(personal))
        assertEquals(setOf(-2), ids(personal.copy(tagIds = setOf(1, 2), kanjiIds = setOf(sun, moon)), tags))
        assertTrue(ids(personal.copy(tagIds = setOf(3), kanjiIds = setOf(moon)), tags).isEmpty())
        assertTrue(ids(personal.copy(sourceId = 1)).isEmpty())
    }
    @Test fun unionDeduplicatesByIdAndEmptyGlobalRestrictionIncludesKana() {
        val materials = listOf(RecipeMaterial("k", MaterialType.Kanji), RecipeMaterial("w", MaterialType.Words, wordIds = setOf(1, -2, 999)),
            RecipeMaterial("b", MaterialType.Book, sourceId = 1))
        assertEquals(listOf(1, -2, -3), snapshot.recipeWords(materials, emptyList()).map { it.id })
        assertEquals(setOf(1, -2), ids(materials[0].copy(kanjiIds = setOf(moon))))
        assertTrue(ids(materials[1].copy(wordIds = emptySet())).isEmpty())
    }
}
