package ru.mobui.kanji.data.catalog

import android.content.Context
import android.content.res.AssetManager
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.mobui.kanji.domain.catalog.CatalogContent
import ru.mobui.kanji.domain.catalog.CatalogLink
import ru.mobui.kanji.domain.catalog.CatalogPlacement
import ru.mobui.kanji.domain.catalog.CatalogRadical
import ru.mobui.kanji.domain.catalog.CatalogRepository
import ru.mobui.kanji.domain.catalog.CatalogSnapshot
import ru.mobui.kanji.domain.catalog.CatalogWord
import ru.mobui.kanji.domain.catalog.DictionaryException
import ru.mobui.kanji.domain.catalog.DictionaryFailure
import ru.mobui.kanji.domain.catalog.PersonalDictionaryRepository
import ru.mobui.kanji.domain.catalog.ReadingPart
import ru.mobui.kanji.domain.catalog.WordDraft
import ru.mobui.kanji.domain.catalog.normalized

class RoomCatalogRepository
internal constructor(
    private val database: CatalogDatabase,
    private val assets: AssetManager,
    private val io: CoroutineDispatcher,
) : CatalogRepository, PersonalDictionaryRepository {
    constructor(
        context: Context,
        io: CoroutineDispatcher = Dispatchers.IO,
    ) : this(CatalogDatabase.open(context), context.applicationContext.assets, io)

    val recipesRepository: ru.mobui.kanji.domain.catalog.RecipesRepository =
        RoomRecipesRepository(database, { load() }, io)

    val trainingsRepository: ru.mobui.kanji.domain.practice.TrainingsRepository =
        RoomTrainingsRepository(database, { load() }, io)

    private val mutex = Mutex()
    private var snapshot: CatalogSnapshot? = null
    private val mutableRevision = MutableStateFlow(0L)
    val revision = mutableRevision.asStateFlow()

    private fun invalidateSnapshot() {
        snapshot = null
        mutableRevision.value += 1
    }

    override fun observeWords(): Flow<List<CatalogWord>> = flow {
        load() // Installation finishes before Room starts emitting an initially empty database.
        emitAll(
            database.catalogDao().observePersonalWords().map { rows ->
                rows.map { row ->
                    CatalogWord(
                        row.word.id,
                        row.word.writtenForm,
                        row.word.reading,
                        row.translations.associate { it.languageTag to it.translation },
                        row.parts
                            .sortedBy { it.position }
                            .map { ReadingPart(it.writtenPart, it.readingPart) },
                        row.word.isUserCreated == 1,
                    )
                }
            },
        )
    }
        .flowOn(io)

    override suspend fun saveWord(id: Int?, draft: WordDraft): Int =
        withContext(io) {
            val validated = draft.normalized()
            load()
            mutex.withLock {
                database
                    .withTransaction {
                        val dao = database.catalogDao()
                        val target = dao.wordWriteTarget(id, validated)
                        val old = target.previous
                        val wordId = target.id
                        dao.putWords(
                            listOf(WordsRow(wordId, validated.written, validated.reading, 0, 1)),
                        )
                        dao.clearWordTranslations(wordId)
                        dao.putWordsTranslations(
                            validated.translations.map {
                                WordsTranslationsRow(wordId, it.languageTag, it.text)
                            },
                        )
                        if (
                            old != null &&
                                (old.writtenForm != validated.written ||
                                    old.reading != validated.reading)
                        )
                            dao.clearParts(listOf(wordId))
                        if (id == null) dao.addPersonalLink(wordId)
                        if (validated.tagIds != null || validated.pendingTags.isNotEmpty()) {
                            val existing = validated.tagIds ?: database.recipesDao().allWordTags()
                                .filter { it.wordId == wordId }.map { it.tagId }.toSet()
                            val created = validated.pendingTags.map { saveTagInTransaction(database, null, it) }
                            assignTagsInTransaction(database, wordId, existing + created)
                        }
                        // Catalog details may have been opened via an unscoped kanji; avoid
                        // returning a stale snapshot.
                        wordId
                    }
                    .also { invalidateSnapshot() }
            }
        }

    override suspend fun deleteWord(id: Int) =
        withContext(io) {
            load()
            mutex.withLock {
                database
                    .withTransaction {
                        val dao = database.catalogDao()
                        val old =
                            dao.word(id) ?: throw DictionaryException(DictionaryFailure.Missing)
                        if (old.isUserCreated != 1)
                            throw DictionaryException(DictionaryFailure.ReadOnly)
                        if (old.isArchived == 1)
                            throw DictionaryException(DictionaryFailure.Missing)
                        dao.clearPersonalLinks(id)
                        // Archive instead of breaking references held by other sources or future
                        // statistics.
                        dao.archiveManual(id)
                    }
                    .also { invalidateSnapshot() }
            }
        }

    override suspend fun load(): CatalogSnapshot =
        withContext(io) {
            mutex.withLock {
                snapshot
                    ?: run {
                        CatalogInstaller(database, assets).install()
                        readSnapshot(database.catalogDao()).also { snapshot = it }
                    }
            }
        }
}

