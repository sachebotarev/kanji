package ru.mobui.kanji.feature.dashboard.history

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mobui.kanji.domain.catalog.Recipe
import ru.mobui.kanji.domain.catalog.RecipeName
import ru.mobui.kanji.domain.practice.Training
import ru.mobui.kanji.domain.practice.TrainingGradeLevel
import ru.mobui.kanji.domain.practice.TrainingRecipeName
import ru.mobui.kanji.domain.practice.TrainingStatus
import ru.mobui.kanji.domain.practice.TrainingSymbol
import ru.mobui.kanji.domain.practice.TrainingSymbolRole
import ru.mobui.kanji.domain.practice.TrainingSymbolScript
import ru.mobui.kanji.domain.practice.TrainingSymbolStatus
import ru.mobui.kanji.domain.practice.TrainingTask
import ru.mobui.kanji.domain.practice.TrainingTaskStatus
import ru.mobui.kanji.domain.practice.summarize

class TrainingHistoryModelsTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 8)

    @Test
    fun filtersByPeriodAndRecipeQuery() {
        val trainings =
            listOf(
                training("a", "Умный микс", today),
                training("b", "Tobira", today.minusDays(3)),
                training("c", "N5", today.minusDays(20)),
            )
        val week =
            filterHistory(trainings, TrainingHistoryPeriod.Days7, "", "ru", zone, today)
        assertEquals(listOf("a", "b"), week.map { it.id })
        val todayOnly =
            filterHistory(trainings, TrainingHistoryPeriod.Today, "", "ru", zone, today)
        assertEquals(listOf("a"), todayOnly.map { it.id })
        val search =
            filterHistory(trainings, TrainingHistoryPeriod.All, "tob", "ru", zone, today)
        assertEquals(listOf("b"), search.map { it.id })
    }

    @Test
    fun excellentTrainingEligibleForStamp() {
        val completed =
            training(
                id = "pass",
                name = "Микс",
                day = today,
                status = TrainingStatus.Completed,
                endedAt = today.atTime(12, 0).toInstant(zone).toEpochMilli(),
            )
        val summary = completed.summarize()
        assertEquals(TrainingGradeLevel.Excellent, summary.grade)
        assertTrue(completed.isExcellent())

        val inProgress = completed.copy(status = TrainingStatus.InProgress)
        org.junit.Assert.assertFalse(inProgress.isExcellent())
    }

    @Test
    fun keepsLongUnfinishedOutsidePeriod() {
        val unfinished =
            training(
                "live",
                "Долгая",
                today.minusDays(40),
                status = TrainingStatus.InProgress,
                endedAt = null,
            )
        val todayOnly =
            filterHistory(
                listOf(unfinished, training("old", "N5", today.minusDays(40))),
                TrainingHistoryPeriod.Today,
                "",
                "ru",
                zone,
                today,
            )
        assertEquals(listOf("live"), todayOnly.map { it.id })
    }

    @Test
    fun groupsNewestDaysFirst() {
        val trainings =
            listOf(
                training("old", "A", today.minusDays(2)),
                training("new", "B", today),
                training("mid", "C", today.minusDays(1)),
            )
        val groups = groupHistoryByDay(trainings, zone)
        assertEquals(listOf(today, today.minusDays(1), today.minusDays(2)), groups.map { it.day })
    }

    @Test
    fun taskCountsIgnorePending() {
        val counts =
            training(
                    "x",
                    "Mix",
                    today,
                    listOf(
                        task("1", TrainingTaskStatus.Correct),
                        task("2", TrainingTaskStatus.Assisted),
                        task("3", TrainingTaskStatus.Skipped),
                        task("4", TrainingTaskStatus.TimedOut),
                        task("5", TrainingTaskStatus.Pending),
                    ),
                )
                .taskCounts()
        assertEquals(1, counts.correct)
        assertEquals(1, counts.assisted)
        assertEquals(2, counts.missed)
        assertEquals(4, counts.total)
        assertTrue(dayRelation(today.minusDays(1), today) == 1L)
    }

    @Test
    fun stampResetsWhenRecipeRevisionChanges() {
        val recipeA = Recipe(
            id = "rec-a",
            names = listOf(RecipeName("ru", "Рецепт А")),
            materials = emptyList(),
            revision = 1,
        )
        val excellentTrainingRev1 = training(
            id = "t1",
            name = "Рецепт А",
            day = today,
            recipeId = "rec-a",
            recipeRevision = 1,
        )
        assertEquals(
            setOf("rec-a"),
            findExcellentRecipeIds(listOf(recipeA), listOf(excellentTrainingRev1)),
        )

        // When recipe is edited, its revision increments to 2; stamp resets
        val recipeAEdited = recipeA.copy(revision = 2)
        assertEquals(
            emptySet<String>(),
            findExcellentRecipeIds(listOf(recipeAEdited), listOf(excellentTrainingRev1)),
        )

        // When a new training with revision 2 is completed on excellent, stamp returns
        val excellentTrainingRev2 = training(
            id = "t2",
            name = "Рецепт А",
            day = today,
            recipeId = "rec-a",
            recipeRevision = 2,
        )
        assertEquals(
            setOf("rec-a"),
            findExcellentRecipeIds(listOf(recipeAEdited), listOf(excellentTrainingRev1, excellentTrainingRev2)),
        )
    }

    private fun training(
        id: String,
        name: String,
        day: LocalDate,
        tasks: List<TrainingTask> = listOf(task("t-$id", TrainingTaskStatus.Correct)),
        status: TrainingStatus = TrainingStatus.Completed,
        endedAt: Long? = null,
        recipeId: String? = null,
        recipeRevision: Long? = null,
    ): Training {
        val started = day.atStartOfDay(zone).toInstant().toEpochMilli()
        return Training(
            id = id,
            recipeId = recipeId,
            recipeRevision = recipeRevision,
            recipeNames = listOf(TrainingRecipeName("ru", name)),
            status = status,
            startedAt = started,
            endedAt = endedAt ?: if (status == TrainingStatus.InProgress) null else started + 60_000,
            tasks = tasks,
        )
    }

    private fun task(id: String, status: TrainingTaskStatus): TrainingTask =
        TrainingTask(
            id = id,
            position = 0,
            writtenForm = "山",
            reading = "やま",
            translations = emptyList(),
            status = status,
            startedAt = 1,
            endedAt = 2,
            elapsedMs = 1000,
            symbols =
                listOf(
                    TrainingSymbol(
                        id = "$id-s",
                        position = 0,
                        occurrenceId = "$id-o",
                        role = TrainingSymbolRole.Practice,
                        script = TrainingSymbolScript.Kanji,
                        glyph = "山",
                        status = TrainingSymbolStatus.Correct,
                    ),
                ),
        )
}
