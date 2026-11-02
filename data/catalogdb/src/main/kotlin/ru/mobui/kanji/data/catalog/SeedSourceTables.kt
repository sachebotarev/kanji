package ru.mobui.kanji.data.catalog

import org.json.JSONObject

internal fun parseSources(root: JSONObject): List<SourcesRow> =
    SeedTable(root, "sources", listOf("id", "source_type", "original_title", "is_archived")).map {
        row ->
        SourcesRow(
            row.getInt("id"),
            row.getString("source_type"),
            row.getString("original_title"),
            row.getInt("is_archived"),
            root.optJSONObject("sourceCovers")?.nullableString(row.getInt("id").toString()),
        )
    }

internal fun parseSourcesTranslations(root: JSONObject): List<SourcesTranslationsRow> =
    SeedTable(root, "sources_translations", listOf("source_id", "language_tag", "title")).map { row
        ->
        SourcesTranslationsRow(
            row.getInt("source_id"),
            row.getString("language_tag"),
            row.getString("title"),
            root.optJSONArray("sourceDescriptions")?.let { values ->
                (0 until values.length())
                    .map { values.getJSONObject(it) }
                    .firstOrNull {
                        it.getInt("source_id") == row.getInt("source_id") &&
                            it.getString("language_tag") == row.getString("language_tag")
                    }
                    ?.getString("text")
            },
        )
    }

internal fun parseSourceKanji(root: JSONObject): List<SourceKanjiRow> =
    SeedTable(
            root,
            "source_kanji",
            listOf("source_id", "kanji_id", "kanji_number", "chapter_number"),
        )
        .map { row ->
            SourceKanjiRow(
                row.getInt("source_id"),
                row.getInt("kanji_id"),
                row.getInt("kanji_number"),
                row.getInt("chapter_number"),
            )
        }

internal fun parseSourceWords(root: JSONObject): List<SourceWordsRow> =
    SeedTable(root, "source_words", listOf("id", "source_id", "word_id", "kanji_id")).map { row ->
        SourceWordsRow(
            row.getInt("id"),
            row.getInt("source_id"),
            row.getInt("word_id"),
            if (row.isNull("kanji_id")) null else row.getInt("kanji_id"),
        )
    }
