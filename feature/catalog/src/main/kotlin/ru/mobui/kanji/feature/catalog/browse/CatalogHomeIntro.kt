package ru.mobui.kanji.feature.catalog.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.feature.catalog.CatalogSection
import ru.mobui.kanji.feature.catalog.R
import ru.mobui.kanji.feature.catalog.components.CatalogArtwork
import ru.mobui.kanji.core.designsystem.R as DesignR

@Composable
internal fun CatalogHomeIntro(section: CatalogSection, modifier: Modifier = Modifier) {
    val references = section == CatalogSection.References
    Row(
        modifier.fillMaxWidth().heightIn(min = 112.dp).testTag("catalog-intro").padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            stringResource(if (references) R.string.catalog_references_intro else R.string.catalog_textbooks_intro),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        CatalogArtwork(
            if (references) DesignR.drawable.mascot_sticker_jp_inkstone_puzzle
            else DesignR.drawable.mascot_sticker_jp_bound_book,
            Modifier.size(96.dp).testTag("catalog-intro-mascot"),
        )
    }
}
