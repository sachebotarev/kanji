package ru.mobui.kanji.feature.catalog

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogPlacement
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogSource

class CatalogLocaleRestorationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun languageChangePreservesBrowseAndFilterSearchAndSelectedStrokes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val language = mutableStateOf("ru")
        val data = CatalogSnapshot(
            1,
            CatalogContent(
                listOf(CatalogSource(1, "Учебник", mapOf("en" to "Textbook"))),
                emptyList(), listOf(kanji(1, "一", 1), kanji(2, "二", 2)), emptyList(),
                listOf(CatalogPlacement(1, 1, 1, 1), CatalogPlacement(1, 2, 1, 2)), emptyList(),
            ),
        )
        val repository = object : CatalogRepository {
            override suspend fun load() = data
        }
        compose.setContent {
            val configuration = Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language.value))
            }
            CompositionLocalProvider(
                LocalContext provides context.createConfigurationContext(configuration),
                LocalConfiguration provides configuration,
            ) {
                KanjiTheme { CatalogRoute(repository, {}, initialSourceId = 1) }
            }
        }
        compose.onNodeWithText("Кандзи").performClick()
        compose.onNodeWithText("Все черты").performClick()
        compose.onNodeWithTag("facet-option-1").performClick()
        compose.onNodeWithTag("facet-apply").performClick()
        compose.onNodeWithContentDescription("Открыть поиск").performClick()
        compose.onNodeWithTag("catalog-search").performTextInput("一")
        compose.onNodeWithTag("catalog-search").performImeAction()
        compose.runOnIdle { language.value = "en" }
        compose.onNodeWithText("Textbook").assertIsDisplayed()
        compose.onNodeWithTag("catalog-search").assertTextContains("一")
        compose.onNodeWithContentDescription("Clear: 1 stroke").assertIsDisplayed()
        compose.onNodeWithTag("kanji-1").assertIsDisplayed()
        compose.onNodeWithTag("kanji-2").assertDoesNotExist()
        compose.onNodeWithTag("catalog-filter-chapter").performClick()
        compose.onNodeWithTag("catalog-search-toggle").performClick()
        compose.onNodeWithTag("chapter-search").performTextInput("1")
        compose.onNodeWithTag("chapter-search").performImeAction()
        compose.runOnIdle { language.value = "de" }
        compose.onNodeWithTag("chapter-search").assertTextContains("1")
        compose.onNodeWithTag("chapter-1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithTag("catalog-search").assertTextContains("一")
        compose.onNodeWithContentDescription("Сбросить: 1 черта").assertIsDisplayed()
    }

    private fun kanji(id: Int, symbol: String, strokes: Int) = CatalogKanji(
        id, symbol, strokes, 1, false, false, false, emptyMap(), emptyList(), emptyList(),
    )
}
