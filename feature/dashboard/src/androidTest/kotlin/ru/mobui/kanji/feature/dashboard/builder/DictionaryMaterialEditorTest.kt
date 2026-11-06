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
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogLink
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogSource
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.TagColor
import ru.mobui.kanji.domain.catalog.TagTranslation
import ru.mobui.kanji.domain.catalog.WordTagAssignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class DictionaryMaterialEditorTest {
    @get:Rule val compose = createComposeRule()
    private val sun = '日'.code
    private val moon = '月'.code
    private fun sign(id: Int, text: String) = CatalogKanji(id, text, 4, null, false, true, false,
        mapOf("ru" to listOf(text)), emptyList(), emptyList())
    private val snapshot = CatalogSnapshot(1, CatalogContent(
        listOf(CatalogSource(-7, "Personal", mapOf("ru" to "Мой словарь"), sourceType = "personal"),
            CatalogSource(1, "Book", emptyMap(), sourceType = "textbook")),
        listOf(CatalogWord(-1, "日", "ひ", mapOf("ru" to "солнце")),
            CatalogWord(-2, "月", "つき", mapOf("ru" to "луна")),
            CatalogWord(-3, "ありがとう", "ありがとう", mapOf("ru" to "спасибо")),
            CatalogWord(-4, "日月", "じつげつ", mapOf("ru" to "солнце и луна")),
            CatalogWord(1, "日", "ひ", mapOf("ru" to "из учебника"))),
        listOf(sign(sun, "日"), sign(moon, "月")), emptyList(), emptyList(),
        listOf(CatalogLink(-7, -1, null), CatalogLink(-7, -2, null), CatalogLink(-7, -3, null),
            CatalogLink(-7, -4, null), CatalogLink(-7, -1, sun), CatalogLink(1, 1, sun))))
    private val tags = listOf(DictionaryTag(1, TagColor.Green, listOf(TagTranslation("ru", "SUN"))),
        DictionaryTag(2, TagColor.Blue, listOf(TagTranslation("ru", "MOON"))),
        DictionaryTag(3, TagColor.Neutral, listOf(TagTranslation("ru", "Пустой"))))
    private val assignments = listOf(WordTagAssignment(-1, setOf(1)), WordTagAssignment(-2, setOf(2)),
        WordTagAssignment(-3, setOf(1, 2)), WordTagAssignment(1, setOf(1)))
    private fun initial(tags: Set<Long> = emptySet(), kanji: Set<Int> = emptySet()) =
        RecipeMaterial("personal-material", MaterialType.Dictionary, sourceId = -7, tagIds = tags, kanjiIds = kanji)
    private fun click(tag: String) = compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)

    @Test fun unrestrictedIncludesKanaAndDeduplicatesOnlyPersonalWords() {
        var result: RecipeMaterial? = null
        var opened: Int? = null
        compose.setContent { KanjiTheme {
            DictionaryMaterialEditorScreen(initial(), snapshot, tags, assignments, "ru", { result = it }, {}, { opened = it }, {})
        } }
        click("material-preview")
        compose.onAllNodesWithTag("word--1").assertCountEquals(1)
        compose.onNodeWithTag("word--3").assertExists()
        compose.onNodeWithTag("word-1").assertDoesNotExist()
        click("word--1")
        compose.runOnIdle { assertEquals(-1, opened) }
        click("material-back")
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(), result) }
    }

    @Test fun tagsUseOrAndLiteralKanjiUseAndRatherThanStudyLinks() {
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            DictionaryMaterialEditorScreen(initial(setOf(1, 2), setOf(sun, moon)), snapshot, tags, assignments, "en",
                { result = it }, {}, {}, {}, editing = true)
        } }
        click("material-preview")
        compose.onNodeWithTag("word--1").assertExists()
        compose.onNodeWithTag("word--2").assertExists()
        compose.onNodeWithTag("word--3").assertDoesNotExist()
        compose.onNodeWithTag("word--4").assertDoesNotExist()
        click("material-back")
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(setOf(1, 2), setOf(sun, moon)), result) }
    }

    @Test fun applyingTagsRestrictsSymbolsAndPrunesIncompatibleKanji() {
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            DictionaryMaterialEditorScreen(initial(kanji = setOf(moon)), snapshot, tags, assignments, "ru", { result = it }, {}, {}, {})
        } }
        click("material-tags")
        click("tag-1")
        click("tags-apply")
        click("material-kanji")
        compose.onNodeWithTag("kanji-$moon").assertDoesNotExist()
        click("kanji-$sun")
        click("symbol-picker-apply")
        compose.runOnIdle { assertNull(result) }
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(setOf(1), setOf(sun)), result) }
    }

    @Test fun selectorsAndFormCancelWithoutPublishing() {
        var open by mutableStateOf(true)
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            if (open) DictionaryMaterialEditorScreen(initial(), snapshot, tags, assignments, "ru", { result = it }, { open = false }, {}, {})
            else TextButton({ open = true }, Modifier.testTag("reopen")) { Text("Open") }
        } }
        click("material-tags")
        click("tag-1")
        click("tags-back")
        click("material-tags")
        compose.onNodeWithTag("tag-1").assertIsNotSelected()
        click("tag-2")
        click("tags-apply")
        click("material-kanji")
        click("kanji-$moon")
        click("symbol-picker-back")
        click("material-back")
        click("reopen")
        click("material-tags")
        compose.onNodeWithTag("tag-2").assertIsNotSelected()
        compose.runOnIdle { assertNull(result) }
    }

    @Test fun hiddenTagSearchAndOpenDraftRestoreWithoutPublishing() {
        val restoration = StateRestorationTester(compose)
        var result: RecipeMaterial? = null
        var managed = 0
        restoration.setContent { KanjiTheme {
            DictionaryMaterialEditorScreen(initial(), snapshot, tags, assignments, "en", { result = it }, {}, {}, { managed++ })
        } }
        click("material-tags")
        click("tag-1")
        click("tags-search-toggle")
        compose.onNodeWithTag("tags-search").performTextReplacement("ｍｏｏｎ")
        compose.onNodeWithTag("tag-1").assertDoesNotExist()
        click("tag-2")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("tag-2").assertIsSelected()
        compose.onNodeWithTag("tags-search").performTextClearance()
        compose.onNodeWithTag("tag-1").assertIsSelected()
        click("tags-menu")
        click("tags-manage")
        compose.runOnIdle { assertEquals(1, managed); assertNull(result) }
        click("tags-apply")
        restoration.emulateSavedInstanceStateRestore()
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(setOf(1, 2)), result) }
    }

    @Test fun emptyTagPoolBlocksApplyAndClearRepairsIt() {
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            DictionaryMaterialEditorScreen(initial(setOf(3)), snapshot, tags, assignments, "ru", { result = it }, {}, {}, {})
        } }
        compose.onNodeWithTag("material-preview").assertDoesNotExist()
        click("material-apply")
        compose.onNodeWithTag("material-error").assertExists()
        compose.runOnIdle { assertNull(result) }
        click("material-tags")
        click("tags-clear")
        click("tags-apply")
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(), result) }
    }

    @Test fun deletedTagAndItsAssociationsAreNotReturnedAsDanglingIds() {
        var available by mutableStateOf(tags)
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            DictionaryMaterialEditorScreen(initial(setOf(1)), snapshot, available, assignments, "ru", { result = it }, {}, {}, {})
        } }
        click("material-tags")
        compose.runOnIdle { available = tags.drop(1) }
        click("tags-apply")
        click("material-apply")
        compose.runOnIdle { assertEquals(initial(), result) }
    }

    @Test fun missingPersonalSourceDoesNotBorrowTextbookPool() {
        var result: RecipeMaterial? = null
        val noPersonal = CatalogSnapshot(1, CatalogContent(listOf(snapshot.sources.first { it.id == 1 }), snapshot.words,
            snapshot.kanji, emptyList(), emptyList(), snapshot.links.filter { it.sourceId == 1 }))
        compose.setContent { KanjiTheme {
            DictionaryMaterialEditorScreen(initial(), noPersonal, tags, assignments, "ru", { result = it }, {}, {}, {})
        } }
        click("material-apply")
        compose.onNodeWithTag("material-error").assertExists()
        compose.onNodeWithTag("material-preview").assertDoesNotExist()
        compose.runOnIdle { assertNull(result) }
    }
}
