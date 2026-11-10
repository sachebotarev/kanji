package ru.mobui.kanji.feature.dashboard

data class VocabularyOverview(
    val textbookCount: Int,
    val activeTextbooks: Int,
    val personalTotal: Int?,
    val personalLearned: Int,
    val personalErrors: Int,
    val personalAssisted: Int,
)

data class DashboardState(
    val statistics: WordStatistics,
    val activity: ActivityOverview,
    val vocabulary: VocabularyOverview,
)

object DashboardDemo {
    val state =
        DashboardState(
            WordStatistics(8, 2, 1),
            ActivityOverview(
                listOf(
                    0,
                    2,
                    5,
                    1,
                    8,
                    3,
                    0,
                    4,
                    1,
                    6,
                    3,
                    0,
                    9,
                    2,
                    1,
                    3,
                    8,
                    2,
                    4,
                    7,
                    0,
                    5,
                    3,
                    9,
                    1,
                    6,
                    2,
                    4,
                    0,
                    7,
                    3,
                    5,
                    1,
                    8,
                    2,
                    4,
                    2,
                    6,
                    9,
                    4,
                    7,
                    3,
                ),
                7,
                ActivityTone.KeepItUp,
            ),
            VocabularyOverview(12, 4, 156, 92, 18, 12),
        )
}
