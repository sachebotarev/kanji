package ru.mobui.kanji.feature.dashboard.builder

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.domain.catalog.CatalogSource
import ru.mobui.kanji.domain.catalog.title

@Composable
internal fun MaterialBookChoice(source: CatalogSource, language: String, selected: Boolean,
    onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onSelect),
        color = if (selected) scheme.secondaryContainer else scheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RadioButton(selected, null, Modifier.size(20.dp))
            val cover = when (source.coverKey) {
                "sage" -> ru.mobui.kanji.core.designsystem.R.drawable.catalog_tobira
                "blue" -> ru.mobui.kanji.core.designsystem.R.drawable.catalog_basic
                else -> null
            }
            if (cover != null) Image(painterResource(cover), null, Modifier.size(36.dp))
            Text(
                source.title(language),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = if (selected) scheme.onSecondaryContainer else scheme.onSurface,
            )
        }
    }
}
