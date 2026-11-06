package ru.mobui.kanji.feature.dashboard.builder

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import ru.mobui.kanji.core.designsystem.KanjiTheme
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.RecipeName

@Preview(name = "Рецепт · крупный русский", locale = "ru", fontScale = 1.5f, showBackground = true)
@Preview(name = "Recipe · English dark", locale = "en", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun RecipeEditorPreview() {
    val snapshot = CatalogSnapshot(1, CatalogContent(emptyList(),
        listOf(CatalogWord(1, "こんにちは", "こんにちは", mapOf("ru" to "здравствуйте", "en" to "hello"))),
        emptyList(), emptyList(), emptyList(), emptyList()))
    val recipe = Recipe("preview", listOf(RecipeName("ru", "Первые фразы"), RecipeName("en", "First phrases")),
        listOf(RecipeMaterial("words", MaterialType.Words, wordIds = setOf(1))))
    KanjiTheme { RecipeEditorScreen(recipe, snapshot, emptyList(), emptyList(), LocalConfiguration.current.locales[0].language, {}, {}, {}, {}) }
}
