package ru.mobui.kanji.core.wordui

import ru.mobui.kanji.core.designsystem.inputShape

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import ru.mobui.kanji.core.designsystem.kanjiOutlinedTextFieldColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.shape
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.meaning
import ru.mobui.kanji.domain.catalog.translation

@Composable
fun WordInfoPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null)
            Text(
                title,
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
fun WordSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
fun WordNavigationRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectedLabel: String? = null,
    containerColor: Color? = null,
    compact: Boolean = false,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = if (compact) 0.dp else 16.dp,
        vertical = if (compact) 8.dp else 12.dp),
    trailing: @Composable () -> Unit = {},
    subtitleContent: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick,
        modifier.fillMaxWidth().semantics { this.selected = selected },
        shape = shape,
        color = containerColor ?: MaterialTheme.colorScheme.surfaceContainer,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(
            Modifier.fillMaxWidth()
                .semantics(mergeDescendants = true) { this.selected = selected }
                .heightIn(min = if (compact) 48.dp else 64.dp)
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (selectedLabel != null) {
                Box(Modifier.width(16.dp), contentAlignment = Alignment.Center) {
                    if (selected) Text(selectedLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                )
                when {
                    subtitleContent != null -> subtitleContent()
                    subtitle != null -> Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            trailing()
        }
    }
}

@Composable
fun WordRow(
    word: CatalogWord,
    language: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showOrigin: Boolean = false,
    chosen: Boolean? = null,
    stacked: Boolean = false,
    footer: @Composable () -> Unit = {},
) {
    Surface(
        onClick,
        modifier.fillMaxWidth().testTag("word-${word.id}").semantics {
            if (chosen != null) selected = chosen
        },
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = if (stacked) MaterialTheme.shapes.medium else RoundedCornerShape(24.dp),
        border = if (chosen == true) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        val origin =
            stringResource(if (word.isUserCreated) R.string.word_manual else R.string.word_catalog)
        Row(
            Modifier.padding(if (stacked) 16.dp else 12.dp).semantics(mergeDescendants = true) {
                if (showOrigin) stateDescription = origin
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (chosen != null) {
                Box(Modifier.width(16.dp), contentAlignment = Alignment.Center) {
                    if (chosen) {
                        Text("✓", Modifier.clearAndSetSemantics {}, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (stacked) 8.dp else 3.dp)) {
                if (stacked) Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(word.written, style = JapaneseSymbolStyle.copy(fontSize = 24.sp, lineHeight = 32.sp),
                        color = MaterialTheme.colorScheme.primary)
                    Text(word.reading, style = JapaneseSymbolStyle.copy(fontSize = 16.sp, lineHeight = 24.sp),
                        color = MaterialTheme.colorScheme.onSurface)
                } else FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        word.written,
                        Modifier.alignByBaseline(),
                        style =
                            MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 24.sp,
                                lineHeight = 30.sp,
                            ),
                    )
                    Text(
                        word.reading,
                        Modifier.alignByBaseline(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    word.translation(language) ?: stringResource(R.string.word_missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (stacked) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                )
                footer()
            }
            if (showOrigin)
                Icon(
                    painterResource(
                        if (word.isUserCreated) R.drawable.word_origin_manual
                        else R.drawable.word_origin_catalog,
                    ),
                    null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
        }
    }
}

@Composable
fun WordList(
    words: List<CatalogWord>,
    language: String,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    showOrigin: Boolean = false,
    bottomPadding: Int = 20,
    stacked: Boolean = false,
    footer: @Composable (CatalogWord) -> Unit = {},
) {
    LazyColumn(
        modifier.testTag("word-list"),
        state = listState,
        contentPadding =
            PaddingValues(start = 18.dp, end = 18.dp, top = if (stacked) 16.dp else 12.dp, bottom = bottomPadding.dp),
        verticalArrangement = Arrangement.spacedBy(if (stacked) 12.dp else 8.dp),
    ) {
        items(words, key = { it.id }) { word ->
            WordRow(word, language, { onClick(word.id) }, showOrigin = showOrigin, stacked = stacked, footer = { footer(word) })
        }
    }
}

@Composable
fun WordSearch(
    query: String,
    label: String,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ru.mobui.kanji.core.designsystem.KanjiSearchBar(
        query = query,
        onQuery = onQuery,
        modifier = modifier,
        placeholder = label,
        autoFocus = false,
        leadingIcon = true,
        testTag = "word-search",
        onClear = { onQuery("") },
    )
}

data class WordStudyItem(
    val sourceId: Int,
    val kanjiId: Int?,
    val title: String,
    val subtitle: String? = null,
)

@Composable
fun WordDetailsContent(word: CatalogWord, language: String, kanji: List<CatalogKanji>, studyItems: List<WordStudyItem>, actions: WordDetailsActions,
    modifier: Modifier = Modifier, showOrigin: Boolean = false, footer: @Composable () -> Unit = {}, bottomPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    with(actions) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showOrigin)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    stringResource(
                        if (word.isUserCreated) R.string.word_manual else R.string.word_catalog,
                    ),
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        WordInfoPanel {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    word.written,
                    Modifier.fillMaxWidth().testTag("word-written"),
                    textAlign = TextAlign.Center,
                    style =
                        if (word.written.codePointCount(0, word.written.length) > 8)
                            MaterialTheme.typography.headlineLarge
                        else MaterialTheme.typography.displayMedium,
                )
                Text(
                    stringResource(R.string.word_reading),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    word.reading,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.word_translation),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    word.translation(language) ?: stringResource(R.string.word_missing),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
        footer()
        if (word.parts.isNotEmpty())
            WordInfoPanel(title = stringResource(R.string.word_parts)) {
                word.parts.forEach { Text(it.written + " · " + it.reading) }
            }
        if (kanji.isNotEmpty())
            WordSection(stringResource(R.string.word_kanji)) {
                kanji.forEach { item ->
                    WordNavigationRow(item.symbol, item.meaning(language), { onKanji(item.id) })
                }
            }
        if (studyItems.isNotEmpty())
            WordSection(stringResource(R.string.word_links)) {
                studyItems.forEach { item ->
                    WordNavigationRow(item.title, item.subtitle, { onStudy(item) })
                }
            }
    }

    }
}

@Preview(locale = "ru")
@Composable
private fun WordPreview() =
    KanjiTheme(true) {
        WordDetailsContent(CatalogWord(
                -1,
                "木漏れ日",
                "こもれび",
                mapOf("ru" to "солнечный свет сквозь листву"),
                isUserCreated = true,
            ), "ru", emptyList(), emptyList(), WordDetailsActions({}, {}), showOrigin = true)
    }
