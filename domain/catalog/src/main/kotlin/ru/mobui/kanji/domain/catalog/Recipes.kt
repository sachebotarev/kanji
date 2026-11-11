package ru.mobui.kanji.domain.catalog

import java.util.Locale
import kotlinx.coroutines.flow.Flow

/** Identifiers, not translated labels or arbitrary UI colors. */
enum class TagColor(val key: String) {
    Neutral("neutral"), Green("green"), Blue("blue"), Violet("violet"),
    Pink("pink"), Orange("orange"), Gold("gold"),
}

data class TagTranslation(val languageTag: String, val name: String)
data class DictionaryTag(val id: Long, val color: TagColor, val translations: List<TagTranslation>) {
    fun name(languageTag: String): String = translations.firstOrNull {
        it.languageTag.equals(languageTag, ignoreCase = true)
    }?.name ?: translations.firstOrNull {
        it.languageTag.equals(languageTag.substringBefore('-'), ignoreCase = true)
    }?.name ?: translations.firstOrNull { it.languageTag.equals("ru", ignoreCase = true) }?.name
        ?: translations.sortedBy { it.languageTag }.firstOrNull()?.name.orEmpty()
}

data class TagDraft(val color: TagColor, val translations: List<TagTranslation>)
data class WordTagAssignment(val wordId: Int, val tagIds: Set<Long>)

enum class MaterialType(val key: String) {
    Book("book"), Dictionary("dictionary"), Kanji("kanji"), Words("words"),
}

data class RecipeMaterial(
    val id: String,
    val type: MaterialType,
    val sourceId: Int? = null,
    val chapters: Set<Int> = emptySet(),
    val kanjiIds: Set<Int> = emptySet(),
    val wordIds: Set<Int> = emptySet(),
    val tagIds: Set<Long> = emptySet(),
    /** Exact JLPT numbers 1…5 and/or `-1` (no catalog level). Kanji material only. */
    val jlptLevels: Set<Int> = emptySet(),
)

enum class RecipeHintMode(val key: String) {
    Translation("translation"),
    Reading("reading"),
    None("none");

    companion object {
        /** Null or unknown keeps the recipe default. */
        fun fromKey(key: String?): RecipeHintMode {
            if (key == null) return None
            return when (key.trim().lowercase()) {
                "translation" -> Translation
                "reading" -> Reading
                "none" -> None
                else -> None
            }
        }
    }
}

enum class RecipeStrokeHintMode(val key: String) {
    NextStroke("next_stroke"),
    FullCharacter("full_character");

    companion object {
        /** Null or unknown keeps the recipe default. */
        fun fromKey(key: String?): RecipeStrokeHintMode {
            if (key == null) return NextStroke
            return when (key.trim().lowercase()) {
                "next_stroke" -> NextStroke
                "full_character", "full", "full_kanji" -> FullCharacter
                else -> NextStroke
            }
        }
    }
}

data class RecipeName(val languageTag: String, val name: String)
data class Recipe(
    val id: String,
    val names: List<RecipeName>,
    val materials: List<RecipeMaterial>,
    val taskCount: Int? = null,
    val secondsPerSymbol: Int? = null,
    val hintMode: RecipeHintMode = RecipeHintMode.None,
    val strokeHintMode: RecipeStrokeHintMode = RecipeStrokeHintMode.NextStroke,
    val isActive: Boolean = false,
    val revision: Long = 1,
)

/** Localized display name; Russian first when the requested language is missing. */
fun Recipe.name(languageTag: String): String = names.firstOrNull {
    it.languageTag.equals(languageTag, ignoreCase = true)
}?.name ?: names.firstOrNull {
    it.languageTag.equals(languageTag.substringBefore('-'), ignoreCase = true)
}?.name ?: names.firstOrNull { it.languageTag.equals("ru", ignoreCase = true) }?.name
    ?: names.firstOrNull()?.name.orEmpty()

/** Thrown when a recipe cannot be persisted due to material or revision rules. */
class RecipeValidationException(message: String) : IllegalArgumentException(message)

/** Recipe configuration only. Training execution lives in TrainingsRepository (`:domain:practice`). */
interface RecipesRepository {
    fun observeTags(): Flow<List<DictionaryTag>>
    fun observeWordTags(): Flow<List<WordTagAssignment>>
    suspend fun saveTag(id: Long?, draft: TagDraft): Long
    suspend fun deleteTag(id: Long)
    suspend fun assignTags(wordId: Int, tagIds: Set<Long>)
    fun observeRecipes(): Flow<List<Recipe>>
    suspend fun saveRecipe(recipe: Recipe)
    suspend fun activateRecipe(id: String)
    /** Exact permutation, never reorders a filtered subset. */
    suspend fun reorderRecipes(ids: List<String>)
    suspend fun deleteRecipe(id: String)
}

fun TagDraft.validated(): TagDraft {
    val normalized = translations.map { TagTranslation(validLanguage(it.languageTag), it.name.trim()) }
    require(normalized.isNotEmpty() && normalized.all { it.name.length in 1..60 })
    require(normalized.map { it.languageTag.lowercase(Locale.ROOT) }.distinct().size == normalized.size)
    return copy(translations = normalized)
}

fun Recipe.validated(): Recipe {
    require(id.isNotBlank() && revision > 0)
    require(taskCount == null || taskCount > 0)
    require(secondsPerSymbol == null || secondsPerSymbol > 0)
    val normalized = names.map { RecipeName(validLanguage(it.languageTag), it.name.trim()) }
    require(normalized.isNotEmpty() && normalized.all { it.name.length in 1..120 })
    require(normalized.map { it.languageTag.lowercase(Locale.ROOT) }.distinct().size == normalized.size)
    require(materials.isNotEmpty() && materials.map { it.id }.distinct().size == materials.size)
    materials.forEach { material ->
        require(material.id.isNotBlank() && material.chapters.all { it > 0 })
        require(material.tagIds.all { it > 0 })
        require(material.jlptLevels.all { it == -1 || it in 1..5 })
        require((material.type in setOf(MaterialType.Book, MaterialType.Dictionary)) == (material.sourceId != null))
        require(material.type == MaterialType.Book || material.chapters.isEmpty())
        require(material.type == MaterialType.Dictionary || material.tagIds.isEmpty())
        require(material.type == MaterialType.Words || material.wordIds.isEmpty())
        require(material.type == MaterialType.Kanji || material.jlptLevels.isEmpty())
        require(material.type != MaterialType.Words || (material.kanjiIds.isEmpty() && material.wordIds.isNotEmpty()))
    }
    return copy(names = normalized)
}

private fun validLanguage(value: String): String {
    val locale = Locale.Builder().setLanguageTag(value.trim()).build()
    require(locale.language.isNotBlank() && locale.language != "und")
    return locale.toLanguageTag()
}
