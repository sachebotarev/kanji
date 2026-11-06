package ru.mobui.kanji.feature.dashboard.builder

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.DictionaryTag
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.title
import ru.mobui.kanji.feature.dashboard.R

/** Card/detail chapter chip: first three numbers, then localized "and N more". */
internal fun chapterTagLabel(chapters: Collection<Int>, chaptersWord: String, andMore: (Int) -> String): String {
    val sorted = chapters.sorted()
    if (sorted.isEmpty()) return chaptersWord
    val preview = sorted.take(3).joinToString(", ")
    return if (sorted.size > 3) "$chaptersWord $preview ${andMore(sorted.size - 3)}" else "$chaptersWord $preview"
}

/** Personal-dictionary tags as one chip: first two names, then " · N more". */
internal fun dictionaryTagsLabel(names: Collection<String>, more: (Int) -> String): String {
    val list = names.filter { it.isNotBlank() }
    if (list.isEmpty()) return ""
    val preview = list.take(2).joinToString(", ")
    return if (list.size > 2) "$preview · ${more(list.size - 2)}" else preview
}

/** One library chip per material: join source and filters, keep the material type color. */
internal fun compactMaterialTag(group: List<Pair<String, MaterialType?>>): Pair<String, MaterialType?> {
    val labels = group.map { it.first }.filter { it.isNotBlank() }
    require(labels.isNotEmpty())
    return labels.joinToString(" · ") to group.first().second
}

@Composable
internal fun recipeMaterialTags(
    recipe: Recipe,
    snapshot: CatalogSnapshot?,
    tags: List<DictionaryTag>,
    language: String,
    typeLabels: Map<MaterialType, String>,
): List<Pair<String, MaterialType?>> = recipeMaterialTagGroups(
    recipe, snapshot, tags, language, typeLabels,
).map(::compactMaterialTag)

@Composable
internal fun recipeMaterialTagGroups(
    recipe: Recipe,
    snapshot: CatalogSnapshot?,
    tags: List<DictionaryTag>,
    language: String,
    typeLabels: Map<MaterialType, String>,
): List<List<Pair<String, MaterialType?>>> {
    val chaptersWord = stringResource(R.string.material_chapters)
    val andMorePattern = stringResource(R.string.material_chapters_and_more)
    val moreTagsPattern = stringResource(R.string.material_more)
    val kanjiPattern = stringResource(R.string.library_kanji)
    return recipe.materials.map { material ->
        buildMaterialTagGroup(
            material, snapshot, tags, language, typeLabels, chaptersWord,
            andMore = { andMorePattern.format(it) },
            moreTags = { moreTagsPattern.format(it) },
            kanjiLabel = { kanjiPattern.format(it) },
        )
    }
}

internal fun buildMaterialTagGroup(
    material: RecipeMaterial,
    snapshot: CatalogSnapshot?,
    tags: List<DictionaryTag>,
    language: String,
    typeLabels: Map<MaterialType, String>,
    chaptersWord: String,
    andMore: (Int) -> String,
    moreTags: (Int) -> String,
    kanjiLabel: (Int) -> String,
): List<Pair<String, MaterialType?>> = buildList {
    add(
        (material.sourceId?.let { snapshot?.sourceById?.get(it)?.title(language) }
            ?: typeLabels.getValue(material.type)) to material.type,
    )
    if (material.chapters.isNotEmpty()) {
        add(chapterTagLabel(material.chapters, chaptersWord, andMore) to null)
    }
    if (material.kanjiIds.isNotEmpty()) {
        add(kanjiLabel(material.kanjiIds.size) to MaterialType.Kanji)
    }
    val tagNames = tags.filter { it.id in material.tagIds }.map { it.name(language) }
    if (tagNames.isNotEmpty()) {
        add(dictionaryTagsLabel(tagNames, moreTags) to MaterialType.Dictionary)
    }
    if (material.type == MaterialType.Words) {
        add(material.wordIds.size.toString() to null)
    }
}
