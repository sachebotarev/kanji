package ru.mobui.kanji.feature.dashboard.widgets

import ru.mobui.kanji.feature.dashboard.R

internal enum class DashboardIcon(
    val resource: Int,
    val opticalScale: Float = 1f,
    val stationery: Boolean = false,
) {
    Settings(R.drawable.art_settings, stationery = true),
    Textbooks(R.drawable.art_home_textbook, stationery = true),
    References(R.drawable.art_home_references, stationery = true),
    Personal(R.drawable.art_home_dictionary, stationery = true),
    RecipeBook(R.drawable.art_home_training_recipes, stationery = true),
    WordSet(R.drawable.art_personal, stationery = true),
    History(R.drawable.art_history_v4, STANDARD_ILLUSTRATION_SCALE, stationery = true),
    Builder(R.drawable.art_builder, BUILDER_ILLUSTRATION_SCALE, stationery = true),
    Support(R.drawable.art_support, SUPPORT_ILLUSTRATION_SCALE, stationery = true),
    Start(R.drawable.art_start_v4),
    Expand(R.drawable.art_paw_v4),
}

private const val STANDARD_ILLUSTRATION_SCALE = 1.1f

private const val BUILDER_ILLUSTRATION_SCALE = 1.15f

private const val SUPPORT_ILLUSTRATION_SCALE = 1.3f
