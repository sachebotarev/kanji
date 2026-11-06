package ru.mobui.kanji.feature.catalog.browse

import ru.mobui.kanji.feature.catalog.R
import ru.mobui.kanji.feature.catalog.components.CatalogArtwork
import ru.mobui.kanji.feature.catalog.components.WordRow
import ru.mobui.kanji.feature.catalog.presentation.SourceWordsPresentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ru.mobui.kanji.domain.catalog.CatalogWordGroup

@Composable
internal fun CatalogSourceWords(groups: List<CatalogWordGroup>, language: String, presentation: SourceWordsPresentation, onWord: (Int) -> Unit,
    modifier: Modifier = Modifier, header: (@Composable () -> Unit)? = null) {
    with(presentation) {
    val listState = rememberCatalogListState()
    LazyColumn(
        modifier.testTag("word-list"),
        state = listState,
        userScrollEnabled = listState.canScrollForward || listState.canScrollBackward,
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        if (header != null) item(key = "header") { header() }
        if (groups.isEmpty()) item { Text(stringResource(R.string.catalog_empty)) }
        groups.forEachIndexed { index, group ->
            if (showGroups && group.chapter > 0) {
                if (index > 0)
                    item(key = "chapter-gap-${group.chapter}") { Spacer(Modifier.height(16.dp)) }
                stickyHeader(key = "chapter-${group.chapter}") {
                    val title =
                        androidx.compose.ui.text.buildAnnotatedString {
                            append(stringResource(R.string.catalog_chapter, group.chapter))
                            if (groups.size > 1) {
                            pushStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                                ),
                            )
                            append(
                                stringResource(
                                    R.string.catalog_group_volume,
                                    pluralStringResource(
                                        R.plurals.catalog_word_count,
                                        group.words.count(),
                                        group.words.count(),
                                    ),
                                ),
                            )
                            pop()
                            }
                        }
                    Text(
                        title,
                        Modifier.fillMaxWidth()
                            .zIndex(1f)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 12.dp)
                            .semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            items(group.words, key = { it.id }) { word ->
                Box(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 12.dp)) {
                    WordRow(word, language, { onWord(word.id) })
                }
            }
        }
        if (showEnd && !fixedChapter)
            item {
                Row(
                    Modifier.fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 28.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CatalogArtwork(R.drawable.catalog_reading, Modifier.size(120.dp), mascot = true)
                    Text(
                        stringResource(
                            if (fixedChapter) R.string.catalog_chapter_end
                            else R.string.catalog_textbook_end,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
    }

    }
}
