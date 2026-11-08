package ru.mobui.kanji.feature.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeTaskLabelsTest {
    @Test
    fun explicitTaskCountWins() {
        assertEquals(20, effectiveRecipeTaskCount(20, 150))
    }

    @Test
    fun nullTaskCountUsesPoolSize() {
        assertEquals(150, effectiveRecipeTaskCount(null, 150))
        assertEquals(0, effectiveRecipeTaskCount(null, 0))
    }
}
