package ru.mobui.kanji.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** CompositionLocal allowing child components/headers to observe whether search is currently expanded. */
val LocalSearchOpen = compositionLocalOf { false }

/**
 * Standard toggle button for showing/hiding search bar across all screens.
 * Follows the Catalog reference standard:
 * - When closed: shows ic_search with "Search" description.
 * - When open: shows ic_search_off with "Close search" description.
 * - Minimum 48.dp touch target, 24.dp vector icon.
 * - Styled with surfaceContainer background for clear distinction in the app bar.
 */
@Composable
fun KanjiSearchToggle(
    open: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "search-toggle",
    contentDescription: String? = null,
    enabled: Boolean = true,
) {
    val description = contentDescription ?: stringResource(
        if (open) R.string.core_search_close else R.string.core_search_open,
    )
    IconButton(
        onClick = onToggle,
        enabled = enabled,
        modifier = modifier
            .testTag(testTag)
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
                RoundedCornerShape(24.dp),
            )
            .semantics { this.contentDescription = description },
    ) {
        Icon(
            painter = painterResource(
                if (open) R.drawable.ic_search_off else R.drawable.ic_search,
            ),
            contentDescription = description,
            modifier = Modifier.size(24.dp),
        )
    }
}
