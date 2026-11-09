package ru.mobui.kanji.feature.dashboard.builder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.mobui.kanji.core.designsystem.KanjiFormContentPadding
import ru.mobui.kanji.core.designsystem.KanjiScreenHeader
import ru.mobui.kanji.core.designsystem.KanjiSectionTitle
import ru.mobui.kanji.core.designsystem.KanjiSupportingText
import ru.mobui.kanji.core.wordui.WordList
import ru.mobui.kanji.core.wordui.WordNavigationRow
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.feature.dashboard.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

internal data class MaterialFilterRow(
    val title: String,
    val subtitle: String?,
    val onClick: () -> Unit,
    val modifier: Modifier = Modifier,
    val invalid: Boolean = false,
)

/** Shared horizontal/vertical padding for RECIPE forms — app-wide Kanji token. */
internal val MaterialFormContentPadding = KanjiFormContentPadding

/** Section heading: Материал / Учебник / Фильтры — theme onSurface. */
@Composable
internal fun MaterialSectionTitle(text: String, modifier: Modifier = Modifier) {
    KanjiSectionTitle(text, modifier)
}

/** Type hint and empty-pool notes under controls. */
@Composable
internal fun MaterialSupportingText(text: String, modifier: Modifier = Modifier) {
    KanjiSupportingText(text, modifier)
}

/** Title + segmented filter rows with a 3dp hairline gap, like home textbooks/dictionary. */
@Composable
internal fun MaterialFiltersSection(
    rows: List<MaterialFilterRow>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().testTag("material-filters"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MaterialSectionTitle(stringResource(R.string.material_filters_title))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            rows.forEachIndexed { index, row ->
                MaterialNavigationRow(
                    row.title,
                    row.subtitle,
                    row.onClick,
                    row.modifier,
                    invalid = row.invalid,
                    shape = filterSegmentShape(index, rows.count()),
                    onFilterBed = true,
                )
            }
        }
    }
}

private fun filterSegmentShape(index: Int, count: Int): Shape = when {
    count <= 1 -> RoundedCornerShape(24.dp)
    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    index == count - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    else -> RoundedCornerShape(4.dp)
}

@Composable
internal fun MaterialNavigationRow(title: String, subtitle: String?, onClick: () -> Unit,
    modifier: Modifier = Modifier, invalid: Boolean = false, compact: Boolean = false,
    onFilterBed: Boolean = false, shape: Shape = RoundedCornerShape(24.dp)) {
    WordNavigationRow(title, subtitle, onClick,
        if (invalid) modifier.border(1.dp, MaterialTheme.colorScheme.error, shape) else modifier,
        containerColor = if (onFilterBed) MaterialTheme.colorScheme.surfaceContainer
            else MaterialTheme.colorScheme.surface,
        compact = compact,
        shape = shape,
        contentPadding = when {
            compact -> PaddingValues(vertical = 12.dp)
            invalid || onFilterBed -> PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            else -> PaddingValues(horizontal = 4.dp, vertical = 12.dp)
        },
        trailing = {
            Icon(
                painterResource(ru.mobui.kanji.core.designsystem.R.drawable.ic_chevron_right),
                null,
                Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        })
}

/** Secondary action to open the word list; no navigation chevron. */
@Composable
internal fun MaterialWordsPreviewLink(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick,
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
    ) {
        Text(
            pluralStringResource(R.plurals.material_view_words, count, count),
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
internal fun MaterialHeader(title: String, onBack: () -> Unit) {
    KanjiScreenHeader(
        title,
        onBack,
        backIcon = painterResource(ru.mobui.kanji.core.designsystem.R.drawable.ic_arrow_back),
        backContentDescription = stringResource(R.string.material_back),
        backTestTag = "material-back",
    )
}

/** Confirms before leaving a dirty material draft. Returns the back action for header and system back. */
@Composable
internal fun rememberMaterialLeave(dirty: Boolean, onBack: () -> Unit): () -> Unit {
    var leaving by rememberSaveable { mutableStateOf(false) }
    val request = {
        if (dirty) leaving = true else onBack()
    }
    BackHandler(onBack = request)
    if (leaving) {
        AlertDialog(
            onDismissRequest = { leaving = false },
            title = { Text(stringResource(R.string.recipe_leave)) },
            text = { Text(stringResource(R.string.recipe_unsaved_message)) },
            confirmButton = {
                TextButton(onBack, Modifier.testTag("material-discard")) {
                    Text(stringResource(R.string.recipe_discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton({ leaving = false }, Modifier.testTag("material-stay")) {
                    Text(stringResource(R.string.recipe_stay), color = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }
    return request
}

@Composable
internal fun MaterialWordsPreview(words: List<CatalogWord>, language: String, onWord: (Int) -> Unit,
    onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    Scaffold(modifier) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MaterialHeader(stringResource(R.string.material_words_preview), onBack)
            WordList(words, language, onWord, Modifier.weight(1f))
        }
    }
}
