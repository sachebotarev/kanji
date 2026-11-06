package ru.mobui.kanji.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Placeholder / empty-label ink for text fields.
 * Paler than [androidx.compose.material3.ColorScheme.onSurfaceVariant] so hints don't look like entered text.
 */
val MaterialTheme.inputPlaceholderColor: Color
    @Composable get() = colorScheme.onSurfaceVariant.copy(alpha = 0.48f)

/** Shared OutlinedTextField colors with pale placeholders; pass container/border overrides when needed. */
@Composable
fun kanjiOutlinedTextFieldColors(
    focusedContainerColor: Color = Color.Unspecified,
    unfocusedContainerColor: Color = Color.Unspecified,
    disabledContainerColor: Color = Color.Unspecified,
    errorContainerColor: Color = Color.Unspecified,
    focusedBorderColor: Color = Color.Unspecified,
    unfocusedBorderColor: Color = Color.Unspecified,
    disabledBorderColor: Color = Color.Unspecified,
    errorBorderColor: Color = Color.Unspecified,
): TextFieldColors {
    val placeholder = MaterialTheme.inputPlaceholderColor
    val disabled = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    return OutlinedTextFieldDefaults.colors(
        focusedPlaceholderColor = placeholder,
        unfocusedPlaceholderColor = placeholder,
        disabledPlaceholderColor = disabled,
        errorPlaceholderColor = placeholder,
        unfocusedLabelColor = placeholder,
        disabledLabelColor = disabled,
        focusedContainerColor = focusedContainerColor,
        unfocusedContainerColor = unfocusedContainerColor,
        disabledContainerColor = disabledContainerColor,
        errorContainerColor = errorContainerColor,
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        disabledBorderColor = disabledBorderColor,
        errorBorderColor = errorBorderColor,
    )
}
