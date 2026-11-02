package ru.mobui.kanji.data.catalog

import org.json.JSONObject

internal fun parseWords(root: JSONObject): List<WordsRow> =
    SeedTable(root, "words", listOf("id", "written_form", "reading", "is_archived")).map { row ->
        WordsRow(
            row.getInt("id"),
            row.getString("written_form"),
            row.getString("reading"),
            row.getInt("is_archived"),
        )
    }

internal fun parseWordsTranslations(root: JSONObject): List<WordsTranslationsRow> =
    SeedTable(root, "words_translations", listOf("word_id", "language_tag", "translation")).map {
        row ->
        root
            .optJSONObject("wordContent")
            ?.optJSONObject("${row.getInt("word_id")}:${row.getString("language_tag")}")
            ?.let {
                WordsTranslationsRow(
                    row.getInt("word_id"),
                    row.getString("language_tag"),
                    it.getString("translation"),
                    if (it.isNull("usage_note")) null else it.getString("usage_note"),
                )
            }
            ?: WordsTranslationsRow(
                row.getInt("word_id"),
                row.getString("language_tag"),
                row.getString("translation"),
            )
    }

internal fun parseWordReadingParts(root: JSONObject): List<WordReadingPartsRow> =
    SeedTable(
            root,
            "word_reading_parts",
            listOf("word_id", "position", "written_part", "reading_part"),
        )
        .map { row ->
            WordReadingPartsRow(
                row.getInt("word_id"),
                row.getInt("position"),
                row.getString("written_part"),
                row.getString("reading_part"),
            )
        }
