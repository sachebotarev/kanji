package ru.mobui.kanji.feature.dashboard.builder

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogLink
import ru.mobui.kanji.domain.catalog.CatalogPlacement
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogSource
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class BookMaterialEditorTest {
    @get:Rule val compose = createComposeRule()
    private val sun = '日'.code
    private val moon = '月'.code
    private val fire = '火'.code
    private fun sign(id: Int, text: String) = CatalogKanji(id, text, 4, null, false, true, false,
        mapOf("ru" to listOf(text)), emptyList(), emptyList())
    private val snapshot = CatalogSnapshot(1, CatalogContent(
        listOf(CatalogSource(1, "Book A", mapOf("ru" to "Учебник А"), sourceType = "textbook"),
            CatalogSource(2, "Book B", mapOf("ru" to "Учебник Б"), sourceType = "book"),
            CatalogSource(-1, "Personal", mapOf("ru" to "Мой словарь"), sourceType = "personal")),
        listOf(CatalogWord(1, "月", "つき", mapOf("ru" to "Учебная связь с 日")),
            CatalogWord(2, "月曜日", "げつようび", mapOf("ru" to "понедельник")),
            CatalogWord(4, "日", "ひ", mapOf("ru" to "Другое издание")),
            CatalogWord(-5, "日", "ひ", mapOf("ru" to "Личная запись"))),
        listOf(sign(sun, "日"), sign(moon, "月"), sign(fire, "火")), emptyList(),
        listOf(CatalogPlacement(1, sun, 1, 1), CatalogPlacement(1, moon, 2, 2),
            CatalogPlacement(1, fire, 3, 3), CatalogPlacement(2, sun, 1, 7)),
        listOf(CatalogLink(1, 1, sun), CatalogLink(1, 2, moon), CatalogLink(1, 2, sun),
            CatalogLink(2, 4, sun), CatalogLink(-1, -5, sun))))
    private fun initial(source: Int? = 1, chapters: Set<Int> = emptySet(), kanji: Set<Int> = emptySet()) =
        RecipeMaterial("book-material", MaterialType.Book, sourceId = source, chapters = chapters, kanjiIds = kanji)
    private fun click(tag: String) = compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)

    @Test fun firstBookIsSelectedByDefaultAndOnlyBookSourcesAreOffered() {
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            BookMaterialEditorScreen(initial(null), snapshot, "ru", { result = it }, {}, {})
        } }
        compose.onNodeWithTag("material-book-1").assertIsSelected()
        compose.onNodeWithTag("material-book-2").assertExists()
        compose.onNodeWithTag("material-book--1").assertDoesNotExist()
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(), result) }
    }

    @Test fun missingBookInExistingMaterialIsNotSilentlyReplacedByDefault() {
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            BookMaterialEditorScreen(initial(99), snapshot, "ru", { result = it }, {}, {}, editing = true)
        } }
        compose.onNodeWithTag("material-book-1").assertIsNotSelected()
        click("material-apply")
        compose.onNodeWithTag("material-error").assertExists()
        compose.runOnIdle { assertNull(result) }
    }

    @Test fun actualStudyLinksAndOrSetsStayInsideSelectedTextbook() {
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            BookMaterialEditorScreen(initial(chapters = setOf(1, 2), kanji = setOf(sun, moon)), snapshot, "ru",
                { result = it }, {}, {}, editing = true)
        } }
        click("material-preview")
        compose.onNodeWithTag("word-1").assertExists()
        compose.onAllNodesWithTag("word-2").assertCountEquals(1)
        compose.onNodeWithTag("word-4").assertDoesNotExist()
        compose.onNodeWithTag("word--5").assertDoesNotExist()
        click("material-back")
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(chapters = setOf(1, 2), kanji = setOf(sun, moon)), result) }
    }

    @Test fun chapterRangeRestrictsKanjiAndSwitchingBookPrunesOldChoices() {
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            BookMaterialEditorScreen(initial(), snapshot, "ru", { result = it }, {}, {})
        } }
        click("material-chapters")
        compose.onNodeWithTag("chapter-7").assertDoesNotExist()
        click("chapter-2")
        click("chapter-apply")
        click("material-kanji")
        compose.onNodeWithTag("kanji-$sun").assertDoesNotExist()
        click("kanji-$moon")
        click("symbol-picker-apply")
        compose.runOnIdle { assertNull(result) }
        click("material-book-2")
        click("material-preview")
        compose.onNodeWithTag("word-4").assertExists()
        compose.onNodeWithTag("word-2").assertDoesNotExist()
        click("material-back")
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(2), result) }
    }

    @Test fun cancellingSelectorsAndWholeFormDoesNotPublish() {
        var open by mutableStateOf(true)
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            if (open) BookMaterialEditorScreen(initial(), snapshot, "ru", { result = it }, { open = false }, {})
            else TextButton({ open = true }, Modifier.testTag("reopen")) { Text("Open") }
        } }
        click("material-chapters")
        click("chapter-2")
        click("symbol-picker-back")
        click("material-chapters")
        compose.onNodeWithTag("chapter-2").assertIsNotSelected()
        click("chapter-2")
        click("chapter-apply")
        click("material-back")
        click("reopen")
        click("material-chapters")
        compose.onNodeWithTag("chapter-2").assertIsNotSelected()
        compose.runOnIdle { assertNull(result) }
    }

    @Test fun emptyStudyPoolBlocksApplyAndClearingRestrictionsRepairsIt() {
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            BookMaterialEditorScreen(initial(chapters = setOf(3)), snapshot, "ru", { result = it }, {}, {})
        } }
        compose.onNodeWithTag("material-preview").assertDoesNotExist()
        click("material-apply")
        compose.onNodeWithTag("material-error").assertExists()
        compose.runOnIdle { assertNull(result) }
        click("material-chapters")
        click("chapter-clear")
        click("chapter-apply")
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(), result) }
    }

    @Test fun literalSpellingDoesNotBorrowStudyLinksFromAnotherChapter() {
        compose.setContent { KanjiTheme {
            BookMaterialEditorScreen(initial(chapters = setOf(2), kanji = setOf(moon)), snapshot, "ru", {}, {}, {})
        } }
        click("material-preview")
        compose.onNodeWithTag("word-1").assertDoesNotExist()
        compose.onNodeWithTag("word-2").assertExists()
        click("material-back")
        click("material-chapters")
        compose.onNodeWithTag("chapter-1").assertDoesNotExist()
        compose.onNodeWithTag("chapter-2").assertExists()
    }

    @Test fun restoredChapterDraftIsNotPublishedUntilFinalApply() {
        val restoration = StateRestorationTester(compose)
        var result: RecipeMaterial? = null
        restoration.setContent { KanjiTheme {
            BookMaterialEditorScreen(initial(), snapshot, "ru", { result = it }, {}, {})
        } }
        click("material-chapters")
        click("chapter-2")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("chapter-2").assertIsSelected()
        compose.runOnIdle { assertNull(result) }
        click("chapter-apply")
        restoration.emulateSavedInstanceStateRestore()
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(chapters = setOf(2)), result) }
    }
}
