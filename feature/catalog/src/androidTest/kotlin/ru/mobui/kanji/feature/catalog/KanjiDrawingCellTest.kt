package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.handwriting.ReferenceDrawing
import ru.mobui.kanji.domain.handwriting.ReferencePoint
import ru.mobui.kanji.feature.catalog.detail.KanjiDrawing

class KanjiDrawingCellTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun horizontalStrokeAndFallbackKeepTheSameSquareCell() {
        val drawing = mutableStateOf<ReferenceDrawing?>(
            ReferenceDrawing(listOf(listOf(ReferencePoint(0.1f, 0.5f), ReferencePoint(0.9f, 0.5f)))),
        )
        compose.setContent {
            KanjiTheme {
                Box(Modifier.width(260.dp)) { KanjiDrawing("一", drawing.value) }
            }
        }
        compose.onNodeWithContentDescription("一")
            .assertWidthIsEqualTo(188.dp).assertHeightIsEqualTo(188.dp)
        compose.runOnIdle { drawing.value = null }
        compose.onNodeWithContentDescription("一")
            .assertWidthIsEqualTo(188.dp).assertHeightIsEqualTo(188.dp)
    }

    @Test
    fun cellStaysSquareWhenAvailableWidthIsSmaller() {
        compose.setContent {
            KanjiTheme {
                Box(Modifier.width(140.dp)) { KanjiDrawing("日", null) }
            }
        }
        compose.onNodeWithContentDescription("日")
            .assertWidthIsEqualTo(140.dp).assertHeightIsEqualTo(140.dp)
    }
}
