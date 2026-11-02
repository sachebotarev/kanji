package ru.mobui.kanji.feature.catalog

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.*

class CatalogChapterAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun chapterAnnouncesSelectionWithoutReadingTheEntirePreview() {
        val request = FilterRequest.ChaptersForKanji(1, KanjiFilters(chapter = 3))
        val state = CatalogLoad.Ready(FilterContent.Chapters(listOf(CatalogChapter(3, 3, 12, "一 二 三")), 3, 3..3, false))
        compose.setContent { KanjiTheme { CatalogFilterScreen(request, state, SearchState(), "ru", FilterActions({}, {}, {}, {}, {})) } }
        compose.onNodeWithTag("chapter-3").assertIsSelected()
            .assert(hasText("一 二 三").not())
    }
}
