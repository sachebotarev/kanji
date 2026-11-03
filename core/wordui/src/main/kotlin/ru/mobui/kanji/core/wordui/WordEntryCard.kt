package ru.mobui.kanji.core.wordui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared responsive word card, matching the unified vertical stack layout. */
@Composable
fun WordEntryCard(
    written: String,
    reading: String,
    translation: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    japaneseStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineSmall,
    japaneseColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    readingStyle: androidx.compose.ui.text.TextStyle =
        japaneseStyle.copy(fontSize = 16.sp, lineHeight = 24.sp),
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = written,
                    style = japaneseStyle.copy(fontSize = 24.sp, lineHeight = 32.sp),
                    color = japaneseColor,
                )
                Text(
                    text = reading,
                    style = readingStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = translation,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
