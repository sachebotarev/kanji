package ru.mobui.kanji.data.catalog

import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

internal interface PersonalWordsDao {
    @Transaction
    @Query(
        """SELECT words.* FROM words WHERE is_archived=0 AND id IN
        (SELECT word_id FROM source_words JOIN sources ON sources.id=source_words.source_id
         WHERE sources.source_type='personal' AND sources.is_archived=0)
        ORDER BY (SELECT MIN(source_words.id) FROM source_words JOIN sources ON sources.id=source_words.source_id
                  WHERE source_words.word_id=words.id AND sources.source_type='personal') ASC""",
    )
    fun observePersonalWords(): Flow<List<PersonalWordRow>>

    @Query("SELECT * FROM words WHERE id=:id") suspend fun word(id: Int): WordsRow?

    @Query(
        "SELECT * FROM words WHERE written_form=:written AND reading=:reading AND id != :excluded",
    )
    suspend fun duplicate(written: String, reading: String, excluded: Int): WordsRow?

    @Query("SELECT MIN(id) FROM words") suspend fun minimumWordId(): Int?

    @Query("SELECT MIN(id) FROM sources") suspend fun minimumSourceId(): Int?

    @Query("SELECT MIN(id) FROM source_words") suspend fun minimumLinkId(): Int?

    @Query("DELETE FROM words_translations WHERE word_id=:id")
    suspend fun clearWordTranslations(id: Int)

    @Query("UPDATE words SET is_archived=1 WHERE id=:id AND is_user_created=1")
    suspend fun archiveManual(id: Int)

    @Query(
        "DELETE FROM source_words WHERE word_id=:id " +
            "AND source_id IN (SELECT id FROM sources WHERE source_type='personal')",
    )
    suspend fun clearPersonalLinks(id: Int)
}
