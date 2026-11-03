package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.CatalogChapter
import ru.mobui.kanji.feature.catalog.components.CatalogFilterChip
import ru.mobui.kanji.feature.catalog.components.ChapterChoiceCard
import ru.mobui.kanji.feature.catalog.components.radicalContainerColor

class CatalogVisualContractsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun lockedChapterFilterKeepsResetAvailable() {
        var cleared = false
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val clear = context.getString(R.string.catalog_clear_filter, "Глава 1")
        compose.setContent {
            KanjiTheme {
                CatalogFilterChip("Глава 1", {}, { cleared = true }, openEnabled = false)
            }
        }
        compose.onNodeWithText("Глава 1").assertIsNotEnabled()
        compose.onNodeWithContentDescription(clear).performClick()
        compose.runOnIdle { assertTrue(cleared) }
    }

    @Test
    fun chapterChoiceHasNoDecorativeCheckmarkAndCanBeSelected() {
        var picked = false
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val chapterTitle = context.getString(R.string.catalog_chapter, 1)
        compose.setContent {
            KanjiTheme {
                Box(Modifier.width(320.dp)) {
                    ChapterChoiceCard(
                        CatalogChapter(1, 10, 45, "日 月 木 山 川 田 人 口 車 門"),
                        showWordCount = false,
                        selected = true,
                    ) { picked = true }
                }
            }
        }
        compose.onNodeWithText("✓").assertDoesNotExist()
        compose.onNodeWithText(chapterTitle).performClick()
        compose.runOnIdle { assertTrue(picked) }
    }

    @Test
    fun fourRadicalTonesRepeatByStrokeCountInBothThemes() {
        var light = emptyList<Color>()
        var dark = emptyList<Color>()
        compose.setContent {
            KanjiTheme(darkTheme = false) {
                light = (1..5).map { radicalContainerColor(it) }
            }
            KanjiTheme(darkTheme = true) {
                dark = (1..5).map { radicalContainerColor(it) }
            }
        }
        compose.runOnIdle {
            for (colors in listOf(light, dark)) {
                assertEquals(4, colors.take(4).distinct().size)
                assertEquals(colors.first(), colors.last())
            }
        }
    }
}
