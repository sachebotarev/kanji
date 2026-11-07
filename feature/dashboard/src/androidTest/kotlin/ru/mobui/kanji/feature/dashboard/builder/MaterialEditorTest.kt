package ru.mobui.kanji.feature.dashboard.builder

import android.content.res.Configuration
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.TagColor
import ru.mobui.kanji.domain.catalog.TagTranslation
import ru.mobui.kanji.domain.catalog.WordTagAssignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MaterialEditorTest {
    @get:Rule val compose = createComposeRule()
    private val sun = '日'.code
    private val moon = '月'.code
    private fun sign(id: Int, text: String) = CatalogKanji(id, text, 4, null, false, true, false,
        mapOf("ru" to listOf(text)), emptyList(), emptyList())
    private val snapshot = CatalogSnapshot(1, CatalogContent(
        listOf(CatalogSource(1, "Book", mapOf("ru" to "Учебник"), sourceType = "textbook"),
            CatalogSource(-1, "Personal", mapOf("ru" to "Словарь"), sourceType = "personal")),
        listOf(CatalogWord(1, "日", "ひ", mapOf("ru" to "солнце")), CatalogWord(-2, "月", "つき", mapOf("ru" to "луна"))),
        listOf(sign(sun, "日"), sign(moon, "月")), emptyList(), listOf(CatalogPlacement(1, sun, 1, 1)),
        listOf(CatalogLink(1, 1, sun), CatalogLink(-1, -2, null))))
    private val tags = listOf(DictionaryTag(1, TagColor.Blue, listOf(TagTranslation("ru", "Луна"))))
    private val assignments = listOf(WordTagAssignment(-2, setOf(1)))
    private val initial = RecipeMaterial("same-material", MaterialType.Book, sourceId = 1, chapters = setOf(1))
    private fun click(tag: String) = compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)

    @Test fun largeEnglishTypeLabelsHaveEqualHeightWithoutFixedTextClipping() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.ENGLISH) }
        val localized = context.createConfigurationContext(configuration)
        compose.setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration,
                LocalDensity provides Density(context.resources.displayMetrics.density, 1.5f)) {
                KanjiTheme { MaterialEditorScreen(initial, snapshot, tags, assignments, "en", {}, {}, {}, {},
                    Modifier.width(320.dp)) }
            }
        }
        compose.onNodeWithText("From a dictionary").assertIsDisplayed()
        val book = compose.onNodeWithTag("material-type-book").fetchSemanticsNode().boundsInRoot
        val dictionary = compose.onNodeWithTag("material-type-dictionary").fetchSemanticsNode().boundsInRoot
        assertEquals(book.height, dictionary.height, 1f)
        assertEquals(book.top, dictionary.top, 1f)
        val kanji = compose.onNodeWithTag("material-type-kanji").fetchSemanticsNode().boundsInRoot
        val words = compose.onNodeWithTag("material-type-words").fetchSemanticsNode().boundsInRoot
        assertEquals(kanji.height, words.height, 1f)
        compose.onNodeWithTag("material-type-book").assertIsSelected()
        click("material-type-dictionary")
        compose.onNodeWithTag("material-type-dictionary").assertIsSelected()
    }

    @Test fun switchingTypesRetainsDraftsAndReturnsOnlyCurrentTypeWithSameId() {
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme { MaterialEditorScreen(initial, snapshot, tags, assignments, "ru", { result = it }, {}, {}, {}) } }
        compose.onNodeWithTag("material-type-book").assertIsSelected()
        click("material-type-words")
        click("material-words")
        compose.onNodeWithTag("material-type-words").assertDoesNotExist()
        click("word--2")
        click("word-picker-apply")
        click("material-type-book")
        click("material-chapters")
        compose.onNodeWithTag("chapter-1").assertIsSelected()
        click("symbol-picker-back")
        click("material-type-words")
        click("material-words")
        compose.onNodeWithTag("word--2").assertIsSelected()
        click("word-picker-apply")
        compose.runOnIdle { assertNull(result) }
        click("material-apply")
        compose.runOnIdle { assertEquals(RecipeMaterial(initial.id, MaterialType.Words, wordIds = setOf(-2)), result) }
    }

    @Test fun dictionaryAndKanjiDraftsAreIndependentAndUseActualSharedSelectors() {
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme { MaterialEditorScreen(initial, snapshot, tags, assignments, "ru", { result = it }, {}, {}, {}) } }
        click("material-type-dictionary")
        click("material-tags")
        click("tag-1")
        click("tags-apply")
        click("material-kanji")
        compose.onNodeWithTag("kanji-$sun").assertDoesNotExist()
        click("kanji-$moon")
        click("symbol-picker-apply")
        click("material-type-kanji")
        click("material-kanji")
        compose.onNodeWithTag("kanji-$moon").assertIsNotSelected()
        click("kanji-$sun")
        click("symbol-picker-apply")
        click("material-type-dictionary")
        click("material-kanji")
        compose.onNodeWithTag("kanji-$moon").assertIsSelected()
        click("symbol-picker-back")
        click("material-apply")
        compose.runOnIdle { assertEquals(RecipeMaterial(initial.id, MaterialType.Dictionary, sourceId = -1,
            tagIds = setOf(1), kanjiIds = setOf(moon)), result) }
    }

    @Test fun inactiveDraftsAndOpenPickerSurviveRestoration() {
        val restoration = StateRestorationTester(compose)
        var result: RecipeMaterial? = null
        restoration.setContent { KanjiTheme { MaterialEditorScreen(initial, snapshot, tags, assignments, "ru", { result = it }, {}, {}, {}) } }
        click("material-type-words")
        click("material-words")
        click("word--2")
        click("word-picker-apply")
        click("material-type-book")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("material-type-book").assertIsSelected()
        click("material-type-words")
        click("material-words")
        compose.onNodeWithTag("word--2").assertIsSelected()
        click("word-1")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("word-1").assertIsSelected()
        compose.onNodeWithTag("word--2").assertIsSelected()
        compose.runOnIdle { assertNull(result) }
        click("word-picker-apply")
        click("material-apply")
        compose.runOnIdle { assertEquals(RecipeMaterial(initial.id, MaterialType.Words, wordIds = setOf(1, -2)), result) }
    }

    @Test fun validationBelongsToEachTypeAndDoesNotPublishOtherDrafts() {
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme { MaterialEditorScreen(initial, snapshot, tags, assignments, "ru", { result = it }, {}, {}, {}) } }
        click("material-type-words")
        click("material-apply")
        compose.onNodeWithTag("material-error").assertExists()
        click("material-type-kanji")
        compose.onNodeWithTag("material-error").assertDoesNotExist()
        click("material-type-words")
        compose.onNodeWithTag("material-error").assertExists()
        compose.runOnIdle { assertNull(result) }
        click("material-type-kanji")
        click("material-apply")
        compose.runOnIdle { assertEquals(RecipeMaterial(initial.id, MaterialType.Kanji), result) }
    }

    @Test fun abandoningHostDiscardsAllTypeDrafts() {
        var open by mutableStateOf(true)
        var result: RecipeMaterial? = null
        compose.setContent { KanjiTheme {
            if (open) MaterialEditorScreen(initial, snapshot, tags, assignments, "ru", { result = it }, { open = false }, {}, {})
            else TextButton({ open = true }, Modifier.testTag("reopen")) { Text("Open") }
        } }
        click("material-type-kanji")
        click("material-kanji")
        click("kanji-$moon")
        click("symbol-picker-apply")
        click("material-back")
        click("reopen")
        compose.onNodeWithTag("material-type-book").assertIsSelected()
        click("material-type-kanji")
        click("material-kanji")
        compose.onNodeWithTag("kanji-$moon").assertIsNotSelected()
        compose.runOnIdle { assertNull(result) }
    }

    @Test fun managementAndWordCallbacksAreForwardedWithoutApplyingMaterial() {
        var result: RecipeMaterial? = null
        var managed = 0
        var word: Int? = null
        compose.setContent { KanjiTheme { MaterialEditorScreen(initial, snapshot, tags, assignments, "ru",
            { result = it }, {}, { word = it }, { managed++ }) } }
        click("material-type-dictionary")
        click("material-tags")
        click("tags-menu")
        click("tags-manage")
        click("tags-back")
        click("material-preview")
        click("word--2")
        compose.runOnIdle { assertEquals(1, managed); assertEquals(-2, word); assertNull(result) }
    }
}
