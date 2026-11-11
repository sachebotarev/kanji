package ru.mobui.kanji.data.catalog

import androidx.sqlite.db.SupportSQLiteDatabase

/** Room has no CHECK annotation. Install equivalent guards on every open, also after replacement. */
internal fun installRecipeChecks(db: SupportSQLiteDatabase) {
    val checks = mapOf(
        "dictionary_tags" to
            "NEW.color IN ('neutral','green','blue','violet','pink','orange','gold') AND NEW.position>=0",
        "dictionary_tags_translations" to
            "length(trim(NEW.language_tag))>0 AND length(trim(NEW.name)) BETWEEN 1 AND 60 AND length(NEW.name_key)>0",
        "word_tags" to
            "EXISTS(SELECT 1 FROM words WHERE id=NEW.word_id AND is_user_created=1 AND is_archived=0)",
        "recipes" to (
            "length(trim(NEW.id))>0 AND NEW.position>=0 AND NEW.is_active IN (0,1) AND " +
                "NEW.is_archived IN (0,1) AND (NEW.is_archived=0 OR NEW.is_active=0) AND " +
                "NEW.revision>0 AND NEW.selection_mode='shuffled_cycles' AND " +
                "(NEW.task_count IS NULL OR NEW.task_count>0) AND " +
                "(NEW.seconds_per_symbol IS NULL OR NEW.seconds_per_symbol>0) AND " +
                "NEW.hint_mode IN ('translation','reading','none') AND " +
                "NEW.stroke_hint_mode IN ('next_stroke','full_character')"
            ),
        "recipes_translations" to
            "length(trim(NEW.language_tag))>0 AND length(trim(NEW.name)) BETWEEN 1 AND 120",
        "recipe_materials" to (
            "length(trim(NEW.id))>0 AND NEW.position>=0 AND " +
                "((NEW.material_type IN ('kanji','words') AND NEW.source_id IS NULL) OR " +
                "(NEW.material_type IN ('book','dictionary') AND NEW.source_id IS NOT NULL AND " +
                "EXISTS(SELECT 1 FROM sources WHERE id=NEW.source_id AND is_archived=0 AND " +
                "((NEW.material_type='dictionary' AND source_type='personal') OR " +
                "(NEW.material_type='book' AND source_type IN ('textbook','book'))))))"
            ),
        "recipe_materials_chapters" to (
            "NEW.chapter_number>0 AND EXISTS(SELECT 1 FROM recipe_materials s " +
                "JOIN source_kanji k ON k.source_id=s.source_id WHERE " +
                "s.id=NEW.material_id AND s.material_type='book' AND " +
                "k.chapter_number=NEW.chapter_number)"
            ),
        "recipe_materials_kanji" to
            "EXISTS(SELECT 1 FROM recipe_materials WHERE id=NEW.material_id AND material_type IN ('book','dictionary','kanji'))",
        "recipe_materials_words" to
            "EXISTS(SELECT 1 FROM recipe_materials WHERE id=NEW.material_id AND material_type='words')",
        "recipe_materials_tags" to
            "EXISTS(SELECT 1 FROM recipe_materials WHERE id=NEW.material_id AND material_type='dictionary')",
        "recipe_materials_jlpt" to (
            "(NEW.jlpt_level=-1 OR NEW.jlpt_level BETWEEN 1 AND 5) AND " +
                "EXISTS(SELECT 1 FROM recipe_materials WHERE id=NEW.material_id AND material_type='kanji')"
            ),
    )
    checks.forEach { (table, predicate) ->
        listOf("INSERT", "UPDATE").forEach { action ->
            val name = "check_${table}_${action.lowercase()}"
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS $name BEFORE $action ON $table " +
                    "WHEN NOT ($predicate) BEGIN SELECT RAISE(ABORT, 'Invalid $table'); END",
            )
        }
    }
}
