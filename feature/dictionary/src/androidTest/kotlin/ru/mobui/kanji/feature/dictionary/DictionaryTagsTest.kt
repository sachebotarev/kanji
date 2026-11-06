package ru.mobui.kanji.feature.dictionary

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.waitUntil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogKanji
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.PersonalDictionaryRepository
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.RecipesRepository
import ru.mobui.kanji.domain.catalog.TagColor
import ru.mobui.kanji.domain.catalog.TagDraft
import ru.mobui.kanji.domain.catalog.TagTranslation
import ru.mobui.kanji.domain.catalog.WordDraft
import ru.mobui.kanji.domain.catalog.WordTagAssignment

class DictionaryTagsTest {
    @get:Rule val compose = createComposeRule()
    private val repository = TagsRepository()
    private val stores = mutableListOf<ViewModelStore>()

    @org.junit.After fun clearModels() { compose.runOnIdle { stores.forEach { it.clear() } } }
    private fun model(saved: SavedStateHandle = SavedStateHandle()): TagSelectionViewModel {
        lateinit var result: TagSelectionViewModel
        compose.runOnIdle {
            result = TagSelectionViewModel(saved, repository)
            stores += ViewModelStore().apply { put("tags", result) }
        }
        compose.waitUntil { !result.state.value.loading }
        return result
    }
    private fun tag(name: String) = TagDraft(TagColor.Blue, listOf(TagTranslation("ru", name)))

    @Test fun dictionaryCombinesKanjiOrWithTagsAndSearchAndRestoresFilter() {
        fun sign(symbol: String) = CatalogKanji(symbol.codePointAt(0), symbol, 4, null, false, false, false,
            mapOf("ru" to listOf(symbol)), emptyList(), emptyList())
        val words = listOf(CatalogWord(11, "日", "たいよう", mapOf("ru" to "солнце")),
            CatalogWord(12, "月", "つき", mapOf("ru" to "луна")), CatalogWord(13, "日月", "にちげつ", mapOf("ru" to "дни")))
        val snapshot = CatalogSnapshot(1, CatalogContent(emptyList(), words, listOf(sign("日"), sign("月")), emptyList(), emptyList(), emptyList()))
        val personal = object : PersonalDictionaryRepository {
            override fun observeWords(): Flow<List<CatalogWord>> = MutableStateFlow(words)
            override suspend fun saveWord(id: Int?, draft: WordDraft): Int = error("Not used")
            override suspend fun deleteWord(id: Int) = error("Not used")
        }
        repository.assignments.value = listOf(WordTagAssignment(11, setOf(1)), WordTagAssignment(12, setOf(2)), WordTagAssignment(13, setOf(1, 2)))
        val saved = SavedStateHandle()
        lateinit var vm: DictionaryViewModel
        compose.runOnIdle {
            vm = DictionaryViewModel(saved, personal, object : CatalogRepository { override suspend fun load() = snapshot }, "ru", repository)
            stores += ViewModelStore().apply { put("dictionary", vm) }
        }
        compose.setContent { androidx.compose.runtime.LaunchedEffect(Unit) { vm.visible.collect {} } }
        compose.waitUntil { vm.visible.value.size == 3 }
        compose.runOnIdle { vm.kanji(setOf('日'.code, '月'.code)); vm.tags(setOf(1)) }
        compose.waitUntil { vm.visible.value.map { it.id } == listOf(11, 13) }
        compose.runOnIdle { vm.query("たいよう") }
        compose.waitUntil { vm.visible.value.map { it.id } == listOf(11) }
        compose.runOnIdle {
            assertEquals(setOf('日'.code, '月'.code), saved.get<IntArray>("kanji-filter")?.toSet())
            vm.query(""); vm.kanji(setOf('月'.code))
        }
        compose.waitUntil { vm.visible.value.map { it.id } == listOf(13) }
        compose.runOnIdle { vm.kanji(emptySet()) }
        compose.waitUntil { vm.visible.value.map { it.id } == listOf(11, 13) }
    }

