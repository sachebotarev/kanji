package ru.mobui.kanji.feature.dashboard.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.feature.dashboard.R

/** Compact launch surface; decorative mascot is positioned by the training card. */
@Composable
fun StartPracticeWidget(onStartPractice: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) {
    val actionLabel = stringResource(R.string.dashboard_start)
    val mixLabel = subtitle ?: stringResource(R.string.dashboard_mix)
    Box(modifier.testTag("start-practice")
        .clickable(role = Role.Button, onClickLabel = actionLabel, onClick = onStartPractice)) {
        Box(Modifier.matchParentSize()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(24.dp)))
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(Modifier.weight(1f).clearAndSetSemantics { contentDescription = "$actionLabel. $mixLabel" },
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.dashboard_tagline), style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(mixLabel, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            IllustratedIcon(DashboardIcon.Start, Modifier.size(36.dp))
        }
    }
}

/** Original artwork without an added perimeter or halo. */
@Composable
internal fun MascotArtwork(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.mascot_wave), null, modifier)
}
