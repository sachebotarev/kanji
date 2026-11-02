package ru.mobui.kanji.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.domain.catalog.JlptFilter

@Composable
internal fun jlptLabel(value: JlptFilter): String =
    when (value) {
        JlptFilter.AnyLevel -> stringResource(R.string.catalog_all_jlpt)
        JlptFilter.Missing -> stringResource(R.string.catalog_jlpt_missing)
        is JlptFilter.Exact -> stringResource(R.string.catalog_jlpt_level, value.level.number)
    }

internal data class NumericOption<T : Any>(val value: T, val title: String, val count: Int)

@Composable
internal fun <T : Any> NumericFilterScreen(
    options: List<NumericOption<T>>,
    selected: T?,
    kind: MaterialKind,
    clearLabel: String,
    actions: NumericFilterActions<T>,
    modifier: Modifier = Modifier,
    explanation: String? = null,
) {
    with(actions) {
    val listState = rememberCatalogListState()
    LazyColumn(
        modifier,
        state = listState,
        userScrollEnabled = listState.canScrollForward || listState.canScrollBackward,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            FilledTonalButton(
                onClear,
                Modifier.padding(bottom = 8.dp).heightIn(min = 48.dp),
                colors =
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            ) {
                Text(clearLabel)
            }
        }
        if (explanation != null)
            item { Text(explanation, style = MaterialTheme.typography.bodySmall) }
        items(options, key = { it.value.toString() }) { option ->
            CatalogRow(
                option.title,
                pluralStringResource(
                    if (kind == MaterialKind.Radical) R.plurals.catalog_radical_count
                    else R.plurals.catalog_kanji_count,
                    option.count,
                    option.count,
                ),
                { onSelect(option.value) },
                selected = option.value == selected,
                selection = true,
            )
        }
    }

    }
}
