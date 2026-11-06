package ru.mobui.kanji.feature.dictionary

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.core.designsystem.inputShape

class InputShapeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun allCornersUseTheGeneratedThemeDimension() {
        lateinit var shape: CornerBasedShape
        lateinit var density: Density
        compose.setContent {
            KanjiTheme {
                val currentShape = MaterialTheme.inputShape
                val currentDensity = LocalDensity.current
                SideEffect { shape = currentShape; density = currentDensity }
            }
        }
        compose.runOnIdle {
            val expected = InstrumentationRegistry.getInstrumentation().targetContext.resources
                .getDimension(ru.mobui.kanji.core.designsystem.R.dimen.kanji_input_corner_radius)
            val size = Size(400f, 200f)
            listOf(shape.topStart, shape.topEnd, shape.bottomStart, shape.bottomEnd).forEach {
                assertEquals(expected, it.toPx(size, density), 0.01f)
            }
        }
    }
}
