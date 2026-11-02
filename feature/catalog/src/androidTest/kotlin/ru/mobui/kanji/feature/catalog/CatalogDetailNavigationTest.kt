package ru.mobui.kanji.feature.catalog

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogLink
import ru.mobui.kanji.domain.catalog.CatalogPlacement
import ru.mobui.kanji.domain.catalog.CatalogRadical
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogSource
import ru.mobui.kanji.domain.catalog.CatalogWord

class CatalogDetailNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun wordKanjiRadicalAndRelatedWordsKeepTheSourceAndBackStack() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration =
            Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag("ru"))
            }
        val localized = context.createConfigurationContext(configuration)
        val one =
            CatalogKanji(
                19968,
                "一",
                1,
                1,
                false,
                false,
                false,
                mapOf("ru" to listOf("один")),
                emptyList(),
                emptyList(),
            )
        val data =
            CatalogSnapshot(
                1,
                content =
                    CatalogContent(
                        listOf(CatalogSource(1, "Test", emptyMap())),
                        listOf(
                            CatalogWord(1, "一", "いち", mapOf("ru" to "один")),
                            CatalogWord(2, "一つ", "ひとつ", mapOf("ru" to "один предмет")),
                        ),
                        listOf(one),
                        listOf(CatalogRadical(1, "一", 1, mapOf("ru" to "Единица"))),
                        listOf(CatalogPlacement(1, 19968, 1, 3)),
                        listOf(CatalogLink(1, 1, 19968), CatalogLink(1, 2, 19968)),
                    ),
            )
        val repository =
            object : CatalogRepository {
                override suspend fun load() = data
            }
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides configuration,
            ) {
                KanjiTheme { CatalogRoute(repository, {}, initialSourceId = 1) }
            }
        }
        compose.onNodeWithText("Слова").performClick()
        compose.onNodeWithTag("word-1").performClick()
        compose.onNodeWithTag("kanji-19968").performClick()
        compose.onNodeWithTag("kanji-key").assertIsDisplayed().performClick()
        compose.onNodeWithTag("kanji-19968").assertIsDisplayed()
        compose.onNodeWithTag("catalog-back").performClick()
        compose.onNodeWithText("Связанные слова").assertIsDisplayed().performClick()
        compose.onNodeWithTag("word-2").performClick()
        compose.onNodeWithTag("word-written").assertTextEquals("一つ")
        compose.onNodeWithTag("catalog-back").performClick()
        compose.onNodeWithTag("word-1").assertIsDisplayed()
        compose.onNodeWithTag("catalog-back").performClick()
        compose.onNodeWithTag("kanji-key").assertExists()
        compose.onNodeWithTag("catalog-back").performClick()
        compose.onNodeWithTag("word-written").assertTextEquals("一")
    }
}
