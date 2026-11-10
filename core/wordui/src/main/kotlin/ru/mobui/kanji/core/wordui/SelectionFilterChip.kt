package ru.mobui.kanji.core.wordui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import ru.mobui.kanji.core.designsystem.R as DesignR

/** Extracted catalog filter, shared by catalog, dictionary and recipe selections. */
@Composable
fun SelectionFilterChip(
    label: String,
    onOpen: () -> Unit,
    onClear: (() -> Unit)?,
    clearLabel: String,
    modifier: Modifier = Modifier,
    openEnabled: Boolean = true,
) {
    val active = onClear != null
    val scheme = MaterialTheme.colorScheme
    val background = if (active) scheme.tertiaryContainer else scheme.surfaceContainerHigh
    Surface(modifier = modifier.semantics { selected = active }, shape = RoundedCornerShape(24.dp), color = background) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 48.dp)) {
            TextButton(
                onOpen,
                enabled = openEnabled,
                contentPadding = PaddingValues(start = 14.dp, end = if (active) 4.dp else 12.dp, top = 8.dp, bottom = 8.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = scheme.onTertiaryContainer),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                val textColor = if (active) scheme.onTertiaryContainer else scheme.onSurface
                Text(label, style = MaterialTheme.typography.bodyMedium, color = textColor)
                if (!active) {
                    Icon(
                        painterResource(DesignR.drawable.ic_chevron_down),
                        null,
                        Modifier.padding(start = 6.dp).size(16.dp),
                        tint = scheme.onSurfaceVariant,
                    )
                }
            }
            if (onClear != null) {
                IconButton(onClear, Modifier.size(48.dp)) {
                    Icon(
                        painterResource(DesignR.drawable.ic_close),
                        clearLabel,
                        Modifier.size(16.dp),
                        tint = scheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}
