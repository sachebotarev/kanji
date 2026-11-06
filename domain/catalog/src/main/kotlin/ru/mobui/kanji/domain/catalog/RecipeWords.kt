package ru.mobui.kanji.domain.catalog

/** Preview only: storage still validates the complete aggregate and its real source IDs. */
fun CatalogSnapshot.materialWords(material: RecipeMaterial, assignments: List<WordTagAssignment>): List<CatalogWord> {
    val source = sources.firstOrNull { it.id == material.sourceId }
    return when (material.type) {
        MaterialType.Book -> if (source?.sourceType in setOf("book", "textbook"))
            words(CatalogSelection(sourceId = material.sourceId, chapterIds = material.chapters, kanjiIds = material.kanjiIds), "", "ru")
        else emptyList()
        MaterialType.Dictionary -> {
            if (source?.sourceType != "personal") return emptyList()
            val tagged = assignments.filter { assignment -> assignment.tagIds.any { it in material.tagIds } }.map { it.wordId }.toSet()
            words(CatalogSelection(sourceId = material.sourceId), "", "ru").filter { word ->
                (material.tagIds.isEmpty() || word.id in tagged) &&
                    (material.kanjiIds.isEmpty() || literalKanji(word).any { it.id in material.kanjiIds })
            }
        }
        MaterialType.Kanji -> words.filter { word -> material.kanjiIds.isEmpty() || literalKanji(word).any { it.id in material.kanjiIds } }
        MaterialType.Words -> words.filter { it.id in material.wordIds }
    }.distinctBy { it.id }
}

/** Union by stable word ID, preserving catalogue order; the query never changes stored choices. */
fun CatalogSnapshot.recipeWords(materials: List<RecipeMaterial>, assignments: List<WordTagAssignment>): List<CatalogWord> {
    val ids = materials.flatMap { materialWords(it, assignments) }.map { it.id }.toSet()
    return words.filter { it.id in ids }.distinctBy { it.id }
}
