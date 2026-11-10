package ru.mobui.kanji.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard empty state component based on Catalog conventions.
 * Adapts mascot size to available height, shows magnifying mascot on query,
 * and peek-empty mascot when content is empty.
 */
@Composable
fun KanjiEmptyState(
    modifier: Modifier = Modifier,
    hasQuery: Boolean = false,
    title: String = stringResource(if (hasQuery) R.string.core_empty else R.string.core_empty),
    hint: String? = stringResource(R.string.core_empty_hint),
    mascotRes: Int? = null,
    ctaText: String? = null,
    onCtaClick: (() -> Unit)? = null,
    testTag: String = "empty-state",
) {
    BoxWithConstraints(modifier.fillMaxWidth().testTag(testTag)) {
        val available = maxHeight
        val bounded = available != Dp.Infinity
        val cat = if (!bounded) 140.dp else (available - 120.dp).coerceIn(48.dp, 140.dp)
        val imageRes = mascotRes ?: if (hasQuery) {
            R.drawable.mascot_sticker_magnify
        } else {
            R.drawable.mascot_sticker_peek_empty_box
        }

        Column(
            Modifier.fillMaxWidth()
                .then(if (bounded) Modifier.height(available) else Modifier)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            if (!bounded || available > 96.dp) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = null,
                    modifier = Modifier.size(cat).testTag("empty-mascot"),
                )
            }
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!hint.isNullOrEmpty()) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (ctaText != null && onCtaClick != null) {
                TextButton(
                    onClick = onCtaClick,
                    modifier = Modifier.padding(top = 4.dp).testTag("empty-cta"),
                ) {
                    Text(ctaText)
                }
            }
        }
    }
}
