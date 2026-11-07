package ru.mobui.kanji.feature.dashboard.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mobui.kanji.feature.dashboard.R

class DashboardArtworkTest {
    @Test
    fun homeStationeryAssetsAreDistinctWithoutOpticalEnlargement() {
        assertEquals(R.drawable.art_home_textbook, DashboardIcon.Textbooks.resource)
        assertEquals(R.drawable.art_home_references, DashboardIcon.References.resource)
        assertEquals(R.drawable.art_home_dictionary, DashboardIcon.Personal.resource)
        assertEquals(R.drawable.art_home_training_recipes, DashboardIcon.RecipeBook.resource)
        val icons =
            listOf(
                DashboardIcon.Textbooks,
                DashboardIcon.References,
                DashboardIcon.Personal,
                DashboardIcon.RecipeBook,
            )
        assertEquals(4, icons.map { it.resource }.toSet().size)
        icons.forEach {
            assertTrue(it.stationery)
            assertEquals(1f, it.opticalScale, 0f)
        }
    }

    @Test
    fun settingsUsesSharedStationeryPalettePath() {
        assertEquals(R.drawable.art_settings, DashboardIcon.Settings.resource)
        assertTrue(DashboardIcon.Settings.stationery)
        assertEquals(1f, DashboardIcon.Settings.opticalScale, 0f)
    }
}
