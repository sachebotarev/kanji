package ru.mobui.kanji.feature.catalog

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.*

class CatalogScrollRestorationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun kanjiAndWordTabsKeepTheirOwnListPositions() {
        val kanji = (0 until 40).map { offset ->
            val id = 19968 + offset
            CatalogKanji(id, id.toChar().toString(), 1, 1, false, false, false,
                mapOf("ru" to listOf("значение $offset")), emptyList(), emptyList())
        }
        val words = kanji.mapIndexed { index, item -> CatalogWord(index + 1, item.symbol, "いち", mapOf("ru" to "слово $index")) }
        val data = CatalogSnapshot(1, listOf(CatalogSource(1, "Test", emptyMap())), words, kanji, emptyList(),
            kanji.mapIndexed { index, item -> CatalogPlacement(1, item.id, index + 1, 3) },
            kanji.mapIndexed { index, item -> CatalogLink(1, index + 1, item.id) })
        val repository = object : CatalogRepository { override suspend fun load() = data }
        compose.setContent { KanjiTheme { CatalogRoute(repository, {}, initialSourceId = 1) } }
        compose.onNodeWithTag("catalog-tab-1").performClick()
        compose.onNodeWithTag("kanji-grid").performScrollToIndex(30)
        compose.onNodeWithTag("kanji-19998").assertIsDisplayed()
        compose.onNodeWithTag("catalog-tab-2").performClick()
        compose.onNodeWithTag("word-list").performScrollToIndex(30)
        compose.onNodeWithTag("word-31").assertIsDisplayed()
        compose.onNodeWithTag("catalog-tab-1").performClick()
        compose.onNodeWithTag("kanji-19998").assertIsDisplayed()
        compose.onNodeWithTag("catalog-tab-2").performClick()
        compose.onNodeWithTag("word-31").assertIsDisplayed()
    }
}
