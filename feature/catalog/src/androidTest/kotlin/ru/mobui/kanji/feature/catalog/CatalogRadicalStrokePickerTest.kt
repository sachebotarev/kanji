package ru.mobui.kanji.feature.catalog

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import java.util.Locale
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogRadical
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSnapshot

class CatalogRadicalStrokePickerTest {
    @get:Rule val compose = createComposeRule()

    private fun start(restoration: StateRestorationTester? = null) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("ru")) }
        val localized = context.createConfigurationContext(config)
        val snapshot = CatalogSnapshot(1, CatalogContent(emptyList(), emptyList(), emptyList(),
            listOf(CatalogRadical(1, "一", 1, mapOf("ru" to "Один")),
                CatalogRadical(2, "二", 2, mapOf("ru" to "Два")),
                CatalogRadical(3, "口", 3, mapOf("ru" to "Рот"))), emptyList(), emptyList()))
        val repository = object : CatalogRepository { override suspend fun load() = snapshot }
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides config) {
                KanjiTheme { CatalogRoute(repository, {}) }
            }
        }
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
        compose.onNodeWithTag("catalog-all-radicals").performClick()
    }

    @Test fun draftCancelsAndMultipleValuesApplyRestoreAndClear() {
        val restoration = StateRestorationTester(compose)
        start(restoration)
        compose.onNodeWithTag("catalog-filter-strokes").performClick()
        compose.onNodeWithTag("facet-option-1").performClick()
        compose.onNodeWithTag("catalog-back").performClick()
        compose.onNodeWithTag("radical-3").assertIsDisplayed()
        compose.onNodeWithTag("catalog-filter-strokes").performClick()
        compose.onNodeWithTag("facet-option-1").assertIsNotSelected().performClick()
        compose.onNodeWithTag("facet-option-2").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("facet-option-1").assertIsSelected()
        compose.onNodeWithTag("facet-option-2").assertIsSelected()
        compose.onNodeWithTag("facet-apply").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("radical-1").assertIsDisplayed()
        compose.onNodeWithTag("radical-2").assertIsDisplayed()
        compose.onNodeWithTag("radical-3").assertDoesNotExist()
        compose.onNodeWithText("1 черта、2 черты").assertIsDisplayed()
        compose.onNodeWithContentDescription("Сбросить: 1 черта、2 черты").performClick()
        compose.onNodeWithTag("radical-3").assertIsDisplayed()
    }

    @Test fun searchKeepsHiddenChoicesAndEmptyApplyRemovesRestriction() {
        start()
        compose.onNodeWithTag("catalog-filter-strokes").performClick()
        compose.onNodeWithTag("facet-option-1").performClick()
        compose.onNodeWithTag("catalog-search-toggle").performClick()
        compose.onNodeWithTag("facet-search").performTextInput("２")
        compose.onNodeWithTag("facet-option-1").assertDoesNotExist()
        compose.onNodeWithTag("facet-option-2").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        compose.onNodeWithTag("facet-apply").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        compose.onNodeWithTag("radical-1").assertIsDisplayed()
        compose.onNodeWithTag("radical-2").assertIsDisplayed()
        compose.onNodeWithTag("radical-3").assertDoesNotExist()
        compose.onNodeWithTag("catalog-filter-strokes").performClick()
        compose.onNodeWithTag("facet-clear").performClick()
        compose.onNodeWithTag("facet-apply").performClick()
        compose.onNodeWithTag("radical-3").assertIsDisplayed()
    }
}
