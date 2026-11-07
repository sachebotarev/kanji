package ru.mobui.kanji.feature.dashboard.builder

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.RecipeName
import ru.mobui.kanji.domain.catalog.Recipe

class RecipeAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun russianCardsExposeNamedOpenAndReorderActions() = verify("ru")
    @Test fun englishCardsExposeNamedOpenAndReorderActions() = verify("en")

    @Test fun parametersShareOneRowAtNormalScale() = verifyMetrics(1f)
    @Test fun largeTextParametersStackWithoutShrinkingTouchTargets() = verifyMetrics(1.5f)

    private fun verifyMetrics(scale: Float) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("ru"))
            fontScale = scale
        }
        val localized = context.createConfigurationContext(config)
        val recipe = Recipe("a", listOf(RecipeName("ru", "Tobira · глава 10")),
            emptyList(), taskCount = 20, secondsPerWord = 30)
        compose.setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides config,
                LocalDensity provides Density(context.resources.displayMetrics.density, scale)) {
                KanjiTheme {
                    RecipeLibraryScreen(RecipeLibraryState(recipes = listOf(recipe), loading = false), "", "ru",
                        RecipeLibraryActions({}, {}, {}, {}, {}, {}))
                }
            }
        }
        val tasks = compose.onNodeWithText("20 заданий", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val timer = compose.onNodeWithText("30 с на слово", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        if (scale == 1f) {
            assertEquals(tasks.top, timer.top, 1f)
            assertTrue(timer.left > tasks.right)
        } else assertTrue(timer.top >= tasks.bottom)
        compose.onNodeWithTag("library-a")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("library-select-a").assertDoesNotExist()
        compose.onNodeWithTag("library-menu-a").assertDoesNotExist()
    }

    private fun verify(language: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        val localized = context.createConfigurationContext(config)
        val title = if (language == "ru") "Луна" else "Moon"
        val recipes = listOf("a", "b", "c").mapIndexed { index, id ->
            Recipe(id, listOf(RecipeName(language, if (index == 1) title else "$title $index")),
                emptyList(), isActive = index == 1)
        }
        val busy = mutableStateOf(false)
        val query = mutableStateOf("")
        var moved = emptyList<String>()
        compose.setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides config) {
                KanjiTheme {
                    RecipeLibraryScreen(RecipeLibraryState(recipes = recipes, loading = false, busy = busy.value),
                        query.value, language, RecipeLibraryActions({}, {}, {},
                            { moved = it }, { query.value = it }, {}))
                }
            }
        }
        val card = compose.onNodeWithTag("library-b")
        val semantics = card.fetchSemanticsNode().config
        assertTrue(semantics[SemanticsProperties.Text].any { it.text == title })
        assertTrue(semantics.contains(SemanticsActions.OnClick))
        val actions = semantics[SemanticsActions.CustomActions]
        assertEquals(if (language == "ru") listOf("Переместить выше", "Переместить ниже")
            else listOf("Move up", "Move down"), actions.map { it.label })
        card
            .assertContentDescriptionEquals(if (language == "ru") "Открыть рецепт «$title»" else "Open recipe “$title”")
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        compose.onNodeWithTag("library-select-b").assertDoesNotExist()
        compose.onNodeWithTag("library-menu-b").assertDoesNotExist()
        compose.runOnIdle { assertTrue(actions[0].action()) }
        assertEquals(listOf("b", "a", "c"), moved)
        compose.runOnIdle { query.value = title }
        assertTrue(card.fetchSemanticsNode().config[SemanticsActions.CustomActions].isEmpty())
        compose.runOnIdle { query.value = ""; busy.value = true }
        assertTrue(card.fetchSemanticsNode().config[SemanticsActions.CustomActions].isEmpty())
    }
}
