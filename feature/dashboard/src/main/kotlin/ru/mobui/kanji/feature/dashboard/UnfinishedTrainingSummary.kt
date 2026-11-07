package ru.mobui.kanji.feature.dashboard

/** Snapshot shown on HOME when a training is in_progress. */
data class UnfinishedTrainingSummary(
    val recipeName: String,
    val progressLabel: String,
)
