package ru.mobui.kanji.data.catalog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trainings",
    indices = [Index(value = ["started_at", "id"]), Index("recipe_id")],
    foreignKeys =
        [
            ForeignKey(
                entity = RecipesRow::class,
                parentColumns = ["id"],
                childColumns = ["recipe_id"],
                onDelete = ForeignKey.SET_NULL,
            ),
        ],
)
internal data class TrainingsRow(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "recipe_id") val recipeId: String?,
    @ColumnInfo(name = "recipe_revision") val recipeRevision: Long?,
    @ColumnInfo(name = "task_count") val taskCount: Int?,
    @ColumnInfo(name = "seconds_per_word") val secondsPerWord: Int?,
    val status: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
)

@Entity(
    tableName = "trainings_recipe_translations",
    primaryKeys = ["training_id", "language_tag"],
    foreignKeys =
        [
            ForeignKey(
                entity = TrainingsRow::class,
                parentColumns = ["id"],
                childColumns = ["training_id"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
)
internal data class TrainingsRecipeTranslationsRow(
    @ColumnInfo(name = "training_id") val trainingId: String,
    @ColumnInfo(name = "language_tag", collate = ColumnInfo.NOCASE) val languageTag: String,
    val name: String,
)

@Entity(
    tableName = "training_tasks",
    indices =
        [
            Index(value = ["training_id", "position"], unique = true),
            Index("word_id"),
            Index(value = ["training_id", "status"]),
        ],
    foreignKeys =
        [
            ForeignKey(
                entity = TrainingsRow::class,
                parentColumns = ["id"],
                childColumns = ["training_id"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = WordsRow::class,
                parentColumns = ["id"],
                childColumns = ["word_id"],
                onDelete = ForeignKey.SET_NULL,
            ),
        ],
)
internal data class TrainingTasksRow(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "training_id") val trainingId: String,
    val position: Int,
    @ColumnInfo(name = "word_id") val wordId: Int?,
    @ColumnInfo(name = "written_form") val writtenForm: String,
    val reading: String,
    val status: String,
    @ColumnInfo(name = "started_at") val startedAt: Long?,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    @ColumnInfo(name = "elapsed_ms") val elapsedMs: Long,
)

@Entity(
    tableName = "training_task_translations",
    primaryKeys = ["task_id", "language_tag"],
    foreignKeys =
        [
            ForeignKey(
                entity = TrainingTasksRow::class,
                parentColumns = ["id"],
                childColumns = ["task_id"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
)
internal data class TrainingTaskTranslationsRow(
    @ColumnInfo(name = "task_id") val taskId: String,
    @ColumnInfo(name = "language_tag", collate = ColumnInfo.NOCASE) val languageTag: String,
    val translation: String,
)

@Entity(
    tableName = "training_task_symbols",
    indices =
        [
            Index(value = ["task_id", "position"], unique = true),
            Index(value = ["task_id", "occurrence_id"], unique = true),
            Index("kanji_id"),
            Index("kanji_key"),
        ],
    foreignKeys =
        [
            ForeignKey(
                entity = TrainingTasksRow::class,
                parentColumns = ["id"],
                childColumns = ["task_id"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = KanjiRow::class,
                parentColumns = ["id"],
                childColumns = ["kanji_id"],
                onDelete = ForeignKey.SET_NULL,
            ),
        ],
)
internal data class TrainingTaskSymbolsRow(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "task_id") val taskId: String,
    val position: Int,
    @ColumnInfo(name = "occurrence_id") val occurrenceId: String,
    val role: String,
    val script: String,
    val glyph: String,
    @ColumnInfo(name = "kanji_id") val kanjiId: Int?,
    @ColumnInfo(name = "kanji_key") val kanjiKey: String?,
    val status: String,
    @ColumnInfo(name = "hint_request_count") val hintRequestCount: Int,
    @ColumnInfo(name = "elapsed_ms") val elapsedMs: Long,
    @ColumnInfo(name = "readability_verdict") val readabilityVerdict: String?,
    @ColumnInfo(name = "readability_glyph") val readabilityGlyph: String?,
    @ColumnInfo(name = "shape_verdict") val shapeVerdict: String?,
    @ColumnInfo(name = "shape_similarity_percent") val shapeSimilarityPercent: Float?,
    @ColumnInfo(name = "shape_budget_percent") val shapeBudgetPercent: Float?,
    @ColumnInfo(name = "stroke_writing_verdict") val strokeWritingVerdict: String?,
    @ColumnInfo(name = "stroke_expected") val strokeExpected: Int?,
    @ColumnInfo(name = "stroke_actual") val strokeActual: Int?,
    @ColumnInfo(name = "writing_sample_json") val writingSampleJson: String?,
    @ColumnInfo(name = "check_report_json") val checkReportJson: String?,
)
