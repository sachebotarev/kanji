package ru.mobui.kanji.feature.dashboard

/** Presentation snapshot of a reusable recipe configuration, not a running training. */
data class RecipeOverview(
    val id: String,
    val name: String,
    val wordsPerTraining: Int = 10,
    val secondsPerWord: Int = 60,
    val completedTrainingCount: Int = 0,
)
