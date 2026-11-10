package ru.mobui.kanji.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Screen-level horizontal margins and paddings. */
val KanjiScreenPadding = 18.dp

/** Standard vertical gap between items in lazy lists. */
val KanjiItemSpacing = 12.dp

/** Standard card internal content padding. */
val KanjiCardPadding = 16.dp

/** Compact card or row internal padding. */
val KanjiCompactCardPadding = 12.dp

/** Semantic golden tone for assisted / with-hints states across dashboard and history. */
val KanjiColorAssisted = Color(0xFFE4B94F)

/** Horizontal/vertical padding for forms and lists with a bottom FAB. */
val KanjiFormContentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 112.dp)

/** Content padding for scrollable lists without a floating action. */
val KanjiListContentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp)

/** Section heading: titleMedium / SemiBold / onSurface. */
@Composable
fun KanjiSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/** Supporting / hint copy under controls. */
@Composable
fun KanjiSupportingText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Standard back + title app bar based on Catalog conventions. */
@Composable
fun KanjiScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    backIcon: Painter = painterResource(R.drawable.ic_arrow_back),
    backContentDescription: String = stringResource(R.string.core_back),
    backTestTag: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = if (backTestTag != null) Modifier.testTag(backTestTag) else Modifier,
            ) {
                Icon(
                    painter = backIcon,
                    contentDescription = backContentDescription,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                title,
                Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            trailing()
        }
        if (!subtitle.isNullOrEmpty()) {
            Text(
                subtitle,
                Modifier.padding(start = 18.dp, end = 18.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
