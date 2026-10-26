package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** Decorative wallpaper, deliberately too faint to compete with catalog content. */
@Composable
internal fun CatalogBackdrop(modifier: Modifier = Modifier) {
    val paw = ImageBitmap.imageResource(R.drawable.catalog_paw)
    val tint = ColorFilter.tint(MaterialTheme.colorScheme.primary.copy(alpha = .045f))
    Canvas(modifier) {
        val step = 148.dp.toPx()
        val stamp = 38.dp.roundToPx()
        for (row in 0..(size.height / step).toInt()) {
            for (column in -1..(size.width / step).toInt()) {
                val x = column * step + if (row % 2 == 0) step * .25f else step * .75f
                val y = row * step + step * .25f
                rotate(if ((row + column) % 2 == 0) -24f else 22f,
                    pivot = androidx.compose.ui.geometry.Offset(x + stamp / 2f, y + stamp / 2f)) {
                    drawImage(paw, dstOffset = IntOffset(x.toInt(), y.toInt()),
                        dstSize = IntSize(stamp, stamp), colorFilter = tint)
                }
            }
        }
    }
}
