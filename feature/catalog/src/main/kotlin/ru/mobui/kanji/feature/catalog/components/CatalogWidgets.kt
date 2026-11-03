package ru.mobui.kanji.feature.catalog.components

import ru.mobui.kanji.feature.catalog.R
import ru.mobui.kanji.feature.catalog.theme.CatalogJapaneseColor
import ru.mobui.kanji.feature.catalog.theme.CatalogJapaneseStyle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.imeAction
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.shape
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogRadical
import ru.mobui.kanji.domain.catalog.CatalogSource
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.localized
import ru.mobui.kanji.domain.catalog.meaning
import ru.mobui.kanji.domain.catalog.name
import ru.mobui.kanji.domain.catalog.title
import ru.mobui.kanji.domain.catalog.translation

@Composable
internal fun CatalogPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    spacing: androidx.compose.ui.unit.Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(spacing)) {
            if (title != null)
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            content()
        }
    }
}

@Composable
internal fun CatalogSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            Modifier.padding(horizontal = 4.dp).semantics { heading() },
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
        )
        content()
    }
}

@Composable
internal fun CatalogHint(text: String, modifier: Modifier = Modifier, inset: Boolean = true) {
    Text(
        text,
        modifier
            .fillMaxWidth()
            .padding(
                start = if (inset) 20.dp else 0.dp,
                end = if (inset) 20.dp else 0.dp,
                top = 8.dp,
            ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun CatalogRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selection: Boolean = false,
    card: Boolean = true,
    compact: Boolean = false,
    inlineSubtitle: Boolean = false,
) {
    val body: @Composable RowScope.() -> Unit = {
        if (inlineSubtitle) {
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) Text(
                    subtitle,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
        } else Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (compact) 16.sp else 18.sp, lineHeight = 24.sp,
                ),
            )
            if (subtitle != null)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
        }
        if (selected || !selection)
            Text(
                if (selected) "✓" else "›",
                Modifier.clearAndSetSemantics {},
                color =
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 22.sp,
            )
    }
    if (card)
        Surface(
            onClick,
            modifier.fillMaxWidth().semantics { this.selected = selected },
            shape = MaterialTheme.shapes.medium,
            color =
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                Modifier.heightIn(min = if (compact) 56.dp else 80.dp)
                    .padding(horizontal = 16.dp, vertical = if (compact) 12.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                content = { body() },
            )
        }
    else
        Row(
            modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(onClick = onClick)
                .semantics { this.selected = selected }
                .padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = { body() },
        )
}

@Composable
internal fun SourceCard(
    source: CatalogSource,
    language: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CatalogIllustratedCard(
        source.title(language),
        summary,
        if (source.sourceType == "textbook")
            when (source.coverKey) {
                "sage" -> R.drawable.catalog_tobira
                "blue" -> R.drawable.catalog_basic
                else -> R.drawable.catalog_book
            }
        else R.drawable.catalog_book,
        onClick,
        modifier.testTag("source-${source.id}"),
        important = true,
    )
}

private const val SOURCE_CARD_SECONDARY_FRACTION = 0.45f
private const val IDLE_FILTER_STRENGTH = 0.4f

