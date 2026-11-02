package ru.mobui.kanji.data.catalog

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Relation

internal data class PersonalWordRow(
    @Embedded val word: WordsRow,
    @Relation(parentColumn = "id", entityColumn = "word_id")
    val translations: List<WordsTranslationsRow>,
    @Relation(parentColumn = "id", entityColumn = "word_id") val parts: List<WordReadingPartsRow>,
)

@Dao
internal interface CatalogDao :
    PersonalWordsDao,
    ReferenceReadDao,
    LearningReadDao,
    ReferenceWriteDao,
    LearningWriteDao,
    CatalogMaintenanceDao
