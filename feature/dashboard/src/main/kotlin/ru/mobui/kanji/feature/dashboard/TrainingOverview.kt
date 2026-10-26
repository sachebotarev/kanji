package ru.mobui.kanji.feature.dashboard

/** Presentation snapshot of a reusable training configuration, not a running session. */
data class TrainingOverview(
    val id: String,
    val name: String,
    val wordsPerSession: Int = 10,
    val secondsPerWord: Int = 60,
    val completedSessionCount: Int = 0,
)
