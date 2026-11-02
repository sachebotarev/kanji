package ru.mobui.kanji.data.catalog

import org.json.JSONObject
import org.json.JSONArray

internal class SeedTable(root: JSONObject, name: String, expected: List<String>) {
    private val table = root.getJSONObject("tables").getJSONObject(name)
    private val columns = table.getJSONArray("columns")
    init { require((0 until columns.length()).map { columns.getString(it) } == expected) }
    private val rows = table.getJSONArray("rows")
    fun <T> map(convert: (JSONArray) -> T): List<T> = (0 until rows.length()).map { convert(rows.getJSONArray(it)) }
}

internal fun parse_catalog_metadata(root: JSONObject): List<CatalogMetadataRow> =
    SeedTable(root, "catalog_metadata", listOf("singleton", "schema_version", "content_version")).map { row ->
        CatalogMetadataRow(row.getInt(0), row.getInt(1), row.getInt(2))
    }

internal fun parse_radicals(root: JSONObject): List<RadicalsRow> =
    SeedTable(root, "radicals", listOf("id", "symbol", "stroke_count")).map { row ->
        RadicalsRow(row.getInt(0), row.getString(1), row.getInt(2))
    }

internal fun parse_radicals_translations(root: JSONObject): List<RadicalsTranslationsRow> =
    SeedTable(root, "radicals_translations", listOf("radical_id", "language_tag", "name")).map { row ->
        RadicalsTranslationsRow(row.getInt(0), row.getString(1), row.getString(2))
    }

internal fun parse_kanji(root: JSONObject): List<KanjiRow> =
    SeedTable(root, "kanji", listOf("id", "symbol", "stroke_count", "radical_id", "is_kyoiku", "is_joyo", "is_jinmeiyo")).map { row ->
        val classification = root.optJSONObject("classifications")?.optJSONObject(row.getInt(0).toString())
        KanjiRow(row.getInt(0), row.getString(1), row.getInt(2), if (row.isNull(3)) null else row.getInt(3),
            row.getInt(4), row.getInt(5), row.getInt(6), classification?.nullableString("school_stage"),
            classification?.nullableInt("school_year"), classification?.nullableInt("jlpt_level"))
    }

internal fun parse_kanji_translations(root: JSONObject): List<KanjiTranslationsRow> =
    SeedTable(root, "kanji_translations", listOf("kanji_id", "language_tag", "position", "meaning")).map { row ->
        KanjiTranslationsRow(row.getInt(0), row.getString(1), row.getInt(2), row.getString(3))
    }

internal fun parse_kanji_readings(root: JSONObject): List<KanjiReadingsRow> =
    SeedTable(root, "kanji_readings", listOf("kanji_id", "reading_type", "position", "reading")).map { row ->
        KanjiReadingsRow(row.getInt(0), row.getString(1), row.getInt(2), row.getString(3))
    }

internal fun parse_words(root: JSONObject): List<WordsRow> =
    SeedTable(root, "words", listOf("id", "written_form", "reading", "is_archived")).map { row ->
        WordsRow(row.getInt(0), row.getString(1), row.getString(2), row.getInt(3))
    }

internal fun parse_words_translations(root: JSONObject): List<WordsTranslationsRow> =
    SeedTable(root, "words_translations", listOf("word_id", "language_tag", "translation")).map { row ->
        root.optJSONObject("wordContent")?.optJSONObject("${row.getInt(0)}:${row.getString(1)}")?.let {
            WordsTranslationsRow(row.getInt(0), row.getString(1), it.getString("translation"),
                if (it.isNull("usage_note")) null else it.getString("usage_note"))
        } ?: WordsTranslationsRow(row.getInt(0), row.getString(1), row.getString(2))
    }

internal fun parse_word_reading_parts(root: JSONObject): List<WordReadingPartsRow> =
    SeedTable(root, "word_reading_parts", listOf("word_id", "position", "written_part", "reading_part")).map { row ->
        WordReadingPartsRow(row.getInt(0), row.getInt(1), row.getString(2), row.getString(3))
    }

internal fun parse_sources(root: JSONObject): List<SourcesRow> =
    SeedTable(root, "sources", listOf("id", "source_type", "original_title", "is_archived")).map { row ->
        SourcesRow(row.getInt(0), row.getString(1), row.getString(2), row.getInt(3),
            root.optJSONObject("sourceCovers")?.nullableString(row.getInt(0).toString()))
    }

internal fun parse_sources_translations(root: JSONObject): List<SourcesTranslationsRow> =
    SeedTable(root, "sources_translations", listOf("source_id", "language_tag", "title")).map { row ->
        SourcesTranslationsRow(row.getInt(0), row.getString(1), row.getString(2),
            root.optJSONArray("sourceDescriptions")?.let { values ->
                (0 until values.length()).map { values.getJSONObject(it) }.firstOrNull {
                    it.getInt("source_id") == row.getInt(0) && it.getString("language_tag") == row.getString(1)
                }?.getString("text")
            })
    }

internal fun parse_source_kanji(root: JSONObject): List<SourceKanjiRow> =
    SeedTable(root, "source_kanji", listOf("source_id", "kanji_id", "kanji_number", "chapter_number")).map { row ->
        SourceKanjiRow(row.getInt(0), row.getInt(1), row.getInt(2), row.getInt(3))
    }

internal fun parse_source_words(root: JSONObject): List<SourceWordsRow> =
    SeedTable(root, "source_words", listOf("id", "source_id", "word_id", "kanji_id")).map { row ->
        SourceWordsRow(row.getInt(0), row.getInt(1), row.getInt(2), if (row.isNull(3)) null else row.getInt(3))
    }

private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
private fun JSONObject.nullableInt(key: String): Int? = if (isNull(key)) null else getInt(key)
