package ru.mobui.kanji.feature.catalog.detail

import ru.mobui.kanji.feature.catalog.R
import ru.mobui.kanji.feature.catalog.components.CatalogPanel
import ru.mobui.kanji.feature.catalog.components.CatalogSection
import ru.mobui.kanji.feature.catalog.components.DetailColumn
import ru.mobui.kanji.feature.catalog.components.KanjiTile
import ru.mobui.kanji.feature.catalog.theme.CatalogJapaneseColor
import ru.mobui.kanji.feature.catalog.theme.CatalogJapaneseStyle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.mobui.kanji.domain.catalog.translation
import ru.mobui.kanji.domain.catalog.usageNote

@Composable
internal fun WordDetail(state: DetailContent.Word, language: String, onKanji: (Int) -> Unit) {
    val word = state.word
    if (word == null) {
        Text(stringResource(R.string.catalog_not_found))
        return
    }
    DetailColumn {
        Column {
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val compact =
                    word.written.codePointCount(0, word.written.length) >
                        MAX_INLINE_WRITTEN_LENGTH ||
                        word.reading.codePointCount(0, word.reading.length) >
                            MAX_INLINE_READING_LENGTH
                Text(
                    word.written,
                    Modifier.testTag("word-written"),
                    style =
                        CatalogJapaneseStyle.copy(
                            fontSize = if (compact) 30.sp else 40.sp,
                            lineHeight = if (compact) 40.sp else 48.sp,
                        ),
                    color = CatalogJapaneseColor,
                    textAlign = TextAlign.Center,
                )
                Text(
                    word.reading,
                    style =
                        CatalogJapaneseStyle.copy(
                            fontSize = if (compact) 20.sp else 24.sp,
                            lineHeight = if (compact) 28.sp else 32.sp,
                        ),
                    textAlign = TextAlign.Center,
                )
                Text(
                    word.translation(language) ?: stringResource(R.string.catalog_missing),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        word
            .usageNote(language)
            ?.takeIf { it.isNotBlank() }
            ?.let { note ->
                CatalogPanel(title = stringResource(R.string.catalog_usage)) {
                    Text(note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        if (word.parts.isNotEmpty())
            CatalogPanel(title = stringResource(R.string.catalog_reading_parts)) {
                word.parts.forEach { part -> Text("${part.written} · ${part.reading}") }
            }
        val entries = state.kanji
        if (entries.isNotEmpty())
            CatalogSection(stringResource(R.string.catalog_word_kanji)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    entries.forEach { entry ->
                        KanjiTile(
                            entry,
                            language,
                            { onKanji(entry.id) },
                            Modifier.width(
                                with(androidx.compose.ui.platform.LocalDensity.current) {
                                    116.dp * fontScale.coerceAtLeast(1f)
                                },
                            ),
                        )
                    }
                }
            }
    }
}

private const val MAX_INLINE_WRITTEN_LENGTH = 6
private const val MAX_INLINE_READING_LENGTH = 12
