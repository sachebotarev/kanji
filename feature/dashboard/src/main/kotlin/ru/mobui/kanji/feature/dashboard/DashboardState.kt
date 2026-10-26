package ru.mobui.kanji.feature.dashboard

/** Presentation snapshots only. Real data sources will replace the demo fixture later. */
data class WordStatistics(val learned: Int, val assisted: Int, val errors: Int) {
    val total: Int get() = learned + assisted + errors
}
data class ActivityOverview(val dailyCounts: List<Int>, val streakDays: Int)
data class VocabularyOverview(
    val textbookCount: Int, val activeTextbooks: Int,
    val personalTotal: Int?, val personalLearned: Int, val personalErrors: Int, val personalAssisted: Int,
)
data class DashboardState(
    val statistics: WordStatistics, val activity: ActivityOverview, val vocabulary: VocabularyOverview,
)

object DashboardDemo {
    val state = DashboardState(
        WordStatistics(874, 249, 125),
        ActivityOverview(listOf(0, 2, 5, 1, 8, 3, 0, 4, 1, 6, 3, 0, 9, 2, 1, 3, 8, 2, 4, 7, 0,
            5, 3, 9, 1, 6, 2, 4, 0, 7, 3, 5, 1, 8, 2, 4, 2, 6, 9, 4, 7, 3), 7),
        VocabularyOverview(12, 4, 156, 92, 18, 12),
    )
}
