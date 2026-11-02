package ru.mobui.kanji.data.catalog

import org.json.JSONObject

internal fun parseCatalogMetadata(root: JSONObject): List<CatalogMetadataRow> =
    SeedTable(root, "catalog_metadata", listOf("singleton", "schema_version", "content_version"))
        .map { row ->
            CatalogMetadataRow(
                row.getInt("singleton"),
                row.getInt("schema_version"),
                row.getInt("content_version"),
            )
        }

internal fun parseRadicals(root: JSONObject): List<RadicalsRow> =
    SeedTable(root, "radicals", listOf("id", "symbol", "stroke_count")).map { row ->
        RadicalsRow(row.getInt("id"), row.getString("symbol"), row.getInt("stroke_count"))
    }

internal fun parseRadicalsTranslations(root: JSONObject): List<RadicalsTranslationsRow> =
    SeedTable(root, "radicals_translations", listOf("radical_id", "language_tag", "name")).map { row
        ->
        RadicalsTranslationsRow(
            row.getInt("radical_id"),
            row.getString("language_tag"),
            row.getString("name"),
        )
    }

internal fun parseKanji(root: JSONObject): List<KanjiRow> =
    SeedTable(
            root,
            "kanji",
            listOf(
                "id",
                "symbol",
                "stroke_count",
                "radical_id",
                "is_kyoiku",
                "is_joyo",
                "is_jinmeiyo",
            ),
        )
        .map { row ->
            val classification =
                root.optJSONObject("classifications")?.optJSONObject(row.getInt("id").toString())
            KanjiRow(
                row.getInt("id"),
                row.getString("symbol"),
                row.getInt("stroke_count"),
                if (row.isNull("radical_id")) null else row.getInt("radical_id"),
                row.getInt("is_kyoiku"),
                row.getInt("is_joyo"),
                row.getInt("is_jinmeiyo"),
                classification?.nullableString("school_stage"),
                classification?.nullableInt("school_year"),
                classification?.nullableInt("jlpt_level"),
            )
        }

internal fun parseKanjiTranslations(root: JSONObject): List<KanjiTranslationsRow> =
    SeedTable(root, "kanji_translations", listOf("kanji_id", "language_tag", "position", "meaning"))
        .map { row ->
            KanjiTranslationsRow(
                row.getInt("kanji_id"),
                row.getString("language_tag"),
                row.getInt("position"),
                row.getString("meaning"),
            )
        }

internal fun parseKanjiReadings(root: JSONObject): List<KanjiReadingsRow> =
    SeedTable(root, "kanji_readings", listOf("kanji_id", "reading_type", "position", "reading"))
        .map { row ->
            KanjiReadingsRow(
                row.getInt("kanji_id"),
                row.getString("reading_type"),
                row.getInt("position"),
                row.getString("reading"),
            )
        }
