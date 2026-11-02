package ru.mobui.kanji.feature.catalog

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
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val soften = ColorMatrix().apply { setToSaturation(.82f) }
    if (mascot) {
        val factor = if (dark) .9f else .936f
        val offset = if (dark) 12.75f else 13.26f
        for (row in 0..2) {
            for (column in 0..3) soften[row, column] *= factor
            soften[row, 4] = offset
        }
    }
    Box(modifier) {
        Image(painterResource(resource), null, Modifier.matchParentSize(), colorFilter = if (mascot) ColorFilter.colorMatrix(soften) else null)
        if (dark) {
            val mask = ColorMatrix(floatArrayOf(
                0f, 0f, 0f, 0f, 54f,
                0f, 0f, 0f, 0f, 84f,
                0f, 0f, 0f, 0f, 61f,
                -3f, -4f, -1f, 2f, 0f,
            ))
            Image(painterResource(resource), null, Modifier.matchParentSize(), colorFilter = ColorFilter.colorMatrix(mask))
        }
    }
}
