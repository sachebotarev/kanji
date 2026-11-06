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

/**
 * Palette-only transform: forest contour becomes sage while keeping warm, muted fills. Alpha and
 * geometry are identical in both themes. No extra perimeter is drawn.
 */
private val NightIllustrationPalette =
    ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                .580645f,
                0f,
                0f,
                0f,
                106.93549f,
                0f,
                .492147f,
                0f,
                0f,
                128.48691f,
                0f,
                0f,
                .580311f,
                0f,
                103.66321f,
                0f,
                0f,
                0f,
                1f,
                0f,
            ),
        ),
    )

/** The approved HTML stationery filter: brightness(1.25) saturate(.7), preserving alpha. */
private val NightStationeryPalette = ColorFilter.colorMatrix(
    ColorMatrix().apply {
        setToSaturation(.7f)
        this *= ColorMatrix().apply { setToScale(1.25f, 1.25f, 1.25f, 1f) }
    },
)

/** Source artwork and optical scale are shared by both themes. */
@Composable
internal fun IllustratedIcon(
    kind: DashboardIcon,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < DARK_BACKGROUND_LUMINANCE
    val monochrome = kind == DashboardIcon.Start || kind == DashboardIcon.Expand
    val filter =
        when {
            monochrome ->
                ColorFilter.tint(
                    if (kind == DashboardIcon.Start) colors.onPrimaryContainer else colors.primary,
                )
            kind.stationery -> if (dark) NightStationeryPalette else null
            dark -> NightIllustrationPalette
            else -> null
        }
    Image(
        ImageBitmap.imageResource(kind.resource),
        contentDescription,
        modifier.graphicsLayer {
            scaleX = kind.opticalScale
            scaleY = kind.opticalScale
            if (kind == DashboardIcon.Expand) rotationZ = EXPAND_ICON_ROTATION
        },
        colorFilter = filter,
        filterQuality = FilterQuality.High,
    )
}

private const val DARK_BACKGROUND_LUMINANCE = .3f
private const val EXPAND_ICON_ROTATION = 45f
