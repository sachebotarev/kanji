package ru.mobui.kanji.feature.catalog

import ru.mobui.kanji.feature.catalog.components.CatalogFilterChip
import ru.mobui.kanji.feature.catalog.theme.CatalogTheme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.left
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.right
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme

class CatalogFilterChipTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun selectedChipWrapsItsContentAndKeepsResetIndependent() {
        var opened = false
        var cleared = false
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val reset = context.getString(R.string.catalog_clear_filter, "Ключ 水")
        compose.setContent {
            KanjiTheme {
                CatalogTheme {
                    Box(Modifier.width(260.dp)) {
                        CatalogFilterChip(
                            "Ключ 水",
                            { opened = true },
                            { cleared = true },
                            Modifier.testTag("chip"),
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("chip").assertIsSelected()
        val bounds = compose.onNodeWithTag("chip").getUnclippedBoundsInRoot()
        assertTrue(bounds.right - bounds.left < 260.dp)
        compose
            .onNodeWithContentDescription(reset)
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertTrue(cleared)
        assertFalse(opened)
        compose.onNodeWithText("Ключ 水").performClick()
        assertTrue(opened)
    }
}
