package ru.mobui.kanji

/**
 * Custom-scheme deep links for root destinations (`kanji://mobui/...`).
 *
 * Not Android App Links: no `https` host verification. PRACTICE is intentionally omitted —
 * it needs in-memory [ActiveTraining] / Room resume, not a cold URI entry.
 */
internal object AppDeepLinks {
    const val SCHEME = "kanji"
    const val HOST = "mobui"
    const val BASE = "$SCHEME://$HOST"

    const val HOME = "$BASE/home"
    const val TRAINING_PICK = "$BASE/training"
    const val SETTINGS = "$BASE/settings"
    const val CHECKER_SETTINGS = "$BASE/settings/checker"
    const val CATALOG = "$BASE/catalog"
    const val REFERENCES = "$BASE/references"
    const val DICTIONARY = "$BASE/dictionary"
    const val RECIPES = "$BASE/recipes"
}
