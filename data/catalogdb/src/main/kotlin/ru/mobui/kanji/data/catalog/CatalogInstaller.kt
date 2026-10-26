package ru.mobui.kanji.data.catalog

import android.content.res.AssetManager
import androidx.room.withTransaction
import kotlinx.coroutines.ensureActive
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** Writes supplied catalog content only; personal mutations use a separate repository contract. */
internal class CatalogInstaller(private val database: CatalogDatabase, private val assets: AssetManager) {
    suspend fun install() {
        val manifest = JSONObject(assets.open("seed/manifest.json").bufferedReader().use { it.readText() })
        val bytes = assets.open("seed/catalog.json").use { it.readBytes() }
        val info = manifest.getJSONObject("files").getJSONObject("catalog.json")
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        require(bytes.size == info.getInt("bytes") && hash == info.getString("sha256")) { "Catalog checksum mismatch" }
        install(JSONObject(bytes.toString(Charsets.UTF_8)), hash, manifest.getInt("content_version"))
    }

    internal suspend fun install(root: JSONObject, hash: String, expectedVersion: Int = root.getInt("content_version")) {
        require(root.getInt("format_version") == 1 && root.getInt("schema_version") == 1)
        val version = root.getInt("content_version")
        require(version > 0 && version == expectedVersion)
        val metadata = parse_catalog_metadata(root).single()
        require(metadata.singleton == 1 && metadata.schema_version == 1 && metadata.content_version == version)
        database.withTransaction {
            val dao = database.catalogDao()
            val current = dao.catalog_metadata().singleOrNull()
            require(current == null || current.content_version <= version) { "Catalog downgrade refused" }
            if (current?.content_version == version) {
                require(dao.installation()?.payloadHash == hash) { "Content changed without version increment" }
                return@withTransaction
            }
            val sources = parse_sources(root)
            require(sources.all { it.id > 0 && it.source_type != "personal" })
            val words = parse_words(root)
            require(words.all { it.id > 0 && it.written_form.isNotBlank() && it.reading.isNotBlank() })
            val kanji = parse_kanji(root)
            require(kanji.all { it.symbol.codePointCount(0, it.symbol.length) == 1 && it.symbol.codePointAt(0) == it.id })
            // Preserve every published identity, including archived entries; never reassign an ID.
            val storedWords = dao.words()
            require(storedWords.none { old -> old.is_user_created == 1 && words.any {
                it.id == old.id || (it.written_form == old.written_form && it.reading == old.reading)
            } }) { "Catalog conflicts with a manually created word; explicit resolution required" }
            val oldWords = storedWords.filter { it.id > 0 }.associateBy { it.id }
            val newWords = words.associateBy { it.id }
            require(oldWords.keys.all { it in newWords }) { "Published word IDs removed" }
            oldWords.forEach { (id, old) ->
                require(newWords[id]?.let { it.written_form == old.written_form && it.reading == old.reading } == true) {
                    "Identity correction requires an explicit migration"
                }
            }
            require(dao.kanji().all { old -> kanji.any { it.id == old.id && it.symbol == old.symbol } })
            val oldSourceIds = dao.sources().filter { it.id > 0 && it.source_type != "personal" }.map { it.id }
            require(sources.map { it.id }.containsAll(oldSourceIds)) { "Sources must be archived, not dropped" }
            coroutineContext.ensureActive()
            dao.clearLinks(oldSourceIds)
            dao.clearPlacements(oldSourceIds)
            dao.put_radicals(parse_radicals(root))
            dao.put_radicals_translations(parse_radicals_translations(root))
            dao.put_kanji(kanji)
            kanji.map { it.id }.chunked(500).forEach { dao.clearReadings(it) }
            parse_kanji_translations(root).map { it.kanji_id to it.language_tag }.distinct().forEach { (id, language) -> dao.clearMeanings(id, language) }
            dao.put_kanji_translations(parse_kanji_translations(root))
            dao.put_kanji_readings(parse_kanji_readings(root))
            dao.put_words(words)
            dao.put_words_translations(parse_words_translations(root))
            words.filter { it.is_archived == 0 }.map { it.id }.chunked(500).forEach { dao.clearParts(it) }
            dao.put_word_reading_parts(parse_word_reading_parts(root))
            dao.put_sources(sources)
            dao.put_sources_translations(parse_sources_translations(root))
            dao.put_source_kanji(parse_source_kanji(root))
            dao.put_source_words(parse_source_words(root))
            coroutineContext.ensureActive()
            dao.putInstallation(InstallationRow(1, hash))
            dao.put_catalog_metadata(listOf(metadata))
        }
    }
}
