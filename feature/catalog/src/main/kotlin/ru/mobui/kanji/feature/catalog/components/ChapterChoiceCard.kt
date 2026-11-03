package ru.mobui.kanji.feature.catalog.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.CatalogChapter
import ru.mobui.kanji.feature.catalog.R
import ru.mobui.kanji.feature.catalog.theme.CatalogJapaneseColor
import ru.mobui.kanji.feature.catalog.theme.CatalogJapaneseStyle

@Composable
internal fun ChapterChoiceCard(
    chapter: CatalogChapter,
    showWordCount: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("chapter-${chapter.number}")
            .semantics { this.selected = selected },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(LABEL_WEIGHT), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.catalog_chapter, chapter.number),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (showWordCount) catalogMaterialSummary(chapter.kanjiCount, chapter.wordCount)
                    else pluralStringResource(R.plurals.catalog_kanji_count, chapter.kanjiCount, chapter.kanjiCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                chapter.preview,
                Modifier.weight(PREVIEW_WEIGHT).clearAndSetSemantics {},
                style = CatalogJapaneseStyle.copy(fontSize = 22.sp, lineHeight = 30.sp),
                color = lerp(MaterialTheme.colorScheme.onSurfaceVariant, CatalogJapaneseColor, PREVIEW_COLOR_STRENGTH),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(widthDp = 320, locale = "ru", showBackground = true)
@Preview(widthDp = 412, locale = "en", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChapterChoicePreview() {
    KanjiTheme {
        ChapterChoiceCard(
            CatalogChapter(1, PREVIEW_KANJI_COUNT, PREVIEW_WORD_COUNT, "日 月 木 山 川 田 人 口 車 門"),
            showWordCount = false,
            selected = false,
            onClick = {},
        )
    }
}

private const val PREVIEW_KANJI_COUNT = 10
private const val PREVIEW_WORD_COUNT = 45
private const val LABEL_WEIGHT = 0.4f
private const val PREVIEW_WEIGHT = 0.6f
private const val PREVIEW_COLOR_STRENGTH = 0.55f
