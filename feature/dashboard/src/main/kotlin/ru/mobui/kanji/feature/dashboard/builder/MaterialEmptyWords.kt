package ru.mobui.kanji.feature.dashboard.builder

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.feature.dashboard.R

/** Filter-result empty for book / kanji forms — with mascot. */
@Composable
internal fun MaterialEmptyWords() {
    Column(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(
            painterResource(ru.mobui.kanji.core.designsystem.R.drawable.mascot_sticker_peek_empty_box),
            null,
            Modifier.size(112.dp),
        )
        Text(
            stringResource(R.string.material_no_words),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.material_no_words_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Compact note when dictionary / word-set pool is empty — text only, no mascot. */
@Composable
internal fun MaterialEmptyWordsNote(message: String, modifier: Modifier = Modifier) {
    MaterialSupportingText(
        message,
        modifier.padding(horizontal = 4.dp, vertical = 4.dp).testTag("material-empty-note"),
    )
}
