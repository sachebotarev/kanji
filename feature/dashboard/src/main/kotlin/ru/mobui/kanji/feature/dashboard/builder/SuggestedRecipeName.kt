package ru.mobui.kanji.feature.dashboard.builder

import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.RecipeMaterial

internal data class RecipeNameCopy(
    val chaptersWord: String,
    val andMoreChapters: (Int) -> String,
    val moreTags: (Int) -> String,
    val kanjiLabel: (Int) -> String,
    val andMoreMaterials: (Int) -> String,
)

/** Builds a draft recipe title from the first material chip; adds "and N more" when needed. */
internal fun suggestedRecipeName(
    materials: List<RecipeMaterial>,
    snapshot: CatalogSnapshot,
    tags: List<DictionaryTag>,
    language: String,
    typeLabels: Map<MaterialType, String>,
    copy: RecipeNameCopy,
): String {
    if (materials.isEmpty()) return ""
    val first = compactMaterialTag(
        buildMaterialTagGroup(
            materials.first(), snapshot, tags, language, typeLabels, copy.chaptersWord,
            andMore = copy.andMoreChapters, moreTags = copy.moreTags, kanjiLabel = copy.kanjiLabel,
        ),
    ).first
    return if (materials.size == 1) first else "$first · ${copy.andMoreMaterials(materials.size - 1)}"
}
