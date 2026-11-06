package ru.mobui.kanji.feature.catalog

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogPlacement
import ru.mobui.kanji.domain.catalog.CatalogRadical
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogSource

class CatalogSectionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun textbooksExcludePersonalSourcesAndReferencesAndKeepChapterNavigation() {
        show(CatalogSection.Textbooks)
        waitTag("source-1")
        compose.onNodeWithText("Учебники").assertIsDisplayed()
        compose.onNodeWithTag("source-2").assertExists()
        compose.onNodeWithTag("source-3").assertDoesNotExist()
        compose.onNodeWithTag("catalog-all-kanji").assertDoesNotExist()
        compose.onNodeWithTag("source-1").performClick()
        waitTag("chapter-3")
        compose.onNodeWithTag("catalog-back").performClick()
        waitTag("source-1")
    }

    @Test
    fun referencesOpenBothGlobalListsAndRootBackExits() {
        var exits = 0
        show(CatalogSection.References) { exits++ }
        waitTag("catalog-all-kanji")
        compose.onNodeWithText("Справочники").assertIsDisplayed()
        compose.onNodeWithTag("source-1").assertDoesNotExist()
        compose.onNodeWithTag("catalog-all-kanji").performClick()
        waitTag("kanji-26376")
        compose.onNodeWithTag("kanji-19968").assertExists()
        compose.onNodeWithTag("catalog-back").performClick()
        waitTag("catalog-all-radicals")
        compose.onNodeWithTag("catalog-all-radicals").performClick()
        waitTag("radical-1")
        compose.onNodeWithTag("catalog-back").performClick()
        waitTag("catalog-all-kanji")
        compose.onNodeWithTag("catalog-back").performClick()
        compose.runOnIdle { assertEquals(1, exits) }
    }

    @Test
    fun restoredReferenceDetailReturnsToReferencesNotTextbooks() {
        val restoration = StateRestorationTester(compose)
        show(CatalogSection.References, restoration = restoration)
        waitTag("catalog-all-kanji")
        compose.onNodeWithTag("catalog-all-kanji").performClick()
        waitTag("kanji-26376")
        compose.onNodeWithTag("kanji-26376").performClick()
        waitTag("kanji-symbol")
        restoration.emulateSavedInstanceStateRestore()
        waitTag("kanji-symbol")
        compose.onNodeWithTag("catalog-back").performClick()
        waitTag("kanji-26376")
        compose.onNodeWithTag("catalog-back").performClick()
        waitTag("catalog-all-kanji")
        compose.onNodeWithTag("source-1").assertDoesNotExist()
    }

    @Test
    fun textbookIntroAndCardsKeepApprovedDimensions() {
        show(CatalogSection.Textbooks)
        waitTag("source-1")
        val intro = compose.onNodeWithTag("catalog-intro").getUnclippedBoundsInRoot()
        val mascot = compose.onNodeWithTag("catalog-intro-mascot").getUnclippedBoundsInRoot()
        val first = compose.onNodeWithTag("source-1").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithTag("source-2").getUnclippedBoundsInRoot()
        assertEquals(112f, (intro.bottom - intro.top).value, 1f)
        assertEquals(96f, (mascot.right - mascot.left).value, 1f)
        assertEquals(24f, (first.top - intro.bottom).value, 1f)
        assertEquals(80f, (first.bottom - first.top).value, 1f)
        assertEquals(12f, (second.top - first.bottom).value, 1f)
        assertEquals(20.dp.value, first.left.value, 1f)
    }

    private fun show(section: CatalogSection, restoration: StateRestorationTester? = null, onBack: () -> Unit = {}) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("ru")) }
        val localized = context.createConfigurationContext(configuration)
        val snapshot = CatalogSnapshot(1, CatalogContent(
            sources = listOf(
                CatalogSource(1, "Book one", emptyMap(), sourceType = "textbook"),
                CatalogSource(2, "Book two", emptyMap(), sourceType = "book"),
                CatalogSource(3, "Personal", emptyMap(), sourceType = "personal"),
            ),
            words = emptyList(),
            kanji = listOf(
                CatalogKanji(19968, "一", 1, 1, false, false, false, mapOf("ru" to listOf("один")), emptyList(), emptyList()),
                CatalogKanji(26376, "月", 4, 1, false, false, false, mapOf("ru" to listOf("луна")), emptyList(), emptyList()),
            ),
            radicals = listOf(CatalogRadical(1, "一", 1, mapOf("ru" to "Единица"))),
            placements = listOf(CatalogPlacement(1, 19968, 1, 3)),
            links = emptyList(),
        ))
        val repository = object : CatalogRepository { override suspend fun load() = snapshot }
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration,
                LocalDensity provides Density(context.resources.displayMetrics.density, 1f)) {
                KanjiTheme { CatalogRoute(repository, onBack, section = section) }
            }
        }
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
    }

    private fun waitTag(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }
}
