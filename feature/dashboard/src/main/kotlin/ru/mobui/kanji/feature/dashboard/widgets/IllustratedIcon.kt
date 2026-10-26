package ru.mobui.kanji.feature.dashboard.widgets

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.imageResource
import ru.mobui.kanji.feature.dashboard.R

internal enum class DashboardIcon(val resource: Int, val opticalScale: Float = 1f) {
    Settings(R.drawable.art_settings), Textbooks(R.drawable.art_personal, 1.1f),
    Personal(R.drawable.art_textbooks_v4, 1.1f), History(R.drawable.art_history_v4, 1.1f),
    Builder(R.drawable.art_builder, 1.15f), Support(R.drawable.art_support, 1.3f),
    Start(R.drawable.art_start_v4), Expand(R.drawable.art_paw_v4),
}

/** Palette-only transform: forest contour becomes sage while keeping warm, muted fills.
 * Alpha and geometry are identical in both themes. No extra perimeter is drawn.
 */
private val NightIllustrationPalette = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
    .580645f, 0f, 0f, 0f, 106.93549f,
    0f, .492147f, 0f, 0f, 128.48691f,
    0f, 0f, .580311f, 0f, 103.66321f,
    0f, 0f, 0f, 1f, 0f,
)))

/** A single source bitmap and optical scale per icon, shared by both themes. */
@Composable
internal fun IllustratedIcon(kind: DashboardIcon, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < .3f
    val monochrome = kind == DashboardIcon.Start || kind == DashboardIcon.Expand
    val filter = when {
        monochrome -> ColorFilter.tint(if (kind == DashboardIcon.Start) colors.onPrimaryContainer else colors.primary)
        dark -> NightIllustrationPalette
        else -> null
    }
    Image(ImageBitmap.imageResource(kind.resource), contentDescription, modifier.graphicsLayer {
        scaleX = kind.opticalScale
        scaleY = kind.opticalScale
        if (kind == DashboardIcon.Expand) rotationZ = 45f
    }, colorFilter = filter, filterQuality = FilterQuality.High)
}
