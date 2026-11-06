package ru.mobui.kanji.feature.catalog.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.CatalogChapter
import ru.mobui.kanji.feature.catalog.R

@Composable
internal fun ChapterChoiceCard(
    chapter: CatalogChapter,
    showWordCount: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ru.mobui.kanji.core.wordui.ChapterChoiceCard(
        title = stringResource(R.string.catalog_chapter, chapter.number),
        summary = if (showWordCount) catalogMaterialSummary(chapter.kanjiCount, chapter.wordCount)
            else pluralStringResource(R.plurals.catalog_kanji_count, chapter.kanjiCount, chapter.kanjiCount),
        preview = chapter.preview,
        selected = selected,
        onClick = onClick,
        modifier = Modifier.testTag("chapter-${chapter.number}"),
    )
}

@Preview(widthDp = 320, locale = "ru", showBackground = true)
@Preview(widthDp = 412, locale = "en", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChapterChoicePreview() {
    KanjiTheme {
        ChapterChoiceCard(CatalogChapter(1, 10, 45, "日 月 木 山 川 田 人 口 車 門"),
            showWordCount = false, selected = false, onClick = {})
    }
}
