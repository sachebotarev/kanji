package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun CatalogEmptyState(hasQuery: Boolean, modifier: Modifier = Modifier, explanation: String? = null, reportScroll: Boolean = true) {
    if (reportScroll) ReportCatalogScroll(false)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val available = maxHeight
        val bounded = available != Dp.Infinity
        val cat = if (!bounded) 156.dp else (available - 120.dp).coerceIn(48.dp, 156.dp)
        Column(Modifier.fillMaxWidth().then(if (bounded) Modifier.height(available) else Modifier).padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
            if (hasQuery && (!bounded || available > 96.dp)) CatalogArtwork(ru.mobui.kanji.core.designsystem.R.drawable.mascot_sticker_magnify,
                Modifier.size(cat), mascot = true)
            Text(stringResource(if (hasQuery) R.string.catalog_empty else R.string.catalog_no_materials),
                Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(explanation ?: stringResource(if (hasQuery) R.string.catalog_empty_hint else R.string.catalog_no_materials_hint),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
