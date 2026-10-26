package ru.mobui.kanji.data.catalog

import android.content.Context
import android.content.res.AssetManager
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.mobui.kanji.domain.catalog.*

class RoomCatalogRepository internal constructor(private val database: CatalogDatabase,
    private val assets: AssetManager, private val io: CoroutineDispatcher) : CatalogRepository, PersonalDictionaryRepository {
    constructor(context: Context, io: CoroutineDispatcher = Dispatchers.IO) :
        this(CatalogDatabase.open(context), context.applicationContext.assets, io)
    private val mutex = Mutex()
    private var snapshot: CatalogSnapshot? = null

    override fun observeWords(): Flow<List<CatalogWord>> = flow {
        load() // Installation finishes before Room starts emitting an initially empty database.
        emitAll(database.catalogDao().observePersonalWords().map { rows -> rows.map { row ->
            CatalogWord(row.word.id, row.word.written_form, row.word.reading,
                row.translations.associate { it.language_tag to it.translation },
                row.parts.sortedBy { it.position }.map { ReadingPart(it.written_part, it.reading_part) },
                row.word.is_user_created == 1)
        } })
    }.flowOn(io)

    override suspend fun saveWord(id: Int?, draft: WordDraft): Int = withContext(io) {
        val validated = draft.normalized()
        load()
        mutex.withLock { database.withTransaction {
            val dao = database.catalogDao()
            val old = id?.let { dao.word(it) ?: throw DictionaryException(DictionaryFailure.Missing) }
            if (old != null && old.is_user_created != 1) throw DictionaryException(DictionaryFailure.ReadOnly)
            if (old?.is_archived == 1) throw DictionaryException(DictionaryFailure.Missing)
            val duplicate = dao.duplicate(validated.written, validated.reading, id ?: 0)
            val restoring = id == null && duplicate?.is_archived == 1 && duplicate.is_user_created == 1
            if (duplicate != null && !restoring) throw DictionaryException(DictionaryFailure.Duplicate)
            val wordId = id ?: duplicate?.id ?: (minOf(dao.minimumWordId() ?: 0, 0) - 1)
            dao.put_words(listOf(WordsRow(wordId, validated.written, validated.reading, 0, 1)))
            dao.clearWordTranslations(wordId)
            dao.put_words_translations(validated.translations.map { WordsTranslationsRow(wordId, it.languageTag, it.text) })
            if (old != null && (old.written_form != validated.written || old.reading != validated.reading))
                dao.clearParts(listOf(wordId))
            if (id == null) {
                val source = dao.sources().firstOrNull { it.source_type == "personal" && it.is_archived == 0 }
                    ?: SourcesRow(minOf(dao.minimumSourceId() ?: 0, 0) - 1, "personal", "personal", 0).also {
                        dao.put_sources(listOf(it))
                    }
                dao.put_source_words(listOf(SourceWordsRow(minOf(dao.minimumLinkId() ?: 0, 0) - 1, source.id, wordId, null)))
            }
            // Catalog details may have been opened via an unscoped kanji; avoid returning a stale snapshot.
            wordId
        }.also { snapshot = null } }
    }

    override suspend fun deleteWord(id: Int) = withContext(io) {
        load()
        mutex.withLock { database.withTransaction {
            val dao = database.catalogDao()
            val old = dao.word(id) ?: throw DictionaryException(DictionaryFailure.Missing)
            if (old.is_user_created != 1) throw DictionaryException(DictionaryFailure.ReadOnly)
            if (old.is_archived == 1) throw DictionaryException(DictionaryFailure.Missing)
            dao.clearPersonalLinks(id)
            // Archive instead of breaking references held by other sources or future statistics.
            dao.archiveManual(id)
        }.also { snapshot = null } }
    }

    override suspend fun load(): CatalogSnapshot = withContext(io) {
        mutex.withLock {
            snapshot ?: run {
                CatalogInstaller(database, assets).install()
                readSnapshot(database.catalogDao()).also { snapshot = it }
            }
        }
    }
}

internal suspend fun readSnapshot(dao: CatalogDao): CatalogSnapshot {
    val wordTranslations = dao.words_translations().groupBy { it.word_id }
    val sourceTranslations = dao.sources_translations().groupBy { it.source_id }
    val meanings = dao.kanji_translations().sortedBy { it.position }.groupBy { it.kanji_id }
    val readings = dao.kanji_readings().sortedBy { it.position }.groupBy { it.kanji_id }
    val names = dao.radicals_translations().groupBy { it.radical_id }
    val parts = dao.word_reading_parts().sortedBy { it.position }.groupBy { it.word_id }
    val sources = dao.sources().filter { it.source_type != "personal" && it.is_archived == 0 }.map {
        CatalogSource(it.id, it.original_title, sourceTranslations[it.id].orEmpty().associate { r -> r.language_tag to r.title })
    }
    val sourceIds = sources.map { it.id }.toSet()
    return CatalogSnapshot(dao.catalog_metadata().single().content_version, sources,
        dao.words().filter { it.is_archived == 0 }.map {
            CatalogWord(it.id, it.written_form, it.reading,
                wordTranslations[it.id].orEmpty().associate { r -> r.language_tag to r.translation },
                parts[it.id].orEmpty().map { r -> ReadingPart(r.written_part, r.reading_part) }, it.is_user_created == 1)
        },
        dao.kanji().map {
            CatalogKanji(it.id, it.symbol, it.stroke_count, it.radical_id, it.is_kyoiku == 1, it.is_joyo == 1,
                it.is_jinmeiyo == 1, meanings[it.id].orEmpty().groupBy { r -> r.language_tag }
                    .mapValues { (_, rows) -> rows.map { r -> r.meaning } },
                readings[it.id].orEmpty().filter { r -> r.reading_type == "on" }.map { r -> r.reading },
                readings[it.id].orEmpty().filter { r -> r.reading_type == "kun" }.map { r -> r.reading })
        },
        dao.radicals().map { CatalogRadical(it.id, it.symbol, it.stroke_count,
            names[it.id].orEmpty().associate { r -> r.language_tag to r.name }) },
        dao.source_kanji().filter { it.source_id in sourceIds }.map { CatalogPlacement(it.source_id, it.kanji_id, it.kanji_number, it.chapter_number) },
        dao.source_words().filter { it.source_id in sourceIds }.map { CatalogLink(it.source_id, it.word_id, it.kanji_id) })
}
