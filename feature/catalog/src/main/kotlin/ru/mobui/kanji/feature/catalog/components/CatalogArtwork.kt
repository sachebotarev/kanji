package ru.mobui.kanji.feature.catalog.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource

/** The prototype's dark ink mask, expressed through stable native color filters. */
@Composable
internal fun CatalogArtwork(resource: Int, modifier: Modifier = Modifier, mascot: Boolean = false) {
    val dark = MaterialTheme.colorScheme.background.luminance() < DARK_BACKGROUND_LUMINANCE
    val soften = ColorMatrix().apply { setToSaturation(COVER_SATURATION) }
    if (mascot) {
        val factor = if (dark) DARK_COVER_GAIN else LIGHT_COVER_GAIN
        val offset = if (dark) DARK_COVER_OFFSET else LIGHT_COVER_OFFSET
        for (row in 0..2) {
            for (column in 0..LAST_COLOR_COMPONENT) soften[row, column] *= factor
            soften[row, COLOR_OFFSET_COMPONENT] = offset
        }
    }
    Box(modifier) {
        Image(
            painterResource(resource),
            null,
            Modifier.matchParentSize(),
            colorFilter = if (mascot) ColorFilter.colorMatrix(soften) else null,
        )
        if (dark) {
            val mask =
                ColorMatrix(
                    floatArrayOf(
                        0f,
                        0f,
                        0f,
                        0f,
                        GLYPH_MARK_SIZE,
                        0f,
                        0f,
                        0f,
                        0f,
                        GLYPH_OUTLINE_SIZE,
                        0f,
                        0f,
                        0f,
                        0f,
                        GLYPH_SHADOW_SIZE,
                        -GLYPH_SHADOW_X,
                        -GLYPH_SHADOW_Y,
                        -1f,
                        2f,
                        0f,
                    ),
                )
            Image(
                painterResource(resource),
                null,
                Modifier.matchParentSize(),
                colorFilter = ColorFilter.colorMatrix(mask),
            )
        }
    }
}

private const val DARK_BACKGROUND_LUMINANCE = .5f
private const val COVER_SATURATION = .82f
private const val DARK_COVER_GAIN = .9f
private const val LIGHT_COVER_GAIN = .936f
private const val DARK_COVER_OFFSET = 12.75f
private const val LIGHT_COVER_OFFSET = 13.26f
private const val LAST_COLOR_COMPONENT = 3
private const val COLOR_OFFSET_COMPONENT = 4
private const val GLYPH_MARK_SIZE = 54f
private const val GLYPH_OUTLINE_SIZE = 84f
private const val GLYPH_SHADOW_SIZE = 61f

private const val GLYPH_SHADOW_X = 3f
private const val GLYPH_SHADOW_Y = 4f
