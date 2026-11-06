package ru.mobui.kanji.feature.catalog.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ru.mobui.kanji.core.wordui.ChapterPickerItem
import ru.mobui.kanji.core.wordui.ChapterPickerScreen
import ru.mobui.kanji.feature.catalog.R
import ru.mobui.kanji.feature.catalog.components.catalogMaterialSummary

@Composable
internal fun CatalogChapterPicker(content: FilterContent.Chapters, actions: FilterActions) {
    val entries = content.entries.map { chapter ->
        ChapterPickerItem(
            chapter.number,
            stringResource(R.string.catalog_chapter, chapter.number),
            if (content.showWordCount) catalogMaterialSummary(chapter.kanjiCount, chapter.wordCount)
            else pluralStringResource(R.plurals.catalog_kanji_count, chapter.kanjiCount, chapter.kanjiCount),
            chapter.preview,
        )
    }
    ChapterPickerScreen(
        entries, content.selectedIds,
        onApply = { actions.select(FilterResult.Chapter(it)) },
        onBack = actions.back,
        backTag = "catalog-back",
        searchTag = "catalog-search-toggle",
    )
}
