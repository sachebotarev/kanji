package ru.mobui.kanji.feature.catalog.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import ru.mobui.kanji.domain.catalog.CatalogRadical
import ru.mobui.kanji.domain.catalog.name
import ru.mobui.kanji.feature.catalog.R

@Composable
internal fun radicalDisplayName(radical: CatalogRadical, language: String): String =
    if (radical.id == SECOND_RADICAL_ID) stringResource(R.string.catalog_radical_second)
    else radical.name(language) ?: stringResource(R.string.catalog_missing)

/** Continuous grid: only the fill changes at a stroke-count boundary. */
@Composable
internal fun radicalContainerColor(strokes: Int): Color {
    val colors = MaterialTheme.colorScheme
    val tone = (strokes.coerceAtLeast(1) - 1) % RADICAL_TONE_COUNT
    val accent = when (tone) {
        0 -> colors.primaryContainer
        1 -> colors.secondaryContainer
        2 -> colors.tertiaryContainer
        else -> colors.surfaceContainerLow
    }
    val strength = when (tone) {
        1 -> SECONDARY_STRENGTH
        NEUTRAL_TONE -> NEUTRAL_STRENGTH
        else -> DEFAULT_STRENGTH
    }
    return lerp(colors.surfaceContainer, accent, strength)
}

private const val SECOND_RADICAL_ID = 5
private const val RADICAL_TONE_COUNT = 4
private const val NEUTRAL_TONE = 3
private const val SECONDARY_STRENGTH = 0.42f
private const val NEUTRAL_STRENGTH = 0.65f
private const val DEFAULT_STRENGTH = 0.32f