internal suspend fun readSnapshot(dao: CatalogDao): CatalogSnapshot {
    val wordTranslations = dao.wordsTranslations().groupBy { it.wordId }
    val sourceTranslations = dao.sourcesTranslations().groupBy { it.sourceId }
    val meanings = dao.kanjiTranslations().sortedBy { it.position }.groupBy { it.kanjiId }
    val readings = dao.kanjiReadings().sortedBy { it.position }.groupBy { it.kanjiId }
    val names = dao.radicalsTranslations().groupBy { it.radicalId }
    val parts = dao.wordReadingParts().sortedBy { it.position }.groupBy { it.wordId }
    // Keep the personal dictionary source and its links: recipe materials and recipe
    // previews resolve Dictionary pools from this snapshot, not from observeWords alone.
    val sources =
        dao.sources()
            .filter { it.isArchived == 0 }
            .map {
                it.toCatalogSource(sourceTranslations[it.id].orEmpty())
            }
    val sourceIds = sources.map { it.id }.toSet()
    return CatalogSnapshot(
        dao.catalogMetadata().single().contentVersion,
        content =
            CatalogContent(
                sources,
                dao.words()
                    .filter { it.isArchived == 0 }
                    .map {
                        it.toCatalogWord(wordTranslations[it.id].orEmpty(), parts[it.id].orEmpty())
                    },
                dao.kanji().map {
                    it.toCatalogKanji(meanings[it.id].orEmpty(), readings[it.id].orEmpty())
                },
                dao.radicals().map {
                    CatalogRadical(
                        it.id,
                        it.symbol,
                        it.strokeCount,
                        names[it.id].orEmpty().associate { r -> r.languageTag to r.name },
                    )
                },
                dao.sourceKanji()
                    .filter { it.sourceId in sourceIds }
                    .map {
                        CatalogPlacement(it.sourceId, it.kanjiId, it.kanjiNumber, it.chapterNumber)
                    },
                dao.sourceWords()
                    .filter { it.sourceId in sourceIds }
                    .map { CatalogLink(it.sourceId, it.wordId, it.kanjiId) },
            ),
    )
}

private data class WordWriteTarget(val previous: WordsRow?, val id: Int)

private suspend fun CatalogDao.checkWordTarget(id: Int?, validated: WordDraft): DictionaryFailure? {
    val old = id?.let { word(it) }
    val duplicate = duplicate(validated.written, validated.reading, id ?: 0)
    val restoring = id == null && duplicate?.isArchived == 1 && duplicate.isUserCreated == 1
    return when {
        id != null && (old == null || old.isArchived == 1) -> DictionaryFailure.Missing
        id != null && old != null && old.isUserCreated != 1 -> DictionaryFailure.ReadOnly
        duplicate != null && !restoring -> DictionaryFailure.Duplicate
        else -> null
    }
}

private suspend fun CatalogDao.wordWriteTarget(id: Int?, validated: WordDraft): WordWriteTarget {
    checkWordTarget(id, validated)?.let { throw DictionaryException(it) }
    val old = id?.let { word(it) }
    val duplicate = duplicate(validated.written, validated.reading, id ?: 0)
    return WordWriteTarget(old, id ?: duplicate?.id ?: (minOf(minimumWordId() ?: 0, 0) - 1))
}

private suspend fun CatalogDao.addPersonalLink(wordId: Int) {
    val source =
        sources().firstOrNull {
            it.sourceType == "personal" && it.isArchived == 0
        }
            ?: SourcesRow(
                    minOf(minimumSourceId() ?: 0, 0) - 1,
                    "personal",
                    "personal",
                    0,
                )
                .also {
                    putSources(listOf(it))
                }
    putSourceWords(
        listOf(
            SourceWordsRow(
                minOf(minimumLinkId() ?: 0, 0) - 1,
                source.id,
                wordId,
                null,
            ),
        ),
    )
}
