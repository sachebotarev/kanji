package ru.mobui.kanji.feature.dictionary

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import ru.mobui.kanji.core.designsystem.preferredLanguageTag
import ru.mobui.kanji.domain.catalog.RecipesRepository

/** App-level composition entry; callers provide a dedicated navigation entry owner. */
@Composable
fun DictionaryTagManagementRoute(repository: RecipesRepository, onBack: () -> Unit) {
    DictionaryTagsRoute(repository, "management", emptySet(), emptyList(), false,
        LocalConfiguration.current.preferredLanguageTag(), { _, _ -> }, onBack, managementOnly = true)
}
