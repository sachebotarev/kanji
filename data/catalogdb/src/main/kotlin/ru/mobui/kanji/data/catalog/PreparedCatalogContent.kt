package ru.mobui.kanji.data.catalog

import android.content.res.AssetManager
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** Editorial supplements are keyed by stable IDs; never matched by translated text. */
internal class PreparedCatalogContent(assets: AssetManager) {
    private val files = listOf("kanji-classifications.json", "words-translations.json", "source-descriptions.json", "source-presentation.json")
        .associateWith { assets.open("content/$it").use { stream -> stream.readBytes() } }
    val hash: String = MessageDigest.getInstance("SHA-256").digest(files.values.fold(byteArrayOf()) { a, b -> a + b })
        .joinToString("") { "%02x".format(it) }

    fun applyTo(root: JSONObject) {
        fun rows(name: String) = JSONArray(files.getValue(name).toString(Charsets.UTF_8))
        val classifications = JSONObject()
        val kanji = rows("kanji-classifications.json")
        for (index in 0 until kanji.length()) {
            val item = kanji.getJSONObject(index)
            val stage = if (item.isNull("school_stage")) null else item.getString("school_stage")
            val year = if (item.isNull("school_year")) null else item.getInt("school_year")
            require(stage == null || stage in setOf("elementary", "junior_high", "high_school"))
            require(year == null || (stage != null && year in 1..if (stage == "elementary") 6 else 3))
            require(item.isNull("jlpt_level") || item.getInt("jlpt_level") in 1..5)
            val id = item.getInt("kanji_id")
            require(!classifications.has(id.toString()))
            classifications.put(id.toString(), item)
        }
        root.put("classifications", classifications)
        val translations = JSONObject()
        val words = rows("words-translations.json")
        for (index in 0 until words.length()) {
            val item = words.getJSONObject(index)
            translations.put("${item.getInt("word_id")}:${item.getString("language_tag")}", item)
        }
        root.put("wordContent", translations)
        val table = root.getJSONObject("tables").getJSONObject("sources_translations").getJSONArray("rows")
        val descriptions = rows("source-descriptions.json")
        val sources = root.getJSONObject("tables").getJSONObject("sources").getJSONArray("rows")
        for (index in 0 until descriptions.length()) {
            val item = descriptions.getJSONObject(index)
            val id = item.getInt("source_id")
            // Existing titles remain authoritative; fill missing translations with the original title.
            if ((0 until table.length()).none { table.getJSONArray(it).getInt(0) == id && table.getJSONArray(it).getString(1) == item.getString("language_tag") }) {
                val source = (0 until sources.length()).map { sources.getJSONArray(it) }.single { it.getInt(0) == id }
                table.put(JSONArray().put(id).put(item.getString("language_tag")).put(source.getString(2)))
            }
        }
        root.put("sourceDescriptions", descriptions)
        val covers = JSONObject()
        val presentation = rows("source-presentation.json")
        for (index in 0 until presentation.length()) {
            val item = presentation.getJSONObject(index)
            covers.put(item.getInt("source_id").toString(), item.getString("cover_key"))
        }
        root.put("sourceCovers", covers)
    }
}