    @Test fun stagedChangesDoNotPublishAndCancelReopensOriginalSelection() {
        val model = model()
        compose.runOnIdle {
            model.start(setOf(1), emptyList())
            model.toggle(2)
            model.create(tag("Черновик"), true) {}
            assertEquals(setOf(1L, 2L), model.selectedIds())
            assertEquals(1, model.selectedPending().size)
            assertEquals(2, repository.tags.value.size)
            model.finish()
            model.start(setOf(1), emptyList())
            assertEquals(setOf(1L), model.selectedIds())
            assertTrue(model.selectedPending().isEmpty())
        }
    }

    @Test fun restoredSessionKeepsSelectedPendingQueryAndManagement() {
        val saved = SavedStateHandle()
        val first = model(saved)
        compose.runOnIdle {
            first.start(emptySet(), emptyList())
            first.toggle(2)
            first.create(tag("Поездка"), true) {}
            first.query("По")
            first.manage(true)
        }
        // Recreate from the same serializable values, not the old ViewModel state.
        val restoredHandle = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
        val restored = model(restoredHandle)
        compose.runOnIdle {
            restored.start(emptySet(), emptyList())
            assertEquals(setOf(2L), restored.selectedIds())
            assertEquals("Поездка", restored.selectedPending().single().translations.single().name)
            assertEquals("По", restored.state.value.query)
            assertTrue(restored.state.value.manage)
        }
    }

    @Test fun draftDuplicatesUseUnicodeCaseFoldingAndDeletedIdsArePruned() {
        val model = model()
        compose.runOnIdle {
            model.start(setOf(1), emptyList())
            model.create(tag("Straße"), true) {}
            model.create(tag("STRASSE"), true) { fail("Duplicate was accepted") }
            assertTrue(model.state.value.error)
            assertEquals(1, model.state.value.pending.size)
            repository.tags.value = repository.tags.value.filterNot { it.id == 1L }
        }
        compose.waitUntil { 1L !in model.state.value.selected }
        compose.runOnIdle { assertTrue(model.selectedIds().isEmpty()) }
    }

    @Test fun applyReturnsStagedIdsWithoutChangingRepository() {
        var applied: Set<Long>? = null
        compose.setContent {
            KanjiTheme {
                DictionaryTagsRoute(repository, "apply", setOf(1), emptyList(), false, "ru",
                    { ids, _ -> applied = ids }, {})
            }
        }
        compose.onNodeWithTag("tag-2").performClick().assertIsSelected()
        assertNull(applied)
        compose.onNodeWithTag("tags-apply").performClick()
        compose.runOnIdle {
            assertEquals(setOf(1L, 2L), applied)
            assertEquals(0, repository.writes)
        }
    }

    @Test fun tagUsageCountsFollowAssignmentsAndDeduplicateWords() {
        repository.assignments.value = listOf(WordTagAssignment(10, setOf(1, 2)),
            WordTagAssignment(11, setOf(1)), WordTagAssignment(10, setOf(1)))
        val vm = model()
        compose.runOnIdle { assertEquals(mapOf(1L to 2, 2L to 1), vm.state.value.usageCounts) }
        compose.runOnIdle { repository.assignments.value = listOf(WordTagAssignment(11, setOf(2))) }
        compose.waitUntil { vm.state.value.usageCounts == mapOf(2L to 1) }
    }

    @Test fun editingMissingEnglishTranslationDoesNotCopyRussianFallback() {
        compose.setContent {
            KanjiTheme { DictionaryTagsRoute(repository, "tag-english", emptySet(), emptyList(), false,
                "en", { _, _ -> }, {}, managementOnly = true) }
        }
        compose.onNodeWithTag("tag-1").performClick()
        compose.onNodeWithTag("tag-name").assert(!hasText("Первый", substring = true)).performTextInput("First")
        compose.runOnIdle { assertEquals(0, repository.writes) }
        compose.onNodeWithTag("tag-save").performClick()
        compose.waitUntil { repository.writes == 1 }
        compose.runOnIdle {
            val tag = repository.tags.value.single { it.id == 1L }
            assertEquals("Первый", tag.name("ru"))
            assertEquals("First", tag.name("en"))
        }
    }

