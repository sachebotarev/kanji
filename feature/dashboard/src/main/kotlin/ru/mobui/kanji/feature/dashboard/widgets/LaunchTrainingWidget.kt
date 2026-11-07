package ru.mobui.kanji.feature.dashboard.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.feature.dashboard.R

/**
 * HOME.W06 — launch training or open unfinished choice.
 * Mascot overflows the filled button; ripple is clipped to the green fill only.
 */
@Composable
fun LaunchTrainingWidget(
    unfinished: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tagline = stringResource(R.string.dashboard_tagline)
    val subtitle =
        if (unfinished) stringResource(R.string.training_unfinished)
        else stringResource(R.string.dashboard_start)
    val shape = RoundedCornerShape(22.dp)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .testTag("start-practice")
            .padding(top = CAT_TOP_OVERFLOW, bottom = CAT_BOTTOM_OVERFLOW)
            .fillMaxWidth()
            .height(LAUNCH_MIN_HEIGHT),
    ) {
        // Green fill owns the press target and bounded ripple (rounded, not square).
        Box(
            Modifier.fillMaxSize()
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer, shape)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(bounded = true),
                    role = Role.Button,
                    onClickLabel = tagline,
                    onClick = onClick,
                ),
        ) {
            Row(
                Modifier.fillMaxSize().padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Column(
                    Modifier.weight(1f).clearAndSetSemantics {
                        contentDescription = "$tagline. $subtitle"
                    },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        tagline,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (unfinished) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = if (unfinished) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (unfinished) Modifier.testTag("unfinished-hint") else Modifier,
                    )
                }
                Spacer(Modifier.width(CAT_COLUMN_WIDTH))
                IllustratedIcon(DashboardIcon.Start, Modifier.size(26.dp))
            }
        }
        // Outside the clipped fill — peek above the button (HTML margin-top: -43px).
        MascotArtwork(
            Modifier.align(Alignment.BottomEnd)
                .padding(end = CAT_END_INSET)
                .offset(y = CAT_OVERFLOW_Y)
                .requiredSize(width = CAT_WIDTH, height = CAT_HEIGHT)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        )
    }
}

private val LAUNCH_MIN_HEIGHT = 94.dp
private val CAT_COLUMN_WIDTH = 98.dp
private val CAT_WIDTH = 122.dp
private val CAT_HEIGHT = 133.dp
/** Space above the fill so the peeking head is not clipped by scroll. */
private val CAT_TOP_OVERFLOW = 28.dp
private val CAT_BOTTOM_OVERFLOW = 4.dp
/** Negative Y lifts the cat so the head sticks out above the green fill. */
private val CAT_OVERFLOW_Y = (-28).dp
private val CAT_END_INSET = 36.dp
