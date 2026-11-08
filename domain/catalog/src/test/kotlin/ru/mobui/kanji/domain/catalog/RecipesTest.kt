package ru.mobui.kanji.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RecipesTest {
    private val material = RecipeMaterial("material", MaterialType.Words, wordIds = setOf(-1))
    private val recipe = Recipe("recipe", listOf(RecipeName("ru", "  Повторение  ")), listOf(material))

    @Test fun recipesNormalizeNamesAndAcceptPersonalNegativeWordIds() {
        assertEquals("Повторение", recipe.validated().names.single().name)
        assertEquals(setOf(-1), recipe.validated().materials.single().wordIds)
    }
    @Test fun rejectsInvalidCountsAndTypeSpecificConstraints() {
        listOf(recipe.copy(taskCount = 0), recipe.copy(secondsPerSymbol = -1),
            recipe.copy(materials = emptyList()),
            recipe.copy(materials = listOf(material.copy(tagIds = setOf(1)))),
            recipe.copy(materials = listOf(material.copy(chapters = setOf(1)))),
            recipe.copy(materials = listOf(material.copy(sourceId = -1))),
            recipe.copy(materials = listOf(material.copy(wordIds = emptySet()))),
            recipe.copy(materials = listOf(material, material)),
        ).forEach { invalid -> assertThrows(IllegalArgumentException::class.java) { invalid.validated() } }
    }
    @Test fun localizedTagNamesFallBackToRussianNotInterfaceLabels() {
        val tag = DictionaryTag(1, TagColor.Gold, listOf(TagTranslation("ru", "Путешествия"), TagTranslation("en", "Travel")))
        assertEquals("Travel", tag.name("en-US"))
        assertEquals("Путешествия", tag.name("de"))
    }

    @Test fun localizedRecipeNamesFallBackToRussian() {
        val named = recipe.copy(names = listOf(RecipeName("ru", "Луна"), RecipeName("en", "Moon")))
        assertEquals("Moon", named.name("en-US"))
        assertEquals("Луна", named.name("de"))
        assertEquals("Луна", named.name("ru"))
    }
    @Test fun tagTranslationsStayExtensibleAndRejectDuplicateLocales() {
        assertEquals("pt-BR", TagDraft(TagColor.Neutral, listOf(TagTranslation("pt-br", " Viagem "))).validated().translations.single().languageTag)
        assertThrows(IllegalArgumentException::class.java) {
            TagDraft(TagColor.Blue, listOf(TagTranslation("ru", "Один"), TagTranslation("RU", "Два"))).validated()
        }
        assertThrows(IllegalArgumentException::class.java) {
            TagDraft(TagColor.Blue, listOf(TagTranslation("ru", "  "))).validated()
        }
    }
}