@Composable
internal fun CatalogIllustratedCard(
    title: String,
    subtitle: String,
    illustration: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    important: Boolean = false,
) {
    val containerColor =
        if (important) {
            androidx.compose.ui.graphics.lerp(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.secondaryContainer,
                SOURCE_CARD_SECONDARY_FRACTION,
            )
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    Surface(
        onClick,
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
    ) {
        Row(
            Modifier.heightIn(min = 80.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                CatalogArtwork(illustration, Modifier.size(48.dp))
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    title,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        ),
                )
                Text(
                    subtitle,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun WordRow(word: CatalogWord, language: String, onClick: () -> Unit) =
    ru.mobui.kanji.core.wordui.WordEntryCard(
        word.written,
        word.reading,
        word.translation(language) ?: stringResource(R.string.catalog_missing),
        onClick,
        Modifier.testTag("word-${word.id}"),
        CatalogJapaneseStyle,
        CatalogJapaneseColor,
        readingStyle = CatalogJapaneseStyle.copy(fontSize = 16.sp, lineHeight = 24.sp),
    )

@Composable
internal fun CatalogSearch(
    query: String,
    label: String,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
    inset: Boolean = true,
    numeric: Boolean = false,
) {
    val focus = androidx.compose.runtime.remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val focusRequested = androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf(false)
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!focusRequested.value) {
            focusRequested.value = true
            focus.requestFocus()
            keyboard?.show()
        }
    }
    OutlinedTextField(
        query,
        onQuery,
        modifier
            .then(if (inset) Modifier.padding(horizontal = 20.dp, vertical = 8.dp) else Modifier)
            .fillMaxWidth()
            .testTag("catalog-search")
            .focusRequester(focus),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(painterResource(R.drawable.catalog_search), null, Modifier.size(24.dp))
        },
        trailingIcon = {
            if (query.isNotEmpty())
                TextButton({ onQuery("") }) { Text(stringResource(R.string.catalog_clear_search)) }
        },
        keyboardOptions =
            androidx.compose.foundation.text.KeyboardOptions(
                keyboardType =
                    if (numeric) androidx.compose.ui.text.input.KeyboardType.Number
                    else androidx.compose.ui.text.input.KeyboardType.Text,
                imeAction = androidx.compose.ui.text.input.ImeAction.Search,
            ),
        keyboardActions =
            androidx.compose.foundation.text.KeyboardActions(onSearch = {
                focusManager.clearFocus()
                keyboard?.hide()
            }),
        shape = RoundedCornerShape(24.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
    )
}

@Composable
internal fun CatalogFilterChip(
    label: String,
    onOpen: () -> Unit,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier,
    openEnabled: Boolean = true,
) {
    val active = onClear != null
    val containerColor =
        if (active) MaterialTheme.colorScheme.tertiaryContainer
        else androidx.compose.ui.graphics.lerp(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.tertiaryContainer,
            IDLE_FILTER_STRENGTH,
        )
    val contentColor = MaterialTheme.colorScheme.onTertiaryContainer

    Surface(
        modifier = modifier.semantics { selected = active },
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 40.dp),
        ) {
            TextButton(
                onClick = onOpen,
                enabled = openEnabled,
                contentPadding =
                    PaddingValues(
                        start = 14.dp,
                        end = if (active) 4.dp else 12.dp,
                        top = 8.dp,
                        bottom = 8.dp,
                    ),
                colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                )
                if (!active) {
                    Icon(
                        painter = painterResource(R.drawable.catalog_chevron_down),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 6.dp).size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (onClear != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.catalog_close),
                        contentDescription = stringResource(R.string.catalog_clear_filter, label),
                        modifier = Modifier.size(16.dp),
                        tint = contentColor,
                    )
                }
            }
        }
    }
}

@Composable
internal fun CatalogClearFilterButton(label: String, onClear: () -> Unit, inset: Boolean = true) {
    FilledTonalButton(
        onClick = onClear,
        modifier =
            Modifier.padding(start = if (inset) 20.dp else 0.dp)
                .heightIn(min = 40.dp),
        shape = RoundedCornerShape(24.dp),
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = androidx.compose.ui.graphics.lerp(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    IDLE_FILTER_STRENGTH,
                ),
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

@Composable
internal fun NumberingHelpButton(
    text: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.catalog_numbering_title),
    description: String = stringResource(R.string.catalog_numbering_help),
) {
    val open = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    IconButton(
        onClick = { open.value = true },
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.catalog_help),
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
    if (open.value) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { open.value = false },
            confirmButton = {
                TextButton(onClick = { open.value = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            title = { Text(title) },
            text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        )
    }
}
