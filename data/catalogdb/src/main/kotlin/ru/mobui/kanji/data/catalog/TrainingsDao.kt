package ru.mobui.kanji.data.catalog

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

internal data class TrainingTaskAggregate(
    @Embedded val task: TrainingTasksRow,
    @Relation(parentColumn = "id", entityColumn = "task_id")
    val translations: List<TrainingTaskTranslationsRow>,
    @Relation(parentColumn = "id", entityColumn = "task_id")
    val symbols: List<TrainingTaskSymbolsRow>,
)

internal data class TrainingAggregate(
    @Embedded val training: TrainingsRow,
    @Relation(parentColumn = "id", entityColumn = "training_id")
    val recipeNames: List<TrainingsRecipeTranslationsRow>,
    @Relation(entity = TrainingTasksRow::class, parentColumn = "id", entityColumn = "training_id")
    val tasks: List<TrainingTaskAggregate>,
)

@Dao
internal interface TrainingsDao {
    @Transaction
    @Query("SELECT * FROM trainings WHERE status='in_progress' LIMIT 1")
    fun observeInProgress(): Flow<TrainingAggregate?>

    @Transaction
    @Query("SELECT * FROM trainings WHERE status='in_progress' LIMIT 1")
    suspend fun inProgress(): TrainingAggregate?

    @Transaction
    @Query("SELECT * FROM trainings WHERE id=:id")
    suspend fun training(id: String): TrainingAggregate?

    @Query("SELECT id FROM trainings WHERE status='in_progress'")
    suspend fun inProgressIds(): List<String>

    @Query(
        "UPDATE trainings SET status='interrupted', ended_at=:endedAt " +
            "WHERE status='in_progress'",
    )
    suspend fun interruptInProgress(endedAt: Long)

    @Upsert suspend fun putTraining(row: TrainingsRow)

    @Query("DELETE FROM trainings_recipe_translations WHERE training_id=:id")
    suspend fun clearRecipeNames(id: String)

    @Insert suspend fun insertRecipeNames(rows: List<TrainingsRecipeTranslationsRow>)

    @Query("DELETE FROM training_tasks WHERE training_id=:id")
    suspend fun clearTasks(id: String)

    @Insert suspend fun insertTask(row: TrainingTasksRow)

    @Insert suspend fun insertTasks(rows: List<TrainingTasksRow>)

    @Insert suspend fun insertTaskTranslations(rows: List<TrainingTaskTranslationsRow>)

    @Insert suspend fun insertSymbols(rows: List<TrainingTaskSymbolsRow>)

    @Query("DELETE FROM trainings")
    suspend fun deleteAll()
}
