package ru.mobui.kanji.feature.dashboard.builder

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.waitUntil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.PersonalDictionaryRepository
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.RecipeName
import ru.mobui.kanji.domain.catalog.RecipesRepository
import ru.mobui.kanji.domain.catalog.TagDraft
import ru.mobui.kanji.domain.catalog.WordDraft
import ru.mobui.kanji.domain.catalog.WordTagAssignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class RecipeLibraryTest {
    @get:Rule val compose = createComposeRule()
    private val stores = mutableListOf<ViewModelStore>()
    private val word = CatalogWord(-1, "かな", "かな", mapOf("ru" to "кана"))
    private val snapshot = CatalogSnapshot(1, CatalogContent(emptyList(), listOf(word), emptyList(), emptyList(), emptyList(), emptyList()))
    private val first = Recipe("a", listOf(RecipeName("ru", "Луна"), RecipeName("en", "Moon")),
        listOf(RecipeMaterial("ma", MaterialType.Words, wordIds = setOf(-1))), isActive = true)
    private val second = first.copy(id = "b", names = listOf(RecipeName("ru", "Солнце")), isActive = false,
        materials = listOf(first.materials.single().copy(id = "mb")))
    private val catalog = object : CatalogRepository { override suspend fun load() = snapshot }
    private val personal = object : PersonalDictionaryRepository {
        override fun observeWords() = MutableStateFlow(listOf(word))
        override suspend fun saveWord(id: Int?, draft: WordDraft): Int = error("unused")
        override suspend fun deleteWord(id: Int) = error("unused")
    }
    private inner class Learning : RecipesRepository {
        val recipes = MutableStateFlow(listOf(first, second))
        var gate: CompletableDeferred<Unit>? = null
        var fail = false
        var writes = 0
        override fun observeRecipes() = recipes
        override fun observeTags() = MutableStateFlow<List<DictionaryTag>>(emptyList())
        override fun observeWordTags() = MutableStateFlow<List<WordTagAssignment>>(emptyList())
        override suspend fun saveRecipe(recipe: Recipe) {
            writes++; gate?.await(); check(!fail)
            recipes.value = recipes.value.filterNot { it.id == recipe.id } + recipe
        }
        override suspend fun activateRecipe(id: String) { recipes.value = recipes.value.map { it.copy(isActive = it.id == id) } }
        override suspend fun reorderRecipes(ids: List<String>) { writes++; recipes.value = ids.map { id -> recipes.value.single { it.id == id } } }
        override suspend fun deleteRecipe(id: String) { recipes.value = recipes.value.filterNot { it.id == id } }
        override suspend fun saveTag(id: Long?, draft: TagDraft): Long = error("unused")
        override suspend fun deleteTag(id: Long) = error("unused")
        override suspend fun assignTags(wordId: Int, tagIds: Set<Long>) = error("unused")
    }
    @After fun clear() { compose.runOnIdle { stores.forEach { it.clear() } } }
    private fun model(repo: Learning, saved: SavedStateHandle = SavedStateHandle()): RecipeLibraryViewModel {
        lateinit var result: RecipeLibraryViewModel
        compose.runOnIdle { result = RecipeLibraryViewModel(saved, catalog, personal, repo)
            stores += ViewModelStore().also { it.put("library", result) } }
        compose.waitUntil(5_000) { !result.state.value.loading }
        return result
    }
    private fun click(tag: String) = compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
    private fun ready(tag: String) = compose.waitUntil(5_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun actions(
        select: (String) -> Unit = {},
        reorder: (List<String>) -> Unit = {},
        open: (String) -> Unit = {},
        query: (String) -> Unit = {},
    ) = RecipeLibraryActions({}, {}, open, select, reorder, query, {})

    @Test fun selectionReorderSearchGuardAndActiveDeletionUsePersistentChoice() {
        val repo = Learning(); val saved = SavedStateHandle(); val model = model(repo, saved)
        compose.runOnIdle { model.select("b") }
        compose.waitUntil { model.state.value.recipes.single { it.isActive }.id == "b" }
        compose.runOnIdle { model.reorder(listOf("b", "a")) }
        compose.waitUntil { model.state.value.recipes.first().id == "b" }
        compose.runOnIdle { model.query("Moon"); model.reorder(listOf("a", "b")) }
        assertEquals("Moon", saved.get<String>("query"))
        assertEquals("b", model.state.value.recipes.first().id)
        compose.runOnIdle { model.delete("b") }
        compose.waitUntil { model.state.value.recipes.size == 1 }
        assertFalse(model.state.value.recipes.single().isActive)
    }
    @Test fun duplicateIsPrivateRestorableDraftAndBusyFailureCanRetry() {
        val repo = Learning(); val saved = SavedStateHandle()
        fun copyModel(handle: SavedStateHandle): RecipeEditorViewModel {
            lateinit var result: RecipeEditorViewModel
            compose.runOnIdle {
                result = RecipeEditorViewModel(handle, catalog, personal, repo, null, first.id) { "$it (copy)" }
                stores += ViewModelStore().also { it.put("copy", result) }
            }
            compose.waitUntil(5_000) { result.state.value.initial != null && !result.state.value.loading }
            return result
        }
        val model = copyModel(saved)
        val copy = requireNotNull(model.state.value.initial)
        assertEquals(0, repo.writes); assertEquals(2, repo.recipes.value.size)
        assertNotEquals(first.id, copy.id); assertNotEquals(first.materials.single().id, copy.materials.single().id)
        assertEquals(1L, copy.revision); assertFalse(copy.isActive)
        assertEquals(first.materials.single().wordIds, copy.materials.single().wordIds)
        assertEquals("Луна (copy)", recipeName(copy, "ru")); assertEquals("Moon (copy)", recipeName(copy, "en"))
        repo.recipes.value = listOf(first.copy(names = listOf(RecipeName("ru", "Changed"))), second)
        val restored = copyModel(SavedStateHandle(mapOf("new-recipe-id" to copy.id,
            "copy-initial" to requireNotNull(saved.get<android.os.Bundle>("copy-initial")))))
        assertEquals(copy, restored.state.value.initial); assertEquals(0, repo.writes)
        repo.gate = CompletableDeferred(); repo.fail = true
        compose.runOnIdle { model.save(copy); model.save(copy) }
        assertEquals(1, repo.writes)
        compose.runOnIdle { repo.gate!!.complete(Unit) }
        compose.waitUntil { !model.state.value.saving && model.state.value.writeError }
        assertEquals(2, repo.recipes.value.size)
        repo.fail = false; repo.gate = null
        compose.runOnIdle { model.save(copy) }
        compose.waitUntil { model.state.value.completed }
        assertEquals(3, repo.recipes.value.size); assertEquals(copy, repo.recipes.value.last())
        assertTrue(repo.recipes.value.single { it.id == first.id }.isActive)
    }

    @Test fun missingDuplicateSourceDoesNotBecomeAnEmptyNewRecipe() {
        val repo = Learning(); repo.recipes.value = emptyList()
        lateinit var model: RecipeEditorViewModel
        compose.runOnIdle {
            model = RecipeEditorViewModel(SavedStateHandle(), catalog, personal, repo, null, first.id)
            stores += ViewModelStore().also { it.put("missing-copy", model) }
        }
        compose.waitUntil(5_000) { model.state.value.loadError == RecipeLoadError.Missing }
        assertNull(model.state.value.initial); assertEquals(0, repo.writes)
    }

    @Test fun onlyRadioSelectsAndBodyOpensViewerWithoutSelecting() {
        var selected = ""; var opened = ""
        compose.setContent { KanjiTheme { RecipeLibraryScreen(RecipeLibraryState(listOf(first, second), snapshot, loading = false), "", "ru",
            actions(select = { selected = it }, open = { opened = it })) } }
        compose.onNodeWithTag("library-open-a").performClick()
        assertEquals("", selected); assertEquals("a", opened)
        click("library-select-b"); assertEquals("b", selected)
        compose.onNodeWithTag("library-menu-a").assertDoesNotExist()
    }
    @Test fun emptyLibraryUsesHeaderMascotWithoutIntroAndSearchMissKeepsIntro() {
        compose.setContent {
            KanjiTheme {
                RecipeLibraryScreen(
                    RecipeLibraryState(emptyList(), snapshot, loading = false),
                    "",
                    "ru",
                    actions(),
                )
            }
        }
        compose.onNodeWithTag("library-empty").assertExists()
        compose.onNodeWithTag("library-empty-mascot").assertExists()
        compose.onNodeWithText("Выберите или создайте").assertDoesNotExist()

        var query by mutableStateOf("неттакого")
        compose.setContent {
            KanjiTheme {
                RecipeLibraryScreen(
                    RecipeLibraryState(listOf(first, second), snapshot, loading = false),
                    query,
                    "ru",
                    actions(query = { query = it }),
                )
            }
        }
        compose.onNodeWithText("Выберите или создайте").assertExists()
        compose.onNodeWithTag("library-search-empty").assertExists()
        compose.onNodeWithTag("library-empty").assertDoesNotExist()
    }
    @Test fun normalizedSearchRetainsSelectionAndDisablesAccessibleReorder() {
        var query by mutableStateOf(""); var moved = emptyList<String>()
        val restore = StateRestorationTester(compose)
        restore.setContent { KanjiTheme { RecipeLibraryScreen(RecipeLibraryState(listOf(first, second), snapshot, loading = false), query, "ru",
            actions(reorder = { moved = it }, query = { query = it })) } }
        click("library-search-toggle"); compose.onNodeWithTag("library-search").performTextReplacement("ＭＯＯＮ")
        compose.onNodeWithTag("library-b").assertDoesNotExist(); compose.onNodeWithTag("library-select-a").assertIsSelected()
        assertTrue(compose.onNodeWithTag("library-a").fetchSemanticsNode().config[SemanticsActions.CustomActions].isEmpty())
        restore.emulateSavedInstanceStateRestore(); compose.onNodeWithTag("library-search").assertTextContains("ＭＯＯＮ")
        assertTrue(moved.isEmpty()); compose.onNodeWithTag("library-search").performTextReplacement("")
        val action = compose.onNodeWithTag("library-a").fetchSemanticsNode().config[SemanticsActions.CustomActions].single()
        compose.runOnIdle { assertTrue(action.action()) }
        assertEquals(listOf("b", "a"), moved)
    }
    @Test fun longPressDragCommitsAndCancellationDoesNotCommit() {
        var moved = emptyList<String>()
        compose.setContent { KanjiTheme { RecipeLibraryScreen(RecipeLibraryState(listOf(first, second), snapshot, loading = false), "", "ru",
            actions(reorder = { moved = it })) } }
        val start = compose.onNodeWithTag("library-title-a", useUnmergedTree = true).fetchSemanticsNode().boundsInWindow.center
        val end = compose.onNodeWithTag("library-b").fetchSemanticsNode().boundsInWindow.center
        platformRecipeDrag(compose, start, end, cancel = true)
        assertTrue(moved.isEmpty())
        val restoredStart = compose.onNodeWithTag("library-title-a", useUnmergedTree = true).fetchSemanticsNode().boundsInWindow.center
        val restoredEnd = compose.onNodeWithTag("library-b").fetchSemanticsNode().boundsInWindow.center
        platformRecipeDrag(compose, restoredStart, restoredEnd)
        compose.runOnIdle { assertEquals(listOf("b", "a"), moved) }
    }
    @Test fun platformDragDoesNotStartFromRadioOrDuringSearch() {
        var query by mutableStateOf("")
        var commits = 0
        compose.setContent { KanjiTheme {
            RecipeLibraryScreen(RecipeLibraryState(listOf(first, second), snapshot, loading = false), query, "ru",
                actions(reorder = { commits++ }, query = { query = it }))
        } }
        val end = compose.onNodeWithTag("library-b").fetchSemanticsNode().boundsInWindow.center
        val start = compose.onNodeWithTag("library-select-a").fetchSemanticsNode().boundsInWindow.center
        platformRecipeDrag(compose, start, end)
        compose.runOnIdle { assertEquals(0, commits) }
        click("library-search-toggle")
        compose.onNodeWithTag("library-search").performTextReplacement("Moon")
        compose.onNodeWithTag("library-b").assertDoesNotExist()
        val searchStart = compose.onNodeWithTag("library-title-a", useUnmergedTree = true).fetchSemanticsNode().boundsInWindow.center
        platformRecipeDrag(compose, searchStart, end)
        compose.runOnIdle { assertEquals(0, commits) }
    }
    @Test fun platformDragAutoScrollsBeyondInitialViewportAndCommitsOnce() {
        val originals = (0 until 16).map { index -> first.copy(
            id = "r$index", isActive = index == 0,
            names = listOf(RecipeName("ru", "Рецепт $index")),
            materials = listOf(first.materials.single().copy(id = "m$index")),
        ) }
        var recipes by mutableStateOf(originals)
        var commits = 0
        var selections = 0
        compose.setContent { KanjiTheme {
            RecipeLibraryScreen(RecipeLibraryState(recipes, snapshot, loading = false), "", "ru",
                actions(select = { selections++ }, reorder = { ids ->
                    commits++
                    recipes = ids.map { id -> originals.single { it.id == id } }
                }))
        } }
        compose.onNodeWithTag("library-r10").assertDoesNotExist()
        val start = compose.onNodeWithTag("library-title-r0", useUnmergedTree = true).fetchSemanticsNode().boundsInWindow.center
        val viewport = compose.onNodeWithTag("recipe-library").fetchSemanticsNode().boundsInWindow
        val edge = Offset(viewport.left + viewport.width / 3f, viewport.bottom - 24f)
        platformRecipeDrag(compose, start, edge, holdMillis = 3_000)
        compose.runOnIdle {
            assertEquals(1, commits)
            assertTrue(recipes.indexOfFirst { it.id == "r0" } > 3)
            assertEquals(originals.map { it.id }.toSet(), recipes.map { it.id }.toSet())
            assertEquals("r0", recipes.single { it.isActive }.id)
            assertEquals(0, selections)
        }
        // The first drop can leave this row clipped in Android's mandatory navigation-gesture
        // region. Bring it fully into view before starting another real touchscreen gesture.
        compose.onNodeWithTag("library-r0").performScrollTo()
        val dragged = compose.onNodeWithTag("library-title-r0", useUnmergedTree = true).fetchSemanticsNode().boundsInWindow.center
        platformRecipeDrag(compose, dragged, Offset(edge.x, viewport.top + 24f), holdMillis = 6_000)
        compose.runOnIdle {
            assertEquals(2, commits)
            assertEquals("r0", recipes.first().id)
        }
    }
    @Test fun eachCreationGetsFreshNavigationOwnerAndIndependentIds() {
        val repo = Learning(); repo.recipes.value = emptyList()
        compose.setContent { KanjiTheme { RecipeBuilderRoute(catalog, personal, repo, RecipeBuilderActions({}, { _, _ -> }, { _ -> })) } }
        repeat(2) { index ->
            ready("library-create"); click("library-create"); ready("recipe-name")
            compose.onNodeWithTag("recipe-name").assertTextEquals("")
            compose.onNodeWithTag("recipe-name").performTextReplacement("Новая $index")
            compose.onNodeWithTag("recipe-add").performScrollTo(); click("recipe-add")
            click("material-type-words"); click("material-words")
            click("word--1"); click("word-picker-apply"); click("material-apply"); click("recipe-save")
            ready("recipe-detail"); compose.waitUntil { repo.recipes.value.size == index + 1 }
            click("recipe-detail-back"); ready("library-create")
        }
        assertEquals(2, repo.recipes.value.map { it.id }.distinct().size)
        assertEquals(2, repo.recipes.value.flatMap { it.materials }.map { it.id }.distinct().size)
    }
    @Test fun homeSummaryTracksSameActiveRecipeAndClearsOnDeletion() {
        val repo = Learning()
        compose.setContent { KanjiTheme { RecipeHomeSummary(catalog, personal, repo) { recipe ->
            androidx.compose.material3.Text(recipe.id, androidx.compose.ui.Modifier.testTag("home-id"))
        } } }
        compose.waitUntil(5_000) { compose.onNodeWithTag("home-id").fetchSemanticsNode().config[SemanticsProperties.Text].single().text == "a" }
        compose.runOnIdle { repo.recipes.value = repo.recipes.value.map { it.copy(isActive = it.id == "b") } }
        compose.waitUntil { compose.onNodeWithTag("home-id").fetchSemanticsNode().config[SemanticsProperties.Text].single().text == "b" }
        compose.runOnIdle { repo.recipes.value = repo.recipes.value.filterNot { it.id == "b" } }
        compose.waitUntil { compose.onNodeWithTag("home-id").fetchSemanticsNode().config[SemanticsProperties.Text].single().text.isEmpty() }
    }
    @Test fun typedGraphKeepsDraftAcrossWordDetailAndSavePopsEntry() {
        val repo = Learning()
        compose.setContent { KanjiTheme { RecipeBuilderRoute(catalog, personal, repo, RecipeBuilderActions({},
            { _, back -> androidx.compose.material3.TextButton(back,
                androidx.compose.ui.Modifier.testTag("fake-word-back")) { androidx.compose.material3.Text("Back") } },
            { _ -> })) } }
        ready("library-a"); click("library-open-a"); ready("recipe-detail"); click("recipe-detail-edit"); ready("recipe-name")
        compose.onNodeWithTag("recipe-name").performTextReplacement("Изменено")
        compose.onNodeWithTag("recipe-preview").performScrollTo(); click("recipe-preview")
        compose.onNodeWithTag("word--1").performClick(); ready("fake-word-back"); click("fake-word-back")
        // The real preview back action returns to the existing editor, not a new ViewModel.
        compose.onNodeWithTag("material-back").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("recipe-name").assertTextEquals("Изменено")
        click("recipe-save"); ready("recipe-detail")
        assertEquals("Изменено", recipeName(repo.recipes.value.single { it.id == "a" }, "ru"))
        click("recipe-detail-back"); ready("library-a")
        click("library-create"); ready("recipe-name"); compose.onNodeWithTag("recipe-name").assertTextEquals("")
    }
}
