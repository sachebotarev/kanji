package ru.mobui.kanji.core.wordui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared responsive word card, independent of repositories, navigation and localization lookup. */
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
        japaneseStyle.copy(fontSize = 14.sp, lineHeight = 20.sp),
) {
    Surface(
        onClick,
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        BoxWithConstraints(Modifier.heightIn(min = 72.dp).padding(16.dp)) {
            val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale
            val stacked =
                maxWidth < 320.dp * scale ||
                    written.codePointCount(0, written.length) > MAX_INLINE_WRITTEN_LENGTH ||
                    reading.length > MAX_INLINE_READING_LENGTH
            val japanese: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        written,
                        style = japaneseStyle.copy(fontSize = 24.sp, lineHeight = 32.sp),
                        color = japaneseColor,
                    )
                    Text(reading, style = readingStyle)
                }
            }
            val translation: @Composable () -> Unit = {
                Text(
                    translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (stacked)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    japanese()
                    translation()
                }
            else
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(Modifier.weight(1f)) { japanese() }
                    Box(Modifier.weight(TRANSLATION_COLUMN_WEIGHT)) { translation() }
                }
        }
    }
}

private const val MAX_INLINE_WRITTEN_LENGTH = 6
private const val MAX_INLINE_READING_LENGTH = 12
private const val TRANSLATION_COLUMN_WEIGHT = 1.35f
