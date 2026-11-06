package ru.mobui.kanji.feature.dashboard.builder

import android.os.Bundle
import androidx.compose.runtime.saveable.Saver
import ru.mobui.kanji.domain.catalog.MaterialType
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.RecipeMaterial
import ru.mobui.kanji.domain.catalog.RecipeName

internal fun RecipeMaterial.toBundle() = Bundle().apply {
    putString("id", id); putString("type", type.key)
    sourceId?.let { putInt("source", it) }
    putIntArray("chapters", chapters.toIntArray()); putIntArray("kanji", kanjiIds.toIntArray())
    putIntArray("words", wordIds.toIntArray()); putLongArray("tags", tagIds.toLongArray())
}
internal fun Bundle.toMaterial() = RecipeMaterial(requireNotNull(getString("id")),
    MaterialType.entries.first { it.key == getString("type") }, if (containsKey("source")) getInt("source") else null,
    (getIntArray("chapters") ?: intArrayOf()).toSet(), (getIntArray("kanji") ?: intArrayOf()).toSet(),
    (getIntArray("words") ?: intArrayOf()).toSet(), (getLongArray("tags") ?: longArrayOf()).toSet())

internal fun recipeBundle(recipe: Recipe): Bundle = Bundle().apply {
    putString("id", recipe.id); putLong("revision", recipe.revision); putBoolean("active", recipe.isActive)
    recipe.taskCount?.let { putInt("tasks", it) }; recipe.secondsPerWord?.let { putInt("seconds", it) }
    putStringArrayList("languages", ArrayList(recipe.names.map { it.languageTag }))
    putStringArrayList("names", ArrayList(recipe.names.map { it.name }))
    putInt("size", recipe.materials.size)
    recipe.materials.forEachIndexed { index, material -> putBundle("material-$index", material.toBundle()) }
}

internal fun recipeFromBundle(bundle: Bundle): Recipe = Recipe(requireNotNull(bundle.getString("id")),
    bundle.getStringArrayList("languages").orEmpty().zip(bundle.getStringArrayList("names").orEmpty()).map { RecipeName(it.first, it.second) },
    (0 until bundle.getInt("size")).map { requireNotNull(bundle.getBundle("material-$it")).toMaterial() },
    if (bundle.containsKey("tasks")) bundle.getInt("tasks") else null,
    if (bundle.containsKey("seconds")) bundle.getInt("seconds") else null,
    bundle.getBoolean("active"), bundle.getLong("revision"))

internal val RecipeSaver = Saver<Recipe, Bundle>(save = { recipeBundle(it) }, restore = ::recipeFromBundle)