    @Test fun managementHasIndependentRowDeleteWithCancelAndConfirmation() {
        compose.setContent {
            KanjiTheme { DictionaryTagsRoute(repository, "management", emptySet(), emptyList(), false,
                "ru", { _, _ -> }, {}, managementOnly = true) }
        }
        compose.onNodeWithTag("tag-delete-1").performClick()
        compose.onNodeWithTag("tag-delete-confirm").assertExists()
        compose.onNodeWithText("Отмена").performClick()
        compose.runOnIdle { assertEquals(0, repository.writes) }
        compose.onNodeWithTag("tag-1").performClick()
        compose.onNodeWithTag("tag-name").assertExists()
        compose.onNodeWithTag("tag-delete-confirm").assertDoesNotExist()
        compose.onNodeWithText("Отмена").performClick()
        compose.onNodeWithTag("tag-delete-1").performClick()
        compose.onNodeWithTag("tag-delete-confirm").performClick()
        compose.waitUntil { repository.tags.value.none { it.id == 1L } }
        compose.runOnIdle { assertEquals(1, repository.writes) }
    }

    @Test fun searchDoesNotDiscardHiddenSelectionAndSelectedViewCanReturn() {
        compose.setContent {
            KanjiTheme { DictionaryTagsRoute(repository, "search", setOf(1), emptyList(), false, "ru", { _, _ -> }, {}) }
        }
        compose.onNodeWithTag("tags-search-toggle").performClick()
        compose.onNodeWithTag("tags-search").performTextReplacement("Второй")
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("tag-2").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("tag-1").assertDoesNotExist()
        compose.onNodeWithTag("tag-2").performClick()
        compose.onNodeWithTag("tags-search").performTextClearance()
        compose.onNodeWithTag("tag-1").assertIsSelected()
        compose.onNodeWithTag("tag-2").assertIsSelected()
        compose.onNodeWithTag("tags-selected-only").performClick().performClick()
        compose.onNodeWithTag("tag-1").assertIsSelected()
    }
}

private class TagsRepository : RecipesRepository {
    val tags = MutableStateFlow(listOf(
        DictionaryTag(1, TagColor.Green, listOf(TagTranslation("ru", "Первый"))),
        DictionaryTag(2, TagColor.Blue, listOf(TagTranslation("ru", "Второй"))),
    ))
    var writes = 0
    override fun observeTags(): Flow<List<DictionaryTag>> = tags
    val assignments = MutableStateFlow<List<WordTagAssignment>>(emptyList())
    override fun observeWordTags(): Flow<List<WordTagAssignment>> = assignments
    override fun observeRecipes(): Flow<List<Recipe>> = MutableStateFlow(emptyList())
    override suspend fun saveTag(id: Long?, draft: TagDraft): Long {
        writes++
        val key = id ?: (tags.value.maxOfOrNull { it.id } ?: 0) + 1
        tags.value = tags.value.filterNot { it.id == key } + DictionaryTag(key, draft.color, draft.translations)
        return key
    }
    override suspend fun deleteTag(id: Long) { writes++; tags.value = tags.value.filterNot { it.id == id } }
    override suspend fun assignTags(wordId: Int, tagIds: Set<Long>) { error("Not used") }
    override suspend fun saveRecipe(recipe: Recipe) { error("Not used") }
    override suspend fun activateRecipe(id: String) { error("Not used") }
    override suspend fun reorderRecipes(ids: List<String>) { error("Not used") }
    override suspend fun deleteRecipe(id: String) { error("Not used") }
}
