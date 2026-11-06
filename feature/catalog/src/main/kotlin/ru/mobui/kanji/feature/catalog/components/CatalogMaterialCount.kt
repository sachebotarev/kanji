package ru.mobui.kanji.feature.catalog.components

import ru.mobui.kanji.feature.catalog.R
import ru.mobui.kanji.feature.catalog.presentation.MaterialKind

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun CatalogMaterialCount(
    count: Int,
    total: Int,
    kind: MaterialKind,
    modifier: Modifier = Modifier,
    inset: Boolean = true,
    action: (@Composable () -> Unit)? = null,
) {
    val quantity =
        when (kind) {
            MaterialKind.Kanji -> R.plurals.catalog_kanji_count
            MaterialKind.Word -> R.plurals.catalog_word_count
            MaterialKind.Radical -> R.plurals.catalog_radical_count
            MaterialKind.Chapter -> R.plurals.catalog_chapter_count
        }
    val horizontal = if (inset) 18.dp else 0.dp
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = horizontal, end = horizontal, top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val totalLabel = pluralStringResource(quantity, total, total)
        Text(
            modifier = Modifier.weight(1f),
            text =
                if (count == total) totalLabel
                else stringResource(R.string.catalog_shown_count, count, totalLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) {
            action()
        }
    }
}

@Composable
internal fun catalogMaterialSummary(kanjiCount: Int, wordCount: Int): String =
    stringResource(
        R.string.catalog_material_summary,
        pluralStringResource(R.plurals.catalog_kanji_count, kanjiCount, kanjiCount),
        pluralStringResource(R.plurals.catalog_word_count, wordCount, wordCount),
    )
