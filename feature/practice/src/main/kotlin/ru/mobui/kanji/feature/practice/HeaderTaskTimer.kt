package ru.mobui.kanji.feature.practice

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily

/** A count-up without a limit, a color-coded countdown with one. Only time is rendered. */
@Composable
internal fun HeaderTaskTimer(elapsedMs: Long, limitMs: Long?, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val color = when (limitMs?.let { countdownPhase(elapsedMs, it) } ?: CountdownPhase.Normal) {
        CountdownPhase.Normal -> MaterialTheme.colorScheme.onBackground
        CountdownPhase.Warning -> if (dark) Color(0xFFFFD666) else Color(0xFF8A6500)
        CountdownPhase.Critical -> if (dark) Color(0xFFFF7979) else Color(0xFFB3261E)
    }
    val time = if (limitMs == null) formatTime(elapsedMs.coerceAtLeast(0)) else {
        val remaining = remainingCountdownMs(elapsedMs, limitMs)
        // Round up without overflowing at very large limits.
        formatTimerSeconds(remaining / 1000 + if (remaining % 1000 > 0) 1 else 0)
    }
    val description = stringResource(if (limitMs == null) R.string.time_elapsed else R.string.time_remaining, time)
    Text(time, modifier.testTag("header-timer").semantics { contentDescription = description },
        color = color, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
}
