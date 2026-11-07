package ru.mobui.kanji.data.catalog

import androidx.sqlite.db.SupportSQLiteDatabase

/** Room has no CHECK annotation. Install equivalent guards on every open. */
internal fun installTrainingChecks(db: SupportSQLiteDatabase) {
    val verdict =
        "IN ('correct','incorrect','incomplete','unavailable')"
    val checks =
        mapOf(
            "trainings" to
                (
                    "length(trim(NEW.id))>0 AND NEW.status IN ('in_progress','completed','interrupted') AND " +
                        "(NEW.recipe_revision IS NULL OR NEW.recipe_revision>0) AND " +
                        "(NEW.task_count IS NULL OR NEW.task_count>0) AND " +
                        "(NEW.seconds_per_word IS NULL OR NEW.seconds_per_word>0) AND " +
                        "((NEW.status='in_progress' AND NEW.ended_at IS NULL) OR " +
                        "(NEW.status IN ('completed','interrupted') AND NEW.ended_at IS NOT NULL))"
                    ),
            "trainings_recipe_translations" to
                "length(trim(NEW.language_tag))>0 AND length(trim(NEW.name)) BETWEEN 1 AND 120",
            "training_tasks" to
                (
                    "length(trim(NEW.id))>0 AND NEW.position>=0 AND " +
                        "length(trim(NEW.written_form))>0 AND length(trim(NEW.reading))>0 AND " +
                        "NEW.status IN ('pending','in_progress','correct','assisted','skipped','timed_out') AND " +
                        "NEW.elapsed_ms>=0"
                    ),
            "training_task_translations" to
                "length(trim(NEW.language_tag))>0 AND length(trim(NEW.translation))>0",
            "training_task_symbols" to
                (
                    "length(trim(NEW.id))>0 AND NEW.position>=0 AND length(trim(NEW.occurrence_id))>0 AND " +
                        "NEW.role IN ('practice','display') AND " +
                        "NEW.script IN ('kanji','hiragana','katakana','other') AND " +
                        "length(trim(NEW.glyph))>0 AND " +
                        "(NEW.kanji_key IS NULL OR length(trim(NEW.kanji_key))>0) AND " +
                        "NEW.status IN ('empty','editing','correct','incorrect','prefilled') AND " +
                        "NEW.hint_request_count>=0 AND NEW.elapsed_ms>=0 AND " +
                        "(NEW.readability_verdict IS NULL OR NEW.readability_verdict $verdict) AND " +
                        "(NEW.shape_verdict IS NULL OR NEW.shape_verdict $verdict) AND " +
                        "(NEW.stroke_writing_verdict IS NULL OR NEW.stroke_writing_verdict $verdict) AND " +
                        "(NEW.shape_similarity_percent IS NULL OR " +
                        "(NEW.shape_similarity_percent>=0 AND NEW.shape_similarity_percent<=100)) AND " +
                        "(NEW.shape_budget_percent IS NULL OR NEW.shape_budget_percent>=0) AND " +
                        "(NEW.stroke_expected IS NULL OR NEW.stroke_expected>=0) AND " +
                        "(NEW.stroke_actual IS NULL OR NEW.stroke_actual>=0) AND " +
                        "((NEW.role='display' AND NEW.status='prefilled') OR NEW.role='practice')"
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
    db.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS idx_trainings_one_in_progress ON trainings(status) " +
            "WHERE status='in_progress'",
    )
}
