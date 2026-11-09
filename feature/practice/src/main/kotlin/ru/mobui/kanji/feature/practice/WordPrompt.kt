package ru.mobui.kanji.feature.practice

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Practice translation: at most two lines. On overflow, Instagram-style inline «… ещё»
 * opens the full text in a dialog without taking an extra vertical row.
 */
@Composable
internal fun WordPrompt(word: String, modifier: Modifier = Modifier) {
    var showFull by rememberSaveable(word) { mutableStateOf(false) }
    val moreLabel = stringResource(R.string.prompt_more)
    val style =
        MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    val linkColor = MaterialTheme.colorScheme.primary
    val minTwoLines = with(LocalDensity.current) { (style.lineHeight * 2).toDp() }
    val textMeasurer = rememberTextMeasurer()
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .heightIn(min = minTwoLines)
            .padding(vertical = 4.dp),
    ) {
        val maxWidthPx = constraints.maxWidth
        val collapsed =
            remember(word, moreLabel, maxWidthPx, style, linkColor, textMeasurer) {
                collapsePrompt(word, moreLabel, maxWidthPx, style, linkColor, textMeasurer) {
                    showFull = true
                }
            }
        Text(
            collapsed,
            Modifier.fillMaxWidth().testTag("word-prompt"),
            style = style,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Clip,
        )
    }
    if (showFull) {
        Dialog(onDismissRequest = { showFull = false }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column(Modifier.padding(24.dp).widthIn(max = 440.dp)) {
                    Text(
                        stringResource(R.string.prompt_full_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        word,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        onClick = { showFull = false },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.debug_close))
                    }
                }
            }
        }
    }
}

private fun collapsePrompt(
    word: String,
    moreLabel: String,
    maxWidthPx: Int,
    style: TextStyle,
    linkColor: Color,
    textMeasurer: TextMeasurer,
    onMore: () -> Unit,
) =
    buildAnnotatedString {
        if (maxWidthPx <= 0) {
            append(word)
            return@buildAnnotatedString
        }
        val constraints = Constraints(maxWidth = maxWidthPx)
        val full =
            textMeasurer.measure(
                text = word,
                style = style,
                constraints = constraints,
                maxLines = 2,
            )
        if (!full.hasVisualOverflow) {
            append(word)
            return@buildAnnotatedString
        }
        val ellipsis = "… "
        val suffixPlain = ellipsis + moreLabel
        var low = 0
        var high = word.length
        var best = 0
        while (low <= high) {
            val mid = (low + high) / 2
            val candidate = word.take(mid).trimEnd() + suffixPlain
            val layout =
                textMeasurer.measure(
                    text = candidate,
                    style = style,
                    constraints = constraints,
                    maxLines = 2,
                )
            if (!layout.hasVisualOverflow) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        var prefix = word.take(best).trimEnd()
        val space = prefix.lastIndexOf(' ')
        if (space in 1 until prefix.length) {
            val atWord =
                textMeasurer.measure(
                    text = prefix.take(space) + suffixPlain,
                    style = style,
                    constraints = constraints,
                    maxLines = 2,
                )
            if (!atWord.hasVisualOverflow) prefix = prefix.take(space)
        }
        if (prefix.isEmpty()) {
            // Extremely narrow: keep the action visible.
            withLink(
                LinkAnnotation.Clickable(
                    tag = "prompt-more",
                    styles = TextLinkStyles(style = SpanStyle(color = linkColor)),
                    linkInteractionListener = { onMore() },
                ),
            ) {
                append(moreLabel)
            }
            return@buildAnnotatedString
        }
        append(prefix)
        append(ellipsis)
        withLink(
            LinkAnnotation.Clickable(
                tag = "prompt-more",
                styles = TextLinkStyles(style = SpanStyle(color = linkColor)),
                linkInteractionListener = { onMore() },
            ),
        ) {
            append(moreLabel)
        }
    }
