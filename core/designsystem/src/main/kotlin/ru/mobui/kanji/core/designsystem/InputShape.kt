package ru.mobui.kanji.core.designsystem

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource

/** Semantic input shape. Radius is generated from theme-source.json, not set in screens. */
val MaterialTheme.inputShape: CornerBasedShape
    @Composable get() = RoundedCornerShape(dimensionResource(R.dimen.kanji_input_corner_radius))
