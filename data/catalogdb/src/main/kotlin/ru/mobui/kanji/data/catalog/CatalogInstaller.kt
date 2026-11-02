package ru.mobui.kanji.data.catalog

import android.content.res.AssetManager
import androidx.room.withTransaction
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject

/** Writes supplied catalog content only; personal mutations use a separate repository contract. */
internal class CatalogInstaller(
    private val database: CatalogDatabase,
    private val assets: AssetManager,
) {
    suspend fun install() {
        val manifest =
            JSONObject(assets.open("seed/manifest.json").bufferedReader().use { it.readText() })
        val bytes = assets.open("seed/catalog.json").use { it.readBytes() }
        val info = manifest.getJSONObject("files").getJSONObject("catalog.json")
        val hash =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
                "%02x".format(it)
            }
        require(bytes.size == info.getInt("bytes") && hash == info.getString("sha256")) {
            "Catalog checksum mismatch"
        }
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        val content = PreparedCatalogContent(assets)
        content.applyTo(root)
        install(root, hash + content.hash, manifest.getInt("content_version"))
    }

    internal suspend fun install(
        root: JSONObject,
        hash: String,
        expectedVersion: Int = root.getInt("content_version"),
    ) {
        val (version, metadata) = validatePayload(root, expectedVersion)
        database.withTransaction {
            val dao = database.catalogDao()
            val current = dao.catalogMetadata().singleOrNull()
            require(current == null || current.contentVersion <= version) {
                "Catalog downgrade refused"
            }
            if (current?.contentVersion == version) {
                require(dao.installation()?.payloadHash == hash) {
                    "Content changed without version increment"
                }
                return@withTransaction
            }
            val sources = parseSources(root)
            require(sources.all { it.id > 0 && it.sourceType != "personal" })
            val words = parseWords(root)
            require(
                words.all { it.id > 0 && it.writtenForm.isNotBlank() && it.reading.isNotBlank() },
            )
            val kanji = parseKanji(root)
            require(
                kanji.all {
                    it.symbol.codePointCount(0, it.symbol.length) == 1 &&
                        it.symbol.codePointAt(0) == it.id
                },
            )
            validateCompatibility(dao, words, kanji, sources)
            coroutineContext.ensureActive()
            insertCatalogRows(dao, root, sources, words, kanji)
            coroutineContext.ensureActive()
            dao.putInstallation(InstallationRow(1, hash))
            dao.putCatalogMetadata(listOf(metadata))
        }
    }

    private fun validatePayload(
        root: JSONObject,
        expectedVersion: Int,
    ): Pair<Int, CatalogMetadataRow> {
        require(root.getInt("format_version") == 1 && root.getInt("schema_version") == 1)
        val version = root.getInt("content_version")
        require(version > 0 && version == expectedVersion)
        val metadata = parseCatalogMetadata(root).single()
        require(
            metadata.singleton == 1 &&
                metadata.schemaVersion == 1 &&
                metadata.contentVersion == version,
        )
        return version to metadata
    }

    private suspend fun validateCompatibility(
        dao: CatalogDao,
        words: List<WordsRow>,
        kanji: List<KanjiRow>,
        sources: List<SourcesRow>,
    ) {
        // Preserve every published identity, including archived entries; never reassign an ID.
        val storedWords = dao.words()
        require(
            storedWords.none { old ->
                old.isUserCreated == 1 &&
                    words.any {
                        it.id == old.id ||
                            (it.writtenForm == old.writtenForm && it.reading == old.reading)
                    }
            },
        ) {
            "Catalog conflicts with a manually created word; explicit resolution required"
        }
        val oldWords = storedWords.filter { it.id > 0 }.associateBy { it.id }
        val newWords = words.associateBy { it.id }
        require(oldWords.keys.all { it in newWords }) { "Published word IDs removed" }
        oldWords.forEach { (id, old) ->
            require(
                newWords[id]?.let {
                    it.writtenForm == old.writtenForm && it.reading == old.reading
                } == true,
            ) {
                "Identity correction requires an explicit migration"
            }
        }
        require(
            dao.kanji().all { old -> kanji.any { it.id == old.id && it.symbol == old.symbol } },
        )
        val oldSourceIds =
            dao.sources().filter { it.id > 0 && it.sourceType != "personal" }.map { it.id }
        require(sources.map { it.id }.containsAll(oldSourceIds)) {
            "Sources must be archived, not dropped"
        }
    }

    private suspend fun insertCatalogRows(
        dao: CatalogDao,
        root: JSONObject,
        sources: List<SourcesRow>,
        words: List<WordsRow>,
        kanji: List<KanjiRow>,
    ) {
        val oldSourceIds =
            dao.sources().filter { it.id > 0 && it.sourceType != "personal" }.map { it.id }
        dao.clearLinks(oldSourceIds)
        dao.clearPlacements(oldSourceIds)
        dao.putRadicals(parseRadicals(root))
        dao.putRadicalsTranslations(parseRadicalsTranslations(root))
        dao.putKanji(kanji)
        kanji.map { it.id }.chunked(SQL_ID_BATCH_SIZE).forEach { dao.clearReadings(it) }
        parseKanjiTranslations(root)
            .map { it.kanjiId to it.languageTag }
            .distinct()
            .forEach { (id, language) -> dao.clearMeanings(id, language) }
        dao.putKanjiTranslations(parseKanjiTranslations(root))
        dao.putKanjiReadings(parseKanjiReadings(root))
        dao.putWords(words)
        dao.putWordsTranslations(parseWordsTranslations(root))
        words
            .filter { it.isArchived == 0 }
            .map { it.id }
            .chunked(SQL_ID_BATCH_SIZE)
            .forEach { dao.clearParts(it) }
        dao.putWordReadingParts(parseWordReadingParts(root))
        dao.putSources(sources)
        dao.putSourcesTranslations(parseSourcesTranslations(root))
        dao.putSourceKanji(parseSourceKanji(root))
        dao.putSourceWords(parseSourceWords(root))
    }
}

private const val SQL_ID_BATCH_SIZE = 500
